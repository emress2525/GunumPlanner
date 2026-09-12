package com.emre.nexusai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 11, 18);
    private static final int CARD = Color.rgb(18, 21, 32);
    private static final int CARD_2 = Color.rgb(24, 28, 42);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(157, 166, 190);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int CYAN = Color.rgb(60, 211, 255);

    private EditText promptInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(buildHeader());
        root.addView(space(22));
        root.addView(buildPromptPanel());
        root.addView(space(24));
        root.addView(sectionTitle("Hızlı modlar", "Ne yapmak istediğini seç veya yukarıya direkt yaz."));
        root.addView(space(12));
        root.addView(buildQuickGrid());
        root.addView(space(28));
        root.addView(sectionTitle("Uzman motorlar", "Her görev için güçlü aracı tek merkezden aç."));
        root.addView(space(12));

        for (ProviderRegistry.Provider provider : ProviderRegistry.all()) {
            root.addView(buildProviderCard(provider));
            root.addView(space(10));
        }

        root.addView(space(14));
        TextView note = text("Nexus AI, sağlayıcıların resmi web uygulamalarını açar. Hesap gerektiren servislerde giriş kendi hesabınla yapılır; gizli API anahtarı APK içine gömülmez.", 12, MUTED, Typeface.NORMAL);
        note.setLineSpacing(0f, 1.25f);
        root.addView(note);

        setContentView(scroll);
    }

    private View buildHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView orb = text("N", 18, Color.WHITE, Typeface.BOLD);
        orb.setGravity(Gravity.CENTER);
        orb.setBackground(round(PURPLE, 16));
        row.addView(orb, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = text("NEXUS AI", 23, TEXT, Typeface.BOLD);
        title.setLetterSpacing(0.08f);
        titles.addView(title);
        titles.addView(text("Akıllı çoklu-yapay-zekâ merkezi", 13, MUTED, Typeface.NORMAL));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView live = text("● CANLI", 11, CYAN, Typeface.BOLD);
        live.setPadding(dp(10), dp(7), dp(10), dp(7));
        live.setBackground(strokeRound(Color.rgb(12, 36, 48), CYAN, 14, 1));
        row.addView(live);
        box.addView(row);

        TextView hero = text("Tek istek.\nDoğru yapay zekâ.", 36, TEXT, Typeface.BOLD);
        hero.setLineSpacing(0f, 0.92f);
        hero.setPadding(0, dp(30), 0, dp(8));
        box.addView(hero);

        TextView sub = text("Kodlama, araştırma, görsel, video, ses ve müzik için isteğini analiz eder; uygun uzman motora yönlendirir.", 15, MUTED, Typeface.NORMAL);
        sub.setLineSpacing(0f, 1.25f);
        box.addView(sub);
        return box;
    }

    private View buildPromptPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(strokeRound(CARD, Color.rgb(48, 54, 76), 24, 1));

        promptInput = new EditText(this);
        promptInput.setHint("Ne yapmak istiyorsun? Örn: Android uygulamamı kodla ve test et…");
        promptInput.setHintTextColor(Color.rgb(104, 113, 139));
        promptInput.setTextColor(TEXT);
        promptInput.setTextSize(16);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        promptInput.setMinLines(4);
        promptInput.setMaxLines(8);
        promptInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        promptInput.setBackground(round(Color.rgb(12, 15, 24), 17));
        panel.addView(promptInput, new LinearLayout.LayoutParams(-1, dp(128)));
        panel.addView(space(12));

        TextView route = actionButton("✦  AKILLI YÖNLENDİR", PURPLE);
        route.setOnClickListener(v -> smartRoute());
        panel.addView(route, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView micro = text("Görev türünü otomatik algılar • Prompt panoya da kopyalanır", 11, MUTED, Typeface.NORMAL);
        micro.setGravity(Gravity.CENTER);
        micro.setPadding(0, dp(10), 0, 0);
        panel.addView(micro);
        return panel;
    }

    private View buildQuickGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        addQuick(grid, "</>", "Kodlama", "CLAUDE");
        addQuick(grid, "⌕", "Araştırma", "PERPLEXITY");
        addQuick(grid, "◈", "Görsel", "GROK");
        addQuick(grid, "▶", "Video", "GEMINI");
        addQuick(grid, "◉", "Ses", "ELEVENLABS");
        addQuick(grid, "♫", "Müzik", "SUNO");
        return grid;
    }

    private void addQuick(GridLayout grid, String icon, String label, String key) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(14), dp(14), dp(14), dp(14));
        tile.setBackground(strokeRound(CARD, Color.rgb(43, 48, 68), 18, 1));
        tile.setOnClickListener(v -> openProvider(key));

        TextView iconView = text(icon, 18, CYAN, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        tile.addView(iconView, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView labelView = text(label, 14, TEXT, Typeface.BOLD);
        labelView.setPadding(dp(8), 0, 0, 0);
        tile.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1f));

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(64);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(0, 0, dp(8), dp(8));
        grid.addView(tile, lp);
    }

    private View buildProviderCard(ProviderRegistry.Provider provider) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(14), dp(13), dp(14));
        card.setBackground(strokeRound(CARD_2, Color.rgb(45, 51, 72), 20, 1));
        card.setOnClickListener(v -> openProvider(provider.key));

        TextView avatar = text(provider.name.substring(0, 1), 16, Color.WHITE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(providerColor(provider.key), 15));
        card.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);
        info.addView(text(provider.name, 15, TEXT, Typeface.BOLD));
        TextView spec = text(provider.specialty, 12, MUTED, Typeface.NORMAL);
        spec.setPadding(0, dp(3), 0, 0);
        info.addView(spec);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView badge = text(provider.badge, 10, CYAN, Typeface.BOLD);
        badge.setPadding(dp(9), dp(6), dp(9), dp(6));
        badge.setBackground(round(Color.rgb(17, 41, 54), 12));
        card.addView(badge);

        TextView arrow = text("  ›", 24, MUTED, Typeface.NORMAL);
        card.addView(arrow);
        return card;
    }

    private void smartRoute() {
        String prompt = promptInput.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Önce isteğini yaz.", Toast.LENGTH_SHORT).show();
            promptInput.requestFocus();
            return;
        }
        String key = Router.route(prompt);
        ProviderRegistry.Provider provider = ProviderRegistry.get(key);
        Toast.makeText(this, "En uygun motor: " + (provider == null ? key : provider.name), Toast.LENGTH_SHORT).show();
        openProvider(key);
    }

    private void openProvider(String key) {
        ProviderRegistry.Provider provider = ProviderRegistry.get(key);
        if (provider == null) return;
        Intent intent = new Intent(this, ProviderWebActivity.class);
        intent.putExtra("providerKey", provider.key);
        intent.putExtra("prompt", promptInput == null ? "" : promptInput.getText().toString().trim());
        startActivity(intent);
    }

    private View sectionTitle(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(title, 19, TEXT, Typeface.BOLD));
        TextView sub = text(subtitle, 12, MUTED, Typeface.NORMAL);
        sub.setPadding(0, dp(4), 0, 0);
        box.addView(sub);
        return box;
    }

    private TextView actionButton(String label, int color) {
        TextView v = text(label, 14, Color.WHITE, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setLetterSpacing(0.04f);
        v.setBackground(round(color, 17));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans", style));
        return v;
    }

    private View space(int dp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return s;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable strokeRound(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable g = round(fill, radiusDp);
        g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private int providerColor(String key) {
        switch (key) {
            case "CLAUDE": return Color.rgb(196, 111, 70);
            case "GEMINI": return Color.rgb(70, 110, 232);
            case "GROK": return Color.rgb(45, 48, 56);
            case "PERPLEXITY": return Color.rgb(31, 144, 151);
            case "DEEPSEEK": return Color.rgb(72, 104, 214);
            case "ELEVENLABS": return Color.rgb(76, 76, 86);
            case "SUNO": return Color.rgb(151, 73, 212);
            default: return PURPLE;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
