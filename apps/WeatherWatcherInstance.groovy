/**
 *  Weather Watcher (child app of Weather Watcher Manager)
 *
 *  Copyright 2026 Rob Cummings
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 *
 *  Portions adapted from Rachio-Community (https://github.com/lnjustin/Rachio-Community),
 *  Copyright 2018 Anthony Santilli, Copyright 2024 lnjustin, Apache License 2.0:
 *  the SVG-at-an-OAuth-endpoint approach, the forecast strip layout, the CSS-class color scheme,
 *  and the Open-Meteo request shape.
 *
 *  Weather icon SVG paths live in the libraries cummingsrob.WeatherWatcherIcons and
 *  cummingsrob.WeatherWatcherIconsOutline (libraries/) and are derived from erikflowers/weather-icons (https://github.com/erikflowers/weather-icons),
 *  licensed under the SIL Open Font License 1.1 (see ICONS-LICENSE.txt). The filled set is
 *  Rob Cummings' color-modified version. No Reserved Font Name is used.
 */

import groovy.transform.Field

#include cummingsrob.WeatherWatcherIcons
#include cummingsrob.WeatherWatcherIconsOutline
#include cummingsrob.WeatherWatcherIconsMeteocons
#include cummingsrob.WeatherWatcherIconsMeteoconsAnimated
#include cummingsrob.WeatherWatcherIconsMono

definition(
    name: "Weather Watcher",
    namespace: "cummingsrob",
    author: "Rob Cummings",
    description: "One Weather Watcher: a forecast-strip dashboard tile and a device with weather attributes for rules.",
    category: "Convenience",
    parent: "cummingsrob:Weather Watcher Manager",
    oauth: true,
    iconUrl: "", iconX2Url: "", iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/cummingsrob/hubitat-weather-watcher/main/apps/WeatherWatcherInstance.groovy"
)

preferences {
    page(name: "mainPage")
}

mappings {
    path("/forecast.svg")  { action: [GET: "serveSvg"] }
    path("/forecast.json") { action: [GET: "serveJson"] }
}

@Field static final String DRIVER_NAME = "Weather Watcher Device"
@Field static final String NS = "cummingsrob"
@Field static final Integer COL_W = 64
// In-memory SVG cache (per app id). Rebuilt from state.forecast after a hub reboot; keeps big SVG out of state.
// Entries are [hash: String, svg: String]; the hash is checked against state.svgHash before serving.
@Field static java.util.concurrent.ConcurrentHashMap<String, Map> SVG_CACHE = new java.util.concurrent.ConcurrentHashMap<String, Map>()
@Field static final Map SOURCES = ["open-meteo": "Open-Meteo (free, global, default)", "nws": "NWS / weather.gov (free, US only)",
                                   "custom": "Keyed provider (OpenWeatherMap One Call compatible)"]
// Icon pack provider methods (each comes from an #include'd library; see WeatherWatcherIcons header)
@Field static final List WW_ICON_PACK_PROVIDERS = ["wwIconPacks", "wwIconPacksOutline", "wwIconPacksMeteocons", "wwIconPacksMeteoconsAnimated", "wwIconPacksMono"]
@Field static final Map REFRESH_OPTS = ["15": "15 minutes", "30": "30 minutes", "60": "1 hour", "180": "3 hours"]
@Field static final Map THEMES = [
    dark : [bg: "#1a1a1a", text: "#ffffff", cloud: "#ffffff", light: "#f6d32d", precip: "#99c1f1", frozen: "#dceeff",
            high: "#ffffff", low: "#9aa0a6", rainText: "#99c1f1", muted: "#8a8a8a"],
    light: [bg: "#ffffff", text: "#202124", cloud: "#9aa0a6", light: "#f2b300", precip: "#3b82f6", frozen: "#7fb2e5",
            high: "#202124", low: "#6b7280", rainText: "#2563eb", muted: "#6b7280"]
]
// Internal condition -> [[icon keys, first one present in the pack wins], display text]
@Field static final Map CONDITIONS = [
    clear: [["sunny", "clear"], "Clear"],
    mostly_sunny: [["mostlysunny"], "Mostly sunny"],
    partly_cloudy: [["partlycloudy"], "Partly cloudy"],
    mostly_cloudy: [["mostlycloudy"], "Mostly cloudy"],
    cloudy: [["cloudy"], "Cloudy"],
    fog: [["fog"], "Fog"],
    haze: [["hazy", "fog"], "Haze"],
    drizzle: [["drizzle", "chancerain"], "Drizzle"],
    chance_rain: [["chancerain"], "Chance of rain"],
    showers: [["showers", "chancerain"], "Showers"],
    rain: [["rain"], "Rain"],
    chance_tstorms: [["chancetstorms"], "Chance of storms"],
    tstorms: [["tstorms"], "Thunderstorms"],
    chance_flurries: [["chanceflurries"], "Chance of flurries"],
    flurries: [["flurries"], "Flurries"],
    chance_snow: [["chancesnow"], "Chance of snow"],
    snow: [["snow"], "Snow"],
    chance_sleet: [["chancesleet"], "Chance of sleet"],
    sleet: [["sleet"], "Sleet / freezing rain"],
    hail: [["hail", "sleet"], "Hail"],
    wind: [["wind", "cloudy"], "Windy"],
    unknown: [["cloudy"], "Unknown"]
]

// ---------------------------------------------------------------- UI

