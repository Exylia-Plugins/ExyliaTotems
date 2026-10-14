# ExyliaTotems

Free, open-source plugin for Paper and Folia that **honours a totem of undying the player reached for a moment too late**.

A player on a bad connection can swap a totem into their hand and still die: the server processed the death before their swap packet arrived. ExyliaTotems holds the death open for a few ticks, watches what the player did in that window (hotbar scroll, number keys, offhand swap, inventory clicks), and saves them if the totem was on its way.

## Requirements

- Paper or Folia 1.21+, Java 21
- [PacketEvents](https://modrinth.com/plugin/packetevents)
- [ExyliaLib](https://github.com/DiGround-s/ExyliaLib/releases). If it is missing, the plugin downloads the latest release on first start and asks for a restart.

## Updates

ExyliaLib keeps both itself and this plugin up to date from their GitHub releases: a new version is downloaded in the background and applied on the next restart. A new major version waits for you to install it by hand, and a release that needs a newer ExyliaLib waits until the library has updated. Turn it off or tune it under `plugin-updates` in `plugins/ExyliaLib/config.yml`; `/exylialib updates` shows what is pending.

## Configuration

- **`config.yml`**: `totem.delay-ticks` (default `3`) is how long a death is held open. Longer forgives worse pings. `language` picks the messages' language (`default` follows ExyliaLib's).
- **`lang/<code>/messages.yml`**: the message shown when a player is saved, in English, Spanish, Portuguese, French, Russian and Turkish.

`/exyliatotem-reload` reloads both.

## For developers

Every save fires `PlayerTotemSaveEvent` on the public Exylia API, and a plugin that runs its own deaths can keep a death out of the hold.

## Building

```
./gradlew build
```

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[GNU General Public License v3.0](LICENSE).
