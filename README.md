# ExyliaTotems

Free, open-source plugin for Paper and Folia that **honours a totem of undying the player reached for a moment too late**.

A player on a bad connection can swap a totem into their hand and still die: the server processed the death before their swap packet arrived. ExyliaTotems holds the death open for a few ticks, watches what the player did in that window (hotbar scroll, number keys, offhand swap, inventory clicks), and saves them if the totem was on its way.

## Requirements

- Paper or Folia 1.21+, Java 21
- [PacketEvents](https://modrinth.com/plugin/packetevents)
- [ExyliaLib](https://modrinth.com/plugin/exylialib) ([GitHub releases](https://github.com/DiGround-s/ExyliaLib/releases)). Install it first; without it the plugin stays off and says where to get it.

## Updates

ExyliaLib checks this plugin's GitHub releases and announces a new version in the console and to admins. Nothing is downloaded on its own: run `/exylialib plugins` to download the update, which is applied on the next restart. A new major version waits for you to install it by hand, and a release that needs a newer ExyliaLib waits until the library has updated. Tune it under `plugin-updates` in `plugins/ExyliaLib/config.yml`.

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
