package org.afetatlas.offline;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class VisualGuides {
    private static final String[] TITLES={
        "112: Acil çağrı", "Şiddetli kanama: Doğrudan bası",
        "Bilinçsiz ve solumuyor: CPR", "Yanık: Akan serin su",
        "Deprem: Çök – Kapan – Tutun", "Duman: Güvenli çıkış",
        "Nükleer serpinti: İçeri gir", "Su: Güvenli arıtma",
        "Sel: Yükseğe çık", "Bitki: Kesin tanı olmadan tüketme"
    };
    private static final String[] DESCRIPTIONS={
        "Önce güvenli alana geç. Konum, olay türü ve yaralı sayısını 112 operatörüne söyle.",
        "Temiz bez ile yaraya kesintisiz, doğrudan baskı uygula. Ağır kanamada hemen 112.",
        "Yetişkinde normal solunum yoksa 112'yi ara ve operatör yönlendirmesiyle göğüs ortasına bası yap. AED varsa talimatını izle.",
        "Yanık bölgesini yaklaşık 20 dakika serin akan suyla soğut. Buz, diş macunu, yağ veya bitki sürme.",
        "Sarsıntı anında sağlam bir eşyanın altında/yanında korun; baş ve enseni koru, tutun.",
        "Güvenli çıkışa yönel; asansör kullanma, yoğun dumanda alçakta kal. Dışarı çıkınca geri dönme.",
        "Sağlam bina ve mümkünse bodrum veya penceresiz iç odaya geç. Yetkililerin duyurularını izle.",
        "Berrak olmayan suyu uygun biçimde filtrele ve mikroplar için gerektiği kadar kaynat. Kaynatma kimyasal/radyoaktif kirlenmeyi gidermez.",
        "Taşkın suyuna girme veya araçla geçme. Mümkünse güvenli yüksek noktaya çık.",
        "Görsel benzerlik tür doğrulaması değildir. Bilinmeyen yabani bitki veya mantarları yemek ve yaraya sürmek risklidir."
    };
    private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    static View screen(Activity a){
        ScrollView scroll=new ScrollView(a);
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(a);title.setText("ÇEVRİMDIŞI GÖRSEL REHBER");
        title.setTextSize(20f);title.setTextColor(0xff193247);
        title.setPadding(dp(a,16),dp(a,12),dp(a,16),dp(a,8));list.addView(title);
        TextView intro=new TextView(a);
        intro.setText("Şemalar eğitsel ve basitleştirilmiştir. Teknik/anatomik eğitim, doktor değerlendirmesi veya 112 yönlendirmesinin yerini tutmaz.");
        intro.setTextColor(0xff4c5662);intro.setTextSize(13f);
        intro.setPadding(dp(a,16),dp(a,8),dp(a,16),dp(a,15));list.addView(intro);
        for(int i=0;i<TITLES.length;i++){
            TextView heading=new TextView(a);heading.setText((i+1)+". "+TITLES[i]);
            heading.setTextColor(0xff153349);heading.setTextSize(17f);
            heading.setPadding(dp(a,16),dp(a,18),dp(a,16),dp(a,5));
            list.addView(heading);
            Sketch sketch=new Sketch(a,i);
            sketch.setContentDescription(TITLES[i]+": "+DESCRIPTIONS[i]);
            list.addView(sketch,new LinearLayout.LayoutParams(-1,dp(a,210)));
            TextView detail=new TextView(a);detail.setText(DESCRIPTIONS[i]);
            detail.setTextSize(15f);detail.setTextColor(0xff273b4d);
            detail.setPadding(dp(a,16),dp(a,6),dp(a,16),dp(a,10));
            list.addView(detail);
        }
        scroll.addView(list);return scroll;
    }
    private static final class Sketch extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int type;
        private float w,h;
        Sketch(Activity activity,int type){super(activity);this.type=type;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        private void fill(Canvas c,int color,float l,float t,float r,float b,float radius){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
        }
        private void line(Canvas c,int color,float x1,float y1,float x2,float y2,float stroke){
            p.setColor(color);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
        }
        private void circle(Canvas c,int color,float x,float y,float radius){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,radius,p);
        }
        private void txt(Canvas c,String s,float x,float y,int color,float size){
            p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            c.drawText(s,x,y,p);
        }
        private void arrow(Canvas c,int color,float sx,float sy,float ex,float ey){
            line(c,color,sx,sy,ex,ey,6);
            float ang=(float)Math.atan2(ey-sy,ex-sx);
            float len=19;
            line(c,color,ex,ey,ex-len*(float)Math.cos(ang-0.55),ey-len*(float)Math.sin(ang-0.55),6);
            line(c,color,ex,ey,ex-len*(float)Math.cos(ang+0.55),ey-len*(float)Math.sin(ang+0.55),6);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            w=getWidth();h=getHeight();
            if(w<=0||h<=0)return;
            c.save();c.scale(w/400f,h/190f);
            fill(c,0xffe7f0f5,6,5,394,184,18);
            final int navy=0xff24455e,red=0xffc94542,blue=0xff2980a7,green=0xff24866a;
            switch(type){
                case 0:
                    fill(c,navy,143,15,257,172,18);
                    fill(c,Color.WHITE,153,32,247,143,7);
                    circle(c,green,200,154,11);
                    txt(c,"112",200,92,red,41);
                    txt(c,"ACİL",200,117,navy,15);
                    break;
                case 1:
                    fill(c,0xffd4a885,63,45,350,144,50);
                    fill(c,0xfff4f4ec,153,65,253,124,16);
                    circle(c,red,201,94,19);
                    fill(c,0xffe9decb,122,51,153,145,12);
                    fill(c,0xffe9decb,250,51,283,145,12);
                    arrow(c,navy,200,8,200,55);
                    txt(c,"SÜREKLİ BASI",200,166,navy,17);
                    break;
                case 2:
                    circle(c,0xffd4a885,200,33,22);
                    fill(c,0xff77a0ae,146,55,252,142,34);
                    line(c,navy,152,97,246,97,3);
                    circle(c,red,200,93,20);
                    fill(c,0xffe9c7a5,163,74,207,91,12);
                    fill(c,0xffe9c7a5,169,86,213,103,12);
                    arrow(c,navy,225,59,225,99);
                    txt(c,"GÖĞÜS ORTASI",200,165,navy,16);
                    break;
                case 3:
                    line(c,blue,107,19,107,53,14);line(c,blue,107,53,224,53,14);
                    for(int i=0;i<5;i++){
                        line(c,blue,135+i*25,73,127+i*25,108,4);
                        circle(c,blue,125+i*25,113,5);
                    }
                    fill(c,0xffd4a885,84,119,296,152,19);
                    txt(c,"20 DK SERİN SU",200,172,navy,15);
                    break;
                case 4:
                    fill(c,navy,75,56,333,75,5);
                    fill(c,navy,98,69,115,155,5);
                    fill(c,navy,295,69,312,155,5);
                    circle(c,0xffd4a885,194,116,18);
                    line(c,navy,194,135,172,154,8);line(c,navy,172,154,231,155,8);
                    line(c,navy,172,154,144,166,8);
                    txt(c,"ÇÖK–KAPAN–TUTUN",201,35,navy,17);
                    break;
                case 5:
                    fill(c,navy,56,21,66,172,5);
                    fill(c,navy,56,20,336,31,5);
                    fill(c,navy,326,21,336,172,5);
                    fill(c,0xffb7c4ca,225,45,301,166,5);
                    circle(c,0xffd4a885,135,123,13);
                    line(c,navy,135,136,166,152,7);
                    line(c,navy,166,152,180,170,7);
                    arrow(c,green,166,110,250,108);
                    txt(c,"ÇIKIŞ",275,101,green,16);
                    break;
                case 6:
                    fill(c,navy,100,72,301,161,5);
                    p.setColor(0xff91aaba);p.setStyle(Paint.Style.FILL);
                    Path roof=new Path();roof.moveTo(75,73);roof.lineTo(200,16);roof.lineTo(326,73);roof.close();
                    c.drawPath(roof,p);
                    fill(c,0xffabc5cc,133,95,181,143,3);
                    fill(c,0xffabc5cc,220,95,268,143,3);
                    arrow(c,green,201,167,201,119);
                    txt(c,"İÇERİ GİR",201,181,green,15);
                    break;
                case 7:
                    fill(c,navy,100,122,302,150,3);
                    fill(c,0xffc2d4d8,139,95,263,124,5);
                    for(int i=0;i<4;i++){circle(c,blue,154+32*i,78,9);line(c,blue,154+32*i,18,154+32*i,57,4);}
                    txt(c,"KAYNATMA",200,168,navy,16);
                    break;
                case 8:
                    p.setColor(0xffa8b7a2);Path hill=new Path();hill.moveTo(19,174);hill.lineTo(213,59);hill.lineTo(382,174);hill.close();c.drawPath(hill,p);
                    line(c,blue,0,158,400,158,8);line(c,blue,0,175,400,175,8);
                    circle(c,navy,212,47,9);line(c,navy,212,59,212,83,6);
                    arrow(c,green,97,145,189,76);
                    txt(c,"YÜKSEĞE ÇIK",196,26,navy,16);
                    break;
                default:
                    circle(c,0xff6a9c68,203,97,61);
                    line(c,green,204,157,204,35,7);
                    line(c,green,204,102,164,67,6);
                    line(c,green,204,102,246,66,6);
                    p.setColor(red);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);
                    c.drawCircle(202,97,77,p);
                    c.drawLine(147,152,258,42,p);p.setStyle(Paint.Style.FILL);
                    txt(c,"KESİN TANIMADAN YEME",200,185,navy,14);
            }
            c.restore();
        }
    }
}
