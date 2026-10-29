# Changelog

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
