#!/usr/bin/env python3
"""Download offline botanical thumbnail examples in only two MediaWiki API batches.
Source: English Wikipedia species article's pageimage, followed by Commons
per-image license check. Attribution and source retained in APK. Never claim
image is an authoritative diagnostic species identification.
"""
from pathlib import Path
import urllib.parse
import urllib.request
import urllib.error
import json
import re
import html
import time
import os

ROOT=Path(__file__).resolve().parent
PLANTS=ROOT/"app/src/main/java/org/afetatlas/offline/Plants.java"
OUTPUT=ROOT/"app/src/main/assets/plant_photos"
OUTPUT.mkdir(parents=True,exist_ok=True)
UA="HayattaKalmaAtlasi/0.4 (+https://github.com/emress2525/GunumPlanner; attribution-only offline educational thumbnails)"
plants=re.findall(r'new Species\("([^"]+)",\s*"([^"]+)"',PLANTS.read_text(encoding="utf-8"))
if len(plants)!=47:
    raise SystemExit(f"Plant catalogue mismatch: {len(plants)} expected 47")
def page_title(name):
    if " spp." in name: return name.split()[0]
    if " agg." in name: return name.split()[0]+" "+name.split()[1]
    return name.strip()

def striphtml(value):
    value=re.sub(r'<[^>]*>',' ',value or "")
    return " ".join(html.unescape(value).split())[:350]

def api(endpoint, params, retries=3):
    url=endpoint+"?"+urllib.parse.urlencode(params)
    req=urllib.request.Request(url,headers={"User-Agent":UA,"Accept":"application/json"})
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(req,timeout=45) as res:
                return json.loads(res.read(10_000_000).decode('utf-8'))
        except urllib.error.HTTPError as exc:
            if exc.code!=429 or attempt+1==retries: raise
            wait=(attempt+1)*15
            print("API throttle; sleeping",wait,flush=True)
            time.sleep(wait)

def batches(collection, n=40):
    for i in range(0,len(collection),n):yield collection[i:i+n]

titles=sorted(set(page_title(scientific) for common,scientific in plants))
photoByArticle={}
for batch in batches(titles,40):
    results=api("https://en.wikipedia.org/w/api.php",{
        "action":"query","format":"json","redirects":"1",
        "titles":"|".join(batch),"prop":"pageimages","piprop":"original"
    })
    query=results.get("query",{})
    for obj in query.get("pages",{}).values():
        original=(obj.get("original") or {}).get("source")
        photo=obj.get("pageimage")
        if not photo and original:
            photo=urllib.parse.unquote(urllib.parse.urlparse(original).path.rsplit("/",1)[-1]).replace("_"," ")
        if photo:photoByArticle[obj["title"].lower()]=photo
    for entry in query.get("redirects",[]):
        target=photoByArticle.get(entry.get("to","").lower())
        if target:photoByArticle[entry["from"].lower()]=target
print("Wikipedia species/genus pageimages",len(photoByArticle),"/",len(titles),flush=True)
fileTitles=sorted(set("File:"+x for x in photoByArticle.values()) | {"File:Sideritis scardica kz05.jpg"})
metaByFile={}
for batch in batches(fileTitles,38):
    results=api("https://commons.wikimedia.org/w/api.php",{
        "action":"query","format":"json","titles":"|".join(batch),
        "prop":"imageinfo","iiprop":"url|extmetadata","iiurlwidth":"720"
    })
    for obj in results.get("query",{}).get("pages",{}).values():
        data=obj.get("imageinfo") or []
        if data:metaByFile[obj["title"].lower()]=data[0]
print("Commons image records",len(metaByFile),"/",len(fileTitles),flush=True)

def licensed(item):
    ext=item.get('extmetadata') or {}
    lic=striphtml(ext.get("LicenseShortName",{}).get("value",""))
    usage=striphtml(ext.get("UsageTerms",{}).get("value",""))
    s=(lic+" "+usage).lower()
    permitted=bool(re.search(r"cc[ -]?by(?:[ -]?sa)?\b|cc0|public[ -]domain",s))
    restricted=bool(re.search(r"\bby[ -]?(nc|nd)\b|non[- ]commercial|no[- ]derivatives",s))
    return permitted and not restricted,lic,ext
def download(url,max_bytes=1_500_000):
    req=urllib.request.Request(url,headers={"User-Agent":UA,"Accept":"image/jpeg,image/png,*/*"})
    for i in range(3):
        try:
            with urllib.request.urlopen(req,timeout=45) as res:
                body=res.read(max_bytes+1)
            if len(body)>max_bytes:raise ValueError("Thumbnail too large")
            if body.startswith(b'\xff\xd8\xff'):return body,".jpg"
            if body.startswith(b'\x89PNG\r\n\x1a\n'):return body,".png"
            raise ValueError("Unknown image format")
        except urllib.error.HTTPError as exc:
            if exc.code!=429 or i==2:raise
            time.sleep(10*(i+1))

found={}
for idx,(common,scientific) in enumerate(plants,1):
    article=page_title(scientific)
    image=("Sideritis scardica kz05.jpg" if scientific=="Sideritis spp." else photoByArticle.get(article.lower()))
    if not image:
        print("NO_PAGEIMAGE",idx,scientific,flush=True)
        continue
    ii=metaByFile.get(("File:"+image).lower())
    if not ii:
        print("NO_COMMONS_METADATA",idx,scientific,flush=True)
        continue
    allowed,lic,ext=licensed(ii)
    if not allowed:
        print("LICENSE_NOT_ALLOWED",idx,scientific,lic,flush=True)
        continue
    uri=ii.get("thumburl") or ii.get("url")
    if not uri:
        print("IMAGE_URL_MISSING",idx,scientific,flush=True)
        continue
    try:
        data,extension=download(uri)
        filename="plant_%02d"%idx+extension
        (OUTPUT/filename).write_bytes(data)
        title="File:"+image
        found[scientific]={
            "filename":filename,
            "common":common,
            "scientific":scientific,
            "source":"https://commons.wikimedia.org/wiki/"+urllib.parse.quote(title.replace(" ","_")),
            "license":lic,
            "license_url":striphtml(ext.get("LicenseUrl",{}).get("value","")),
            "author":striphtml(ext.get("Artist",{}).get("value","")) or "Wikimedia Commons",
            "note":("Sideritis scardica, cins düzeyinde TEMSİLİ fotoğraf; Sideritis tür teşhisi değildir" if scientific=="Sideritis spp." else "Cins düzeyinde TEMSİLİ görüntü; tür teşhisi değildir" if (" spp." in scientific)
                else "Makale fotoğrafı; fotoğrafa bakarak tür teşhisi güvenli değildir")
        }
        print("PHOTO_OK",idx,scientific,lic,flush=True)
    except Exception as exc:
        print("PHOTO_UNAVAILABLE",idx,scientific,str(exc),flush=True)
    time.sleep(1.0)
(OUTPUT/"photo_metadata.json").write_text(json.dumps(found,ensure_ascii=False,indent=2),encoding="utf-8")
print("LICENSED_PHOTOS",len(found),"/",len(plants),flush=True)
if len(found)<12:raise SystemExit("Not enough license-confirmed photos, build blocked")
