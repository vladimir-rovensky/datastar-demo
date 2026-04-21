# Feature: Boost in-app navigation to decouple tabID from the URL

## Goal

Stop leaking server-side tab state through URLs. Today every URL carries `?tabID=`, so opening a link in a new browser tab produces two clients hitting the same `ClientSession`, and F5 reuses the same session instead of starting fresh. The fix is to turn in-app `<a>` navigation into DataStar-driven fragment swaps so the tabID no longer needs to live in the URL at all — it becomes a document-lifetime value, minted on every full HTML load and sent via the existing `X-tabID` header. F5 and open-in-new-tab then naturally produce a fresh session, while clicking links within the same tab preserves state as it does today.

## Key Decisions

- **tabID is document-lifetime, not persisted.** No `sessionStorage`, no cookie. A fresh JS context means a fresh tabID, which is exactly what makes F5 and open-in-new-tab produce fresh state.
- **Boost via `@get` + `pushState`.** Anchors keep a real `href` (for right-click "open in new tab" → fresh session); a click handler suppresses default for plain left-clicks, fires `@get(href)`, and pushes history.
- **Trust DataStar to morph the full HTML document.** Handlers keep returning the full `<!DOCTYPE html>…` from `Shell.render()`; no boosted-request branch on the server. Revisit only if morphing misbehaves in practice.
- **`data-on:popstate__window` handles Back/Forward.** A single window-scoped listener in `Shell` re-issues `@get(location.pathname + location.search)`.
- **Don't manage request-ordering races.** Rapid Back/Forward may produce out-of-order responses; DataStar's request cancellation capability should handle this cleanly.
- **Preserve cross-screen state within a tab.** Trades → Positions → Trades reuses the same `ClientSession`; this is intentional and already how things work once the navigation is boosted.
- **Drop speculation rules entirely.** `prerender` creates unused server sessions; `prefetch` can't satisfy the boosted `@get` XHR (wrong headers) and creates orphan sessions too. If nav latency ever becomes a problem, do a hover-intent `fetch` with proper headers instead — out of scope for this plan.

## Implementation Steps

1. **Boost one link end-to-end as a verification spike.**
   Modify `Link.render` (or add a one-off variant) so that the Trades link in `MainNav` fires `@get(href)` on plain click, pushes history via `pushState`, and guards against modifier/middle clicks (`!evt.ctrlKey && !evt.metaKey && !evt.shiftKey && !evt.altKey && evt.button === 0`). Leave everything else (tabID in URL, `history.replaceState` in `Shell`, server-side gate) untouched. Click Trades ↔ Positions, confirm the title updates, the body morphs, the existing `ClientSession` is reused (check server logs — no new session created), and the `window.fetch` monkey-patch is not double-installed.

2. **Roll the boosted click behavior out to all anchors.**
   Apply the same click handler to every `<a>` produced by `Link.render` so all in-app nav is boosted. Verify every link in the nav behaves the same as Trades did in step 1.

3. **Add the popstate handler in `Shell`.**
   Attach `data-on:popstate__window="@get(window.location.pathname + window.location.search)"` to the `<body>` (or a similarly long-lived element). Click Trades → Positions → Back → Forward; confirm content matches the URL at each step and no new sessions are created on the server.

4. **Remove tabID from the URL and loosen the server-side gate.**
   In one commit:
   - `Link.getHref()` stops appending `?tabID=`.
   - `Shell`'s inline script drops the `history.replaceState` block that re-adds tabID to the URL.
   - `SessionRegistry.getOrCreateSession` drops the `request.param("tabID").isPresent()` check; `TabID.forExisting` already falls back to the `X-tabID` header, and a request with neither will miss the map and fall through to `createSession`.

   Verify: F5 on any screen produces a new session in logs; open-in-new-tab (Ctrl-click, right-click, paste) produces a new session; clicking links in-tab continues to reuse the existing session.

5. **Remove the speculation-rules block from `Shell`.**
   Delete the `<script type="speculationrules">` element and the `prerenderURLs` plumbing that feeds it. Load any screen, confirm the server no longer creates extra orphan sessions on initial paint (sessions count in logs should equal the number of tabs actually open).

## Out of Scope

- Hover-intent or any other prefetching of nav targets.
- Fixing DataStar's request-ordering behavior under rapid Back/Forward.
- Window-scroll save/restore on navigation (the app layout doesn't scroll the window; per-element scroll is server-tracked and rides along with morphed content).
- Server handlers returning body-only fragments for boosted requests. Only add this if step 1 reveals that full-document morphing misbehaves.

## Open Questions

- **Does DataStar support `data-on:popstate__window`?** Verify against the DataStar reference when implementing step 3. If the `__window` modifier isn't supported, fall back to a plain `window.addEventListener('popstate', …)` in `Shell` that dispatches a custom event onto an element with `data-on:<custom>="@get(…)"`.
- **Does morphing re-execute the inline `<script>` that monkey-patches `window.fetch`?** Step 1 is designed to catch this. If it does double-wrap, move the patch out of `Shell`'s render path — e.g., into a separate static JS file loaded via `<script src>` (which morph will skip on re-render).
