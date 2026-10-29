#!/usr/bin/env python3
"""Build bundle zips and an include-expanded copy of the app for local compile checks.

  bundle.zip          libraries + driver + apps (Hubitat Developer tools > Bundles > Import ZIP)
  bundle-library.zip  libraries only (used by HPM; apps/driver are separate HPM entries so OAuth gets enabled)
  build/WeatherTileInstance.expanded.groovy  app with #include replaced by library code (minus library(...))
"""
import os, re, zipfile, time
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NS, BUNDLE = "cummingsrob", "WeatherTile"
LIBS = ["libraries/cummingsrob.WeatherTileIcons.groovy", "libraries/cummingsrob.WeatherTileIconsOutline.groovy",
        "libraries/cummingsrob.WeatherTileIconsMeteocons.groovy", "libraries/cummingsrob.WeatherTileIconsMeteoconsAnimated.groovy",
        "libraries/cummingsrob.WeatherTileIconsMono.groovy"]
DRIVERS = [("drivers/WeatherTileDevice.groovy", "cummingsrob.WeatherTileDevice.groovy")]
APPS = [("apps/WeatherTileManager.groovy", "cummingsrob.WeatherTileManager.groovy"),   # parent first
        ("apps/WeatherTileInstance.groovy", "cummingsrob.WeatherTileInstance.groovy")]

def rd(p): return open(os.path.join(ROOT, p), encoding="utf-8").read()

def write_zip(name, entries):
    lines = [NS, BUNDLE] + ["%s %s" % (t, arc) for t, _, arc in entries]
    manifest = "\n".join(lines)          # Hubitat-exported bundles have no trailing newline
    dt = (2026, 10, 28, 0, 0, 0)   # fixed date -> reproducible zip
    with zipfile.ZipFile(os.path.join(ROOT, name), "w", zipfile.ZIP_DEFLATED) as z:
        for _, src, arc in entries:
            z.writestr(zipfile.ZipInfo(arc, dt), rd(src), zipfile.ZIP_DEFLATED)
        for f in ("install.txt", "update.txt"):
            z.writestr(zipfile.ZipInfo(f, dt), manifest, zipfile.ZIP_DEFLATED)

libs = [("library", p, os.path.basename(p)) for p in LIBS]
write_zip("bundle.zip", libs + [("driver", s, a) for s, a in DRIVERS] + [("app", s, a) for s, a in APPS])
write_zip("bundle-library.zip", libs)

def expand(src):
    def repl(m):
        lib = rd("libraries/%s.groovy" % m.group(1))
        return re.sub(r"(?ms)^library\s*\(.*?^\)\s*$", "", lib)
    out = re.sub(r"(?m)^#include\s+([\w.]+)\s*$", repl, src)
    # Hubitat hoists imports from included libraries; do the same so the expanded file compiles
    imports = list(dict.fromkeys(re.findall(r"(?m)^import\s+[\w.*]+\s*$", out)))
    out = re.sub(r"(?m)^import\s+[\w.*]+\s*$\n?", "", out)
    return "\n".join(imports) + "\n" + out
os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
open(os.path.join(ROOT, "build/WeatherTileInstance.expanded.groovy"), "w").write(expand(rd("apps/WeatherTileInstance.groovy")))
print("built bundle.zip, bundle-library.zip, build/WeatherTileInstance.expanded.groovy")

# Cache-busting: point every manifest location at ?v=<version> so hubs skip GitHub raw's stale cache
import json
mp = os.path.join(ROOT, "packageManifest.json")
m = json.load(open(mp, encoding="utf-8"))
v = m["version"]
for sect in ("bundles", "apps", "drivers"):
    for e in m.get(sect, []):
        e["location"] = e["location"].split("?", 1)[0] + "?v=" + v
        if sect == "bundles":
            e["version"] = v
open(mp, "w", encoding="utf-8").write(json.dumps(m, indent=2, ensure_ascii=True) + "\n")
print("manifest locations set to ?v=" + v)
