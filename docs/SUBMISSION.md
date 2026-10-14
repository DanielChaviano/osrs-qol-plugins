# Plugin Hub submission

Do not submit until the exact candidate commit passes the clean dependency build
and live checklist. This is not a claim of approval.

1. Fork https://github.com/runelite/plugin-hub to your GitHub account.
2. In your fork create a branch such as `add-combat-coach` from current master.
3. Confirm `plugins/combat-coach` is not already taken. Add that one extensionless
   file with:

```properties
repository=https://github.com/DanielChaviano/osrs-qol-plugins.git
commit=FULL_40_CHARACTER_TESTED_RELEASE_COMMIT
```

4. Commit the marker. Open a pull request against `runelite/plugin-hub:master`.
5. Title: `Add Combat Coach`. Suggested body:

```markdown
## Features
Combat training suggestions, per-skill local ETA goals, optional read-only HUD,
and configurable milestone screenshots with an animated card and local gallery.
Shortest Path integration is optional and manual. No automatic movement or input.

## Security / privacy
No third-party runtime network calls or telemetry. PNGs remain local. Images are
created by RuneLite ImageCapture. Reads use Filepath; the single scoped
Filepath.Unchecked root for RuneLite's existing screenshot folder is disclosed
in docs/REVIEW-NOTES.md and explicitly needs manual review. No LinkBrowser.open,
Desktop file opening, subprocesses or credential access. Preview dialogs are
user-triggered and no focus-forcing call is used.

## Validation
[Insert actual clean-checkout build result and checked live test outcomes here.]
Model suggestions are not verified maximum XP/hour; F2P routes are not certified.

## Source / licensing
Two modules in one build=standard Java 11 project. BSD-2-Clause.
AI-assisted code and artwork; provenance is in ARTWORK.md.

Generated-by: ChatGPT
```

6. Respond to CI and reviewer comments in the same PR. Fix the source, test the new
   commit, and update the marker's commit= rather than opening duplicate PRs.
7. Only after acceptance, merge and distribution should users install it through
   the normal RuneLite Plugin Hub. Updates also require reviewed source commits.

Official instructions: https://github.com/runelite/plugin-hub/blob/master/README.md
Rules: https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
