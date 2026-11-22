#!/usr/bin/env python3
"""Build reproducible OFFLINE thumbnails from explicitly licensed Wikimedia Commons files.
Images are illustrative, NOT reliable positive taxonomic identification.
A network/metadata failure never silently invents or substitutes a species.
"""
import html
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path

ROOT=Path(__file__).resolve().parent
PLANTS=ROOT/"app/src/main/java/org/afetatlas/offline/Plants.java"
ASSETS=ROOT/"app/src/main/assets/plant_photos"
ASSETS.mkdir(parents=True,exist_ok=True)
UA="HayattaKalmaAtlasi/0.3 (offline educational image attribution; GitHub emress2525/GunumPlanner)"
body=PLANTS.read_text(encoding="utf-8")
items=re.findall(r'new Species\("([^"]+)",\s*"([^"]+)"',body)
if len(items)<47:
    raise SystemExit("Expected 47 unique botanical names; read "+str(len(items)))
API="https://commons.wikimedia.org/w/api.php"
LICENSES=("cc-by","cc0","public domain","public-domain","pd-")

def getjson(url):
    req=urllib.request.Request(url,headers={"User-Agent":UA,"Accept":"application/json"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req,timeout=30) as r:
                return json.loads(r.read(4_000_000).decode("utf-8"))
        except urllib.error.HTTPError as ex:
            if ex.code!=429 or attempt==3: raise
            wait=10*(attempt+1)
            print("Commons rate limit; waiting",wait,"seconds",flush=True)
            time.sleep(wait)

def fetch(url,limit=900_000):
    req=urllib.request.Request(url,headers={"User-Agent":UA,"Accept":"image/jpeg,image/png,*/*"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req,timeout=30) as r:
                data=r.read(limit+1)
            break
        except urllib.error.HTTPError as ex:
            if ex.code!=429 or attempt==3: raise
            wait=10*(attempt+1)
            print("Image rate limit; waiting",wait,"seconds",flush=True)
            time.sleep(wait)
    if len(data)>limit: raise ValueError("Thumbnail oversized")
    if data[:3]!=b"\xff\xd8\xff" and data[:8]!=b"\x89PNG\r\n\x1a\n":raise ValueError("Image not JPEG/PNG")
    return data

def clean(s):
    s=re.sub(r'<[^>]+>',' ',s or "")
    s=html.unescape(s)
    return " ".join(s.split())[:350]

out={}
for idx,(turkish,scientific) in enumerate(items,1):
    # Prefer a filename containing BOTH Latin genus and species, never category-level stock photos.
    binomial=scientific.split(" spp.")[0].replace(" agg.","")
    parts=binomial.split()
    genus=parts[0]
    species=parts[1] if len(parts)>=2 else None
    if scientific.endswith("spp."):
        species=None
    query="filetype:bitmap "+genus+(" "+species if species else "")
    params={
       "action":"query","format":"json","generator":"search",
       "gsrsearch":query,"gsrnamespace":"6","gsrlimit":"30",
       "prop":"imageinfo","iiprop":"url|extmetadata","iiurlwidth":"720"
    }
    url=API+"?"+urllib.parse.urlencode(params)
    try:
        pages=list(getjson(url).get("query",{}).get("pages",{}).values())
        def score(p):
            name=p.get("title","").lower()
            return 0 if genus.lower() in name and (not species or species.lower() in name) else 1
        pages.sort(key=score)
        selected=None
        for page in pages:
            title=page.get("title","")
            lt=title.lower().replace("_"," ")
            if genus.lower() not in lt or (species and species.lower() not in lt):
                continue
            ii=(page.get("imageinfo") or [{}])[0]
            url=ii.get("thumburl") or ii.get("url")
            meta=ii.get("extmetadata") or {}
            license_short=clean(meta.get("LicenseShortName",{}).get("value","")).lower()
            usage=clean(meta.get("UsageTerms",{}).get("value","")).lower()
            if not any(term in license_short or term in usage for term in LICENSES): continue
            if not url or not (".jpg" in url.lower() or ".jpeg" in url.lower() or ".png" in url.lower()):
                continue
            author=clean(meta.get("Artist",{}).get("value","")) or "Wikimedia Commons katkıda bulunanları"
            data=fetch(url)
            ext=".png" if data.startswith(b"\x89PNG") else ".jpg"
            filename="plant_%02d"%idx+ext
            (ASSETS/filename).write_bytes(data)
            selected={
                "filename":filename,
                "scientific":scientific,
                "common":turkish,
                "source":"https://commons.wikimedia.org/wiki/"+urllib.parse.quote(title.replace(" ","_")),
                "license":clean(meta.get("LicenseShortName",{}).get("value","")),
                "license_url":clean(meta.get("LicenseUrl",{}).get("value","")),
                "author":author,
                "note":("Wikimedia cins düzeyinde örnek fotoğrafı; TÜRE AİT KESİN GÖRSEL DEĞİLDİR." if not species else "Wikimedia fotoğrafı; tür teşhisi için güvenli dayanak değildir.")
            }
            break
        if selected:
            out[scientific]=selected
            print("PHOTO",idx,scientific,selected["license"],flush=True)
        else:
            print("NO LICENSED EXACT-TITLE PHOTO",idx,scientific,flush=True)
    except Exception as exc:
        print("UNAVAILABLE",idx,scientific,str(exc),flush=True)
    time.sleep(2.25)

(ASSETS/"photo_metadata.json").write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding="utf-8")
print("LICENSED_PHOTOS",len(out),"/",len(items),flush=True)
if len(out)<12:
    raise SystemExit("Too few properly licensed species-labelled photos; blocking release")