def mainPage() {
    dynamicPage(name: "mainPage", title: "Weather Watcher", install: true, uninstall: true) {
        boolean hasToken = state.accessToken as boolean
        section("Status") {
            paragraph statusText()
            if (hasToken && state.svgHash) {
                paragraph "<img src='${svgUrl(settings.endpoint ?: 'local')}&v=${state.svgHash}' style='width:100%;max-width:520px;height:auto' alt='forecast preview'>"
            }
            input "btnRefresh", "button", title: "Refresh forecast now", width: 4
            if (hasToken) href name: "openTile", title: "Open tile image", url: svgUrl(settings.endpoint ?: 'local'), style: "external", width: 4
        }
        section("Name & source") {
            label title: "Name", required: true, defaultValue: "Weather Watcher", width: 6
            input "refreshMinutes", "enum", title: "Refresh interval", options: REFRESH_OPTS, defaultValue: "30", required: true, width: 6
            input "source", "enum", title: "Weather source", options: SOURCES, defaultValue: "open-meteo", required: true, submitOnChange: true
            if (settings.source == "custom") {
                input "customUrl", "text", title: "API base URL", required: true, width: 8,
                      defaultValue: "https://api.openweathermap.org/data/3.0/onecall",
                      description: "Called as BASE?lat=..&lon=..&units=..&appid=KEY&exclude=current,minutely,hourly,alerts"
                input "customKey", "password", title: "API key", required: true, width: 4
            }
        }
        section("Location") {
            paragraph "Default is the hub location (${location.latitude}, ${location.longitude})."
            input "useCustomLocation", "bool", title: "Use a custom location", defaultValue: false, submitOnChange: true, width: 6
            input "locationLabel", "text", title: "Location label (optional)", required: false, width: 6
            if (settings.useCustomLocation) {
                input "latitude", "decimal", title: "Latitude (-90..90)", range: "-90..90", required: true, width: 6
                input "longitude", "decimal", title: "Longitude (-180..180)", range: "-180..180", required: true, width: 6
            }
        }
        section("Forecast content") {
            input "numDays", "enum", title: "Days shown (including today)", options: ["1","2","3","4","5","6","7"], defaultValue: "7", required: true, width: 4
            input "units", "enum", title: "Units", options: ["hub": "Follow hub (${location.temperatureScale == 'C' ? 'metric' : 'imperial'})",
                  "imperial": "Imperial (°F, in)", "metric": "Metric (°C, mm)"], defaultValue: "hub", required: true, width: 4
            input "dayLabelStyle", "enum", title: "Day labels", options: ["todayShort": "Today, Thu, Fri...", "short": "Wed, Thu, Fri...",
                  "todayTomorrow": "Today, Tomorrow, Fri...", "date": "10/28, 10/29..."], defaultValue: "todayShort", width: 4
            input "showTemp", "bool", title: "Show high / low", defaultValue: true, width: 3
            input "showPrecipChance", "bool", title: "Show rain %", defaultValue: true, width: 3
            input "showPrecipAmount", "bool", title: "Show rain amount", defaultValue: true, width: 3
            input "hideZero", "bool", title: "Hide zero rain values", defaultValue: true, width: 3
        }
        section("Look") {
            input "theme", "enum", title: "Theme", options: ["dark": "Dark background, light icons", "light": "Light background, dark text"],
                  defaultValue: "dark", required: true, width: 6
            input "iconPack", "enum", title: "Icon pack", options: iconPacks().collectEntries { k, v -> [(k): v.label] },
                  defaultValue: "filled", required: true, submitOnChange: true, width: 6
            if (iconPacks()[settings.iconPack ?: "filled"]?.animated) {
                input "animateIcons", "bool", title: "Animate icons", defaultValue: true, width: 6
            }
            input "transparentBg", "bool", title: "Transparent background", defaultValue: false, width: 6
            input "cornerRadius", "number", title: "Corner radius (px)", range: "0..40", defaultValue: 8, width: 4
            input "fontScale", "number", title: "Font size (%)", range: "60..200", defaultValue: 100, width: 4
            input "fontFamily", "text", title: "Font family", defaultValue: "sans-serif", width: 4
        }
        section("Last-updated line") {
            input "showUpdated", "bool", title: "Show last-updated line", defaultValue: true, width: 4
            input "updatedShowPlace", "bool", title: "Start with the location", defaultValue: true, width: 4
            input "updatedFormat", "text", title: "Time format (Java date pattern)", defaultValue: "MMM d, h:mm a", width: 4
            paragraph "Sample: ${esc(fmtUpdated(now()))}"
        }
        section("Color overrides (optional, e.g. #ffcc00; blank = theme default)", hideable: true, hidden: true) {
            ["bg": "Background", "text": "Day text", "cloud": "Clouds", "light": "Sun", "precip": "Rain", "frozen": "Snow / ice",
             "high": "High temp", "low": "Low temp", "rainText": "Rain text"].each { k, v ->
                input "color_${k}", "text", title: v, required: false, width: 4
            }
        }
        section("Dashboard") {
            input "endpoint", "enum", title: "Tile URL", options: ["local": "Local (LAN only)", "cloud": "Cloud (works remotely)"],
                  defaultValue: "local", required: true
            if (hasToken) {
                paragraph "Add the device <b>${tileDeviceName()}</b> to a dashboard with template <b>Attribute</b>, attribute <b>weatherWatcher</b>. " +
                          "Attribute tiles keep working if the token changes; Image tiles that use a copied link do not."
                input "btnRegen", "button", title: "Regenerate access token (breaks copied links)"
            } else {
                paragraph "<b>Enable OAuth</b> for this app in Apps Code, then click Done."
            }
        }
        section("Advanced · Logging", hideable: true, hidden: true) {
            input "logLevel", "enum", title: "Logging", options: ["warn", "info", "debug"], defaultValue: "info", width: 4
        }
    }
}

private String statusText() {
    String st = (state.status ?: "waiting").toString().toUpperCase()
    String src = (SOURCES[settings.source ?: "open-meteo"] ?: "").toString().replaceAll(/\s*\(.*\)$/, "")
    List lines = ["<b>Status: ${st}</b>", "Source: ${esc(src)}"]
    if (state.lastSuccess) {
        Long last = state.lastSuccess as Long
        lines << "Last update: ${esc(new Date(last).format('MMM d, h:mm a', location.timeZone))}"
        Long mins = Math.max(0L, (long) Math.ceil(((last + refreshMin() * 60000L) - now()) / 60000.0d))
        lines << "Next refresh: ${mins > 0 ? "in about ${mins} min" : "due now"}"
    } else {
        lines << "No forecast yet"
    }
    if (state.statusMessage) lines << esc(state.statusMessage.toString())
    return lines.join("<br>")
}

