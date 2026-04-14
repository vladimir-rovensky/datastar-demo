package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.TemplatingEngine.html;

public class Loader implements Renderable {

    private final boolean isLoading;
    private final String text;

    private Loader(boolean isLoading, String text) {
        this.isLoading = isLoading;
        this.text = text;
    }

    public static Loader loader(boolean isLoading, String text) {
        return new Loader(isLoading, text);
    }

    @Override
    public EscapedHtml render() {
        if (!isLoading) {
            return EscapedHtml.blank();
        }

        return html("""
                <dialog class="loading-dialog" data-init="el.showModal()">
                    <div class="spinner"></div>
                    <span class="loading-text">${text}</span>
                </dialog>
                """,
                "text", text);
    }
}
