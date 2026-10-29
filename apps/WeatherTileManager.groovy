/**
 *  Weather Watcher Manager (parent app; was "Weather Tile Manager" before 0.2.12)
 *
 *  Copyright 2026 Rob Cummings
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 *
 *  Design inspired by Rachio-Community (c) 2018 Anthony Santilli, (c) 2024 lnjustin, Apache-2.0.
 */

definition(
    name: "Weather Watcher Manager",
    namespace: "cummingsrob",
    author: "Rob Cummings",
    description: "Multi-day forecast strip tiles for Hubitat dashboards (Open-Meteo, NWS, or keyed provider).",
    category: "Convenience",
    singleInstance: true,
    installOnOpen: true,
    iconUrl: "", iconX2Url: "", iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/cummingsrob/hubitat-weather-tile/main/apps/WeatherTileManager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Weather Watcher Manager", install: true, uninstall: true) {
        section("Weather Watchers") {
            paragraph "Each Weather Watcher is its own child app with its own device (dashboard tile plus weather attributes for rules). Add one per location or style."
            app(name: "tiles", appName: "Weather Watcher", namespace: "cummingsrob", title: "Add a Weather Watcher", multiple: true)
        }
        section("About") {
            paragraph "<small>Apache-2.0. Weather icons derived from erikflowers/weather-icons (SIL OFL 1.1). " +
                      "SVG approach adapted from Rachio-Community (lnjustin, Apache-2.0).</small>"
        }
    }
}

def installed() { initialize() }
def updated()   { initialize() }
def initialize() {
    log.info "Weather Watcher Manager: ${getChildApps()?.size() ?: 0} watcher(s)"
}
