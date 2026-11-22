#!/usr/bin/env python3
"""Render 10 original, silent, offline text-slide emergency learning clips.
Not live-action first-aid instruction; clinical content needs specialist review.
Licensing: all script text is original in this repository.
"""
from pathlib import Path
import subprocess
import shutil
import json

ROOT=Path(__file__).resolve().parent
TARGET=ROOT/"app/src/main/assets/educational_videos"
TARGET.mkdir(parents=True,exist_ok=True)
FONT="/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FFMPEG=shutil.which("ffmpeg")
if not FFMPEG or not Path(FONT).is_file():
    raise SystemExit("ffmpeg or DejaVuSans font unavailable")
TUTORIALS={
 "acil_cagri":["112 ACİL ÇAĞRI","Önce kendini güvende tut.\nGüvenli yerde yardım iste.","Konumunu ve olay türünü\nkısa ve net söyle.","Yaralı sayısını ve riskleri belirt.\nOperatörün talimatını izle."],
 "kanama":["ŞİDDETLİ KANAMA","Acil durumda 112'yi ara.\nTemiz bir bez hazırla.","Doğrudan ve sürekli bası uygula.\nBezi gereksiz kaldırma.","Yaşamı tehdit eden kanamada\noperatör yönlendirmesini izle."],
 "yanik":["YANIK","Kişiyi sıcak kaynaktan uzaklaştır.\nKendi güvenliğini sağla.","Yanığı yaklaşık 20 dakika\nserin akan suyla soğut.","Buz, yağ, diş macunu sürme.\nCiddi yanıkta 112'yi ara."],
 "cpr":["BİLİNÇ VE SOLUNUM","Kişiye seslen ve tepki kontrol et.\nNormal nefesi değerlendir.","Bilinçsiz ve normal solumuyorsa\n112'yi ara, hoparlöre al.","Operatörün CPR yönergesini izle.\nAED varsa cihazı kullan."],
 "deprem":["DEPREM","Sarsıntıda ÇÖK - KAPAN - TUTUN.\nCam ve devrilecek eşyadan uzaklaş.","Asansör ve merdivene koşma.\nSarsıntı bitene kadar korun.","Artçılara hazırlıklı ol.\nHasarlı yapıya geri dönme."],
 "sel":["SEL","Su dolu yola ve alt geçide girme.\nAraçla seli geçmeye çalışma.","Güvenli yüksek noktaya çık.\nElektrik hatlarından uzak dur.","Yetkili tahliye duyurusunu izle.\nTehlikedeysen 112'yi ara."],
 "yangin":["YANGIN","Duman ve alevleri görünce\nhemen güvenli çıkışı değerlendir.","Yayılan yangını kendin söndürmeye\nçalışma, dışarıya tahliye ol.","112'ye konumu bildir.\nTehlikeli yapıya geri dönme."],
 "serpinti":["NÜKLEER SERPİNTİ","Yetkililer aksini söylemedikçe\nsağlam bina içine gir.","Bodrum veya iç odada kal.\nDış kıyafeti ayrı tut.","Resmî talimatları takip et.\nİyot tabletini kendiliğinden alma."],
 "su":["İÇME SUYU","Öncelikle güvenilir ambalajlı\nveya güvenli şebeke suyu kullan.","Mikrobiyal riskte uygun kaynatma\nveya onaylı arıtma gerekebilir.","Kimyasal / radyoaktif kirlenmeyi\nkaynatmak gidermez."],
 "hipotermi":["HİPOTERMİ","Kişiyi rüzgâr ve ıslaklıktan\nuzak, güvenli yere taşı.","Islak kıyafeti kuru olanla değiştir.\nGövdeyi kademeli ısıt.","Bilinç bozukluğu varsa 112.\nSertçe ovuşturma veya hızlı ısıtma."]
}
idx={}
for code,slides in TUTORIALS.items():
    tmp=TARGET/("_"+code)
    tmp.mkdir(exist_ok=True)
    filters=[]
    for i,slide in enumerate(slides):
        p=tmp/("slide_%d.txt"%i)
        p.write_text(slide,encoding="utf-8")
        # Single readable slide for 8 seconds, no musical playback.
        filters.append(
           "drawtext=fontfile="+FONT+
           ":textfile="+str(p)+
           ":fontsize="+("32" if i==0 else "23")+
           ":fontcolor=white:line_spacing=8"+
           ":x=(w-text_w)/2:y=(h-text_h)/2"+
           ":enable='between(t,"+str(i*8)+","+str((i+1)*8)+")'")
    target=TARGET/(code+".mp4")
    cmd=[FFMPEG,"-hide_banner","-loglevel","error","-y",
         "-f","lavfi","-i","color=c=0x14324a:s=640x360:r=12:d=32",
         "-vf",",".join(filters),
         "-c:v","libx264","-pix_fmt","yuv420p",
         "-crf","32","-preset","ultrafast","-movflags","+faststart",
         "-an",str(target)]
    subprocess.run(cmd,check=True,timeout=90)
    shutil.rmtree(tmp)
    idx[code]={"file":code+".mp4","title":slides[0],
               "duration_seconds":32,"format":"Sessiz, yazılı eğitim slayt videosu"}
    print("VIDEO_OK",code,target.stat().st_size,flush=True)
(TARGET/"index.json").write_text(json.dumps(idx,ensure_ascii=False,indent=2),encoding="utf-8")
if len(list(TARGET.glob("*.mp4")))!=10:
    raise SystemExit("Expected 10 MP4 videos")
