# Changelog

## Next up
- Rename icon packs to credit Rob for both the filled and outline sets (modified from erikflowers/weather-icons).
- Monochrome pack from the erikflowers font files.
- More optional pack libraries: Makin-Things (MIT), Tabler (MIT), Lucide (ISC), amCharts (CC BY 4.0).
- Night icons for today's slot only (before sunrise / after sunset; NWS "Tonight"); future days stay daytime.

## 0.2.9 - 2026-10-29
- New icon packs, each its own library: **Meteocons (static)** (`cummingsrob.WeatherTileIconsMeteocons`) and **Meteocons (animated)** (`cummingsrob.WeatherTileIconsMeteoconsAnimated`), from basmilius/weather-icons (MIT). Every condition key has an icon. IDs are prefixed per icon so several icons share one tile cleanly.
- New setting **Animate icons** (on by default, shown only for the animated pack). Off removes the SVG animation elements.
- `tools/gen_meteocons.py` regenerates both libraries; build.py adds them to bundle.zip and bundle-library.zip.

## 0.2.8 - 2026-10-29
- Fix: after an HPM code update the tile could keep serving an old image (for example yesterday's icons and time) even though the forecast had updated. The in-memory image is now stored with its version, and each image request checks it against the version recorded at the last update; if it is missing or different, the image is redrawn from the stored forecast first.
- Fix: the last-updated line could fall back to the hub name ("Home") while `locationLabel` showed the real place. The last good place name is kept across code updates and resets, location matching tolerates number-format differences, and the tile is redrawn when a lookup returns.

## 0.2.7 - 2026-10-29
- Fix: just after midnight the tile still showed yesterday as the first day (e.g. "Wed, Today, Fri..."). Rendering, the SVG endpoint and `forecast.json` now skip forecast days earlier than today (hub time zone), so labels roll correctly ("Today, Fri, ..."). When the 00:01 re-render finds the forecast was fetched before midnight, it queues one fresh fetch about a minute later (once per day; error backoff still applies).

## 0.2.6 - 2026-10-29
- Last-updated line is now location then time, e.g. "Grosse Pointe Woods, Oct 29, 12:07 AM" (no "Updated" word). Location = the tile's location label, else a place name reverse-geocoded once from latitude/longitude (NWS points `relativeLocation` in the US, BigDataCloud's free client endpoint elsewhere) and cached in state, else the hub name; long names are shortened. New setting to turn the location prefix off; the time-format default is `MMM d, h:mm a`, and tiles still on the old default switch to the new one automatically. The `locationLabel` attribute uses the same name.
- `packageManifest.json` locations carry `?v=<version>` so hubs fetch the new files instead of GitHub raw's cached copies; `tools/build.py` writes these from the manifest version.
- README: new header, quick install and recent changes; crisp 2x preview image.

## 0.2.5 - 2026-10-28
- Fix HPM upgrade failure "Expression [ClassExpression] is not allowed: java.io.Reader": `respJson()` no longer references java.io or java.util.zip classes. It handles Map/List and String bodies directly, anything with `getText()` by duck typing, and bytes via `new String(data, "UTF-8")`; gzip handling dropped (the hub decompresses).

## 0.2.4 - 2026-10-28
- Fix "NWS points parse error: groovy.json.JsonException ... character read is 'e'": responses are no longer parsed with `getJson()`. A new `respJson()` reads the body whether the hub returns it as a Map/List, String, bytes, stream (including gzip) or reader, strips a BOM, and reports the start of any non-JSON body.
- All requests (NWS points and forecast, Open-Meteo, custom) set `contentType: application/json`; NWS sends `Accept: application/geo+json, application/json` and a User-Agent.

## 0.2.3 - 2026-10-28
- Fix "Cannot parse library definition" (the real cause): the hub could not parse the libraries' large `@Field static final Map` of long icon strings. Each icon is now a small method returning its SVG (split into 400-character chunks), and the provider methods map icon keys to those calls. Verified by compiling on a hub.

## 0.2.2 - 2026-10-28
- Fix "Cannot parse library definition" on library import: the header comment no longer mentions the word library or contains parentheses/quotes the hub parser could pick up, the `library (...)` block matches the BirdsLikeWires layout (plain string literals, alphabetical keys) and `documentationLink` has no `#` anchor.

## 0.2.1 - 2026-10-28
- Fix HPM install failure "Failed to install bundle WeatherTile": the library now starts with its `library(...)` metadata, with imports after it (the layout used by working published bundles), instead of an `import` line before it.
- Icons split into two smaller libraries: `cummingsrob.WeatherTileIcons` (filled, default) and `cummingsrob.WeatherTileIconsOutline`; both are in the bundles.
- Build script mirrors Hubitat's import hoisting when expanding `#include`s for local checks.

## 0.2.0 - 2026-10-28
- Icon SVGs moved out of the instance app into the Hubitat library `cummingsrob.WeatherTileIcons` (`#include`d by the app).
- Icon packs are now discovered from provider methods so new packs can ship as separate libraries; conditions use fallback icon keys.
- New `bundle.zip` (library + driver + apps) for Bundles → Import ZIP, and `bundle-library.zip` for HPM.
- `packageManifest.json` adds a `bundles` entry; version 0.2.0.
- `tools/build.py` builds the zips and an include-expanded app for local compile checks.

## 0.1.0 - 2026-10-28
- First release: Weather Tile Manager (parent), Weather Tile Instance (child, OAuth), Weather Tile Device (driver).
- Sources: Open-Meteo (default), NWS, keyed provider (OpenWeatherMap One Call `daily` format).
- Forecast cached on a schedule (15/30/60/180 min) with error and 429 backoff; endpoint never fetches.
- SVG forecast strip at `/forecast.svg` (local or cloud URL); `/forecast.json` for debugging.
- Dark/light themes, filled/outline icon packs, color overrides, 1-7 days, units follow the hub or are overridden, tiny last-updated line.
- Attributes from REQUIREMENTS.md section 11.
- HPM packageManifest.json and repository.json.
