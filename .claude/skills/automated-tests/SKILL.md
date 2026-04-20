---
name: automated-tests
description: Guide to writing Playwright-based automated tests for the bookie app — covers the overall approach, page objects, builders, and infrastructure helpers
version: 1.0.0
---

## Overall Approach

Tests are near end-to-end Playwright tests that run against an embedded Jetty server with an in-memory fake database. There are no mocks — the full application stack runs, but data is seeded directly into fake DAO implementations before each test. This gives high confidence that the UI and backend integrate correctly without the fragility and slowness of hitting a real database.

Most tests follow the same pattern: seed data → navigate to screen → perform actions → assert results.

## TestBase

All screen tests extend `TestBase`, which handles server lifecycle (started once per test class), browser context creation, and data reset between tests. It exposes:

- **Data seeding methods** (`givenExistingBonds`, `givenExistingTrades`, etc.) that populate the fake DAOs before the test runs
- **Navigation methods** (`switchToTrades()`, `switchToPositions()`, `switchToSecurities()`, etc.) that navigate the browser to the correct URL and return a typed page object for that screen
- The raw Playwright `page` object for cases where the page object abstraction isn't enough. The page should not be exposed to the individual tests, they should only use the PageObject and the Helpers.

## Page Objects

Each screen has a dedicated page object class that wraps all Playwright interactions for that screen. Tests never call Playwright APIs directly — they go through the page object. This keeps tests readable and centralises the fragile selector logic.

Page objects expose two kinds of methods:
- **Action methods** that simulate user gestures (clicking buttons, filling forms, submitting)
- **Assertion methods** that verify what's visible on screen

Page objects live alongside their tests under `src/test/java/com/bookie/screens/<screenname>/`.

## Builders

Test data is created with static builder methods (`aBond(cusip)`, `aTrade(id, cusip, direction, quantity)`, `aPosition(cusip, book, quantity)`, etc.). Builders provide sensible defaults for every field so tests only need to specify the values that are relevant to what's being tested. Additional fields can be set fluently on the returned domain object.

Builders live in `src/test/java/com/bookie/infra/builders/`.

## Infrastructure Helpers

The `infra` package contains helpers that page objects use internally:

- **DataGridHelper** — locates rows and cells in data grids by column header name, not by index or CSS
- **FormHelper** — finds form fields by their label text
- **Typed input helpers** (text, number, select, date) — set values on specific input types
- **ButtonHelper / LinkHelper** — find interactive elements by their visible label
- Write new helpers/add logic into existing helpers as necessary, but make sure each helper only contains logic related to its UI component, e.g. TextInput should handle reading / changing the value, but it should NOT handle clicking an unrelated button.
- The helpers also contain APIs to locate the relevant components and create the helper, e.g. ButtonHelper.getByLabel - use these

## Locating by Role, Not by CSS

All helpers locate elements by visible text, ARIA role, or label — never by CSS class or DOM structure. This makes tests resilient to styling changes and keeps them aligned with how a real user (or screen reader) perceives the page. Playwright's `getByRole`, `getByLabel`, and `getByText` are the preferred locator strategies.

## Adding a Test for a New Screen

1. Create `src/test/java/com/bookie/screens/<name>/`
2. Write `<Name>PageObject.java` — expose action and assertion methods, use the infra helpers internally
3. Add a `switchTo<Name>()` method in `TestBase` that navigates and returns the page object
4. Write `<Name>ScreenTest.java` extending `TestBase`
5. Add a builder in `infra/builders/` if the screen introduces a new domain object
