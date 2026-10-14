# ExyliaTotems

Saves a player whose totem of undying arrived a few ticks too late because of ping. Free and open source (GPL-3.0), public at `Exylia-Plugins/ExyliaTotems`.

## Releasing

- Every finished change is released, without asking and without `/exylia-release`: bump `version` in `build.gradle` (patch for fixes, minor for features, major for breaking commands or config), commit `chore: release <version>`, tag that commit `v<version>`, and push the branch and the tag.
- The release workflow runs only on `v*` tags and expects the tag to match the `build.gradle` version; it publishes the GitHub release and the fixed-name `ExyliaTotems.jar` that servers auto-update from. A push to the default branch without a tag only builds.

## ExyliaLib

- Always the newest: `build.gradle` resolves the latest GitHub release (or a newer one in mavenLocal) at build time, and that version becomes plugin.yml's `exylia-lib`. There is no pinned version to bump; `-PexyliaLibVersion` pins one for a single build.
- `ExyliaTotemsPlugin` is the Bukkit entry point and touches no ExyliaLib type: without ExyliaLib it runs `bootstrap/ExyliaLibInstaller` (downloads the latest release, falling back to the built-against one) and disables itself until the restart. With it, it hands over to the core, `ExyliaTotems`.
- Updates of this plugin come from ExyliaLib's `PluginUpdater` (`exylia-updates` in plugin.yml, the fixed-name `ExyliaTotems.jar` on every release).
