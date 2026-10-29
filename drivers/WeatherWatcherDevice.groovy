/**
 *  Weather Watcher Device (driver)
 *
 *  Copyright 2026 Rob Cummings. Apache License, Version 2.0.
 *  Created and updated by the "Weather Watcher" child app; do not add manually.
 */

metadata {
    definition(name: "Weather Watcher Device", namespace: "cummingsrob", author: "Rob Cummings",
               importUrl: "https://raw.githubusercontent.com/cummingsrob/hubitat-weather-watcher/main/drivers/WeatherWatcherDevice.groovy") {
        capability "Sensor"
        capability "Refresh"

        attribute "weatherWatcher", "string"
        attribute "lastUpdated", "string"
        attribute "lastUpdatedDisplay", "string"
        attribute "source", "string"
        attribute "status", "enum", ["ok", "stale", "error"]
        attribute "statusMessage", "string"
        attribute "units", "enum", ["imperial", "metric"]
        attribute "latitude", "number"
        attribute "longitude", "number"
        attribute "locationLabel", "string"
        attribute "todayHigh", "number"
        attribute "todayLow", "number"
        attribute "todayPrecipChance", "number"
        attribute "tomorrowPrecipChance", "number"
        (0..6).each { n ->
            attribute "day${n}Date", "string"
            attribute "day${n}Label", "string"
            attribute "day${n}High", "number"
            attribute "day${n}Low", "number"
            attribute "day${n}Condition", "string"
            attribute "day${n}ConditionText", "string"
            attribute "day${n}Icon", "string"
            attribute "day${n}PrecipChance", "number"
            attribute "day${n}PrecipAmount", "number"
        }
    }
}

def installed() { log.info "${device.displayName} installed" }
def updated()   { }

def refresh() {
    parent?.refreshForecast()
}

/** Called by the parent app with a map of attribute name -> value. Sends events only on change. */
def applyAttributes(Map attrs) {
    attrs?.each { String k, v ->
        String nv = (v == null) ? null : v.toString()
        if (nv == null) return
        if (device.currentValue(k)?.toString() != nv) {
            sendEvent(name: k, value: v)
        }
    }
}
