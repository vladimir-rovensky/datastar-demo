const sharedStylesheet = new CSSStyleSheet();
//language=css
sharedStylesheet.replaceSync(`
    :host { display: contents; }
    :host(.loading) input { opacity: 0; }
    input {
        background-color: var(--clr-surface);
        color: var(--clr-text);
        border: 1px solid var(--input-border-color, var(--clr-border));
        padding: var(--sp-xs) var(--sp-sm);
        font-size: var(--fs-sm);
        outline: none;
        width: 100%;
        height: 100%;
        box-sizing: border-box;
    }
    input:focus    { border-color: var(--clr-accent); }
    input:disabled { background-color: var(--clr-bg); color: var(--clr-text-faint); border-color: var(--clr-border-dim); }
`);

class NumberInputElement extends HTMLElement {
    static observedAttributes = ['name', 'disabled', 'format', 'decimals', 'value'];

    _numericValue = null;
    _previousValue = null;
    _input = null;

    connectedCallback() {
        if (!this._input) {
            this.attachShadow({ mode: 'open' });
            this.shadowRoot.adoptedStyleSheets = [sharedStylesheet];

            this._input = document.createElement('input');
            this._input.type = 'text';
            this._input.name = this.getAttribute('name') ?? '';
            this._input.disabled = this.hasAttribute('disabled');
            this.shadowRoot.appendChild(this._input);

            if (this._numericValue === null) {
                const attrValue = this.getAttribute('value');
                if (attrValue !== null && attrValue !== '') {
                    this._numericValue = Number(attrValue);
                    this._previousValue = this._numericValue;
                }
            }

            this._input.addEventListener('focus', () => {
                if (this._numericValue != null) {
                    this._input.value = this._numericValue.toFixed(this._getDecimals());
                }
            });

            this._input.addEventListener('blur', () => {
                this._numericValue = this._parse(this._input.value);
                this._applyFormatted();
                if (this._numericValue !== this._previousValue) {
                    this.dispatchEvent(new Event('change', { bubbles: true }));
                    this._previousValue = this._numericValue;
                }
            });

            this._input.addEventListener('input', () => {
                this._numericValue = this._parse(this._input.value);
                this.dispatchEvent(new Event('input', { bubbles: true }));
            });

            this._input.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    this._numericValue = this._parse(this._input.value);
                    this._applyFormatted();
                    if (this._numericValue !== this._previousValue) {
                        this.dispatchEvent(new Event('change', { bubbles: true }));
                        this._previousValue = this._numericValue;
                    }
                }
            });
        }

        this._applyFormatted();
    }

    get name() {
        return this.getAttribute('name') ?? '';
    }

    get value() {
        return this._numericValue ?? '';
    }

    set value(v) {
        const number = (v === '' || v == null) ? null : Number(v);
        this._numericValue = (number != null && isNaN(number)) ? null : number;
        if (this._input && document.activeElement !== this) {
            this._previousValue = this._numericValue;
            this._applyFormatted();
        }
    }

    attributeChangedCallback(name, _oldValue, newValue) {
        if (!this._input) return;
        if (name === 'name') this._input.name = newValue ?? '';
        if (name === 'disabled') this._input.disabled = newValue !== null;
        if (name === 'format' || name === 'decimals') this._applyFormatted();
        if (name === 'value') this.value = newValue;
    }

    _getDecimals() {
        return parseInt(this.getAttribute('decimals') ?? '2', 10);
    }

    _parse(text) {
        if (!text || text.trim() === '') return null;
        const number = parseFloat(text.replace(/[$,]/g, ''));
        return isNaN(number) ? null : number;
    }

    _applyFormatted() {
        if (!this._input) return;
        if (this._numericValue == null) {
            this._input.value = '';
            return;
        }
        const format = this.getAttribute('format');
        if (format === 'currency' || format === 'decimal') {
            const decimals = this._getDecimals();
            const formatted = this._numericValue.toLocaleString('en-US', {
                minimumFractionDigits: decimals,
                maximumFractionDigits: decimals
            });
            this._input.value = format === 'currency' ? '$' + formatted : formatted;
        } else {
            const decimals = this._getDecimals();
            this._input.value = this._numericValue.toLocaleString('en-US', {
                minimumFractionDigits: decimals,
                maximumFractionDigits: decimals
            });
        }
    }
}

customElements.define('number-input', NumberInputElement);
