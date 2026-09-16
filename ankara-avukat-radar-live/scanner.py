from __future__ import annotations
import hashlib,json,re,time,unicodedata
from dataclasses import dataclass
from datetime import datetime,timedelta
from pathlib import Path
from urllib.parse import urljoin,urlparse
import requests
from bs4 import BeautifulSoup

UA='AnkaraAvukatFirsatRadari/1.0 public-event-discovery'
MONTHS={'ocak':1,'şubat':2,'subat':2,'mart':3,'nisan':4,'mayıs':5,'mayis':5,'haziran':6,'temmuz':7,'ağustos':8,'agustos':8,'eylül':9,'eylul':9,'ekim':10,'kasım':11,'kasim':11,'aralık':12,'aralik':12}
KEYWORDS=['etkinlik','seminer','panel','zirve','konferans','sempozyum','kongre','çalıştay','calistay','eğitim','egitim','buluşma','bulusma','forum','demo day','hackathon','webinar','fuar','yatırım','yatirim','girişim','girisim','networking','atölye','atolye']
@dataclass(frozen=True)
class Source:
    name:str; url:str; mode:str='generic'; confidence:int=92; max_links:int=24
SOURCES=[
 Source('Ankara Ticaret Odası','https://www.atonet.org.tr/EtkinlikArsivi','ato',99),
 Source('Ankara Barosu','https://www.ankarabarosu.org.tr/calendar','baro',99),
 Source('Ankara Kalkınma Ajansı','https://ankaraka.org.tr/etkinlikler','generic',98,30),
 Source('Ankara Sanayi Odası','https://www.aso.org.tr/','generic',99,30),
 Source('Bilkent CYBERPARK','https://www.cyberpark.com.tr/','generic',96,30),
 Source('OSTİM OSB','https://ostim.org.tr/','generic',97,24),
]
def norm(v):
 v=v.lower().replace('ı','i').replace('ğ','g').replace('ü','u').replace('ş','s').replace('ö','o').replace('ç','c');v=unicodedata.normalize('NFD',v);v=''.join(c for c in v if unicodedata.category(c)!='Mn');return re.sub(r'\s+',' ',re.sub(r'[^a-z0-9]+',' ',v)).strip()
def clean(v):return re.sub(r'\s+',' ',v).strip(' \n\t-|>')
def eid(title,start,url):return hashlib.sha1(f'{norm(title)}|{start.date()}|{urlparse(url).netloc}'.encode()).hexdigest()[:16]
def dates(text,now):
 out=[]
 for m in re.finditer(r'\b(20\d{2})[-/.](\d{1,2})[-/.](\d{1,2})\b',text):
  try:out.append(datetime(*map(int,m.groups())))
  except:pass
 for m in re.finditer(r'\b(\d{1,2})[./-](\d{1,2})[./-](20\d{2})\b',text):
  d,mo,y=map(int,m.groups())
  try:out.append(datetime(y,mo,d))
  except:pass
 mp='|'.join(sorted(MONTHS,key=len,reverse=True))
 for m in re.finditer(rf'\b(\d{{1,2}})\s+({mp})\s+(20\d{{2}})\b',text,re.I):
  d,mon,y=m.groups()
  try:out.append(datetime(int(y),MONTHS[mon.lower()],int(d)))
  except:pass
 floor=now.replace(hour=0,minute=0,second=0,microsecond=0);return sorted({x for x in out if x>=floor})
def dt(text,now):
 ds=dates(text,now)
 if not ds:return None,None
 rng=re.search(r'\b([01]?\d|2[0-3])[:.]([0-5]\d)\s*(?:-|–|—|ile)\s*([01]?\d|2[0-3])[:.]([0-5]\d)\b',text,re.I)
 one=re.search(r'\b([01]?\d|2[0-3])[:.]([0-5]\d)\b',text)
 day=ds[0]
 if rng: sh,sm,eh,em=map(int,rng.groups());start=day.replace(hour=sh,minute=sm);end=day.replace(hour=eh,minute=em)
 elif one: sh,sm=map(int,one.groups());start=day.replace(hour=sh,minute=sm);end=start+timedelta(hours=2)
 else:start=day.replace(hour=9);end=start+timedelta(hours=2)
 if end<=start:end+=timedelta(days=1)
 return start,end
