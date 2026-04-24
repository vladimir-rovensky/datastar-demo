const originalFetch = window.fetch;

class RequestEntry {
    state = 'queued';
    promise;
    #resolve;
    #reject;

    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.#resolve = resolve;
            this.#reject = reject;
        });
    }

    resolve(value) {
        this.#resolve(value);
    }

    reject(error) {
        this.#reject(error);
    }

    supersede() {
        this.resolve(new Response(null, { status: 204, statusText: 'Superseded' }));
    }

    getBlocker() {
        return this.promise.catch(error => {
            if (error?.name === 'AbortError') return;
            throw error;
        });
    }
}

class RequestQueue {
    #map = new Map();

    enqueue(path, entry) {
        let list = this.#map.get(path);
        if (!list) this.#map.set(path, list = []);

        list.push(entry);
    }

    dequeue(path, entry) {
        const list = this.#map.get(path);
        if (!list) return;

        const index = list.indexOf(entry);
        if (index === -1) return;

        list.splice(index, 1);
        if (list.length === 0) this.#map.delete(path);
    }

    supersedeLastIfQueued(path) {
        const list = this.#map.get(path);
        if (!list?.length) return;

        const last = list[list.length - 1];
        if (last.state !== 'queued') return;

        last.supersede();
        this.dequeue(path, last);
    }

    collectBlockers(path, entry) {
        return [...this.#map]
            .filter(([otherPath]) => blocks(otherPath, path))
            .flatMap(([, entries]) => entries)
            .filter(other => other !== entry)
            .map(other => other.getBlocker());
    }

    includes(path, entry) {
        return !!this.#map.get(path)?.includes(entry);
    }

    log(prefix) {
        if(!!window.__logRequestOrdering) {
            let message = this.toString().trim();
            console.log(prefix, message || 'No in-flight requests');
        }
    }

    toString() {
        return [...this.#map.entries()]
            .map(([path, list]) => ({
                path,
                running: list.filter(e => e.state === 'running').length,
                queued: list.filter(e => e.state === 'queued').length,
            }))
            .sort((a, b) => b.running - a.running)
            .map(({ path, running, queued }) => `${path} - ${running} running, ${queued} queued`)
            .join('\n');
    }
}

function getHeader(headers, name) {
    if (!headers) return null;
    if (headers instanceof Headers) return headers.get(name);
    return headers[name] ?? null;
}

function setHeader(init, name, value) {
    if (init.headers instanceof Headers) {
        init.headers.set(name, value);
    } else {
        init.headers = { ...init.headers, [name]: value };
    }
}

function isIdempotent(init) {
    const override = getHeader(init.headers, 'X-Idempotent');
    if (override === 'true') return true;
    if (override === 'false') return false;

    return (init.method ?? 'GET').toUpperCase() !== 'POST';
}

function pathOf(input) {
    const url = typeof input === 'string' ? input
              : input instanceof URL ? input.href
              : input.url;

    return new URL(url, location.href).pathname;
}

function blocks(pathA, pathB) {
    const a = pathA.split('/').filter(Boolean);
    const b = pathB.split('/').filter(Boolean);
    const length = Math.min(a.length, b.length);

    for (let i = 0; i < length; i++) {
        if (a[i] !== b[i]) return false;
    }

    return true;
}

const queue = new RequestQueue();

window.fetch = function(input, init = {}) {
    if (!getHeader(init.headers, 'Datastar-Request'))
        return originalFetch(input, init);

    setHeader(init, 'X-tabID', window.__tabID);

    if (window.__orderRequests !== true)
        return originalFetch(input, init);

    const path = pathOf(input);

    if (isIdempotent(init))
        queue.supersedeLastIfQueued(path);

    const entry = new RequestEntry();

    queue.enqueue(path, entry);

    (async () => {
        try {
            await Promise.all(queue.collectBlockers(path, entry));

            if (!queue.includes(path, entry)) return;

            if (init.signal?.aborted) {
                throw new DOMException('Aborted', 'AbortError');
            }

            entry.state = 'running';
            queue.log('started request');
            entry.resolve(await originalFetch(input, init));
        } catch (error) {
            entry.reject(error);
        } finally {
            queue.dequeue(path, entry);
            queue.log('finished request');
        }
    })();

    return entry.promise;
};
