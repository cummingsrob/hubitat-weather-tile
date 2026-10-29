# Weather Tile for Hubitat

A multi-day weather forecast strip for Hubitat Elevation dashboards. Pick Open-Meteo (worldwide, no key), the US National Weather Service (no key) or a keyed provider. The forecast is drawn as a crisp SVG with colored or outline icons, highs and lows, and rain chance and amount.

<p align="center"><img src="docs/preview.png" alt="Weather Tile preview: 7-day forecast strip" width="896"></p>

- **Light on the hub:** the forecast is fetched on a schedule and cached, and opening a dashboard never calls a weather API. Errors back off and the last good forecast stays up, marked "(stale)".
- **Fits a dashboard tile:** the tile attribute is a short `<img>` link (about 220 characters), served from the app's own endpoint on your LAN or through the Hubitat cloud.
- **Configurable:** 1-7 days, units, dark or light theme, transparent background, fonts and colors, icon pack, and which values show.
- **No dependencies:** built-in Hubitat APIs only.

**Version:** 0.2.7 ([changelog](CHANGELOG.md)). Requirements and roadmap: [REQUIREMENTS.md](REQUIREMENTS.md).

## Quick install
1. **Hubitat Package Manager:** **Install** → **From a URL** → `https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main/packageManifest.json`. HPM installs everything and turns on OAuth.
2. **Apps** → **+ Add User App** → **Weather Tile Manager** → **Done**, then **Add a weather tile**.
3. On a dashboard, add a tile with that device, template **Attribute**, attribute **weatherTile**.

Other ways to install (bundle zip, or file by file) are below.

## Recent changes
- **0.2.7:** after midnight the tile drops yesterday right away and fetches a fresh forecast at about 00:02.
- **0.2.6:** last-updated line shows the location and time ("Grosse Pointe Woods, Oct 29, 12:07 AM"); manifest URLs carry `?v=<version>` to skip GitHub's cache.
- **0.2.5:** HPM install and upgrade fixes (sandbox-safe response parsing, icon libraries the hub can parse), robust JSON handling for NWS.
- **0.2.0:** icons moved into separate Hubitat libraries so more packs can be added.
- **0.1.0:** first release.

Full history: [CHANGELOG.md](CHANGELOG.md).