def classify(title,desc):
 n=norm(title+' '+desc);cats=[]
 mp=[('yatırım',['yatirim','invest','venture capital']),('startup',['startup','girisim']),('sanayi',['sanayi','uretim','imalat','osb']),('dış ticaret',['dis ticaret','ihracat','ithalat']),('teknoloji',['teknoloji','yapay zeka','siber','yazilim','dijital']),('gayrimenkul',['gayrimenkul','emlak','insaat','muteahhit']),('sağlık',['saglik','hastane','klinik','medikal']),('insan kaynakları',['insan kaynaklari','istihdam']),('hukuk',['hukuk','yargi','tahkim','arabuluculuk','dava','kvkk']),('networking',['networking','bulusma','is forumu','zirve','demo day'])]
 for label,toks in mp:
  if any(t in n for t in toks):cats.append(label)
 if not cats:cats=['iş dünyası']
 aud=[]
 for label,toks in [('yatırımcılar',['yatirimci','venture capital','fon']),('startup kurucuları',['startup','girisimci','kurucu']),('şirket sahipleri',['is insan','firma sahip','sirket sahip','kobi']),('şirket yöneticileri',['yonetici','ceo','cfo','genel mudur','ust duzey']),('sanayiciler',['sanayici','sanayi','uretim','imalat']),('ihracatçılar',['ihracat','dis ticaret']),('hukukçular',['avukat','hukukcu','baro','yargi'])]:
  if any(t in n for t in toks):aud.append(label)
 if not aud:aud=['sektör profesyonelleri']
 legal=[]
 if 'yatırım' in cats or 'startup' in cats:legal+=['Yatırım sözleşmeleri','Şirketler hukuku','Pay sahipleri sözleşmeleri','KVKK','Fikri mülkiyet']
 if 'sanayi' in cats or 'dış ticaret' in cats:legal+=['Ticaret hukuku','Sözleşmeler','İş hukuku','Tahsilat','Uluslararası ticaret']
 if 'teknoloji' in cats:legal+=['KVKK','Yazılım sözleşmeleri','Fikri mülkiyet']
 if 'gayrimenkul' in cats:legal+=['Gayrimenkul hukuku','İnşaat hukuku','Eser sözleşmeleri']
 if 'sağlık' in cats:legal+=['Sağlık hukuku','KVKK','İş hukuku']
 if 'hukuk' in cats:legal+=['Mesleki gelişim','Uyuşmazlık çözümü']
 if not legal:legal=['Ticaret hukuku','Sözleşmeler','KVKK']
 legal=list(dict.fromkeys(legal));high=sum(x in cats for x in ['yatırım','startup','sanayi','dış ticaret','teknoloji','gayrimenkul','sağlık']);key=any(x in aud for x in ['yatırımcılar','şirket sahipleri','şirket yöneticileri'])
 return dict(categories=cats,audience=aud,legal_needs=legal,networking_score=min(98,62+high*7+(10 if 'networking' in cats else 0)),decision_maker_score=min(98,58+high*9+(12 if key else 0)),professional_development_score=min(96,68+(8 if 'hukuk' in cats else 0)+high*3),corporate_opportunity_score=min(98,56+high*10+(10 if key else 0)))
def event(title,desc,start,end,venue,district,online,src,url,conf):
 return dict(id=eid(title,start,url),title=clean(title)[:220],description=clean(desc)[:1600],starts_at=start.replace(microsecond=0).isoformat(),ends_at=end.replace(microsecond=0).isoformat(),venue=venue,district=district,city='Ankara',online=online,free=True,organizer=src,source_name=src,source_url=url,verified=True,source_confidence=conf,**classify(title,desc))
def location(text,fallback):
 n=norm(text)
 if any(x in n for x in ['cevrim ici','online','webinar','zoom','teams']):return 'Çevrim içi','Çevrim içi',True
 for token,v,d in [('ato meclis','ATO Meclis Salonu','Çankaya'),('ato congresium','ATO Congresium','Çankaya'),('turkiye odalar ve borsalar birligi','TOBB','Söğütözü'),('bilkent cyberpark','Bilkent CYBERPARK','Çankaya'),('ostim','OSTİM','Yenimahalle'),('ankara barosu','Ankara Barosu','Çankaya')]:
  if token in n:return v,d,False
 return fallback,'Ankara',False
def fetch(url,s):
 r=s.get(url,timeout=18,headers={'User-Agent':UA,'Accept-Language':'tr-TR,tr;q=0.9'});r.raise_for_status();r.encoding=r.apparent_encoding or r.encoding;return r.text
def ato(html,url,now):
 text=BeautifulSoup(html,'html.parser').get_text(' ',strip=True);rows=[]
 p=re.compile(r'(\d{2}\.\d{2}\.20\d{2})\s+(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})\s+(.{2,80}?)\s+([A-ZÇĞİÖŞÜ0-9][A-ZÇĞİÖŞÜ0-9\s&().,\-–/]{5,180}?)(?=\s+\d{2}\.\d{2}\.20\d{2}|$)')
 for m in p.finditer(text):
  ds,st,et,venue,title=m.groups()
  try: day=datetime.strptime(ds,'%d.%m.%Y');start=datetime.combine(day.date(),datetime.strptime(st,'%H:%M').time());end=datetime.combine(day.date(),datetime.strptime(et,'%H:%M').time())
  except:continue
  if end<=now:continue
  full=clean(venue+' '+title)
  for known in ['ATO Meclis Salonu','ATO Congresium','Başkent Millet Bahçesi','BTK']:
   if known.lower() in full.lower(): pos=full.lower().find(known.lower());venue=known;title=clean(full[pos+len(known):]) or title;break
  rows.append(event(title,title,start,end,venue,'Çankaya',False,'Ankara Ticaret Odası',url,99))
 return rows
