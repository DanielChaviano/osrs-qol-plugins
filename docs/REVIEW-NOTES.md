# 4.0.1-rc.1: reviewer notes

Status: release candidate, not Plugin Hub approved.

## Filesystem and image actions

The old direct `java.io.File` reads and `LinkBrowser.open` calls were removed
from runtime source. `CoachScreenshotFiles.root()` is the only
`Filepath.Unchecked` use. It roots a capability at the SDK-owned
`RuneLite.SCREENSHOT_DIR` constant, never a configuration-supplied directory.
This is a restricted API and is explicitly submitted for manual review.
It preserves the folder where RuneLite ImageCapture already saves these PNGs.

All subsequent reads use Filepath. Persisted history is untrusted: only a direct
`<player>/Milestones/<name-containing--level->.png` below that root passes the
boundary. Parent traversal, alternate data streams, unrelated files, non-PNG
extensions and sibling-root-prefix tricks are rejected. The rule is tested with
Windows and Unix strings. There is no recursive filesystem scan or deletion.

Core ImageCapture remains responsible for writing the game PNG and clean-card
PNG and for emitting ScreenshotTaken. The plugin validates a scoped nonempty
file off-thread before confirming success. `CoachThumbnails` uses Filepath
streams, a MemoryCacheImageInputStream, size/dimension limits and subsampling.

A user click opens an in-client modeless image-preview dialog, not an external
program. The folder button copies the validated folder path to the clipboard
only on the user's click. It does not type into game chat. No Desktop,
LinkBrowser.open, subprocesses, reflection, downloaded code or network client
is used. The wider dashboard's explicit toFront call was removed. Auxiliary
windows are user-triggered; the main game window is not moved, resized or
minimized. Requesting component focus after a dashboard click only supports
keyboard navigation within the same window.

## Local data and privacy

Goals, presentation settings and up to eight local screenshot path records use
existing RuneLite ConfigManager keys. No image bytes are saved in config or sent
to a third-party server. Normal RuneLite config-profile behavior applies. No
credentials are read. Old user PNGs are not deleted. Changing the config profile
to a different computer can leave gallery paths unavailable; they are rejected
rather than widened to a new filesystem root.

## Gameplay

No game inputs, walking commands, menu server actions or prayer switching are
generated. The optional Shortest Path handoff posts a local PluginMessage on a
manual navigation click. It does not guarantee a route is available or F2P-safe.
The overlay paints information and captures only UI button clicks inside its
visible popup; it does not automate a game interaction.

The existing equipment heuristics and small melee model are intentionally
retained rather than expanded in this compatibility release. They are not a
verified global XP optimum; attack style, special mechanics, potions, prayers,
crowding and downtime assumptions limit the model. The ETA uses observed XP and
assumes continuous training at the present pace, not the player's life schedule.

## Build and dependencies

The project targets Java 11 with build=standard and no added third-party runtime
dependencies. The complete six existing backend files are retained from the
inspected repository. The local PowerShell installer, fixtures, generated build
outputs, credentials and screenshots are not required or committed. Only source,
small PNG assets, tests, metadata, documentation and Gradle wrapper are shipped.

Tests under src/test use ordinary Java filesystem APIs solely for synthetic
rendered-test output. They are not part of the plugin runtime JAR.

AI assistance was used for implementation, artwork, migration and documentation.
See ARTWORK.md. No game account was controlled during preparation.
