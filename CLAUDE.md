# Bookie
This is a demo app showcasing the library DataStar.
It will be a credit trade booking app, eventually letting the user book trades, see a list of all trades, positions
on various cusips and books and manage reference data for the bonds.
The focus is on simplicity and minimalism. Do not add any dependencies unless I ask for them.

## Environment

- IDE: IntelliJ IDEA on Windows
- Build: Gradle (use `./gradlew` or `gradlew.bat`)
- JDK: 24

## HTML Templating

- Never construct `EscapedHtml` directly via `EscapedHtml.html()`; always use `TemplatingEngine.html()`.
- Always statically import `TemplatingEngine.html` (`import static com.bookie.infra.TemplatingEngine.html`) and call it as `html(...)`.
- Use `${x}` when the value needs HTML encoding, `@{x}` when it is already safe/rendered HTML.

## Git

- Commit messages must be a single line: brief description only, no body, no co-author trailer.