def baro(html,url,now):
 soup=BeautifulSoup(html,'html.parser');rows=[];p=re.compile(r'^(.*?)\s+(20\d{2}-\d{2}-\d{2})\s+(\d{2}:\d{2}):\d{2}\s+(.*)$',re.I)
 chunks=[clean(n.get_text(' ',strip=True)) for n in soup.find_all(['div','li','article']) if re.search(r'20\d{2}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}',clean(n.get_text(' ',strip=True)))]
 for text in chunks:
  m=p.match(text)
  if not m:continue
  title,ds,ts,detail=map(clean,m.groups())
  try:start=datetime.fromisoformat(ds+'T'+ts+':00')
  except:continue
  if start<now-timedelta(hours=2):continue
  title=re.split(r'(?:Etkinlikler|Tüm Etkinlikler|Kapat)\s+',title,flags=re.I)[-1][-160:]
  rows.append(event(title,detail,start,start+timedelta(hours=2),'Ankara Barosu','Çankaya',False,'Ankara Barosu',url,99))
 return rows
def links(html,base,maxn):
 soup=BeautifulSoup(html,'html.parser');host=urlparse(base).netloc.replace('www.','');out=[];seen=set()
 for a in soup.find_all('a',href=True):
  label=clean(a.get_text(' ',strip=True));href=urljoin(base,a['href']);h=urlparse(href).netloc.replace('www.','');hay=norm(label+' '+href)
  if h==host and href not in seen and (any(norm(k) in hay for k in KEYWORDS) or any(x in href.lower() for x in ['/post/','/etkinlik','/haber','/duyuru','/proje-haberleri/'])):
   seen.add(href);out.append((href,label))
  if len(out)>=maxn:break
 return out
def generic(html,src,url,label,now):
 soup=BeautifulSoup(html,'html.parser');tn=soup.find('h1') or soup.find('h2') or soup.find('title');title=clean(tn.get_text(' ',strip=True) if tn else label);text=clean(soup.get_text(' ',strip=True))
 if label and len(label)>8 and (not title or len(title)>180):title=label
 if not title or not any(norm(k) in norm(title+' '+text[:2000]) for k in KEYWORDS):return None
 start,end=dt(text[:8000],now)
 if not start or start>now+timedelta(days=180):return None
 v,d,on=location(text[:5000],src.name);return event(title,text[:1600],start,end,v,d,on,src.name,url,src.confidence)
def sim(a,b):
 aa=set(norm(a).split());bb=set(norm(b).split());return len(aa&bb)/len(aa|bb) if aa and bb else 0
def dedupe(rows):
 out=[]
 for e in sorted(rows,key=lambda x:(-int(x.get('source_confidence',0)),x.get('starts_at',''))):
  dup=False
  for x in out:
   try:a=datetime.fromisoformat(e['starts_at']);b=datetime.fromisoformat(x['starts_at']);same=a.date()==b.date() and abs((a-b).total_seconds())<=14400
   except:same=e.get('starts_at')==x.get('starts_at')
   if same and sim(e.get('title',''),x.get('title',''))>=.60:dup=True;break
  if not dup:out.append(e)
 return sorted(out,key=lambda x:x.get('starts_at',''))
def scan(now):
 s=requests.Session();allrows=[];status=[]
 for src in SOURCES:
  t=time.monotonic()
  try:
   html=fetch(src.url,s)
   if src.mode=='ato':rows=ato(html,src.url,now)
   elif src.mode=='baro':rows=baro(html,src.url,now)
   else:
    rows=[]
    for href,label in links(html,src.url,src.max_links):
     try:
      e=generic(fetch(href,s),src,href,label,now)
      if e:rows.append(e)
      time.sleep(.10)
     except:pass
   rows=[e for e in rows if datetime.fromisoformat(e['ends_at'])>now];allrows+=rows;status.append({'name':src.name,'ok':True,'events':len(rows),'ms':round((time.monotonic()-t)*1000)})
  except Exception as ex:status.append({'name':src.name,'ok':False,'events':0,'error':f'{type(ex).__name__}: {ex}','ms':round((time.monotonic()-t)*1000)})
 return dedupe(allrows),status
def main():
 now=datetime.now();out=Path('ankara-avukat-radar-live/events.json');fresh,status=scan(now);previous=[]
 if out.exists():
  try:previous=[e for e in json.loads(out.read_text('utf-8')).get('events',[]) if datetime.fromisoformat(e['ends_at'])>now]
  except:pass
 events=dedupe(fresh+previous);payload={'schema_version':1,'generated_at':now.replace(microsecond=0).isoformat(),'event_count':len(events),'sources':status,'events':events};out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n','utf-8');Path('ankara-avukat-radar-live/scan_status.json').write_text(json.dumps({'generated_at':now.isoformat(),'fresh_event_count':len(fresh),'event_count':len(events),'sources':status},ensure_ascii=False,indent=2)+'\n','utf-8');print(f'{len(fresh)} fresh, {len(events)} retained; '+', '.join(f"{s[\"name\"]}:{s[\"events\"]}" for s in status));return 0 if any(s['ok'] for s in status) else 2
if __name__=='__main__':raise SystemExit(main())
