# Requirements (draft)

Status: **draft for review**. No code yet.

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
- Units: temperature °F/°C (default from hub), precipitation in/mm.
- Days shown (e.g. 3–7) and the start day (today/tomorrow).
- Refresh/cache interval (default 30 min; min 15 min).
- Icon set: Outline (default), Filled (if supplied, see §9).
- Colors (text, highs/lows, rain, background, icon tint), font family/size, background transparent/solid, corner radius.
- Show/hide rain % and rain amount; hide zero values.
- Day labels: weekday short/long, "Today"/"Tomorrow", date format.
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
- **Filled icons:** designed by Rob (repo owner). Needs confirmation that Rob can supply the SVGs and grants their use here.
- **Excluded:** the Freepik/flaticon-derived rain drop, icicle and rain sensor icons, unless properly licensed.

## 10. Open questions for Rob
1. Filled icons: can you supply the SVG files, and under what license (Apache 2.0, OFL, other)? The Rachio file credits Ashley Jager (modified by lnjustin); please confirm your own versions are separate.
2. Which sources must be in v1: Open-Meteo + NWS only, or OpenWeatherMap/Tomorrow.io too?
3. Default number of days, and should "today" be included?
4. Units default: follow the hub, or always °F and inches?
5. Is a child device with attributes wanted, or only the tile/image?
6. Local only, or cloud dashboard support needed in v1?
7. Multiple instances (e.g. several locations or styles) as child apps?
8. Any extra fields (current conditions, wind, humidity, sunrise/sunset, alerts)?
9. Default colors/fonts: match the Home Restyle dashboard theme?
10. Distribute via Hubitat Package Manager, or private use only?
