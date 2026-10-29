# Changelog

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
