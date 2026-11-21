package com.tevkil.radar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_OCR_IMAGE = 610;
    private static final int BG = 0xff081522;
    private static final int PANEL = 0xff112438;
    private static final int PANEL_ALT = 0xff172e43;
    private static final int PRIMARY = 0xff4ce0c0;
    private static final int MUTED = 0xff9db5c5;
    private static final int WHITE = 0xfff4f9fc;
    private static final int LINE = 0xff294156;
    private static final int DIALOG_TEXT = 0xff17212b;
    private static final int DIALOG_MUTED = 0xff4b5563;
    private static final int DIALOG_OK = 0xff116149;
    private static final int DIALOG_WARNING = 0xff9a3412;
    private LinearLayout root, body, navbar;
    private NoticeStore store;
    private String section = "inbox";
    private long requestedNotice = -1;
    private boolean quickCommentRequested = false;
    private boolean registered = false;
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if ("inbox".equals(section)) render();
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new NoticeStore(this);
        InstagramScanJob.schedule(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(12), dp(16), 0);
        setContentView(root);
        handleIntent(getIntent());
        render();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_OCR_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            readImageAndReview(data.getData());
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); handleIntent(intent); render();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!registered) {
            IntentFilter f = new IntentFilter(RadarListener.UPDATE);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(refreshReceiver, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(refreshReceiver, f);
            registered = true;
        }
        render();
        if (requestedNotice != -1) {
            long id = requestedNotice; requestedNotice = -1;
            boolean quick = quickCommentRequested; quickCommentRequested = false;
            for (NoticeStore.Notice n : store.list(400)) if (n.id == id) {
                if (quick && n.source.toLowerCase(Locale.ROOT).contains("instagram")) prepareInstagramComment(n);
                else showNotice(n);
                break;
            }
        }
    }
    @Override protected void onPause() {
        if (registered) { unregisterReceiver(refreshReceiver); registered = false; }
        super.onPause();
    }
    @Override protected void onDestroy() { store.close(); super.onDestroy(); }

    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable background(int color, float radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    private GradientDrawable borderBackground(int color, int stroke, float radius) {
        GradientDrawable g = background(color, radius); g.setStroke(dp(1), stroke); return g;
    }
    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private TextView action(String text, int color, int fg, boolean outlined, View.OnClickListener click) {
        TextView v = label(text, 13, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(13), dp(11), dp(13), dp(11));
        v.setBackground(outlined ? borderBackground(color, LINE, 13) : background(color, 13));
        v.setClickable(true); v.setOnClickListener(click);
        return v;
    }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private LinearLayout.LayoutParams weight(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h < 0 ? h : dp(h), weight); }
    private void gap(LinearLayout l, int h) { View v=new View(this);l.addView(v,lp(1,h)); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    private void render() {
        if (root == null || store == null) return;
        root.removeAllViews();
        makeHeader();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        body = column(); body.setPadding(0, dp(18), 0, dp(20));
        scroll.addView(body);
        root.addView(scroll,weight(-1,0,1));
        makeNav();
        if ("inbox".equals(section)) showInbox();
        else if ("filters".equals(section)) showFilters();
        else if ("sources".equals(section)) showSources();
        else showSettings();
    }
    private void makeHeader() {
        LinearLayout top = row();
        LinearLayout left = column();
        TextView brand=label("◎   TEVKİL RADARI", 14, PRIMARY, true);
        brand.setLetterSpacing(.12f);
        left.addView(brand);
        gap(left,5);
        left.addView(label("Fırsatı kaçırma.", 25, WHITE, true));
        top.addView(left,weight(0,-2,1));
        boolean active = (monitoringEnabled() && listenerEnabled()) ||
            (Prefs.raw(this).getBoolean("meta_enabled", false) && InstagramSecureConfig.configured(this));
        TextView pill=label(active ? "●  AKTİF" : "●  PASİF", 11, active ? PRIMARY : 0xffffb478, true);
        pill.setGravity(Gravity.CENTER);pill.setPadding(dp(10),dp(9),dp(10),dp(9));
        pill.setBackground(background(PANEL,50));
        top.addView(pill, lp(-2,-2));
        root.addView(top);
    }
    private void makeNav() {
        navbar=row(); navbar.setPadding(0,dp(9),0,dp(12));
        navItem("İlanlar", "inbox");
        navItem("Filtreler", "filters");
        navItem("Kaynaklar", "sources");
        navItem("Ayarlar", "settings");
        root.addView(navbar);
    }
    private void navItem(String text, String key) {
        boolean chosen = section.equals(key);
        TextView b = action(text, chosen ? PANEL_ALT : BG, chosen ? PRIMARY : MUTED, false, v -> {section=key;render();});
        LinearLayout.LayoutParams p = weight(0,47,1);p.setMargins(dp(2),0,dp(2),0);
        navbar.addView(b,p);
    }
    private void title(String name, String sub) {
        body.addView(label(name,20,WHITE,true));
        if (sub!=null) {gap(body,5);body.addView(label(sub,13,MUTED,false));}
        gap(body,14);
    }
    private void showInbox() {
        boolean access=listenerEnabled();
        if (!access || !monitoringEnabled()) {
            LinearLayout card=panel();
            card.addView(label("WhatsApp / Telegram için isteğe bağlı takip",18,WHITE,true)); gap(card,7);
            card.addView(label("Instagram için bildirim izni gerekmiyor: Kaynaklar bölümünde Meta API üzerinden ayrı bir bağlantı kurabilirsin. Bu izin yalnızca telefonuna gelen mesaj bildirimlerini yakalamak içindir.",13,MUTED,false));gap(card,14);
            card.addView(action(access ? "TAKİBİ ETKİNLEŞTİR" : "BİLDİRİM ERİŞİMİ VER",PRIMARY,BG,false,v->enableMonitoring()),lp(-1,-2));
            gap(card,9);
            card.addView(label("İzin verilen bildirim metinleri analiz edilir; sessize alınan sohbetler veya geçmiş mesajlar okunmaz.",11,MUTED,false));
            body.addView(card); gap(body,17);
        }
        LinearLayout metrics=row();
        metric(metrics,"TOPLAM İLAN",Integer.toString(store.count()));
        metric(metrics,"YENİ İLAN",Integer.toString(store.unread()));
        body.addView(metrics); gap(body,22);
        LinearLayout actions=row();
        TextView importBtn=action("＋  İlan ekle",PANEL_ALT,WHITE,false,v->manualAdd());
        TextView demoBtn=action("▶  Örneği dene",PANEL,PRIMARY,true,v->demo());
        LinearLayout.LayoutParams pi=weight(0,45,1);pi.setMargins(0,0,dp(6),0);actions.addView(importBtn,pi);
        LinearLayout.LayoutParams pd=weight(0,45,1);pd.setMargins(dp(6),0,0,0);actions.addView(demoBtn,pd);
        body.addView(actions);gap(body,10);
        body.addView(action("▣  GÖRSELDEN İLAN OKU (OCR)",PRIMARY,BG,false,v->openImagePicker()), lp(-1,48));
        gap(body,7);
        body.addView(label("Ekran görüntüsünü seç. Metin cihazında okunur; kaydetmeden önce kontrol edersin.",12,MUTED,false));
        gap(body,22);
        title("Yakalanan ilanlar","En yeni eşleşmeler üstte gösterilir.");
        List<NoticeStore.Notice> items = store.list(150);
        if (items.isEmpty()) {
            LinearLayout empty = panel();empty.setGravity(Gravity.CENTER);
            empty.addView(label("◎",40,PRIMARY,true));gap(empty,6);
            TextView h=label("Henüz ilan yok",17,WHITE,true);h.setGravity(Gravity.CENTER);empty.addView(h);
            gap(empty,6);
            TextView p=label("Bir örnek ilan ekleyerek filtreyi deneyebilir veya izinleri açıp yeni mesajları bekleyebilirsin.",13,MUTED,false);
            p.setGravity(Gravity.CENTER);empty.addView(p);
            body.addView(empty);
        } else for (NoticeStore.Notice n : items) noticeCard(n);
    }
    private void metric(LinearLayout parent,String label,String value) {
        LinearLayout metric=column();metric.setPadding(dp(16),dp(14),dp(16),dp(14));
        metric.setBackground(borderBackground(PANEL,LINE,17));
        metric.addView(label(label,11,MUTED,true));gap(metric,6);metric.addView(label(value,27,WHITE,true));
        LinearLayout.LayoutParams p=weight(0,-2,1);p.setMargins(dp(3),0,dp(3),0);parent.addView(metric,p);
    }
    private LinearLayout panel() {
        LinearLayout c=column();c.setPadding(dp(16),dp(18),dp(16),dp(18));
        c.setBackground(borderBackground(PANEL,LINE,17));return c;
    }
    private void noticeCard(NoticeStore.Notice n) {
        LinearLayout c=panel();
        LinearLayout head=row();
        int tint = n.source.startsWith("WhatsApp") ? 0xff5de1a3 : n.source.equals("Telegram") ? 0xff71c9ff : 0xfff3a7ca;
        TextView name=label("●  "+n.source,12,tint,true);
        head.addView(name,weight(0,-2,1));
        TextView time=label(relative(n.time),11,MUTED,false);head.addView(time);
        c.addView(head);gap(c,9);
        c.addView(label(n.title.isEmpty()? "İlan paylaşımı":n.title,15,WHITE,true));gap(c,7);
        TextView bodyText=label(n.body,13,MUTED,false);bodyText.setMaxLines(5);bodyText.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(bodyText);gap(c,13);
        LinearLayout foot=row();
        TextView status = label("sent".equals(n.status)?"✓ Yanıtlandı":("prepared".equals(n.status)?"◌ Hazırlandı":"● Yeni"),11,PRIMARY,true);
        foot.addView(status,weight(0,-2,1));
        foot.addView(action("İncele  ›",PANEL_ALT,WHITE,false,v->showNotice(n)));
        c.addView(foot);
        LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(10);body.addView(c,p);
    }
    private String relative(long time) {
        long delta=(System.currentTimeMillis()-time)/60000;
        if (delta<1) return "Şimdi";
        if (delta<60) return delta+" dk önce";
        if (delta<24*60) return (delta/60)+" sa önce";
        return new SimpleDateFormat("dd MMM · HH:mm",new Locale("tr","TR")).format(new Date(time));
    }
    private void showNotice(NoticeStore.Notice n) {
        store.mark(n.id,"seen");
        final String text = n.source+" · "+n.title+"\n\n"+n.body;
        AlertDialog d = new AlertDialog.Builder(this)
           .setTitle("Tevkil ilanı")
           .setMessage(text)
           .setNeutralButton("Kaynağı aç",(dialog,which)->openSource(n))
           .setNegativeButton("Kapat",(dialog,which)->render())
           .setPositiveButton(n.source.toLowerCase(Locale.ROOT).contains("instagram") ? "Yorum için aç" : "Başvuru hazırla",
                (dialog,which)->{ if(n.source.toLowerCase(Locale.ROOT).contains("instagram")) prepareInstagramComment(n); else replyDialog(n); })
           .create(); d.setOnDismissListener(x->render());d.show();
    }
    private void openSource(NoticeStore.Notice n) {
        if (RadarListener.openOriginal(n.id)) return;
        if (!n.url.isEmpty()) {
            try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(n.url)));return; }catch(Exception ignored){}
        }
        toast("Kaynak bağlantısı bulunamadı. Bildirimi açın veya uygulamadaki sohbeti bulun.");
    }
    /** Clipboard plus original post: no unsupported automated Instagram publishing. */
    private void prepareInstagramComment(NoticeStore.Notice n) {
        if (n.url == null || !n.url.startsWith("https://www.instagram.com/")) {
            toast("Gönderi bağlantısı bulunamadı."); openSource(n); return;
        }
        if (!FilterEngine.matches(n.title, n.body, Prefs.rules(this))) {
            new AlertDialog.Builder(this).setTitle("Filtre dışı ilan")
                .setMessage("Bu ilan mevcut Ankara, anahtar kelime veya ücret kurallarına uymuyor. Başvuru hazırlanmadı.")
                .setPositiveButton("Tamam", null).show(); return;
        }
        String comment = Prefs.comment(this).trim();
        if (comment.isEmpty()) { toast("Yorum şablonu boş. Ayarlardan düzenle."); return; }
        ClipboardManager clip = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip == null) { toast("Pano kullanılamıyor."); return; }
        clip.setPrimaryClip(ClipData.newPlainText("Tevkil başvuru yorumu", comment));
        store.mark(n.id, "prepared");
        toast("Yorum kopyalandı. Instagram'da yapıştırıp Gönder'e bas.");
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(n.url))); }
        catch (Exception e) { toast("Gönderi açılamadı. Yorum panoya kopyalandı."); }
        render();
    }

    private void replyDialog(NoticeStore.Notice n) {
        EditText message = new EditText(this);
        message.setText(Prefs.reply(this)); message.setTextSize(15);
        message.setTextColor(Color.BLACK);message.setMinLines(3);message.setMaxLines(8);
        message.setPadding(dp(18),dp(13),dp(18),dp(13));
        boolean direct=RadarListener.canDirectReply(n.id);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(direct ? "Onaylayıp doğrudan yanıtla" : "Mesajı onayla ve paylaş")
                .setMessage(direct ? "Mesaj, ilanın geldiği sohbetin bildirim yanıtı üzerinden gönderilmeye çalışılacak." : "Kaynak uygulama doğrudan yanıt sunmuyor. Metni seçtiğin uygulamada kendin göndereceksin.")
                .setView(message)
                .setNegativeButton("İptal",null)
                .setPositiveButton(direct ? "Gönder" : "Paylaş",null)
                .create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String response=message.getText().toString().trim();
            if(response.isEmpty()){message.setError("Bir mesaj yaz.");return;}
            if(direct && RadarListener.sendApprovedReply(this,n.id,response)) {
                store.mark(n.id,"sent");toast("Yanıt gönderme işlemi başlatıldı. Teslimi sohbetten doğrula.");
            } else {
                store.mark(n.id,"prepared");
                Intent share=new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,response);
                try { startActivity(Intent.createChooser(share,"Yanıtı paylaş")); }
                catch (Exception e) { toast("Paylaşımı açacak uygulama bulunamadı."); }
            }
            dialog.dismiss();render();
        }));
        dialog.show();
    }
    private void showFilters() {
        title("Akıllı filtreler","Hangi tevkil işlerini görmek istediğini sen belirle.");
        FilterEngine.Rules r=Prefs.rules(this);
        LinearLayout info=panel();
        info.addView(label("KONUM",11,PRIMARY,true));gap(info,7);
        info.addView(label(r.ankaraOnly?"Ankara ve ilçeleri":"Tüm Türkiye",17,WHITE,true));gap(info,15);
        info.addView(label("ANAHTAR KELİMELER",11,PRIMARY,true));gap(info,7);
        info.addView(label(r.include,14,WHITE,false));gap(info,15);
        info.addView(label("İLÇE SINIRI",11,PRIMARY,true));gap(info,7);
        info.addView(label(r.districts.isEmpty()?"Ankara geneli":r.districts,14,WHITE,false));gap(info,15);
        info.addView(label("EN AZ ÜCRET",11,PRIMARY,true));gap(info,7);
        info.addView(label(r.minFee==0?"Ücret sınırlaması yok":r.minFee+" TL",14,WHITE,false));gap(info,17);
        info.addView(action("FİLTRELERİ DÜZENLE",PRIMARY,BG,false,v->editFilters()),lp(-1,-2));
        body.addView(info);gap(body,15);
        LinearLayout help=panel();help.addView(label("Filtreler nasıl çalışır?",15,WHITE,true));gap(help,7);
        help.addView(label("Anahtar kelimelerdeki ifadelerden en az biri, ilan metninde geçmeli. Hariç tutulan ifadeler elenir. Ücret alt sınırı varsa, ücret yazmayan ilanlar da elenir.",13,MUTED,false));
        body.addView(help);
    }
    private EditText field(String value, int lines) {
        EditText t=new EditText(this);t.setText(value);t.setTextSize(14);t.setTextColor(DIALOG_TEXT);
        t.setHintTextColor(DIALOG_MUTED);t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PRIMARY));
        t.setMinLines(lines); t.setSelectAllOnFocus(false); return t;
    }
    private void editFilters() {
        FilterEngine.Rules r=Prefs.rules(this);
        ScrollView scroll=new ScrollView(this);
        LinearLayout form=column();form.setPadding(dp(20),dp(9),dp(20),dp(9));
        CheckBox ankara=new CheckBox(this);ankara.setText("Yalnızca Ankara ve ilçeleri");ankara.setTextColor(DIALOG_TEXT);
        ankara.setChecked(r.ankaraOnly);form.addView(ankara);gap(form,13);
        form.addView(label("Aranacak ifadeler (virgülle ayır)",12,DIALOG_MUTED,false));
        EditText include=field(r.include,2);form.addView(include);gap(form,12);
        form.addView(label("Hariç tutulan ifadeler",12,DIALOG_MUTED,false));
        EditText exclude=field(r.exclude,1);form.addView(exclude);gap(form,12);
        form.addView(label("İlçeler (boşsa tüm Ankara)",12,DIALOG_MUTED,false));
        EditText districts=field(r.districts,1);form.addView(districts);gap(form,12);
        form.addView(label("Asgari ilan ücreti (TL, 0 = sınırsız)",12,DIALOG_MUTED,false));
        EditText fee=field(Long.toString(r.minFee),1);fee.setInputType(InputType.TYPE_CLASS_NUMBER);form.addView(fee);gap(form,12);
        scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this)
          .setTitle("Eşleşme kuralları").setView(scroll)
          .setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String inc=include.getText().toString().trim();
            if (inc.isEmpty()) { include.setError("En az bir kelime gir.");return; }
            long amount;
            try { amount=Long.parseLong(fee.getText().toString().trim().isEmpty()?"0":fee.getText().toString().trim()); }
            catch(NumberFormatException e) { fee.setError("Sayısal değer gir.");return; }
            Prefs.raw(this).edit().putBoolean("ankara",ankara.isChecked()).putString("include",inc)
              .putString("exclude",exclude.getText().toString().trim())
              .putString("districts",districts.getText().toString().trim())
              .putLong("minFee",amount).apply();dialog.dismiss();render();toast("Filtreler kaydedildi.");
        }));dialog.show();
    }
    private String watchedInstagram() {
        return Prefs.raw(this).getString("instagram_sources", InstagramSources.DEFAULT);
    }
    private void saveWatchedInstagram(String accounts) {
        Prefs.raw(this).edit().putString("instagram_sources", accounts).apply();
        render();
    }
    private void showSources() {
        title("Takip edilen kaynaklar", "Instagram profil bağlantılarını buradan yönetebilirsin.");
        LinearLayout info = panel();
        info.addView(label("INSTAGRAM · İZLENEN HESAPLAR", 12, PRIMARY, true)); gap(info, 9);
        info.addView(label("Bu profiller resmî Meta API hesabın bağlandıysa uzaktan sorgulanabilir. Instagram telefon bildirimlerini açman gerekmez. Meta erişimi yoksa sadece kayıtlı kaynak olarak kalırlar.", 13, MUTED, false));
        gap(info, 14);
        List<String> accounts = InstagramSources.accounts(watchedInstagram());
        if (accounts.isEmpty()) {
            info.addView(label("Henüz hesap eklenmedi", 14, WHITE, true)); gap(info, 10);
        }
        for (String account : accounts) {
            LinearLayout entry = column();
            entry.setPadding(dp(13), dp(12), dp(13), dp(12));
            entry.setBackground(borderBackground(PANEL_ALT, LINE, 12));
            entry.addView(label("@" + account, 16, WHITE, true)); gap(entry, 10);
            LinearLayout buttons = row();
            buttons.addView(action("Profili aç", PANEL, PRIMARY, true, v -> openInstagramProfile(account)), weight(0, 41, 1));
            gap(buttons, 7);
            buttons.addView(action("Kaldır", PANEL, 0xffffb8c6, true, v -> confirmRemoveInstagram(account)), weight(0, 41, 1));
            entry.addView(buttons);
            LinearLayout.LayoutParams p = lp(-1, -2); p.bottomMargin = dp(10);
            info.addView(entry, p);
        }
        gap(info, 3);
        info.addView(action("＋  INSTAGRAM HESABI EKLE", PRIMARY, BG, false, v -> addInstagramDialog(null)), lp(-1, -2));
        gap(info,8);
        info.addView(action("▣  INSTAGRAM GÖRSELİNDEN OKU", PANEL_ALT, WHITE, true, v -> openImagePicker()), lp(-1,-2));
        body.addView(info); gap(body, 14);
        LinearLayout hint = panel();
        hint.addView(label("BİLDİRİMSİZ INSTAGRAM TAKİBİ · RESMÎ META API", 15, WHITE, true)); gap(hint, 11);
        hint.addView(label("Meta Business Discovery iznine sahip profesyonel Instagram hesabın ve erişim tokenın varsa takip listesindeki profesyonel hesapların gönderileri API üzerinden sorgulanır. Telefonun Instagram bildirimlerini açmana gerek yok. Herkese açık kişisel hesaplar, hikâyeler ve izin verilmeyen içerikler okunamaz.", 13, MUTED, false));
        gap(hint, 12);
        String igStatus = Prefs.raw(this).getString("meta_last_status", "Henüz API taraması yapılmadı");
        hint.addView(label("DURUM: " + igStatus, 12, PRIMARY, false)); gap(hint, 9);
        long last = Prefs.raw(this).getLong("meta_last_time", 0);
        if (last > 0) { hint.addView(label("Son deneme: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date(last)), 12, MUTED, false)); gap(hint, 10); }
        hint.addView(action(InstagramSecureConfig.configured(this) ? "META BAĞLANTISINI DÜZENLE" : "META API BAĞLANTISI KUR", PRIMARY, BG, false, v -> editMetaCredentials()), lp(-1,-2));
        gap(hint, 8);
        hint.addView(action("ŞİMDİ TARA · API TESTİ", PANEL_ALT, WHITE, true, v -> scanInstagramNow()), lp(-1,-2));
        gap(hint, 9);
        hint.addView(action(Prefs.raw(this).getBoolean("meta_enabled", false) ? "PERİYODİK TARAMAYI DURDUR" : "PERİYODİK TARAMAYI BAŞLAT", PANEL_ALT, WHITE, true, v -> toggleInstagramScan()), lp(-1,-2));
        gap(hint, 10);
        hint.addView(label("Android arka plan taraması en erken yaklaşık 15 dakikada bir yapılır; pil tasarrufu ve bağlantı durumuna göre gecikebilir. Gerçek zamanlı sunucu taraması değildir.", 12, MUTED, false));
        body.addView(hint); gap(body, 14);
        LinearLayout limitations = panel();
        limitations.addView(label("Önemli sınırlama", 14, PRIMARY, true)); gap(limitations, 8);
        limitations.addView(label("@tevkiliniz hesabının Business/Creator türünde olduğu veya hesabının Meta API erişimi bulunduğu henüz doğrulanmadı. Bağlantı kurulamazsa durum alanında açık hata gösterilir. Meta erişimi olmadan ilanlar otomatik gelmez. Alternatif olarak Instagram'ın Paylaş menüsünden bir ilanı elle ekleyebilirsin.", 13, MUTED, false));
        body.addView(limitations);
    }
    private void openInstagramProfile(String account) {
        String url = InstagramSources.profileUrl(account);
        if (url.isEmpty()) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("Instagram profili açılamadı."); }
    }
    private void confirmRemoveInstagram(String account) {
        new AlertDialog.Builder(this).setTitle("@" + account + " kaldırılsın mı?")
            .setMessage("Bu hesap API tarama listesinden ve bildirim eşleştirmesinden çıkarılacak.")
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaldır", (d, w) -> { saveWatchedInstagram(InstagramSources.remove(watchedInstagram(), account)); toast("Hesap kaldırıldı."); })
            .show();
    }
    private void addInstagramDialog(String suggested) {
        EditText input = field(suggested == null ? "" : suggested, 1);
        input.setHint("https://www.instagram.com/kullaniciadi/");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setPadding(dp(18), dp(11), dp(18), dp(11));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Instagram hesabı ekle")
            .setMessage("Profil bağlantısını veya @kullaniciadi yaz. Uzaktan takip için ayrıca Meta API erişimi gereklidir.")
            .setView(input).setNegativeButton("Vazgeç", null)
            .setPositiveButton("Ekle", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                saveWatchedInstagram(InstagramSources.add(watchedInstagram(), input.getText().toString()));
                dialog.dismiss(); toast("Hesap izlenen kaynaklara eklendi.");
            } catch (IllegalArgumentException ex) { input.setError(ex.getMessage()); }
        }));
        dialog.show();
    }
    /** Optional API integration. The app never asks for the user's Instagram password. */
    private void editMetaCredentials() {
        LinearLayout form = column(); form.setPadding(dp(20), dp(10), dp(20), dp(4));
        form.addView(label("Kendi profesyonel Instagram IG User ID (sayısal)", 13, DIALOG_TEXT, false));
        EditText id = field(Prefs.raw(this).getString("meta_ig_id", ""), 1);
        id.setInputType(InputType.TYPE_CLASS_NUMBER); id.setHint("1784..."); form.addView(id); gap(form, 12);
        form.addView(label("Meta Graph API erişim tokenı (Instagram şifren DEĞİL)", 13, DIALOG_TEXT, false));
        EditText token = field("", 1); token.setHint(InstagramSecureConfig.getToken(this).isEmpty() ? "Erişim tokenını yapıştır" : "Değiştirmeyeceksen boş bırak");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); form.addView(token);
        gap(form, 12);
        form.addView(label("Bu bağlantı yalnızca yetkili Meta geliştirici erişimiyle çalışır. Instagram şifreni buraya girme. Gerekli izinler yoksa takip işlemi başlamaz.", 12, DIALOG_MUTED, false));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Meta API bağlantısı")
                .setView(form).setNeutralButton("Bağlantıyı sil", (d, which) -> {
                    InstagramSecureConfig.clear(this);toast("Meta API anahtarı silindi.");render();
                })
                .setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String numericId = id.getText().toString().trim();
            if (!numericId.matches("[0-9]{5,30}")) { id.setError("Sayısal IG User ID gerekli"); return; }
            try {
                String supplied = token.getText().toString().trim();
                if (!supplied.isEmpty()) InstagramSecureConfig.saveToken(this, supplied);
                if (InstagramSecureConfig.getToken(this).isEmpty()) {token.setError("Geçerli token gerekli");return;}
                Prefs.raw(this).edit().putString("meta_ig_id", numericId).apply();
                InstagramScanJob.schedule(this);
                dialog.dismiss();render();toast("Bağlantı bilgileri saklandı. Gerçek erişim için Şimdi Tara ile dene.");
            } catch (Exception e) { toast("Token şifrelenemedi; kayıt yapılmadı."); }
        }));
        dialog.show();
    }

    private void toggleInstagramScan() {
        boolean enabled = Prefs.raw(this).getBoolean("meta_enabled", false);
        if (!enabled && !InstagramSecureConfig.configured(this)) { toast("Önce Meta API bağlantısı kurmalısın."); return; }
        Prefs.raw(this).edit().putBoolean("meta_enabled", !enabled).apply();
        InstagramScanJob.schedule(this);
        render();
        toast(enabled ? "Periyodik Instagram taraması durduruldu." : "Periyodik Instagram taraması planlandı.");
    }

    private void scanInstagramNow() {
        if (!InstagramSecureConfig.configured(this)) { toast("Önce Meta API bilgilerini gir."); return; }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 105);
        toast("Resmî Meta API ile sorgulanıyor...");
        new Thread(() -> {
            InstagramRemoteScanner.Result result = InstagramRemoteScanner.scan(getApplicationContext());
            runOnUiThread(() -> {toast(result.status);render();});
        }, "instagram-test-scan").start();
    }

    private void showSettings() {
        title("Kaynaklar ve güvenlik","Hesap şifreleri uygulamaya girilmez.");
        LinearLayout c=panel();
        c.addView(label("BİLDİRİM ERİŞİMİ",11,PRIMARY,true));gap(c,7);
        c.addView(label(listenerEnabled()?"✓ Cihaz izni verildi":"○ İzin verilmedi",16,WHITE,true));gap(c,8);
        c.addView(label("WhatsApp, WhatsApp Business, Telegram ve Instagram bildirimi geldiğinde ilanın metnini inceler.",13,MUTED,false));gap(c,14);
        c.addView(action(listenerEnabled()?"BİLDİRİM İZİN AYARLARI":"BİLDİRİM ERİŞİMİ VER",PRIMARY,BG,false,v->enableMonitoring()),lp(-1,-2));
        gap(c,10);
        c.addView(action(monitoringEnabled()?"TAKİBİ DURDUR":"TAKİBİ BAŞLAT",PANEL_ALT,WHITE,true,v->{
            boolean newState=!monitoringEnabled();
            Prefs.raw(this).edit().putBoolean("monitor",newState).apply();
            if(newState && !listenerEnabled()) requestListener();
            render();
        }),lp(-1,-2));
        body.addView(c);gap(body,13);
        LinearLayout sources = panel();
        sources.addView(label("INSTAGRAM PROFİLLERİ",11,PRIMARY,true));gap(sources,7);
        sources.addView(label(InstagramSources.accounts(watchedInstagram()).size()+" hesap ekli. Meta API erişimi sağlandıysa uzaktan tarama yapılabilir; Instagram bildirimleri şart değil.",13,MUTED,false));gap(sources,12);
        sources.addView(action("KAYNAKLARI YÖNET",PANEL_ALT,WHITE,true,v->{section="sources";render();}),lp(-1,-2));
        body.addView(sources);gap(body,13);
        LinearLayout r=panel();r.addView(label("BAŞVURU MESAJIN",11,PRIMARY,true));gap(r,10);
        r.addView(label(Prefs.reply(this),13,WHITE,false));gap(r,13);
        r.addView(action("HAZIR MESAJI DÜZENLE",PANEL_ALT,WHITE,true,v->editReply()),lp(-1,-2));
        gap(r,10);
        r.addView(label("INSTAGRAM YORUM ŞABLONU",11,PRIMARY,true));gap(r,8);
        r.addView(label(Prefs.comment(this),13,WHITE,false));gap(r,10);
        r.addView(action("YORUM ŞABLONUNU DÜZENLE",PANEL_ALT,WHITE,true,v->editComment()),lp(-1,-2));
        body.addView(r);gap(body,13);
        LinearLayout privacy=panel();privacy.addView(label("VERİLER",11,PRIMARY,true));gap(privacy,8);
        privacy.addView(label("İlanlar cihazındaki yerel veritabanında kalır. Meta API kullanırsan kimlik doğrulama tokenı Android Keystore ile şifrelenir; API isteği doğrudan Meta sunucusuna gönderilir.",13,MUTED,false));gap(privacy,12);
        privacy.addView(action("KAYITLARI SİL",0xff402b34,0xffffb8c6,true,v->confirmClear()),lp(-1,-2));
        body.addView(privacy);gap(body,13);
        LinearLayout scope=panel();scope.addView(label("BU SÜRÜMÜN SINIRLARI",11,PRIMARY,true));gap(scope,8);
        scope.addView(label("• WhatsApp/Telegram bildirim özelliği ayrı ve isteğe bağlıdır.\n\n• Instagram API taraması sadece Meta'nın izin verdiği profesyonel hesaplarda çalışır; her hesaba garanti erişim yoktur.\n\n• Instagram API başka hesapların gönderilerine otomatik yorum yazmaz. Bildirimde Yoruma hazırla ile metin kopyalanır, gönderi açılır; Gönder tuşuna Instagram içinde sen basarsın.",13,MUTED,false));
        body.addView(scope);
    }
    private void editReply() {
        EditText input=field(Prefs.reply(this),4);input.setPadding(dp(18),dp(10),dp(18),dp(10));
        new AlertDialog.Builder(this).setTitle("Varsayılan mesaj")
          .setView(input).setNegativeButton("Vazgeç",null)
          .setPositiveButton("Kaydet",(d,w)->{
            Prefs.raw(this).edit().putString("reply",input.getText().toString()).apply();render();
          }).show();
    }
    private void editComment() {
        EditText input=field(Prefs.comment(this),4);
        input.setPadding(dp(18),dp(10),dp(18),dp(10));
        new AlertDialog.Builder(this).setTitle("Instagram başvuru yorumu")
            .setMessage("Yorum hazırlanır. Son Gönder işlemi Instagram içinde yapılır.")
            .setView(input).setNegativeButton("Vazgeç",null)
            .setPositiveButton("Kaydet",(d,w)->{
                Prefs.raw(this).edit().putString("comment",input.getText().toString()).apply();render();
            }).show();
    }
    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Tüm ilanları sil")
          .setMessage("Kayıtlar cihazdan kalıcı olarak silinecek. Devam edilsin mi?")
          .setNegativeButton("İptal",null)
          .setPositiveButton("Sil",(d,w)->{store.clear();render();}).show();
    }
    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_OCR_IMAGE);
        } catch (Exception e) {
            toast("Görsel seçici açılamadı: " + e.getClass().getSimpleName());
        }
    }

    /** All OCR is processed on-device. Permission is granted by the image picker or Android share sheet. */
    private void readImageAndReview(Uri uri) {
        if (uri == null) return;
        toast("Görsel metni cihazında okunuyor...");
        new Thread(() -> {
            try {
                String content = ImageTextReader.readLocal(getApplicationContext(), uri);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (content.trim().isEmpty()) {
                        new AlertDialog.Builder(this).setTitle("Yazı bulunamadı")
                            .setMessage("Bu görselde okunabilen metin yok. Daha net bir ekran görüntüsü dene.")
                            .setPositiveButton("Tamam", null).show();
                    } else reviewOcr(content);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this)
                        .setTitle("Görsel okunamadı")
                        .setMessage("Görseli açıp okunabilir metin elde edemedim. Başka ekran görüntüsü deneyebilirsin. (" + e.getClass().getSimpleName() + ")")
                        .setPositiveButton("Tamam",null).show();
                });
            }
        }, "local-ocr").start();
    }

    /** OCR can misread dates and file numbers. Never submit without human review. */
    private void reviewOcr(String recognized) {
        TevkilOcrSummary summary = TevkilOcrSummary.fromText(recognized);
        FilterEngine.Rules r = Prefs.rules(this);
        boolean matches = FilterEngine.matches("Görselden okunmuş ilan", recognized, r);
        LinearLayout panel = column();
        panel.setPadding(dp(18),dp(6),dp(18),0);
        String structured = summary.asDisplay();
        panel.addView(label(structured.isEmpty() ? "Mahkeme/dosya/tarih bilgisi güvenle çıkarılamadı."
                : structured, 13, DIALOG_TEXT, true));
        gap(panel,9);
        panel.addView(label(matches ? "✓ Filtreye uygun görünüyor."
                : "⚠ Mevcut Ankara/konu/ücret filtrelerine uymuyor. İstersen yine de kaydedebilirsin.",
                12, matches ? DIALOG_OK : DIALOG_WARNING, true));
        gap(panel,9);
        panel.addView(label("OCR metni hatalı olabilir. Özellikle mahkeme, tarih ve ücret bilgilerini asıl ilandan doğrula.",12,DIALOG_MUTED,false));
        gap(panel,7);
        EditText editable = field(recognized, 7);
        editable.setMaxLines(16);
        panel.addView(editable, lp(-1,-2));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Görselden okunan tevkil ilanı")
            .setView(scroll)
            .setNegativeButton("Vazgeç",null)
            .setPositiveButton("Kontrol ettim, kaydet",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String edited = editable.getText().toString().trim();
            if (edited.isEmpty()) {editable.setError("Metin boş olamaz");return;}
            TevkilOcrSummary actual = TevkilOcrSummary.fromText(edited);
            String headline = actual.court.isEmpty() ? "Görselden okunan ilan" : actual.court;
            long id = store.add("Görsel OCR · Kullanıcı onaylı", headline, edited);
            dialog.dismiss();
            toast(id < 0 ? "Bu metin önceden kaydedilmiş." : "Görselden çıkarılan ilan cihazına kaydedildi.");
            section="inbox";render();
        }));
        dialog.show();
    }

    private void manualAdd() {
        EditText input=field("",5);input.setHint("İlan metnini veya Instagram/Telegram bağlantısını yapıştır...");
        input.setPadding(dp(18),dp(10),dp(18),dp(10));
        new AlertDialog.Builder(this).setTitle("İlan ekle")
          .setView(input).setNegativeButton("İptal",null)
          .setPositiveButton("Kaydet",(d,w)->{addManual(input.getText().toString());}).show();
    }
    private void demo() {
        String text="Ankara Çankaya 4. İş Mahkemesi 18.11.2026 saat 10:30 duruşması için tevkil aranmaktadır. Ücret 1.500 TL. İlgilenen meslektaşlar mesaj yazabilir.";
        if (FilterEngine.matches("Örnek ilan",text,Prefs.rules(this))) {
            long id=store.add("Örnek", "Örnek ilan · Çankaya", text);
            toast(id<0?"Bu örnek bugün zaten kaydedildi.":"Örnek ilan eklendi.");render();
        } else new AlertDialog.Builder(this).setTitle("Örnek filtreye uymadı")
          .setMessage("Mevcut filtreler örnek ilanı elemektedir. Yine de kaydetmek ister misin?")
          .setNegativeButton("Hayır",null).setPositiveButton("Kaydet",(d,w)->{store.add("Örnek","Örnek ilan · Çankaya",text);render();}).show();
    }
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.hasExtra("quick_comment")) {
            quickCommentRequested = intent.getBooleanExtra("quick_comment", false);
            intent.removeExtra("quick_comment");
        }
        if (intent.hasExtra("notice_id")) { requestedNotice=intent.getLongExtra("notice_id",-1);intent.removeExtra("notice_id"); }
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null &&
                intent.getType().startsWith("image/")) {
            Uri shared = null;
            if (Build.VERSION.SDK_INT >= 33) shared = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            else shared = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            intent.removeExtra(Intent.EXTRA_STREAM);
            if (shared != null) readImageAndReview(shared);
            return;
        }
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                intent.removeExtra(Intent.EXTRA_TEXT);
                final String text=shared.toString();
                String account = InstagramSources.parse(text);
                if (account != null && (text.toLowerCase(Locale.ROOT).contains("instagram.com/") || text.trim().startsWith("@"))) {
                    new AlertDialog.Builder(this).setTitle("Instagram kaynağı eklensin mi?")
                      .setMessage("@"+account+" profilini izlenen kaynaklara ekle. Bildirimde profil adı ve metin görünüyorsa eşleştirilebilir.")
                      .setNegativeButton("Vazgeç",null)
                      .setPositiveButton("Kaynağı ekle",(d,w)->{
                          try { saveWatchedInstagram(InstagramSources.add(watchedInstagram(),account)); section="sources";render(); }
                          catch(IllegalArgumentException e) { toast(e.getMessage()); }
                      }).show();
                    return;
                }
                new AlertDialog.Builder(this).setTitle("Paylaşılan tevkil ilanı")
                  .setMessage(text.length()>450 ? text.substring(0,450)+"…":text)
                  .setNegativeButton("Vazgeç",null)
                  .setPositiveButton("İlanlara ekle",(d,w)->addManual(text)).show();
            }
        }
    }
    private void addManual(String text) {
        if (text == null || text.trim().isEmpty()) {toast("İlan metni boş olamaz.");return;}
        long id=store.add("Paylaşım","Manuel eklenen ilan",text.trim());
        toast(id==-1?"Bu ilan bugün zaten kaydedilmiş.":"İlan kaydedildi.");
        section="inbox";render();
    }
    private boolean listenerEnabled() {
        String names = Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        ComponentName component = new ComponentName(this,RadarListener.class);
        return names != null && names.contains(component.flattenToString());
    }
    private boolean monitoringEnabled() {return Prefs.raw(this).getBoolean("monitor",false);}
    private void enableMonitoring() {
        Prefs.raw(this).edit().putBoolean("monitor",true).apply();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
        }
        requestListener();
    }
    private void requestListener() {
        try {startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
        catch(Exception e) {toast("Bildirim erişimi ayarları açılamadı.");}
    }
}
