# Arthou's TPA 🧭

Arthou's TPA brings all the teleport essentials to a NeoForge server: TPA requests between players, homes, warps, a server spawn point, a "back" command for your last location, random teleport (RTP), and a safety net that teleports you home on logout.

## Versions

| Loader | Minecraft | Mod version | JDK | Source |
|---|---|---|---|---|
| neoforge | 1.21.1 | 1.0.1 | 21 | [neoforge/1.21.1](neoforge/1.21.1) |
| neoforge | 1.21.11 | 1.0.0 | 21 | [neoforge/1.21.11](neoforge/1.21.11) |

## Building from source

Install the JDK listed above for the version you want, then build from inside that folder:

```sh
cd neoforge/1.21.11
./gradlew build
```

On Windows, use `gradlew.bat build` instead. The finished jar lands in `build/libs/` for that version. The very first build will take a little longer, since Gradle needs to download itself and every dependency the project declares.

## About this repository

This repo keeps the source code and resources for every published version, each in its own folder. Local caches, test worlds, compiled output and backup copies are intentionally left out — only the real, published code lives here.

## License & credits

Every license, credit and notice file that shipped with each version has been kept as-is. Check the metadata for the version you're looking at before redistributing — publishing the source here doesn't change any declared license.
