# Release-candidate live check

Run against the exact release-candidate commit you plan to submit. Mark only what
was actually tested; leave unchecked items unchecked.

- [ ] Clean checkout: `./gradlew clean verifyRelease` succeeds on actual dependencies.
- [ ] Both plugin modules load in one development client.
- [ ] Training, Analysis and Settings fit at normal sidebar width; scrolling works.
- [ ] Themes, animations, static/reduced motion, optional HUD and dashboard work.
- [ ] Goals persist per skill; ETA shows the full date/year/timezone; idle time adjusts it.
- [ ] Login/logout, world hops and switching skills do not create spurious XP/levels.
- [ ] Test capture: celebration is present in the game PNG; separate card PNG saved.
- [ ] Real configured milestone: same capture and confirmation behavior.
- [ ] Failed/invisible/disabled capture does not show a false saved confirmation.
- [ ] Gallery previews game and card PNGs; Copy folder path yields the actual folder.
- [ ] Old V4 gallery records in the normal Milestones folder still preview locally.
- [ ] Malformed/outside-root gallery entries fail closed and do not crash the client.
- [ ] Manual Shortest Path handoff tested with it present and absent.
- [ ] Disable/re-enable both modules: no stale dialogs/overlays/timers/duplicate captures.

No credentials, personal logs or actual account data should be attached to a public PR.