def appButtonHandler(String btn) {
    if (btn == "btnRefresh") refreshForecast()
    else if (btn == "btnRegen") {
        state.accessToken = null
        ensureToken()
        renderAndPublish()
        logInfo "Access token regenerated; tile attribute updated"
    }
}

// ---------------------------------------------------------------- lifecycle

def installed() { initialize() }
def updated()   { unschedule(); initialize() }
def uninstalled() {
    SVG_CACHE.remove(app.id.toString())
    getChildDevices()?.each { deleteChildDevice(it.deviceNetworkId) }
}

def initialize() {
    ensureToken()
    ensureDevice()
    String key = locationKey()
    if (state.nwsKey != key) { state.nwsForecastUrl = null }
    if (state.placeName && !samePlace(state.placeKey)) { state.placeName = null; state.placeKey = null; state.placeTried = 0L }
    state.failCount = 0
    state.nextAllowed = 0
    Integer m = (settings.refreshMinutes ?: "30") as Integer
    if (m < 15) m = 15
    if (m == 15)       runEvery15Minutes("scheduledRefresh")
    else if (m == 30)  runEvery30Minutes("scheduledRefresh")
    else if (m == 60)  runEvery1Hour("scheduledRefresh")
    else               runEvery3Hours("scheduledRefresh")
    state.nextSunRedraw = null   // updated() unscheduled the sunrise/sunset redraw; renderAndPublish re-arms it
    schedule("0 1 0 * * ?", "renderAndPublish")   // roll days at midnight; refetches ~00:02 if data is from yesterday
    renderAndPublish()                             // settings changed -> re-render cached data
    runIn(2, "refreshForecast")
}

private void ensureToken() {
    if (state.accessToken) return
    try { createAccessToken() }
    catch (e) { log.warn "${app.label}: OAuth is not enabled for Weather Watcher in Apps Code" }
}

private String tileDni() { "WWATCH-${app.id}" }
private String tileDeviceName() { "${app.label ?: 'Weather Watcher'}" }

private def ensureDevice() {
    def d = getChildDevice(tileDni())
    if (!d) {
        try {
            d = addChildDevice(NS, DRIVER_NAME, tileDni(), [name: DRIVER_NAME, label: tileDeviceName(), isComponent: false])
        } catch (e) {
            log.error "${app.label}: could not create device. Is the '${DRIVER_NAME}' driver installed? ${e}"
        }
    } else if (d.label != tileDeviceName()) {
        d.setLabel(tileDeviceName())
    }
    return d
}

// ---------------------------------------------------------------- settings helpers

private BigDecimal lat() { (settings.useCustomLocation && settings.latitude != null) ? settings.latitude : location.latitude }
private BigDecimal lon() { (settings.useCustomLocation && settings.longitude != null) ? settings.longitude : location.longitude }
private String locationKey() { "${lat()},${lon()}" }
private String unitSys() {
    String u = settings.units ?: "hub"
    if (u == "hub") return (location.temperatureScale == "C") ? "metric" : "imperial"
    return u
}
private Integer daysShown() { Math.max(1, Math.min(7, (settings.numDays ?: "7") as Integer)) }
private Integer refreshMin() { (settings.refreshMinutes ?: "30") as Integer }
private boolean opt(String name, boolean dflt) { settings[name] == null ? dflt : (settings[name] as boolean) }

