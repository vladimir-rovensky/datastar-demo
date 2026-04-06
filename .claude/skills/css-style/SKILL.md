---
name: css-style
description: Guidelines for writing CSS in this project. Use when writing or reviewing any CSS styles.
---

# CSS Style Guidelines

All design tokens are defined as variables in `:root` in `global-styles.css`. Read that file to see what's available before adding anything new.

**Variable prefixes:** `--clr-*` · `--fs-*` · `--fw-*` · `--sp-*` · `--z-*`

**Adding variables:** only if the value is used in more than one place — otherwise hardcode it.

**Reuse first.** Extract shared properties to a common ancestor or combined selector rather than duplicating.

**Modifier classes stay thin.** Only put what differs from the base element.

**Use CSS nesting** for hover, focus, disabled, and modifier states.

**Sections in `global-styles.css`:** variables · reset · base elements · buttons · form fields · tables · toolbar · popup · tooltip.