## Pieces
| File | Type | Purpose |
|---|---|---|
| `libraries/cummingsrob.WeatherTileIcons.groovy` | Library | Default icon pack (Rob's colored set). |
| `libraries/cummingsrob.WeatherTileIconsOutline.groovy` | Library | Outline icon pack. |
| `apps/WeatherTileManager.groovy` | App (parent) | Holds your weather tiles. Install once. |
| `apps/WeatherTileInstance.groovy` | App (child, **OAuth**) | One tile: settings, fetching, SVG endpoint. `#include`s the icon library. |
| `drivers/WeatherTileDevice.groovy` | Driver | The device each tile creates; put it on a dashboard. |
| `bundle.zip` | Bundle | All of the above, for **Bundles → Import ZIP**. |
| `bundle-library.zip` | Bundle | Libraries only (used by HPM). |

No external libraries; only built-in Hubitat APIs. The library must be on the hub before the instance app is saved.

## Install option A: Hubitat Package Manager (recommended)
HPM → **Install** → **From a URL** → `https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main/packageManifest.json`. HPM installs the library bundle, the driver and both apps, and enables OAuth for the instance app. Then go to step 5 below.

## Install option B: bundle
1. **Developer tools → Bundles → Import ZIP** → URL `https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main/bundle.zip` (or download and upload it). This installs the library, driver and both apps.
2. **Apps Code → Weather Tile Instance** → **OAuth** → **Enable OAuth in App** → **Update** → **Save**. (Bundles do not enable OAuth.)
3. Continue at step 5 below.

## Install option C: manual, one file at a time
1. **Libraries Code** → **+ New Library** → **Import** → `https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main/libraries/cummingsrob.WeatherTileIcons.groovy` → **Save**. Repeat for `.../libraries/cummingsrob.WeatherTileIconsOutline.groovy`.
2. **Drivers Code** → **+ New Driver** → **Import** → `.../drivers/WeatherTileDevice.groovy` → **Save**.
3. **Apps Code** → **+ New App** → **Import** → `.../apps/WeatherTileManager.groovy` → **Save**.
4. **Apps Code** → **+ New App** → **Import** → `.../apps/WeatherTileInstance.groovy` → **Save** → **OAuth** → **Enable OAuth in App** → **Update** → **Save**.

(`...` = `https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main`)

## Then (all options)
5. **Apps** → **+ Add User App** → **Weather Tile Manager** → **Done**.
6. Open **Weather Tile Manager** → **Add a weather tile** → choose settings → **Done**. A device with the tile's name is created.
7. On a dashboard, add a tile: device = that device, template = **Attribute**, attribute = **weatherTile**.

## Adding an icon pack (developers)
Create a new library (for example `cummingsrob.WeatherTileIconsMeteocons`) with a provider method returning `[packId: [label: "...", icons: [sunny: '<g>...</g>', ...]]]`, drawn in a 64x64 box using the CSS classes `cloud`, `light`, `precip`, `frozenPrecip`. Add `#include` for it in `WeatherTileInstance.groovy`, add the method name to `WT_ICON_PACK_PROVIDERS`, add the library to `LIBS` in `tools/build.py`, and run `python3 tools/build.py` to rebuild the bundles. Missing icon keys fall back (see `CONDITIONS`).

## Settings (per tile)
- **Source:** Open-Meteo (default, global, no key), NWS/weather.gov (US only, no key), or a keyed provider (API base URL + key; OpenWeatherMap One Call 3.0 `daily` format).
- **Refresh interval:** 15 min, 30 min (default), 1 h, 3 h. Errors back off exponentially (up to 6 h; at least 1 h after HTTP 429) and keep the last good forecast, marked "(stale)".
- **Location:** hub latitude/longitude by default; optional custom lat/lon and label.
- **Units:** follow hub (default), imperial, or metric.
- **Days:** 1-7 (default 7: today + 6).
- **Theme:** dark background with light icons (default) or light. Optional transparent background, corner radius, font family, font size, and per-color overrides.
- **Icon pack:** Rob's colored set (default) or outline.
- **Show/hide:** high/low, rain %, rain amount, hide zero values, day-label style.
- **Last updated:** tiny line, location then time, e.g. "Grosse Pointe Woods, Oct 29, 12:07 AM" (hub time zone). The location is the tile's label, or a place name looked up once from latitude/longitude (NWS in the US, BigDataCloud elsewhere), or the hub name. Time format setting (default `MMM d, h:mm a`); the location prefix can be turned off.
- **Tile URL:** local (LAN) or cloud (remote dashboards).
- Buttons: **Refresh forecast now**, **Regenerate access token** (updates the tile attribute).

## Device attributes
`weatherTile`, `lastUpdated` (ISO 8601), `lastUpdatedDisplay`, `source`, `status` (ok/stale/error), `statusMessage`, `units`, `latitude`, `longitude`, `locationLabel`, `todayHigh`, `todayLow`, `todayPrecipChance`, `tomorrowPrecipChance`, and per day n=0..6: `day{n}Date`, `day{n}Label`, `day{n}High`, `day{n}Low`, `day{n}Condition`, `day{n}ConditionText`, `day{n}Icon`, `day{n}PrecipChance`, `day{n}PrecipAmount`. Events are sent only on change. The device's **Refresh** command fetches immediately.

Debug: `<tile URL base>/forecast.json?access_token=...` returns the cached forecast.

## Known limitations
- NWS has no daily precipitation amount, and after about 6 PM "today" has only an overnight low.
- The keyed provider only understands the OpenWeatherMap One Call `daily` format. Tomorrow.io and others need their own normalizer.
- The rendered SVG is cached in memory and rebuilt from saved forecast data after a hub reboot.
- Extra fields (current conditions, wind, humidity, sunrise/sunset, alerts) are not implemented yet (v2).
- HPM tracks the icon libraries through the library bundle; changing a library means rebuilding the zips (`python3 tools/build.py`).
- Animated icon packs, a monochrome pack and night icons are not included yet; see Next up in [REQUIREMENTS.md](REQUIREMENTS.md#next-up-robs-to-dos).

## Credits and licenses
- Code: Apache-2.0 ([LICENSE](LICENSE), [NOTICE](NOTICE)). Portions adapted from Rachio-Community, © 2018 Anthony Santilli, © 2024 lnjustin, Apache-2.0.
- Icons: derived from [erikflowers/weather-icons](https://github.com/erikflowers/weather-icons); the filled set is Rob Cummings' color-modified version. SIL OFL 1.1 ([ICONS-LICENSE.txt](ICONS-LICENSE.txt)).
- Weather data: [Open-Meteo](https://open-meteo.com) (CC BY 4.0) and the US National Weather Service.
