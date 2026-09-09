package com.emre.gunumplanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModernMainActivity extends Activity {
    private static final int VOICE_REQUEST = 1902;
    private final Locale tr = new Locale("tr", "TR");
    private final DateTimeFormatter nice = DateTimeFormatter.ofPattern("d MMMM EEEE", tr);
    private final DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm");

    private Db db;
    private LinearLayout content;
    private LinearLayout chips;
    private TextView pageTitle;
    private TextView pageSub;
    private Button addButton;
    private Button navToday, navHistory, navTopics, navSearch;
    private Screen screen = Screen.TODAY;
    private String day = LocalDate.now().toString();
    private long topicId = 0;
    private boolean voiceTask = true;
    private String lastSearch = "";

    private enum Screen { TODAY, HISTORY, TOPICS, TOPIC, SEARCH }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new Db(this);
        setupWindow();
        buildShell();
        askNotificationPermission();
        showToday();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) refresh();
    }

    @Override public void onBackPressed() {
        if (screen == Screen.TOPIC) { showTopics(); return; }
        if (screen != Screen.TODAY) { showToday(); return; }
        super.onBackPressed();
    }

    private void setupWindow() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(ModernUi.BG);
            getWindow().setNavigationBarColor(ModernUi.SURFACE);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ModernUi.BG);
        root.setPadding(ModernUi.dp(this, 14), ModernUi.dp(this, 12), ModernUi.dp(this, 14), ModernUi.dp(this, 10));

        LinearLayout header = ModernUi.card(this, true);
        header.setPadding(ModernUi.dp(this, 18), ModernUi.dp(this, 16), ModernUi.dp(this, 18), ModernUi.dp(this, 14));

        TextView brand = ModernUi.label(this, "GÜNÜM", 11, ModernUi.PRIMARY, true);
        brand.setLetterSpacing(0.08f);
        header.addView(brand);

        pageTitle = ModernUi.label(this, "", 28, ModernUi.TEXT, true);
        pageTitle.setPadding(0, ModernUi.dp(this, 3), 0, 0);
        header.addView(pageTitle);

        pageSub = ModernUi.label(this, "", 14, ModernUi.SUB, false);
        pageSub.setPadding(0, ModernUi.dp(this, 4), 0, ModernUi.dp(this, 10));
        header.addView(pageSub);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(chips, new HorizontalScrollView.LayoutParams(-2, -2));
        header.addView(hs);
        root.addView(header, ModernUi.margin(this, -1, -2, 0, 0, 0, 10));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, ModernUi.dp(this, 8));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        addButton = ModernUi.primaryButton(this, "＋ Görev / Not ekle");
        addButton.setOnClickListener(v -> addChooser());
        root.addView(addButton, ModernUi.margin(this, -1, ModernUi.dp(this, 56), 0, 4, 0, 8));

        LinearLayout navCard = ModernUi.card(this, false);
        navCard.setPadding(ModernUi.dp(this, 7), ModernUi.dp(this, 7), ModernUi.dp(this, 7), ModernUi.dp(this, 7));
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        navToday = ModernUi.navButton(this, "Bugün", true);
        navHistory = ModernUi.navButton(this, "Geçmiş", false);
        navTopics = ModernUi.navButton(this, "Konular", false);
        navSearch = ModernUi.navButton(this, "Ara", false);
        navToday.setOnClickListener(v -> showToday());
        navHistory.setOnClickListener(v -> pickHistory());
        navTopics.setOnClickListener(v -> showTopics());
        navSearch.setOnClickListener(v -> searchDialog());
        nav.addView(navToday, ModernUi.weight(this));
        nav.addView(navHistory, ModernUi.weight(this));
        nav.addView(navTopics, ModernUi.weight(this));
        nav.addView(navSearch, ModernUi.weight(this));
        navCard.addView(nav, new LinearLayout.LayoutParams(-1, ModernUi.dp(this, 45)));
        root.addView(navCard, new LinearLayout.LayoutParams(-1, ModernUi.dp(this, 59)));

        setContentView(root);
    }

    private void showToday() {
        screen = Screen.TODAY;
        day = LocalDate.now().toString();
        topicId = 0;
        Db.DayStats s = db.getDayStats(day);
        setHeader("Bugün", LocalDate.now().format(nice) + " • gününü sakin ve net tut",
                ModernUi.chip(this, s.open + " açık", ModernUi.PRIMARY_SOFT, ModernUi.PRIMARY),
                ModernUi.chip(this, s.done + " tamam", 0xFFE8F8EE, ModernUi.SUCCESS),
                ModernUi.chip(this, s.notes + " not", 0xFFFFF4DD, ModernUi.WARNING));
        setNav();
        addButton.setText("＋ Görev / Not ekle");
        addButton.setOnClickListener(v -> addChooser());
        content.removeAllViews();
        addWeekStrip();
        addFocusCard();
        addQuickActions();
        section("Bugünün akışı");
        List<Db.Item> list = db.getItemsForDay(day);
        if (list.isEmpty()) {
            emptyCard("Bugün boş görünüyor", "İlk işini ekle. Saat vermek istemezsen saatsiz de bırakabilirsin.");
        } else {
            for (Db.Item item : list) itemCard(item, false);
        }
    }

    private void addWeekStrip() {
        LinearLayout wrap = ModernUi.card(this, false);
        wrap.setPadding(ModernUi.dp(this, 8), ModernUi.dp(this, 8), ModernUi.dp(this, 8), ModernUi.dp(this, 8));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LocalDate today = LocalDate.now();
        for (int d = -3; d <= 3; d++) {
            LocalDate date = today.plusDays(d);
            boolean active = d == 0;
            String label = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, tr) + "\n" + date.getDayOfMonth();
            Button b = ModernUi.navButton(this, label, active);
            b.setMinWidth(ModernUi.dp(this, 58));
            b.setGravity(Gravity.CENTER);
            b.setOnClickListener(v -> {
                if (date.equals(LocalDate.now())) showToday();
                else showHistory(date.toString());
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ModernUi.dp(this, 62), ModernUi.dp(this, 54));
            lp.setMargins(ModernUi.dp(this, 3), 0, ModernUi.dp(this, 3), 0);
            row.addView(b, lp);
        }
        hs.addView(row);
        wrap.addView(hs);
        content.addView(wrap, ModernUi.margin(this, -1, -2, 0, 0, 0, 10));
    }

    private void addFocusCard() {
        Db.Item next = nextOpenTask();
        LinearLayout card = ModernUi.card(this, true);
        card.setPadding(ModernUi.dp(this, 18), ModernUi.dp(this, 14), ModernUi.dp(this, 18), ModernUi.dp(this, 14));
        card.addView(ModernUi.label(this, "ŞİMDİ", 11, ModernUi.PRIMARY, true));
        if (next == null) {
            TextView title = ModernUi.label(this, "Bugün için açık görev kalmadı", 19, ModernUi.TEXT, true);
            title.setPadding(0, ModernUi.dp(this, 5), 0, 0);
            card.addView(title);
            TextView meta = ModernUi.label(this, "Yeni bir iş ekleyebilir veya gün özetine bakabilirsin.", 13, ModernUi.SUB, false);
            meta.setPadding(0, ModernUi.dp(this, 5), 0, 0);
            card.addView(meta);
            card.setOnClickListener(v -> addChooser());
        } else {
            TextView title = ModernUi.label(this, next.title, 19, ModernUi.TEXT, true);
            title.setPadding(0, ModernUi.dp(this, 5), 0, 0);
            card.addView(title);
            TextView meta = ModernUi.label(this, focusMeta(next), 13, ModernUi.SUB, false);
            meta.setPadding(0, ModernUi.dp(this, 5), 0, 0);
            card.addView(meta);
            card.setOnClickListener(v -> details(next));
            card.setOnLongClickListener(v -> { itemActions(next); return true; });
        }
        content.addView(card, ModernUi.margin(this, -1, -2, 0, 0, 0, 10));
    }

    private Db.Item nextOpenTask() {
        long now = System.currentTimeMillis();
        Db.Item fallback = null;
        for (Db.Item i : db.getItemsForDay(LocalDate.now().toString())) {
            if (!Db.TYPE_TASK.equals(i.type) || Db.STATUS_DONE.equals(i.status)) continue;
            if (fallback == null) fallback = i;
            if (i.dueAt >= now && i.dueAt > 0) return i;
        }
        return fallback;
    }

    private String focusMeta(Db.Item i) {
        String m;
        if (i.dueAt > 0) {
            long mins = Math.max(0, (i.dueAt - System.currentTimeMillis()) / 60000L);
            String t = Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock);
            m = t + (mins > 0 ? " • " + mins + " dk kaldı" : " • zamanı geldi");
        } else m = "Saati belirlenmemiş";
        if (i.durationMinutes > 0) m += " • " + i.durationMinutes + " dk";
        return m;
    }

    private void addQuickActions() {
        LinearLayout card = ModernUi.card(this, false);
        card.setPadding(ModernUi.dp(this, 10), ModernUi.dp(this, 10), ModernUi.dp(this, 10), ModernUi.dp(this, 10));
        TextView label = ModernUi.label(this, "Hızlı işlemler", 12, ModernUi.SUB, true);
        label.setPadding(ModernUi.dp(this, 5), 0, 0, ModernUi.dp(this, 8));
        card.addView(label);
        LinearLayout row = new LinearLayout(this);
        Button voice = ModernUi.softButton(this, "🎙 Ses");
        Button plan = ModernUi.softButton(this, "✨ Planla");
        Button summary = ModernUi.softButton(this, "Özet");
        Button more = ModernUi.softButton(this, "•••");
        voice.setOnClickListener(v -> voiceChooser());
        plan.setOnClickListener(v -> smartPlan());
        summary.setOnClickListener(v -> showDaySummary(day));
        more.setOnClickListener(v -> openLegacy());
        row.addView(voice, ModernUi.weight(this));
        row.addView(plan, ModernUi.weight(this));
        row.addView(summary, ModernUi.weight(this));
        row.addView(more, ModernUi.weight(this));
        card.addView(row, new LinearLayout.LayoutParams(-1, ModernUi.dp(this, 44)));
        content.addView(card, ModernUi.margin(this, -1, -2, 0, 0, 0, 10));
    }

    private void showHistory(String key) {
        screen = Screen.HISTORY;
        day = key;
        topicId = 0;
        Db.DayStats s = db.getDayStats(key);
        setHeader("Geçmiş", LocalDate.parse(key).format(nice) + " • o gün ne olmuş?",
                ModernUi.chip(this, s.done + " tamam", 0xFFE8F8EE, ModernUi.SUCCESS),
                ModernUi.chip(this, s.open + " açık", ModernUi.PRIMARY_SOFT, ModernUi.PRIMARY),
                ModernUi.chip(this, s.postponed + " erteleme", 0xFFFFEFEF, ModernUi.DANGER));
        setNav();
        addButton.setText("＋ Bu güne kayıt ekle");
        addButton.setOnClickListener(v -> addChooser());
        content.removeAllViews();

        section("O güne bağlı kayıtlar");
        List<Db.Item> items = db.getItemsForDay(key);
        if (items.isEmpty()) emptyCard("Aktif kayıt yok", "Taşınmış veya silinmiş işler aşağıdaki hareket günlüğünde kalır.");
        else for (Db.Item i : items) itemCard(i, true);

        section("Hareket günlüğü");
        List<Db.Event> events = db.getEventsForDay(key);
        if (events.isEmpty()) emptyCard("Hareket bulunamadı", "Bu tarihte kayıtlı bir işlem görünmüyor.");
        else for (Db.Event e : events) eventCard(e);
    }

    private void pickHistory() {
        LocalDate d;
        try { d = LocalDate.parse(day); } catch (Exception e) { d = LocalDate.now(); }
        new DatePickerDialog(this, (v,y,m,dd) -> showHistory(LocalDate.of(y,m+1,dd).toString()),
                d.getYear(), d.getMonthValue()-1, d.getDayOfMonth()).show();
    }

    private void showTopics() {
        screen = Screen.TOPICS;
        topicId = 0;
        List<Db.Topic> all = db.getTopics();
        setHeader("Konular", "Aynı konuya ait dağınık not ve görevleri tek dosyada tut.",
                ModernUi.chip(this, all.size() + " konu", 0xFFF3ECFF, ModernUi.PURPLE),
                ModernUi.chip(this, "otomatik gruplama", ModernUi.PRIMARY_SOFT, ModernUi.PRIMARY));
        setNav();
        addButton.setText("＋ Yeni not ekle");
        addButton.setOnClickListener(v -> noteDialog(0));
        content.removeAllViews();
        if (all.isEmpty()) emptyCard("Henüz konu yok", "Bir not ekle; uygulama benzer içerikleri otomatik toplamaya başlasın.");
        else for (Db.Topic t : all) topicCard(t);
    }

    private void showTopic(long id) {
        Db.Topic t = db.getTopic(id);
        if (t == null) { showTopics(); return; }
        screen = Screen.TOPIC;
        topicId = id;
        setHeader(t.title, "Bu başlığın kronolojik hafızası",
                ModernUi.chip(this, db.countItemsInTopic(id) + " kayıt", 0xFFF3ECFF, ModernUi.PURPLE),
                ModernUi.chip(this, t.locked ? "başlık kilitli" : "başlık esnek", ModernUi.PRIMARY_SOFT, ModernUi.PRIMARY));
        setNav();
        addButton.setText("＋ Bu konuya not ekle");
        addButton.setOnClickListener(v -> noteDialog(id));
        content.removeAllViews();
        List<Db.Item> items = db.getItemsForTopic(id);
        if (items.isEmpty()) emptyCard("Bu konu boş", "İlk notunu ekleyebilirsin.");
        else for (Db.Item i : items) itemCard(i, true);
    }

    private void searchDialog() {
        EditText q = field("Görev, not veya konu kelimesi", lastSearch);
        new AlertDialog.Builder(this).setTitle("Geçmişte ara").setView(wrapDialog(q))
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ara", (d,w) -> renderSearch(q.getText().toString().trim())).show();
    }

    private void renderSearch(String q) {
        lastSearch = q;
        screen = Screen.SEARCH;
        List<Db.Item> results = db.searchItems(q);
        setHeader("Arama", q.isEmpty() ? "Bir kelime yaz" : "“" + q + "” için sonuçlar",
                ModernUi.chip(this, results.size() + " sonuç", ModernUi.PRIMARY_SOFT, ModernUi.PRIMARY));
        setNav();
        addButton.setText("🔎 Yeni arama");
        addButton.setOnClickListener(v -> searchDialog());
        content.removeAllViews();
        if (results.isEmpty()) emptyCard("Sonuç bulunamadı", "Daha genel bir kelime veya başka bir başlık deneyebilirsin.");
        else for (Db.Item i : results) itemCard(i, true);
    }

    private void itemCard(Db.Item i, boolean showDate) {
        int accent = Db.TYPE_NOTE.equals(i.type) ? ModernUi.WARNING :
                (Db.STATUS_DONE.equals(i.status) ? ModernUi.SUCCESS : ModernUi.PRIMARY);
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        View bar = ModernUi.accent(this, accent);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ModernUi.dp(this, 5), -1);
        blp.setMargins(0, ModernUi.dp(this, 4), ModernUi.dp(this, 8), ModernUi.dp(this, 4));
        outer.addView(bar, blp);

        LinearLayout card = ModernUi.card(this, false);
        card.setPadding(ModernUi.dp(this, 15), ModernUi.dp(this, 13), ModernUi.dp(this, 15), ModernUi.dp(this, 13));
        String icon = Db.TYPE_NOTE.equals(i.type) ? "📝 " : (Db.STATUS_DONE.equals(i.status) ? "✓ " : "○ ");
        String pri = Db.TYPE_TASK.equals(i.type) && i.priority == 0 ? "‼ " : (Db.TYPE_TASK.equals(i.type) && i.priority == 1 ? "! " : "");
        card.addView(ModernUi.label(this, icon + pri + i.title, 16, ModernUi.TEXT, true));
        String meta = itemMeta(i, showDate);
        if (!meta.isEmpty()) {
            TextView m = ModernUi.label(this, meta, 13, ModernUi.SUB, false);
            m.setPadding(0, ModernUi.dp(this, 5), 0, 0);
            card.addView(m);
        }
        if (i.body != null && !i.body.trim().isEmpty()) {
            TextView body = ModernUi.label(this, shortText(i.body), 13, 0xFF7B8494, false);
            body.setPadding(0, ModernUi.dp(this, 6), 0, 0);
            card.addView(body);
        }
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        outer.addView(card, clp);
        outer.setOnClickListener(v -> details(i));
        outer.setOnLongClickListener(v -> { itemActions(i); return true; });
        content.addView(outer, ModernUi.margin(this, -1, -2, 0, 0, 0, 9));
    }

    private String itemMeta(Db.Item i, boolean showDate) {
        List<String> parts = new ArrayList<>();
        if (showDate) parts.add(i.dayKey);
        if (i.dueAt > 0) parts.add(Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock));
        if (i.durationMinutes > 0) parts.add(i.durationMinutes + " dk");
        if (i.recurrence != null && !i.recurrence.isEmpty()) parts.add("↻ " + NaturalLanguageParser.recurrenceLabel(i.recurrence));
        return String.join(" • ", parts);
    }

    private void eventCard(Db.Event e) {
        LinearLayout card = ModernUi.card(this, false);
        card.setPadding(ModernUi.dp(this, 15), ModernUi.dp(this, 12), ModernUi.dp(this, 15), ModernUi.dp(this, 12));
        String tm = Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock);
        card.addView(ModernUi.label(this, tm + "  " + eventName(e.eventType), 13, ModernUi.PRIMARY, true));
        TextView title = ModernUi.label(this, e.titleSnapshot, 15, ModernUi.TEXT, true);
        title.setPadding(0, ModernUi.dp(this, 5), 0, 0);
        card.addView(title);
        if (e.details != null && !e.details.isEmpty()) {
            TextView d = ModernUi.label(this, e.details, 13, ModernUi.SUB, false);
            d.setPadding(0, ModernUi.dp(this, 4), 0, 0);
            card.addView(d);
        }
        content.addView(card, ModernUi.margin(this, -1, -2, 0, 0, 0, 8));
    }

    private void topicCard(Db.Topic t) {
        LinearLayout card = ModernUi.card(this, false);
        card.setPadding(ModernUi.dp(this, 16), ModernUi.dp(this, 14), ModernUi.dp(this, 16), ModernUi.dp(this, 14));
        card.addView(ModernUi.label(this, "📁  " + t.title + (t.locked ? "  🔒" : ""), 17, ModernUi.TEXT, true));
        TextView meta = ModernUi.label(this, db.countItemsInTopic(t.id) + " kayıt • otomatik " + (t.autoGroup ? "açık" : "kapalı"), 13, ModernUi.SUB, false);
        meta.setPadding(0, ModernUi.dp(this, 5), 0, 0);
        card.addView(meta);
        card.setOnClickListener(v -> showTopic(t.id));
        card.setOnLongClickListener(v -> { topicActions(t); return true; });
        content.addView(card, ModernUi.margin(this, -1, -2, 0, 0, 0, 9));
    }

    private void section(String text) {
        TextView t = ModernUi.label(this, text, 13, ModernUi.SUB, true);
        t.setPadding(ModernUi.dp(this, 4), ModernUi.dp(this, 5), 0, ModernUi.dp(this, 8));
        content.addView(t);
    }

    private void emptyCard(String title, String detail) {
        LinearLayout card = ModernUi.card(this, false);
        card.setPadding(ModernUi.dp(this, 18), ModernUi.dp(this, 18), ModernUi.dp(this, 18), ModernUi.dp(this, 18));
        card.addView(ModernUi.label(this, title, 17, ModernUi.TEXT, true));
        TextView d = ModernUi.label(this, detail, 13, ModernUi.SUB, false);
        d.setPadding(0, ModernUi.dp(this, 6), 0, 0);
        card.addView(d);
        content.addView(card, ModernUi.margin(this, -1, -2, 0, 0, 0, 10));
    }

    private void addChooser() {
        new AlertDialog.Builder(this).setTitle("Ne ekleyelim?")
                .setItems(new String[]{"⚡ Hızlı görev","📝 Not","🎙 Sesle görev","🎙 Sesli not","Ayrıntılı görev"}, (d,w) -> {
                    if (w == 0) quickTaskDialog("");
                    else if (w == 1) noteDialog(screen == Screen.TOPIC ? topicId : 0);
                    else if (w == 2) startVoice(true);
                    else if (w == 3) startVoice(false);
                    else detailedTaskDialog(null);
                }).show();
    }

    private void quickTaskDialog(String initial) {
        EditText e = field("Örn: Yarın 14:30 Mehmet'i ara 30 dk", initial);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Hızlı görev").setView(wrapDialog(e))
                .setNegativeButton("Vazgeç", null).setPositiveButton("Devam", null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = e.getText().toString().trim();
            if (raw.isEmpty()) { toast("Görevi yaz"); return; }
            dlg.dismiss();
            confirmParsed(NaturalLanguageParser.parse(raw, safeDay()));
        }));
        dlg.show();
    }

    private void confirmParsed(NaturalLanguageParser.ParsedTask p) {
        StringBuilder msg = new StringBuilder();
        msg.append(p.title).append("\n\nTarih: ").append(p.dayKey);
        if (p.dueAt > 0) msg.append("\nSaat: ").append(Instant.ofEpochMilli(p.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock));
        if (p.durationMinutes > 0) msg.append("\nSüre: ").append(p.durationMinutes).append(" dk");
        if (p.recurrence != null && !p.recurrence.isEmpty()) msg.append("\nTekrar: ").append(NaturalLanguageParser.recurrenceLabel(p.recurrence));
        msg.append("\nÖncelik: ").append(priorityLabel(p.priority));
        new AlertDialog.Builder(this).setTitle("Böyle ekleyeyim mi?").setMessage(msg.toString())
                .setNegativeButton("Vazgeç", null)
                .setNeutralButton("Düzelt", (d,w) -> quickTaskDialog(p.original))
                .setPositiveButton("Ekle", (d,w) -> createParsed(p)).show();
    }

    private void createParsed(NaturalLanguageParser.ParsedTask p) {
        long tid = TopicEngine.findMatchingTopic(db, p.title, p.original);
        long id = db.insertItem(Db.TYPE_TASK, p.title, "", p.dayKey, p.dueAt, tid, p.recurrence, p.durationMinutes, p.priority);
        if (p.dueAt > 0) ReminderScheduler.schedule(this, id, p.dueAt);
        if (tid > 0) TopicEngine.refreshAutoTitle(db, tid);
        toast("Görev eklendi");
        if (p.dayKey.equals(LocalDate.now().toString())) showToday(); else showHistory(p.dayKey);
    }

    private void detailedTaskDialog(Db.Item old) {
        LinearLayout box = dialogColumn();
        EditText title = field("Görev", old == null ? "" : old.title); box.addView(title);
        EditText body = field("Not / açıklama", old == null ? "" : old.body); box.addView(body);
        EditText date = field("Tarih: YYYY-MM-DD", old == null ? safeDay().toString() : old.dayKey); box.addView(date);
        String oldTime = old != null && old.dueAt > 0 ? Instant.ofEpochMilli(old.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock) : "";
        EditText time = field("Saat: HH:mm", oldTime); box.addView(time);
        EditText duration = field("Tahmini süre (dakika)", old == null || old.durationMinutes == 0 ? "" : String.valueOf(old.durationMinutes));
        duration.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(duration);
        CheckBox important = new CheckBox(this); important.setText("Önemli"); important.setChecked(old != null && old.priority < 2); box.addView(important);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(old == null ? "Görev ekle" : "Görevi düzenle").setView(box)
                .setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = title.getText().toString().trim();
            if (n.isEmpty()) { toast("Görev adı yaz"); return; }
            String dk = date.getText().toString().trim();
            String ts = time.getText().toString().trim();
            long due = 0;
            try {
                LocalDate d = LocalDate.parse(dk);
                if (!ts.isEmpty()) due = LocalDateTime.of(d, LocalTime.parse(ts, clock)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) { toast("Tarih veya saat hatalı"); return; }
            int dur = safeInt(duration.getText().toString());
            int pri = important.isChecked() ? 1 : 2;
            if (old == null) {
                long tid = TopicEngine.findMatchingTopic(db, n, body.getText().toString());
                long id = db.insertItem(Db.TYPE_TASK, n, body.getText().toString().trim(), dk, due, tid, "", dur, pri);
                if (due > 0) ReminderScheduler.schedule(this, id, due);
            } else {
                ReminderScheduler.cancel(this, old.id);
                db.updateItem(old.id, n, body.getText().toString().trim(), dk, due, old.topicId, old.recurrence, dur, pri);
                if (due > 0 && Db.STATUS_OPEN.equals(old.status)) ReminderScheduler.schedule(this, old.id, due);
            }
            dlg.dismiss(); refresh();
        }));
        dlg.show();
    }

    private void noteDialog(long forcedTopic) {
        LinearLayout box = dialogColumn();
        EditText title = field("Not başlığı (istersen boş bırak)", ""); box.addView(title);
        EditText body = field("Notunu yaz", ""); body.setMinLines(4); box.addView(body);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Not ekle").setView(box)
                .setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String b = body.getText().toString().trim();
            String n = title.getText().toString().trim();
            if (n.isEmpty() && b.isEmpty()) { toast("Bir şey yaz"); return; }
            if (n.isEmpty()) n = TopicEngine.suggestTitle(b);
            long tid = forcedTopic > 0 ? forcedTopic : TopicEngine.findOrCreateTopic(db, n, b);
            db.insertItem(Db.TYPE_NOTE, n, b, LocalDate.now().toString(), 0, tid);
            TopicEngine.refreshAutoTitle(db, tid);
            dlg.dismiss();
            if (screen == Screen.TOPIC) showTopic(tid); else showToday();
        }));
        dlg.show();
    }

    private void details(Db.Item i) {
        StringBuilder m = new StringBuilder();
        if (i.body != null && !i.body.isEmpty()) m.append(i.body).append("\n\n");
        m.append("Tarih: ").append(i.dayKey);
        if (i.dueAt > 0) m.append("\nSaat: ").append(Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(clock));
        if (i.durationMinutes > 0) m.append("\nSüre: ").append(i.durationMinutes).append(" dk");
        if (i.topicId > 0) { Db.Topic t = db.getTopic(i.topicId); if (t != null) m.append("\nKonu: ").append(t.title); }
        new AlertDialog.Builder(this).setTitle(i.title).setMessage(m.toString()).setNegativeButton("Kapat", null)
                .setPositiveButton("İşlemler", (d,w) -> itemActions(i)).show();
    }

    private void itemActions(Db.Item i) {
        if (Db.TYPE_NOTE.equals(i.type)) {
            new AlertDialog.Builder(this).setTitle(i.title).setItems(new String[]{"Konuya git","Gelişmiş düzenle","Sil"}, (d,w) -> {
                if (w == 0 && i.topicId > 0) showTopic(i.topicId);
                else if (w == 1) openLegacy();
                else if (w == 2) { db.deleteItem(i.id); refresh(); }
            }).show();
            return;
        }
        String first = Db.STATUS_DONE.equals(i.status) ? "Tekrar aç" : "Tamamla";
        new AlertDialog.Builder(this).setTitle(i.title).setItems(new String[]{first,"Yarına ertele","Düzenle","Sil"}, (d,w) -> {
            if (w == 0) {
                if (Db.STATUS_DONE.equals(i.status)) db.reopenItem(i.id);
                else {
                    long next = db.completeItem(i.id);
                    ReminderScheduler.cancel(this, i.id);
                    scheduleNext(next);
                }
                refresh();
            } else if (w == 1) {
                LocalDate nd = LocalDate.parse(i.dayKey).plusDays(1);
                long due = 0;
                if (i.dueAt > 0) {
                    LocalTime tm = Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime();
                    due = LocalDateTime.of(nd, tm).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
                ReminderScheduler.cancel(this, i.id);
                db.postponeItem(i.id, nd.toString(), due);
                if (due > 0) ReminderScheduler.schedule(this, i.id, due);
                refresh();
            } else if (w == 2) detailedTaskDialog(i);
            else {
                ReminderScheduler.cancel(this, i.id);
                db.deleteItem(i.id);
                refresh();
            }
        }).show();
    }

    private void topicActions(Db.Topic t) {
        new AlertDialog.Builder(this).setTitle(t.title).setItems(new String[]{"Başlığı değiştir / kilitle","Otomatik gruplamayı " + (t.autoGroup ? "kapat" : "aç"),"Benzer konuları bul"}, (d,w) -> {
            if (w == 0) renameTopic(t);
            else if (w == 1) { db.setTopicAutoGroup(t.id, !t.autoGroup); showTopics(); }
            else similarTopics(t);
        }).show();
    }

    private void renameTopic(Db.Topic t) {
        LinearLayout box = dialogColumn();
        EditText name = field("Konu başlığı", t.title); box.addView(name);
        CheckBox lock = new CheckBox(this); lock.setText("Başlığı kilitle"); lock.setChecked(t.locked); box.addView(lock);
        new AlertDialog.Builder(this).setTitle("Konu ayarları").setView(box).setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", (d,w) -> {
                    String n = name.getText().toString().trim();
                    if (!n.isEmpty()) db.renameTopic(t.id, n, lock.isChecked());
                    showTopics();
                }).show();
    }

    private void similarTopics(Db.Topic source) {
        List<TopicEngine.TopicMatch> matches = TopicEngine.similarTopics(db, source.id);
        if (matches.isEmpty()) { toast("Yeterince benzer başka konu yok"); return; }
        String[] names = new String[matches.size()];
        for (int i=0;i<matches.size();i++) names[i] = matches.get(i).topic.title + " • %" + (int)Math.round(matches.get(i).score*100);
        new AlertDialog.Builder(this).setTitle("Birleştirme önerileri").setItems(names, (d,w) -> {
            Db.Topic target = matches.get(w).topic;
            new AlertDialog.Builder(this).setTitle("Birleştirilsin mi?")
                    .setMessage("“" + source.title + "” kayıtlarını “" + target.title + "” altında toplayacağım.")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Birleştir", (x,y) -> {
                        db.mergeTopics(source.id, target.id);
                        TopicEngine.refreshAutoTitle(db, target.id);
                        showTopic(target.id);
                    }).show();
        }).show();
    }

    private void showDaySummary(String key) {
        Db.DayStats s = db.getDayStats(key);
        String msg = "Tamamlanan: " + s.done + "\nAçık: " + s.open + "\nNot: " + s.notes + "\nErtelenen: " + s.postponed;
        if (s.open == 0 && s.done > 0) msg += "\n\nBugünün planı tamamlanmış görünüyor.";
        else if (s.postponed >= 3) msg += "\n\nBugün birkaç kez erteleme olmuş; süre tahminlerini biraz büyütmek iyi olabilir.";
        new AlertDialog.Builder(this).setTitle("Gün özeti").setMessage(msg).setPositiveButton("Tamam", null).show();
    }

    private void smartPlan() {
        List<Db.Item> backlog = db.getOpenTasksUpTo(LocalDate.now().toString());
        List<SmartPlanner.Plan> plans = SmartPlanner.propose(backlog, LocalDate.now());
        if (plans.isEmpty()) { toast("Planlanacak saatsiz veya gecikmiş görev yok"); return; }
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (SmartPlanner.Plan p : plans) {
            if (shown++ >= 10) { text.append("\n… ve ").append(plans.size()-10).append(" görev daha"); break; }
            LocalDateTime dt = Instant.ofEpochMilli(p.newDueAt).atZone(ZoneId.systemDefault()).toLocalDateTime();
            text.append(dt.format(DateTimeFormatter.ofPattern("dd MMM HH:mm", tr))).append("  ").append(p.title).append(" • ").append(p.durationMinutes).append(" dk\n");
        }
        new AlertDialog.Builder(this).setTitle("Akıllı plan önerisi").setMessage(text.toString().trim())
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Uygula", (d,w) -> {
                    for (SmartPlanner.Plan p : plans) {
                        ReminderScheduler.cancel(this, p.itemId);
                        db.rescheduleItem(p.itemId, p.newDayKey, p.newDueAt);
                        ReminderScheduler.schedule(this, p.itemId, p.newDueAt);
                    }
                    showToday();
                }).show();
    }

    private void voiceChooser() {
        new AlertDialog.Builder(this).setTitle("Sesle ekle").setItems(new String[]{"Görev","Not"}, (d,w) -> startVoice(w == 0)).show();
    }

    private void startVoice(boolean asTask) {
        voiceTask = asTask;
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, asTask ? "Görevi söyle" : "Notunu söyle");
        try { startActivityForResult(i, VOICE_REQUEST); }
        catch (ActivityNotFoundException ex) { toast("Telefonda konuşma tanıma servisi bulunamadı"); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != VOICE_REQUEST || resultCode != RESULT_OK || data == null) return;
        ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (r == null || r.isEmpty()) return;
        String spoken = r.get(0).trim();
        if (voiceTask) confirmParsed(NaturalLanguageParser.parse(spoken, LocalDate.now()));
        else {
            String n = TopicEngine.suggestTitle(spoken);
            long tid = TopicEngine.findOrCreateTopic(db, n, spoken);
            db.insertItem(Db.TYPE_NOTE, n, spoken, LocalDate.now().toString(), 0, tid);
            TopicEngine.refreshAutoTitle(db, tid);
            toast("Sesli not kaydedildi");
            showToday();
        }
    }

    private void openLegacy() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private void scheduleNext(long id) {
        if (id <= 0) return;
        Db.Item i = db.getItem(id);
        if (i != null && i.dueAt > System.currentTimeMillis()) ReminderScheduler.schedule(this, id, i.dueAt);
    }

    private void setHeader(String title, String sub, TextView... chipViews) {
        pageTitle.setText(title);
        pageSub.setText(sub);
        chips.removeAllViews();
        for (TextView c : chipViews) chips.addView(c);
    }

    private void setNav() {
        ModernUi.styleNav(this, navToday, screen == Screen.TODAY);
        ModernUi.styleNav(this, navHistory, screen == Screen.HISTORY);
        ModernUi.styleNav(this, navTopics, screen == Screen.TOPICS || screen == Screen.TOPIC);
        ModernUi.styleNav(this, navSearch, screen == Screen.SEARCH);
    }

    private void refresh() {
        if (screen == Screen.TODAY) showToday();
        else if (screen == Screen.HISTORY) showHistory(day);
        else if (screen == Screen.TOPICS) showTopics();
        else if (screen == Screen.TOPIC) showTopic(topicId);
        else renderSearch(lastSearch);
    }

    private LinearLayout dialogColumn() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ModernUi.dp(this, 18), ModernUi.dp(this, 8), ModernUi.dp(this, 18), 0);
        return box;
    }

    private LinearLayout wrapDialog(View v) {
        LinearLayout box = dialogColumn();
        box.addView(v);
        return box;
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF98A2B3);
        e.setText(value == null ? "" : value);
        e.setTextColor(ModernUi.TEXT);
        e.setTextSize(16);
        e.setPadding(ModernUi.dp(this, 14), ModernUi.dp(this, 13), ModernUi.dp(this, 14), ModernUi.dp(this, 13));
        e.setBackground(ModernUi.rounded(this, ModernUi.SURFACE, 14, ModernUi.LINE, 1));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, ModernUi.dp(this, 10));
        e.setLayoutParams(lp);
        return e;
    }

    private LocalDate safeDay() {
        try { return LocalDate.parse(day); } catch (Exception e) { return LocalDate.now(); }
    }

    private int safeInt(String s) {
        try { return s == null || s.trim().isEmpty() ? 0 : Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }

    private String shortText(String s) {
        String x = s.replace('\n',' ').trim();
        return x.length() > 110 ? x.substring(0,109) + "…" : x;
    }

    private String priorityLabel(int p) { return p == 0 ? "Acil" : p == 1 ? "Önemli" : "Normal"; }

    private String eventName(String e) {
        if ("CREATED".equals(e)) return "＋ Oluşturuldu";
        if ("NOTE_CREATED".equals(e)) return "📝 Not alındı";
        if ("COMPLETED".equals(e)) return "✓ Tamamlandı";
        if ("POSTPONED".equals(e)) return "→ Ertelendi";
        if ("MOVED_IN".equals(e)) return "← Taşındı";
        if ("EDITED".equals(e)) return "✎ Düzenlendi";
        if ("DELETED".equals(e)) return "⌫ Silindi";
        if ("REOPENED".equals(e)) return "↺ Tekrar açıldı";
        if ("RECUR_CREATED".equals(e)) return "↻ Tekrar oluşturuldu";
        if ("SCHEDULED".equals(e)) return "◷ Planlandı";
        if ("TOPIC_MOVED".equals(e)) return "📁 Konu değişti";
        return e;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 901);
        }
    }
}