private Map colors() {
    Map c = [:] + THEMES[settings.theme ?: "dark"]
    c.keySet().toList().each { k ->
        String v = settings["color_${k}"]
        if (v && (v ==~ /#[0-9A-Fa-f]{3,8}/ || v ==~ /[a-zA-Z]+/)) c[k] = v
    }
    return c
}

// ---------------------------------------------------------------- fetching

def scheduledRefresh() {
    if (now() < (state.nextAllowed ?: 0L)) {
        logDebug "Backing off until ${new Date(state.nextAllowed as Long)}"
        return
    }
    refreshForecast()
}

def refreshForecast() {
    String src = settings.source ?: "open-meteo"
    logDebug "Fetching forecast from ${src}"
    if (!settings.locationLabel && !(state.placeName && samePlace(state.placeKey)) && now() - ((state.placeTried ?: 0L) as Long) > 86400000L) lookupPlace()
    if (src == "nws") fetchNws()
    else if (src == "custom") fetchCustom()
    else fetchOpenMeteo()
}

private void fetchOpenMeteo() {
    boolean metric = unitSys() == "metric"
    Map q = [latitude: lat(), longitude: lon(), timezone: "auto", forecast_days: 7,
             daily: "weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset",
             temperature_unit: metric ? "celsius" : "fahrenheit", precipitation_unit: metric ? "mm" : "inch"]
    asynchttpGet("handleOpenMeteo", [uri: "https://api.open-meteo.com", path: "/v1/forecast", query: q, contentType: "application/json", headers: ["Accept": "application/json"], timeout: 20])
}

def handleOpenMeteo(resp, data) {
    if (!responseOk(resp, "Open-Meteo")) return
    try {
        def d = respJson(resp)?.daily
        List days = []
        d.time.eachWithIndex { t, i ->
            Integer code = d.weathercode[i] as Integer
            days << [date: t, high: d.temperature_2m_max[i], low: d.temperature_2m_min[i],
                     pop: d.precipitation_probability_max ? d.precipitation_probability_max[i] : null,
                     amt: d.precipitation_sum ? d.precipitation_sum[i] : null,
                     cond: wmoToCondition(code), raw: "WMO ${code}",
                     sunrise: d.sunrise ? d.sunrise[i] : null, sunset: d.sunset ? d.sunset[i] : null]
        }
        onForecast(days, "open-meteo")
    } catch (e) { onFailure("Open-Meteo parse error: ${e}") }
}

private String wmoToCondition(Integer c) {
    switch (c) {
        case [0]: return "clear"
        case [1]: return "mostly_sunny"
        case [2]: return "partly_cloudy"
        case [3]: return "cloudy"
        case [45, 48]: return "fog"
        case [51, 53, 55]: return "drizzle"
        case [61]: return "chance_rain"
        case [80, 81]: return "showers"
        case [63, 65, 82]: return "rain"
        case [56, 57, 66, 67]: return "sleet"
        case [71, 77, 85]: return "chance_flurries"
        case [73, 75, 86]: return "snow"
        case [95]: return "tstorms"
        case [96, 99]: return "hail"
    }
    logInfo "Unknown WMO code ${c}"
    return "unknown"
}

/** Parse a response body as JSON whatever form the hub hands it over in (Map/List, String, bytes, or anything with getText()). */
private Object respJson(resp) {
    def d = null
    try { d = resp.getData() } catch (ignored) { }
    if (d instanceof Map || d instanceof List) return d
    String txt = bodyText(d)
    if (!txt) {
        try { return resp.getJson() } catch (ignored) { }
        throw new Exception("empty response body")
    }
    txt = txt.trim()
    if (txt.startsWith("\uFEFF")) txt = txt.substring(1).trim()
    if (!(txt.startsWith("{") || txt.startsWith("["))) {
        try { def j = resp.getJson(); if (j instanceof Map || j instanceof List) return j } catch (ignored) { }
        throw new Exception("response is not JSON (starts: ${txt.take(60)})")
    }
    return new groovy.json.JsonSlurper().parseText(txt)
}

private String bodyText(d) {
    if (d == null) return null
    if (d instanceof String) return d
    try { if (d.respondsTo("getText")) return d.getText() } catch (ignored) { }
    try { return new String(d, "UTF-8") } catch (ignored) { }
    return d.toString()
}

private Map nwsHeaders() { ["User-Agent": "(hubitat-weather-watcher, github.com/cummingsrob/hubitat-weather-watcher)", "Accept": "application/geo+json, application/json"] }

private void fetchNws() {
    if (!state.nwsForecastUrl || state.nwsKey != locationKey()) {
        String p = "/points/${(lat() as BigDecimal).setScale(4, BigDecimal.ROUND_HALF_UP)},${(lon() as BigDecimal).setScale(4, BigDecimal.ROUND_HALF_UP)}"
        asynchttpGet("handleNwsPoints", [uri: "https://api.weather.gov", path: p, contentType: "application/json", headers: nwsHeaders(), timeout: 20])
    } else {
        fetchNwsForecast()
    }
}

def handleNwsPoints(resp, data) {
    if (!responseOk(resp, "NWS points")) return
    try {
        String url = respJson(resp)?.properties?.forecast
        if (!url) { onFailure("NWS: no forecast URL for this location (US only)"); return }
        state.nwsForecastUrl = url
        try {
            def rl = respJson(resp)?.properties?.relativeLocation?.properties
            if (rl?.city) { rememberPlace(rl.city.toString()) }
        } catch (ignored) { }
        state.nwsKey = locationKey()
        fetchNwsForecast()
    } catch (e) { onFailure("NWS points parse error: ${e}") }
}

private void fetchNwsForecast() {
    Map q = unitSys() == "metric" ? [units: "si"] : [units: "us"]
    asynchttpGet("handleNwsForecast", [uri: state.nwsForecastUrl, query: q, contentType: "application/json", headers: nwsHeaders(), timeout: 25])
}

def handleNwsForecast(resp, data) {
    if (!responseOk(resp, "NWS forecast")) {
        if (resp?.status == 404) state.nwsForecastUrl = null   // gridpoint moved; re-resolve next time
        return
    }
    try {
        def periods = respJson(resp)?.properties?.periods
        Map byDate = [:]
        periods.each { p ->
            String date = p.startTime?.substring(0, 10)
            Map d = byDate[date] ?: [date: date, high: null, low: null, pop: null, amt: null, cond: null, raw: null]
            def pop = p.probabilityOfPrecipitation?.value
            if (pop != null) d.pop = Math.max((d.pop ?: 0) as Integer, pop as Integer)
            if (p.isDaytime) {
                d.high = p.temperature
                d.cond = nwsToCondition(p.shortForecast as String)
                d.raw = p.shortForecast
            } else {
                d.low = p.temperature
                if (!d.nightCond) { d.nightCond = nwsToCondition(p.shortForecast as String); d.nightRaw = p.shortForecast }   // NWS "Overnight"/"Tonight"
                if (!d.cond) { d.cond = nwsToCondition(p.shortForecast as String); d.raw = p.shortForecast }
            }
            byDate[date] = d
        }
        onForecast(byDate.values().toList().sort { it.date }, "nws")
    } catch (e) { onFailure("NWS forecast parse error: ${e}") }
}

private String nwsToCondition(String s) {
    String t = (s ?: "").toLowerCase()
    boolean chance = t.contains("chance") || t.contains("slight") || t.contains("isolated") || t.contains("scattered")
    if (t.contains("thunder"))                                    return chance ? "chance_tstorms" : "tstorms"
    if (t.contains("hail"))                                       return "hail"
    if (t.contains("sleet") || t.contains("freezing") || t.contains("ice")) return chance ? "chance_sleet" : "sleet"
    if (t.contains("flurr"))                                      return chance ? "chance_flurries" : "flurries"
    if (t.contains("snow"))                                       return chance ? "chance_snow" : "snow"
    if (t.contains("drizzle"))                                    return "drizzle"
    if (t.contains("shower"))                                     return chance ? "chance_rain" : "showers"
    if (t.contains("rain"))                                       return chance ? "chance_rain" : "rain"
    if (t.contains("fog"))                                        return "fog"
    if (t.contains("haze") || t.contains("smoke") || t.contains("dust")) return "haze"
    if (t.contains("mostly cloudy"))                              return "mostly_cloudy"
    if (t.contains("partly sunny") || t.contains("partly cloudy")) return "partly_cloudy"
    if (t.contains("mostly sunny") || t.contains("mostly clear")) return "mostly_sunny"
    if (t.contains("cloudy") || t.contains("overcast"))           return "cloudy"
    if (t.contains("sunny") || t.contains("clear") || t.contains("fair")) return "clear"
    if (t.contains("wind") || t.contains("breezy"))               return "wind"
    if (t) logInfo "Unmapped NWS forecast text '${s}'"
    return "unknown"
}

private void fetchCustom() {
    if (!settings.customUrl || !settings.customKey) { onFailure("Custom provider needs an API base URL and key"); return }
    Map q = [lat: lat(), lon: lon(), units: unitSys(), appid: settings.customKey, exclude: "current,minutely,hourly,alerts"]
    asynchttpGet("handleCustom", [uri: settings.customUrl, query: q, contentType: "application/json", headers: ["Accept": "application/json"], timeout: 20])
}

def handleCustom(resp, data) {
    if (!responseOk(resp, "Custom provider")) return
    try {
        def j = respJson(resp)
        TimeZone tz = j.timezone ? TimeZone.getTimeZone(j.timezone as String) : location.timeZone
        boolean metric = unitSys() == "metric"
        List days = j.daily.collect { d ->
            Integer id = d.weather ? (d.weather[0].id as Integer) : null
            BigDecimal mm = ((d.rain ?: 0) as BigDecimal) + ((d.snow ?: 0) as BigDecimal)   // OWM reports mm in all unit systems
            [date: new Date((d.dt as Long) * 1000L).format("yyyy-MM-dd", tz), high: d.temp?.max, low: d.temp?.min,
             pop: d.pop != null ? Math.round((d.pop as BigDecimal) * 100) : null,
             amt: metric ? mm : (mm / 25.4), cond: owmToCondition(id), raw: d.weather ? d.weather[0].description : null]
        }
        onForecast(days, "custom")
    } catch (e) { onFailure("Custom provider parse error (expects OpenWeatherMap One Call 'daily' format): ${e}") }
}

private String owmToCondition(Integer id) {
    if (id == null) return "unknown"
    if (id >= 200 && id < 300) return "tstorms"
    if (id >= 300 && id < 400) return "drizzle"
    if (id == 500) return "chance_rain"
    if (id == 511) return "sleet"
    if (id >= 520 && id < 600) return "showers"
    if (id >= 501 && id < 600) return "rain"
    if (id == 600) return "flurries"
    if (id >= 611 && id <= 616) return "sleet"
    if (id >= 600 && id < 700) return "snow"
    if (id == 741) return "fog"
    if (id >= 700 && id < 800) return (id == 771 || id == 781) ? "wind" : "haze"
    if (id == 800) return "clear"
    if (id == 801) return "mostly_sunny"
    if (id == 802) return "partly_cloudy"
    if (id == 803) return "mostly_cloudy"
    if (id == 804) return "cloudy"
    return "unknown"
}

private boolean responseOk(resp, String what) {
    if (resp == null || resp.hasError() || (resp.status as Integer) >= 300) {
        Integer code = resp?.status as Integer
        String msg = "${what} failed: HTTP ${code ?: '?'} ${resp?.hasError() ? resp.getErrorMessage() : ''}".trim()
        onFailure(msg, code == 429)
        return false
    }
    return true
}

private void onFailure(String msg, boolean rateLimited = false) {
    Integer n = ((state.failCount ?: 0) as Integer) + 1
    state.failCount = n
    long backoffMin = Math.min(refreshMin() * (1L << Math.min(n, 4)), 360L)
    if (rateLimited) backoffMin = Math.max(backoffMin, 60L)
    state.nextAllowed = now() + backoffMin * 60000L
    log.warn "${app.label}: ${msg} (failure ${n}; next try in ~${backoffMin} min; keeping last good data)"
    state.status = state.forecast ? "stale" : "error"
    state.statusMessage = msg.take(200)
    renderAndPublish()
}

private void onForecast(List days, String src) {
    String today = new Date().format("yyyy-MM-dd", location.timeZone)
    List keep = days.findAll { it.date && it.date >= today }.take(7).collect { d ->
        [date: d.date, high: num(d.high, 0), low: num(d.low, 0), pop: num(d.pop, 0),
         amt: num(d.amt, unitSys() == "metric" ? 1 : 2), cond: d.cond ?: "unknown", raw: d.raw?.toString(),
         nightCond: d.nightCond, nightRaw: d.nightRaw?.toString(), sunrise: d.sunrise?.toString(), sunset: d.sunset?.toString()]
    }
    if (!keep) { onFailure("${src}: no forecast days returned"); return }
    state.failCount = 0
    state.nextAllowed = 0
    state.forecast = keep
    state.forecastSource = src
    state.forecastUnits = unitSys()
    state.lastSuccess = now()
    state.status = "ok"
    state.statusMessage = "${keep.size()} days from ${src}"
    logInfo "Forecast updated from ${src} (${keep.size()} days)"
    renderAndPublish()
}

private BigDecimal num(v, int scale) {
    if (v == null || v == "") return null
    try { return (v as BigDecimal).setScale(scale, BigDecimal.ROUND_HALF_UP) } catch (e) { return null }
}

// ---------------------------------------------------------------- rendering + publishing

/** Forecast days from today on (hub time zone); drops days that ended since the last fetch. */
private List currentDays() {
    String today = new Date(now()).format("yyyy-MM-dd", location.timeZone)
    return (state.forecast ?: []).findAll { it?.date && it.date >= today }
}

/** After local midnight the cached forecast starts with yesterday: fetch again (once per day, respects backoff). */
private void refreshIfNewDay() {
    if (!state.lastSuccess) return
    String today = new Date(now()).format("yyyy-MM-dd", location.timeZone)
    String fetched = new Date(state.lastSuccess as Long).format("yyyy-MM-dd", location.timeZone)
    if (fetched < today && state.newDayRefresh != today) {
        state.newDayRefresh = today
        runIn(60, "scheduledRefresh")
    }
}

def renderAndPublish() {
    refreshIfNewDay()
    List days = currentDays()
    scheduleSunRedraw(days)
    if (state.lastSuccess && (now() - (state.lastSuccess as Long)) > refreshMin() * 3L * 60000L && state.status == "ok") {
        state.status = "stale"
        state.statusMessage = "No successful update since ${fmtUpdated(state.lastSuccess as Long)}"
    }
    String svg = buildSvg(days)
    String hash = Integer.toHexString(svg.hashCode())
    SVG_CACHE[app.id.toString()] = [hash: hash, svg: svg]
    if (hash != state.svgHash) state.svgHash = hash
    publishAttributes(days)
}

private String dayLabel(String date, int idx) {
    String style = settings.dayLabelStyle ?: "todayShort"
    Date d = Date.parse("yyyy-MM-dd", date)
    String today = new Date().format("yyyy-MM-dd", location.timeZone)
    String tomorrow = new Date(now() + 86400000L).format("yyyy-MM-dd", location.timeZone)
    if (style == "date") return d.format("M/d")
    if (style != "short" && date == today) return "Today"
    if (style == "todayTomorrow" && date == tomorrow) return "Tomorrow"
    return d.format("EEE")
}

private String esc(s) { (s ?: "").toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;") }

@Field static final String WW_OLD_UPDATED_FORMAT = "'Updated' MMM d, h:mm a"
@Field static final String WW_UPDATED_FORMAT = "MMM d, h:mm a"

private String fmtUpdated(Long ms) {
    if (!ms) return ""
    String f = settings.updatedFormat ?: WW_UPDATED_FORMAT
    if (f == WW_OLD_UPDATED_FORMAT) f = WW_UPDATED_FORMAT      // old default -> new default
    String t
    try { t = new Date(ms).format(f, location.timeZone) }
    catch (e) { t = new Date(ms).format(WW_UPDATED_FORMAT, location.timeZone) }
    if (settings.updatedShowPlace == false) return t
    String p = placeLabel()
    if (p && p.length() > 28) p = p.take(27) + "\u2026"
    return p ? "${p}, ${t}" : t
}

/** Same place as the current lat/lon? Compares at ~100 m so number formatting differences don't matter. */
private boolean samePlace(k) {
    if (!k) return false
    try {
        List p = k.toString().split(",")
        return Math.abs((p[0] as BigDecimal) - (lat() as BigDecimal)) < 0.001 && Math.abs((p[1] as BigDecimal) - (lon() as BigDecimal)) < 0.001
    } catch (e) { return false }
}

/** Save a looked-up place name; lastPlaceName survives resets so a pending lookup never falls back to the hub name. */
private void rememberPlace(String p) {
    state.placeName = p; state.placeKey = locationKey()
    state.lastPlaceName = p; state.lastPlaceKey = locationKey()
}

/** Location shown on the tile: the label setting, else a place name looked up from lat/lon, else the hub name. */
private String placeLabel() {
    if (settings.locationLabel) return settings.locationLabel.toString()
    if (state.placeName && samePlace(state.placeKey)) return state.placeName.toString()
    if (state.lastPlaceName && samePlace(state.lastPlaceKey)) return state.lastPlaceName.toString()
    return location?.name ?: ""
}

/** Reverse-geocode lat/lon to a place name once per location (cached in state):
 *  NWS points relativeLocation (US, most local), else BigDataCloud's free client endpoint. */
private void lookupPlace() {
    if (settings.locationLabel) return
    state.placeTried = now()
    try {
        String p = "/points/${(lat() as BigDecimal).setScale(4, BigDecimal.ROUND_HALF_UP)},${(lon() as BigDecimal).setScale(4, BigDecimal.ROUND_HALF_UP)}"
        asynchttpGet("handlePlaceNws", [uri: "https://api.weather.gov", path: p, contentType: "application/json", headers: nwsHeaders(), timeout: 20])
    } catch (e) { lookupPlaceWorld() }
}

def handlePlaceNws(resp, data) {
    try {
        if (resp != null && !resp.hasError() && (resp.status as Integer) < 300) {
            def rl = respJson(resp)?.properties?.relativeLocation?.properties
            if (rl?.city) { rememberPlace(rl.city.toString()); renderAndPublish(); return }
        }
    } catch (ignored) { }
    lookupPlaceWorld()
}

private void lookupPlaceWorld() {
    try {
        asynchttpGet("handlePlace", [uri: "https://api.bigdatacloud.net", path: "/data/reverse-geocode-client",
            query: [latitude: lat(), longitude: lon(), localityLanguage: "en"], contentType: "application/json",
            headers: ["Accept": "application/json"], timeout: 20])
    } catch (e) { log.debug "Weather Watcher: place lookup not started: ${e}" }
}

def handlePlace(resp, data) {
    try {
        if (resp == null || resp.hasError() || (resp.status as Integer) >= 300) return
        def j = respJson(resp)
        String p = (j?.city ?: j?.locality ?: j?.principalSubdivision ?: "").toString().trim()
        if (p) { rememberPlace(p); renderAndPublish() }
    } catch (e) { log.debug "Weather Watcher: place lookup failed: ${e}" }
}

private String buildSvg(List allDays) {
    Map c = colors()
    List days = allDays.take(daysShown())
    int n = Math.max(days.size(), 1)
    BigDecimal fs = ((settings.fontScale ?: 100) as BigDecimal) / 100
    boolean metric = (state.forecastUnits ?: unitSys()) == "metric"
    boolean showT = opt("showTemp", true), showP = opt("showPrecipChance", true), showA = opt("showPrecipAmount", true)
    boolean hideZ = opt("hideZero", true), showU = opt("showUpdated", true)
    boolean anyP = showP && days.any { it.pop != null && (!hideZ || (it.pop as BigDecimal) > 0) }
    boolean anyA = showA && days.any { it.amt != null && (!hideZ || (it.amt as BigDecimal) > 0) }

    int w = COL_W * n
    int y = 4
    int labelY = y + 12
    int iconY = labelY + 2
    int tempY = iconY + 64 + 4
    int popY = tempY + (showT ? 13 : 0)
    int amtY = popY + (anyP ? 12 : 0)
    int h = amtY + (anyA ? 12 : 0) - 8
    int updY = h + 9
    if (showU) h += 11
    h += 4

    String font = esc(settings.fontFamily ?: "sans-serif").replace(";", "")
    String bg = opt("transparentBg", false) ? "none" : c.bg
    Integer rx = (settings.cornerRadius != null ? settings.cornerRadius : 8) as Integer
    def fmt = { BigDecimal v -> v == null ? "" : v.stripTrailingZeros().toPlainString() }

    StringBuilder sb = new StringBuilder(64000)
    sb << '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="100%" viewBox="0 0 ' << w << ' ' << h << '" preserveAspectRatio="xMidYMid meet">'
    sb << '<style>.cloud{fill:' << c.cloud << '}.light{fill:' << c.light << '}.precip{fill:' << c.precip << '}.frozenPrecip{fill:' << c.frozen << '}.mono{fill:' << c.text << '}'
    sb << 'text{font-family:' << font << '}.day{fill:' << c.text << ';font-size:' << (11 * fs) << 'px;text-transform:uppercase}'
    sb << '.hi{fill:' << c.high << '}.lo{fill:' << c.low << '}.t{font-size:' << (12 * fs) << 'px}.p{fill:' << c.rainText << ';font-size:' << (10 * fs) << 'px}'
    sb << '.u{fill:' << c.muted << ';font-size:' << (6.5 * fs) << 'px}</style>'
    sb << '<rect x="0" y="0" width="' << w << '" height="' << h << '" rx="' << rx << '" fill="' << bg << '"/>'

    if (!days) {
        sb << '<text x="' << (w / 2) << '" y="' << (h / 2) << '" text-anchor="middle" class="day">' << esc(state.statusMessage ?: "Waiting for forecast") << '</text>'
    }
    Map icons = packIcons()
    days.eachWithIndex { d, i ->
        int cx = COL_W * i + COL_W.intdiv(2)
        sb << '<g>'
        sb << '<text x="' << cx << '" y="' << labelY << '" text-anchor="middle" class="day">' << esc(dayLabel(d.date as String, i)) << '</text>'
        boolean night = (i == 0 && isNightNow(d))      // night icons for today's slot only
        List cond = CONDITIONS[(night && d.nightCond) ? d.nightCond : d.cond] ?: CONDITIONS.unknown
        String icon = icons[iconKey(cond, icons, night)]
        String tip = night && d.nightCond ? (d.nightRaw ?: cond[1]) : (d.raw ?: cond[1])
        sb << '<g transform="translate(' << (COL_W * i + 2) << ',' << iconY << ')"><title>' << esc(tip) << '</title>' << icon << '</g>'
        if (showT) {
            sb << '<text x="' << cx << '" y="' << tempY << '" text-anchor="middle" class="t">'
            if (d.high != null) sb << '<tspan class="hi">' << fmt(d.high as BigDecimal) << '°</tspan>'
            if (d.low != null) sb << '<tspan class="lo"' << (d.high != null ? ' dx="3"' : '') << '>' << fmt(d.low as BigDecimal) << '°</tspan>'
            sb << '</text>'
        }
        if (anyP && d.pop != null && (!hideZ || (d.pop as BigDecimal) > 0)) {
            sb << '<text x="' << cx << '" y="' << popY << '" text-anchor="middle" class="p">' << fmt(d.pop as BigDecimal) << '%</text>'
        }
        if (anyA && d.amt != null && (!hideZ || (d.amt as BigDecimal) > 0)) {
            sb << '<text x="' << cx << '" y="' << amtY << '" text-anchor="middle" class="p">' << fmt(d.amt as BigDecimal) << (metric ? ' mm' : '"') << '</text>'
        }
        sb << '</g>'
    }
    if (showU && state.lastSuccess) {
        String u = fmtUpdated(state.lastSuccess as Long)
        if (state.status == "stale" || state.status == "error") u += " (stale)"
        sb << '<text x="' << (w - 4) << '" y="' << updY << '" text-anchor="end" class="u">' << esc(u) << '</text>'
    }
    sb << '</svg>'
    return sb.toString()
}

private String svgUrl(String which) {
    String base = (which == "cloud") ? getFullApiServerUrl() : getFullLocalApiServerUrl()
    return "${base}/forecast.svg?access_token=${state.accessToken}"
}

private void publishAttributes(List allDays) {
    def dev = ensureDevice()
    if (!dev) return
    List days = allDays.take(daysShown())
    Map a = [:]
    if (state.accessToken) {
        String src = "${svgUrl(settings.endpoint ?: 'local')}&v=${state.svgHash}"
        String tile = "<img src='${src}' style='width:100%;height:auto;display:block' alt='forecast'>"
        if (tile.length() > 1024) log.warn "${app.label}: tile attribute is ${tile.length()} chars (limit 1024)"
        a.weatherWatcher = tile
    } else {
        a.weatherWatcher = "Enable OAuth for Weather Watcher"
    }
    if (state.lastSuccess) {
        a.lastUpdated = new Date(state.lastSuccess as Long).format("yyyy-MM-dd'T'HH:mm:ssXXX", location.timeZone)
        a.lastUpdatedDisplay = fmtUpdated(state.lastSuccess as Long)
    }
    a.source = state.forecastSource ?: (settings.source ?: "open-meteo")
    a.status = state.status ?: "error"
    a.statusMessage = state.statusMessage ?: "No data yet"
    a.units = state.forecastUnits ?: unitSys()
    a.latitude = lat()
    a.longitude = lon()
    a.locationLabel = placeLabel()
    days.eachWithIndex { d, i ->
        List cond = CONDITIONS[d.cond] ?: CONDITIONS.unknown
        a["day${i}Date"] = d.date
        a["day${i}Label"] = dayLabel(d.date as String, i)
        a["day${i}High"] = d.high
        a["day${i}Low"] = d.low
        a["day${i}Condition"] = d.cond
        a["day${i}ConditionText"] = d.raw ?: cond[1]
        a["day${i}Icon"] = iconKey(cond, packIcons())
        a["day${i}PrecipChance"] = d.pop
        a["day${i}PrecipAmount"] = d.amt
    }
    if (days) {
        a.todayHigh = days[0].high
        a.todayLow = days[0].low
        a.todayPrecipChance = days[0].pop
        if (days.size() > 1) a.tomorrowPrecipChance = days[1].pop
    }
    dev.applyAttributes(a)
}

// ---------------------------------------------------------------- icon packs

private Map iconPacks() {
    Map all = [:]
    WW_ICON_PACK_PROVIDERS.each { String m ->
        try { all.putAll("${m}"() as Map) } catch (e) { log.warn "Icon pack provider ${m}() missing: ${e}" }
    }
    return all
}

private Map packIcons() {
    Map packs = iconPacks()
    Map p = (packs[settings.iconPack ?: "filled"] ?: packs.filled ?: packs.values().first()) ?: [:]
    Map icons = p.icons ?: [:]
    if (p.animated && settings.animateIcons == false) icons = icons.collectEntries { k, v -> [(k): stripAnimation(v as String)] }
    return icons
}

/** Remove SMIL animation elements (animate, animateTransform, animateMotion, set) so the icon shows its first frame. */
private String stripAnimation(String svg) {
    if (!svg) return svg
    return svg.replaceAll(/<(animate|animateTransform|animateMotion|set)\b[^>]*\/>/, "")
              .replaceAll(/(?s)<(animate|animateTransform|animateMotion|set)\b[^>]*>.*?<\/\1>/, "")
}

private String iconKey(List cond, Map icons, boolean night = false) {
    if (night) {
        String n = cond[0].find { icons.containsKey("nt_" + it) }
        if (n) return "nt_" + n                     // pack has a night variant; otherwise use the day icon
    }
    String k = cond[0].find { icons.containsKey(it) }
    if (k) return k
    return icons.containsKey("cloudy") ? "cloudy" : icons.keySet().first()
}

// ---------------------------------------------------------------- night (today's slot)

/** Today's sunrise/sunset: from the forecast (Open-Meteo, local time) or the hub's own sun times. */
private Map sunTimes(Map d) {
    Map r = [:]
    try {
        if (d?.sunrise && d?.sunset) {
            r.sunrise = Date.parse("yyyy-MM-dd'T'HH:mm", d.sunrise.toString().take(16), location.timeZone)
            r.sunset = Date.parse("yyyy-MM-dd'T'HH:mm", d.sunset.toString().take(16), location.timeZone)
        }
    } catch (e) { r = [:] }
    if (!r.sunrise || !r.sunset) {
        try { Map h = getSunriseAndSunset(); r = [sunrise: h?.sunrise, sunset: h?.sunset] } catch (e) { r = [:] }
    }
    return r
}

private boolean isNightNow(Map d) {
    if (!d || d.date != new Date().format("yyyy-MM-dd", location.timeZone)) return false
    Map t = sunTimes(d)
    if (!t.sunrise || !t.sunset) return false
    long n = now()
    return n < (t.sunrise as Date).time || n >= (t.sunset as Date).time
}

/** Redraw at today's next sunrise or sunset so the night icon switches on time. */
private void scheduleSunRedraw(List days) {
    try {
        Map t = sunTimes(days ? days[0] as Map : null)
        Date next = [t.sunrise, t.sunset].findAll { it && (it as Date).time > now() }.min { (it as Date).time } as Date
        if (next && state.nextSunRedraw != next.time) {
            state.nextSunRedraw = next.time
            runOnce(new Date(next.time + 30000L), "sunRedraw", [overwrite: true])
        }
    } catch (e) { log.debug "${app.label}: sun redraw not scheduled: ${e}" }
}

def sunRedraw() { state.nextSunRedraw = null; renderAndPublish() }

// ---------------------------------------------------------------- endpoints

def serveSvg() {
    Map c = SVG_CACHE[app.id.toString()]
    String svg = c?.svg
    if (!svg || c?.hash != state.svgHash) {         // missing, or stale after a code update/reboot: redraw from stored forecast
        svg = buildSvg(currentDays())               // cached data only, never fetches
        SVG_CACHE[app.id.toString()] = [hash: state.svgHash, svg: svg]   // now current for this version
    }
    render contentType: "image/svg+xml", data: svg, status: 200
}

def serveJson() {
    Map out = [source: state.forecastSource, units: state.forecastUnits, status: state.status, statusMessage: state.statusMessage,
               lastSuccess: state.lastSuccess, days: currentDays()]
    render contentType: "application/json", data: groovy.json.JsonOutput.toJson(out), status: 200
}

// ---------------------------------------------------------------- logging

private void logInfo(String m)  { if ((settings.logLevel ?: "info") in ["info", "debug"]) log.info "${app.label}: ${m}" }
private void logDebug(String m) { if (settings.logLevel == "debug") log.debug "${app.label}: ${m}" }
