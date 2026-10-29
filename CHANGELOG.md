# Changelog

## Next up
- More optional pack libraries: Makin-Things (MIT), Tabler (MIT), Lucide (ISC), amCharts (CC BY 4.0).
- Night variants for Rob's filled and Rob's outline packs.

## 0.2.17 - 2026-10-29
- Settings page redone with plain Hubitat sections: removed the emoji/HTML section headers, the styled status badge, the colour swatches and styled help text from 0.2.16, which rendered poorly on the hub.
- Same nine groups (Status, Name & source, Location, Forecast content, Look, Last-updated line, Color overrides, Dashboard, Advanced · Logging) with side-by-side fields via `width:` on inputs. "Open tile image" is now a standard external link.
- No setting names changed.

## 0.2.16 - 2026-10-29
- Weather Watcher settings page reorganized into cards: Status (badge, source, last update, next refresh, preview, refresh button, tile link) at the top, then Name & source, Location, Forecast content, Look, Last-updated line (with a sample), Color overrides (collapsed, theme-default swatches), Dashboard, and Advanced (collapsed logging).
- Side-by-side fields use Hubitat's `width:` grid; no setting names changed, so existing installs keep their values.
- HPM packaging: package-level version only (build no longer adds a bundle version); repository package id, category, tags, gitHubUrl and component descriptions.

## 0.2.15 - 2026-10-29
- Child device network id prefix `WTILE-` is now `WWATCH-`.
- Removed the upgrade/migration notes for the old name from the README and REQUIREMENTS; install fresh.
- Unchanged: HPM package/app/driver/bundle ids and namespace `cummingsrob`.

## 0.2.14 - 2026-10-29
- **Breaking:** device attribute `weatherTile` renamed to `weatherWatcher`. After updating, change dashboard Attribute tiles and rules from `weatherTile` to `weatherWatcher`.
- Rename cleanup: internal `wt*` names (short for the old Weather Tile name) are now `ww*`:
  - icon provider methods `wtIconPacks*` → `wwIconPacks*`
  - icon method prefixes `wtIf_`/`wtIo_`/`wtMs_`/`wtMa_`/`wtMo_` → `wwIf_`/`wwIo_`/`wwMs_`/`wwMa_`/`wwMo_`
  - constants `WT_*` → `WW_*`
- Removed the old-name notes from the app and driver header comments.
- Unchanged: HPM ids, namespace `cummingsrob`, and the child device network id prefix `WTILE-` (changing it would orphan existing devices and dashboards).

## 0.2.13 - 2026-10-29
- Finished the rename to Weather Watcher:
  - GitHub repo `cummingsrob/hubitat-weather-tile` is now `cummingsrob/hubitat-weather-watcher` (old URLs redirect).
  - Files: `apps/WeatherWatcherManager.groovy`, `apps/WeatherWatcherInstance.groovy`, `drivers/WeatherWatcherDevice.groovy`.
  - Icon libraries: `cummingsrob.WeatherTileIcons*` are now `cummingsrob.WeatherWatcherIcons*` (WeatherWatcherIcons, ...Outline, ...Meteocons, ...MeteoconsAnimated, ...Mono), with matching `library()` names and `#include` lines.
  - Bundle name `WeatherTile` is now `WeatherWatcher` (same HPM id).
- Unchanged: namespace `cummingsrob`, HPM package/app/driver/bundle ids, all attribute names (including `weatherTile`).
- After the HPM update, delete the old `cummingsrob.WeatherTileIcons*` libraries and the old WeatherTile bundle (see README).

## 0.2.12 - 2026-10-29
- Renamed to **Weather Watcher** (a dashboard tile plus device attributes for rules). Parent app "Weather Tile Manager" is now **Weather Watcher Manager**, child app "Weather Tile Instance" is now **Weather Watcher**, driver "Weather Tile Device" is now **Weather Watcher Device**; HPM package and repository name **Weather Watcher**. New default label "Weather Watcher".
- Upgrade-safe: namespace `cummingsrob`, file names, library names, attribute names (including `weatherTile`), HPM package/app/driver/bundle ids and the bundle name are unchanged, so HPM updates the existing code in place and keeps app instances, devices, settings and OAuth. Bundle or manual installs: update each existing code entry in place (see README).

## 0.2.11 - 2026-10-29
- New icon pack **Monochrome (erikflowers)** (library `cummingsrob.WeatherTileIconsMono`), built from erikflowers/weather-icons font glyphs exported as SVG paths. Single color that follows the theme text color; day and night variants. `tools/gen_mono.py` regenerates it.
- Night icons for today's slot only: before sunrise and after sunset, today shows a night variant. Sun times come from Open-Meteo (`sunrise`/`sunset`) or the hub location; NWS uses the overnight/tonight period's forecast for the night condition. The tile redraws at sunrise and sunset. Future days stay daytime. Packs without a night variant keep the day icon. The Animate icons setting still applies.
- Meteocons packs gain night variants (clear-night, partly-cloudy-night and so on).

## 0.2.10 - 2026-10-29
- Icon pack menu labels now credit Rob: "Rob's colored set (default)" is **Rob's filled (default)** and "Outline (erikflowers)" is **Rob's outline** (both modified from erikflowers/weather-icons). Library names, file names and pack keys (`filled`, `outline`) are unchanged, so existing tiles keep their setting. ICONS-LICENSE.txt, NOTICE and README updated.

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
