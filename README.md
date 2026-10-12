# Combat Coach

Combat training suggestions, an animated level-goal forecast, an optional HUD,
and milestone screenshot celebrations for RuneLite.

**4.0.1-rc.1 — Plugin Hub release candidate. Not yet approved or available in
Plugin Hub.** The visual features are free; “Premium” in earlier local builds
referred to the presentation, not a subscription or paywall.

## Features

- Training / Analysis / Settings views with the V4 artwork, themes, animated
  forecast, goal ring, skill tiles, compact layout and reduced-motion controls.
- Per-skill level goals with a full local date, year, time and timezone.
  Hourly XP is used internally for the ETA, not shown as a dashboard headline.
- An optional movable in-game HUD. All navigation remains manual.
- A button that sends the suggested destination to the separately installed
  Shortest Path plugin. Missing Shortest Path never provides automatic walking.
- Milestone Screenshots at levels 20, 40, 60, 80 and 99 by default; configurable.
  The celebration renders before capture. Optional clean-card PNGs use the same
  art. Only confirmed files produce success notices.
- A local eight-entry capture gallery with in-client image previews and a
  **Copy folder path** button. This replaces the old external-file-opening calls.

Equipment is considered by the existing backend but is not redundantly listed
in the sidebar. Suggestions and model rankings are estimates, **not verified
maximum XP/hour**. NPC models cover a limited candidate set. F2P transportation
settings do not certify every terrain segment. Long-term goal dates assume
nonstop training at the current pace and can change substantially.

## Development / release-candidate testing

Use a Java 21 JDK to run this repository's Gradle wrapper; plugin bytecode targets
Java 11. From a clean checkout:

```powershell
.\gradlew.bat clean verifyRelease --no-daemon --console=plain
.\gradlew.bat run --no-daemon --console=plain
```

On macOS/Linux use `./gradlew`. Both modules register in one development client.
Jagex-account developers should use RuneLite's official
[development login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
Never commit credentials.properties or share authentication logs.

Once logged in, enable both **Combat Coach** and **Milestone Screenshots** in
RuneLite's configuration. Select a target level, check the full-date forecast,
and use **Settings > Test capture** while the game is visible. A test does not
change your character's level. The game image should contain the fully revealed
celebration; the separate clean-card PNG should also appear. Preview both and
verify that **Copy folder path** pastes a real folder into your file manager.

Screenshot creation still uses RuneLite's normal screenshot directory, normally
`.runelite/screenshots/<character>/Milestones/`. Exact paths appear after saving.
The compatibility release does not move or delete your existing screenshots,
rename your saved config keys, or change your Jagex credentials.

## Validation and approval

`verifyRelease` compiles the real RuneLite dependency and runs the UI, capture,
overlay, theme and file-boundary checks. Those checks do not control a RuneScape
account and cannot verify live capture/route behavior. Use the checklist in
[docs/LIVE-TEST.md](docs/LIVE-TEST.md) before requesting Plugin Hub review.

[Reviewer notes](docs/REVIEW-NOTES.md) disclose one deliberately scoped
`Filepath.Unchecked` screenshot-root capability that requires manual review.
Passing a build or these tests is not Plugin Hub approval.

## Publishing

See [docs/SUBMISSION.md](docs/SUBMISSION.md). Submit a source-commit marker to
`runelite/plugin-hub`, not a JAR, ZIP, PowerShell installer or login file.

## License / attribution

BSD-2-Clause; see LICENSE and [ARTWORK.md](ARTWORK.md). AI-assisted development and
artwork. RuneLite/RuneScape/Jagex names remain their owners' trademarks. This
plugin is not affiliated with or endorsed by RuneLite or Jagex.
