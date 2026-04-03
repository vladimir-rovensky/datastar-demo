---
name: datastar
description: This skill should be used whenever the user asks about DataStar, is writing DataStar attributes, needs to send SSE events to a DataStar frontend, or is building any UI feature in this project (since all frontend interactivity uses DataStar).
version: 1.0.0
---

# DataStar Reference

Documentation: https://data-star.dev/reference/attributes | https://data-star.dev/reference/action_plugins | https://data-star.dev/reference/sse_events | https://data-star.dev/guide/getting_started

DataStar is a lightweight hypermedia framework. The backend drives all UI updates by streaming SSE events. The frontend declares reactivity via `data-*` HTML attributes. No npm, no build step — just a single script tag.

## Installation

```html
<script type="module" src="https://cdn.jsdelivr.net/npm/@sudodevnull/datastar"></script>
```

## Mental Model

- **Signals** — reactive variables, declared with `data-signals` or bound with `data-bind`
- **Expressions** — standard JS snippets used as attribute values; signals are accessed with a `$` prefix (e.g. `$count`)
- **Backend actions** (`@get()`, `@post()`, etc.) — send HTTP requests and process the response as SSE
- **SSE events** — the server pushes DOM patches or signal updates; the browser applies them reactively

## Key HTML Attributes

### State
| Attribute | Purpose |
|---|---|
| `data-signals="{key: value}"` | Declare/patch signals into state. Dot-notation for nesting. Leading `_` excludes from backend requests. |
| `data-bind="$signalName"` | Two-way binding on `<input>`, `<select>`, `<textarea>` |
| `data-computed="$x + $y"` | Read-only derived signal, auto-updates |
| `data-ref="myEl"` | Store a DOM element reference as a signal |

### Rendering
| Attribute | Purpose |
|---|---|
| `data-text="$expr"` | Set element text content |
| `data-show="$expr"` | Show/hide via CSS display |
| `data-class="{active: $isActive}"` | Conditionally toggle CSS classes |
| `data-attr-href="$url"` | Sync any HTML attribute to an expression |
| `data-style-color="$color"` | Bind inline CSS property |

### Events & Effects
| Attribute                             | Purpose |
|---------------------------------------|---|
| `data-on:click="@post('/endpoint')"`  | Attach event listener (any event name after `data-on-`) |
| `data-on:submit="@post('/endpoint')"` | Common: form submit |
| `data-effect="@get('/endpoint')"`     | Run expression on load and on signal change |
| `data-on:interval__1000="..."`        | Run every 1000ms |
| `data-on:intersect="..."`             | Run when element enters viewport |
| `data-init="..."`                     | Run once when element is patched into DOM |

### Utility
| Attribute | Purpose |
|---|---|
| `data-indicator="$loading"` | Signal set to `true` while a fetch is in flight |
| `data-ignore` | Prevent DataStar from processing this subtree |
| `data-ignore-morph` | Skip this element during DOM morphing |

## Backend Actions

All send the current signals automatically (as query params for GET, JSON body for others).

```html
data-on-click="@get('/trades')"
data-on-click="@post('/trades')"
data-on-click="@put('/trades/1')"
data-on-click="@patch('/trades/1')"
data-on-click="@delete('/trades/1')"
```

Response is processed based on `Content-Type`:
- `text/event-stream` → SSE stream (reactive updates)
- `text/html` → DOM patch
- `application/json` → signal update

## SSE Events (Server → Browser)

The server responds with `Content-Type: text/event-stream`. Each event ends with a blank line.

### Patch DOM elements

```
event: datastar-patch-elements
data: selector #my-container
data: mode inner
data: elements <div>new content</div>

```

`mode` options: `outer` (default), `inner`, `replace`, `prepend`, `append`, `before`, `after`, `remove`

### Patch signals

```
event: datastar-patch-signals
data: signals {count: 42, user: "Alice"}

```

Set a signal to `null` to delete it. Use `onlyIfMissing true` to only set if not already present.

## Java SSE Format (Spring)

Each `data:` line in the SSE event maps to one `.data(...)` call on `SseEmitter.SseEventBuilder`.

```java
SseEmitter.event()
    .name("datastar-patch-elements")
    .data("selector #trades-table")
    .data("mode inner")
    .data("elements " + firstLineOfHtml)
    // additional lines of HTML each get their own .data() call
    .build()
```

Multi-line HTML: split on `\n` and emit one `.data("elements " + line)` per line — except only the first line uses the `elements ` prefix; subsequent lines are continuation data lines (no prefix needed, as SSE treats them as separate `data:` fields on the same event).

> **Note:** The existing `ClientChannel.updateFragment()` uses `"fragments "` as the data prefix — this should be `"elements "` to match the `datastar-patch-elements` event spec.

## Modifiers

Append modifiers with `__` to attributes:

```html
data-on-click__debounce_300="..."   <!-- debounce 300ms -->
data-on-click__throttle_500="..."   <!-- throttle 500ms -->
data-on-click__prevent="..."        <!-- preventDefault -->
data-on-click__stop="..."           <!-- stopPropagation -->
data-on-interval__1000="..."        <!-- every 1s -->
```

## Signal Naming Conventions

- camelCase in JS expressions: `$mySignal`
- `data-signals` keys with hyphens convert to camelCase automatically
- Leading `_` (e.g. `_localOnly`) excludes the signal from being sent to the backend

## Evaluation Order

Attributes process depth-first, left-to-right. Declare `data-indicator` before `data-init`/`data-effect` that use it.