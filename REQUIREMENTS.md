# Requirements (draft)

Status: **approved; v0.1.0 implemented (2026-10-28)**. See README for what is and isn't built yet.

## 1. Goals / non-goals

**Goals**
- Standalone Hubitat app (plus optional child device) that renders a multi-day forecast strip visually similar to Rachio-Community's dashboard weather.
- Selectable weather source, configurable look, and low hub load.
- Works on local and cloud (remote) Hubitat dashboards.

**Non-goals**
- No irrigation, schedule, or Rachio skip/watering icons.
- Not a full weather station driver (no radar, alerts, sun/moon in v1).
- No paid services required by default.

## 2. Weather sources
- **v1, no key:** Open-Meteo (global, default) and NWS / weather.gov (US only).
- **Optional, API key:** OpenWeatherMap (One Call; note version 2.5/3.0/4.0 differences) and Tomorrow.io.
- Optional: read from an existing weather driver device on the hub instead of calling a service directly.
- Each source has a normalizer mapping its response to a common daily model: date, high, low, precip probability %, precip amount, condition code → icon.
- Source failure: keep the last good data, show a stale marker, and log a warning.

## 3. Location
- Default: hub latitude/longitude and time zone (Settings → Hub Details).
- Override: manual lat/lon (and optional label). Validate the ranges.
- NWS: resolve the gridpoint once and cache it; re-resolve if the location changes.

## 4. Settings
- Weather source: Open-Meteo (default) or NWS (free, no key), or a custom/keyed provider with **API base URL + API key** fields.
- Units: default follows the hub; override for temperature °F/°C and precipitation in/mm.
- Days shown: configurable (1–7); default **today + 6 days (7 total)**.
- Refresh/cache interval (default 30 min; min 15 min).
- Icon pack: default **Rob's colored set** (derived from erikflowers); other packs per §12.
- Theme: default **dark background with light icons/text**; light (and possibly auto) selectable.
- Colors (text, highs/lows, rain, background, icon tint), font family/size, background transparent/solid, corner radius.
- Show/hide rain % and rain amount; hide zero values.
- Day labels: weekday short/long, "Today"/"Tomorrow", date format.
- Last-updated: show/hide; location then time, e.g. "Grosse Pointe Woods, Oct 29, 12:07 AM" (hub time zone), rendered tiny. Location = the tile's location label, else a place name looked up once from latitude/longitude (NWS points `relativeLocation` in the US, BigDataCloud elsewhere) and cached, else the hub name. Time format setting default `MMM d, h:mm a`; location prefix can be turned off.
- Endpoint: local vs cloud URL for the tile attribute.
- Logging level.

## 5. Outputs
- **Device attributes** (child device): per-day high/low/precip %/amount/condition, `lastUpdated`, `source`, `status`.
- **Dashboard tile attribute:** a short HTML wrapper (`<img>` pointing at the endpoint, with a cache-busting parameter) that stays well under 1,024 characters.
- **Image endpoint:** app mapping (e.g. `/forecast.svg`) returning `image/svg+xml`; optional `/forecast.json` for debugging.

## 6. Caching and hub-load rules
- The endpoint serves cached SVG only; it never triggers an outbound fetch per page view (unlike Rachio, which fetches on each load).
- Scheduled fetch at the configured interval, plus a manual refresh button/command.
- Async HTTP with timeouts; back off on errors and 429 responses; respect provider call limits.
- Re-render the SVG only when data or settings change; send events only on change.

## 7. Icon / condition-code mapping
- One mapping table per source onto a shared internal condition set (clear, partly cloudy, cloudy, fog, drizzle, rain, chance rain, showers, thunderstorm, snow, chance flurries, sleet/freezing, wind, hail, unknown).
- Open-Meteo: WMO weather codes. NWS: icon URL/shortForecast keywords. OWM: condition IDs. Tomorrow.io: weatherCode.
- Day/night variants optional; unknown codes fall back to a generic icon and log the code.

## 8. Security
- The endpoint requires an OAuth access token (`createAccessToken()`); OAuth must be enabled for the app.
- Token is never logged; settings provide a "regenerate token" button that updates the tile attribute.
- Cloud endpoint is off unless selected.
- API keys are stored in settings, masked in the UI and logs.

## 9. Credits / licensing
- This project: Apache License 2.0.
- Design and portions derived from **Rachio-Community**, Apache 2.0, © 2018 Anthony Santilli, © 2024 lnjustin. Keep the notices and NOTICE/attribution.
- **Outline icons:** erikflowers/weather-icons, SIL OFL 1.1. Include the license text.
- **Filled icons:** Rob's own designs (repo owner), separate from Rachio's icons; color-modified from erikflowers/weather-icons (font SIL OFL 1.1, code MIT, docs CC BY 3.0). Shipped under **SIL OFL 1.1** with attribution (see §10.12). No reserved font name used.
- **Excluded:** the Freepik/flaticon-derived rain drop, icicle and rain sensor icons, unless properly licensed.

## 10. Decisions (from Rob, 2026-10-28)
1. **Filled icons:** Rob's own designs, not the Rachio/Ashley Jager set. Derived from erikflowers/weather-icons (verified: icons SIL OFL 1.1, code MIT, docs CC BY 3.0). Rob supplies the SVGs.
2. **v1 sources:** Open-Meteo and NWS (free, no key). Plus a generic "keyed provider" option with **API base URL + API key** settings (OpenWeatherMap, Tomorrow.io, etc.; each still needs a normalizer).
3. **Days:** a setting; default **today + 6 days (7 total)**.
4. **Units:** default follows the hub; overridable in settings.
5. **Outputs:** the tile is primary; forecast data is also exposed as device attributes for automations (see §11).
6. **Dashboards:** local **and** cloud supported in v1.
7. **Instances:** parent app + one child app per instance; each child app creates its own child device (dashboards only show devices; the tile is an attribute). Multiple instances for different locations/styles.
8. **Extra fields** (current conditions, wind, humidity, sunrise/sunset, alerts): optional toggles, **off by default**; v2 candidates.
9. **Look:** selectable icon packs and dark/light mode (see §12). Default colors need not match the Home Restyle dashboard.
10. **Distribution:** Hubitat Package Manager once stable (later milestone).
11. **Last updated:** shown on the tile (format is a setting, hub time zone) and exposed as an attribute. Default since 0.2.6: **"Grosse Pointe Woods, Oct 29, 12:07 AM"** (location, then time; no "Updated" word), rendered **tiny** (small font, low emphasis).
12. **Icon license:** Rob has no preference. Because his icons are modified from erikflowers/weather-icons (SIL OFL 1.1), they ship under **OFL 1.1 with attribution** (required for derivatives); no reserved font name used. Repo code stays **Apache-2.0**.
13. **Attributes:** §11 list approved as a starting point; can be edited later.
14. **Defaults:** theme = **dark background, light icons**; icon pack = **Rob's colored set**. SVG source: the inline SVG definitions in lnjustin/Rachio-Community code (to be extracted at build time; not extracted yet), or files Rob provides.

## 11. Device attributes (approved starting point)
Per day *n* = 0..6 (only up to the configured day count):
- `day{n}Date` (yyyy-MM-dd), `day{n}Label` (e.g. "Today", "Wed")
- `day{n}High`, `day{n}Low` (number, in selected units)
- `day{n}Condition` (internal condition code, e.g. `rain`), `day{n}ConditionText`
- `day{n}Icon` (icon key/name)
- `day{n}PrecipChance` (%), `day{n}PrecipAmount` (in or mm)

Instance-level:
- `weatherTile` (HTML, < 1,024 chars)
- `lastUpdated` (ISO 8601), `lastUpdatedDisplay` (formatted, hub TZ)
- `source` (e.g. `open-meteo`, `nws`, `custom`), `status` (`ok` / `stale` / `error`), `statusMessage`
- `units` (`imperial` / `metric`), `latitude`, `longitude`, `locationLabel`
- Convenience: `todayHigh`, `todayLow`, `todayPrecipChance`, `tomorrowPrecipChance`
- v2 (only when toggled on): `temperature`, `humidity`, `windSpeed`, `windDirection`, `currentCondition`, `sunrise`, `sunset`, `alert`

Attributes are sent only on change.

## 12. Icon packs and theme
Candidates (licenses verified 2026-10-28):
| Pack | Style | License |
|---|---|---|
| erikflowers/weather-icons | monochrome glyphs | Icons SIL OFL 1.1; code MIT |
| Rob's filled set (from erikflowers) | color, filled | Rob's; OFL attribution to upstream |
| Meteocons (basmilius/weather-icons) | color, animated SVG | MIT |
| Makin-Things/weather-icons | color, animated SVG | MIT |
| amCharts animated weather icons | color, animated SVG | CC BY 4.0 (credit amCharts) |
| Tabler Icons (weather subset) | monochrome line | MIT |
| Lucide (weather subset) | monochrome line | ISC |

- Theme: dark / light (and possibly "auto"), driving text/background/icon tint colors.
- Animated SVGs may be heavy or unsupported in `<img>`; evaluate before enabling.
- **Future item (not started):** multicolor variant of erikflowers icons — white clouds, yellow sun, blue rain. Some glyphs are single combined shapes and need hand-splitting. OFL allows modification with attribution and no reserved-name reuse.

## 13. Remaining open questions
None at this time.

## Next up (Rob's to-dos)
- **Rename icon packs** to credit Rob for both sets: Rob's filled set and Rob's outline set, each modified from erikflowers/weather-icons.
- **Monochrome pack** built from the erikflowers font files.
- **More open-source packs as optional libraries:** Makin-Things weather-icons (MIT), Tabler Icons (MIT), Lucide (ISC), amCharts animated weather icons (CC BY 4.0, attribution required).
- Done in 0.2.9: Meteocons static and animated packs, with an Animate icons setting.
- **Night icons for today's slot only:** before sunrise or after sunset (from sunrise/sunset times; NWS "Tonight" period), show the night icon for today. Future days always use daytime icons.
