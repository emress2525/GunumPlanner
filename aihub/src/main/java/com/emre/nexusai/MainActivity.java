package com.emre.nexusai;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 10, 16);
    private static final int CARD = Color.rgb(18, 21, 32);
    private static final int CARD_2 = Color.rgb(24, 28, 42);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(153, 162, 187);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int CYAN = Color.rgb(67, 218, 255);
    private static final int GREEN = Color.rgb(82, 215, 148);

    private EditText promptInput;
    private LinearLayout resultContainer;
    private HorizontalScrollView modeScroll;
    private LinearLayout modeRow;
    private ProgressBar working;
    private TextView sendButton;
    private NexusAiEngine engine;
    private RequestMode selectedMode;
    private TextToSpeech tts;
    private String lastTextResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);

        engine = new NexusAiEngine(this);
        initTts();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(44));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(buildHeader());
        root.addView(space(22));
        root.addView(buildModePicker());
        root.addView(space(14));
        root.addView(buildPromptPanel());
        root.addView(space(22));

        TextView resultTitle = text("Nexus çalışma alanı", 18, TEXT, Typeface.BOLD);
        root.addView(resultTitle);
        TextView resultSub = text("Cevaplar, görseller ve videolar başka uygulama açmadan burada görünür.", 12, MUTED, Typeface.NORMAL);
        resultSub.setPadding(0, dp(4), 0, dp(12));
        root.addView(resultSub);

        resultContainer = new LinearLayout(this);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultContainer, new LinearLayout.LayoutParams(-1, -2));
        showWelcome();

        root.addView(space(18));
        TextView privacy = text(PrivacyNotice.CLOUD_NOTICE, 11, MUTED, Typeface.NORMAL);
        privacy.setLineSpacing(0, 1.2f);
        privacy.setPadding(dp(4), dp(8), dp(4), 0);
        root.addView(privacy);

        setContentView(scroll);
    }

    private View buildHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView orb = text("N", 19, Color.WHITE, Typeface.BOLD);
        orb.setGravity(Gravity.CENTER);
        orb.setBackground(round(PURPLE, 16));
        row.addView(orb, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = text("NEXUS AI", 24, TEXT, Typeface.BOLD);
        title.setLetterSpacing(0.08f);
        titles.addView(title);
        titles.addView(text("Bizim yapay zekâ çalışma alanımız", 13, MUTED, Typeface.NORMAL));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView status = text("● HAZIR", 10, GREEN, Typeface.BOLD);
        status.setPadding(dp(10), dp(7), dp(10), dp(7));
        status.setBackground(strokeRound(Color.rgb(12, 40, 31), GREEN, 14, 1));
        row.addView(status);
        box.addView(row);

        TextView hero = text("Tek uygulama.\nTek sohbet. Çoklu yetenek.", 33, TEXT, Typeface.BOLD);
        hero.setLineSpacing(0, 0.94f);
        hero.setPadding(0, dp(28), 0, dp(10));
        box.addView(hero);

        TextView sub = text("Oturum açma yok. Kodlama, sohbet, araştırma, görsel, video ve yerel sesli okuma tek ekranda.", 15, MUTED, Typeface.NORMAL);
        sub.setLineSpacing(0, 1.25f);
        box.addView(sub);
        return box;
    }

    private View buildModePicker() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.addView(text("Çalışma modu", 15, TEXT, Typeface.BOLD));
        TextView helper = text("Otomatik bırakabilir veya istediğin modu sabitleyebilirsin.", 11, MUTED, Typeface.NORMAL);
        helper.setPadding(0, dp(4), 0, dp(10));
        outer.addView(helper);

        modeScroll = new HorizontalScrollView(this);
        modeScroll.setHorizontalScrollBarEnabled(false);
        modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeScroll.addView(modeRow, new HorizontalScrollView.LayoutParams(-2, -2));
        outer.addView(modeScroll);
        rebuildModeButtons();
        return outer;
    }

    private void rebuildModeButtons() {
        if (modeRow == null) return;
        modeRow.removeAllViews();
        addModeButton("✦ Otomatik", null);
        addModeButton("● Sohbet", RequestMode.CHAT);
        addModeButton("</> Kodlama", RequestMode.CODE);
        addModeButton("⌕ Araştırma", RequestMode.RESEARCH);
        addModeButton("◈ Görsel", RequestMode.IMAGE);
        addModeButton("▶ Video", RequestMode.VIDEO);
        addModeButton("◉ Ses", RequestMode.AUDIO);
    }

    private void addModeButton(String label, RequestMode mode) {
        boolean active = selectedMode == mode;
        TextView chip = text(label, 12, active ? Color.WHITE : MUTED, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(10), dp(14), dp(10));
        chip.setBackground(strokeRound(active ? PURPLE : CARD_2, active ? PURPLE : Color.rgb(49, 55, 76), 16, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
        lp.setMargins(0, 0, dp(8), 0);
        chip.setOnClickListener(v -> {
            selectedMode = mode;
            rebuildModeButtons();
        });
        modeRow.addView(chip, lp);
    }

    private View buildPromptPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(strokeRound(CARD, Color.rgb(48, 54, 76), 24, 1));

        promptInput = new EditText(this);
        promptInput.setHint("Ne yapmak istiyorsun? Örn: Bu Android kodunu düzelt, test et ve temiz halini ver…");
        promptInput.setHintTextColor(Color.rgb(101, 110, 136));
        promptInput.setTextColor(TEXT);
        promptInput.setTextSize(16);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        promptInput.setMinLines(4);
        promptInput.setMaxLines(10);
        promptInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        promptInput.setBackground(round(Color.rgb(11, 14, 23), 17));
        panel.addView(promptInput, new LinearLayout.LayoutParams(-1, dp(138)));
        panel.addView(space(12));

        sendButton = actionButton("✦  NEXUS'A GÖNDER", PURPLE);
        sendButton.setOnClickListener(v -> submit());
        panel.addView(sendButton, new LinearLayout.LayoutParams(-1, dp(54)));

        working = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        working.setIndeterminate(true);
        working.setIndeterminateTintList(ColorStateList.valueOf(CYAN));
        working.setVisibility(View.GONE);
        LinearLayout.LayoutParams workLp = new LinearLayout.LayoutParams(-1, dp(3));
        workLp.setMargins(0, dp(10), 0, 0);
        panel.addView(working, workLp);

        TextView micro = text("Görev otomatik algılanır • başka AI sitesine yönlendirme yapılmaz", 11, MUTED, Typeface.NORMAL);
        micro.setGravity(Gravity.CENTER);
        micro.setPadding(0, dp(9), 0, 0);
        panel.addView(micro);
        return panel;
    }

    private void submit() {
        String prompt = NexusAiEngine.normalizePrompt(promptInput.getText().toString());
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Önce isteğini yaz.", Toast.LENGTH_SHORT).show();
            promptInput.requestFocus();
            return;
        }

        RequestMode mode = selectedMode == null ? RequestClassifier.classify(prompt) : selectedMode;
        setWorking(true, mode);
        resultContainer.removeAllViews();
        resultContainer.addView(messageCard("SEN", prompt, Color.rgb(34, 39, 56)));
        resultContainer.addView(space(10));
        resultContainer.addView(messageCard("NEXUS • " + mode.label.toUpperCase(new Locale("tr", "TR")), "Çalışıyorum…", Color.rgb(25, 24, 45)));

        engine.execute(prompt, mode, result -> runOnUiThread(() -> {
            setWorking(false, mode);
            renderResult(result, mode);
        }));
    }

    private void renderResult(NexusResult result, RequestMode mode) {
        resultContainer.removeAllViews();
        if (result == null) {
            resultContainer.addView(messageCard("NEXUS", "Beklenmeyen boş yanıt.", Color.rgb(55, 24, 31)));
            return;
        }

        if (result.kind == NexusResult.Kind.ERROR) {
            resultContainer.addView(messageCard("NEXUS • HATA", result.content, Color.rgb(55, 24, 31)));
            return;
        }

        if (result.kind == NexusResult.Kind.TEXT) {
            lastTextResult = result.content;
            resultContainer.addView(messageCard("NEXUS • " + mode.label.toUpperCase(new Locale("tr", "TR")), result.content, Color.rgb(25, 24, 45)));
            resultContainer.addView(space(10));
            resultContainer.addView(buildTextActions(result.content));
            if (mode == RequestMode.AUDIO) speak(result.content);
            return;
        }

        if (result.kind == NexusResult.Kind.IMAGE) {
            resultContainer.addView(messageCard("NEXUS • GÖRSEL", "Görsel üretildi.", Color.rgb(25, 24, 45)));
            resultContainer.addView(space(10));
            showRemoteImage(result.content);
            return;
        }

        if (result.kind == NexusResult.Kind.VIDEO) {
            resultContainer.addView(messageCard("NEXUS • VİDEO", "Video üretildi. Oynatmak için dokun.", Color.rgb(25, 24, 45)));
            resultContainer.addView(space(10));
            showVideo(result.content);
        }
    }

    private View buildTextActions(String content) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView copy = smallButton("KOPYALA");
        copy.setOnClickListener(v -> copyText(content));
        row.addView(copy, new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView speak = smallButton("SESLE OKU");
        LinearLayout.LayoutParams speakLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        speakLp.setMargins(dp(8), 0, 0, 0);
        speak.setOnClickListener(v -> speak(content));
        row.addView(speak, speakLp);
        return row;
    }

    private View messageCard(String label, String body, int fill) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(strokeRound(fill, Color.rgb(48, 54, 75), 20, 1));

        TextView tag = text(label, 10, CYAN, Typeface.BOLD);
        tag.setLetterSpacing(0.08f);
        card.addView(tag);
        TextView content = text(body, 14, TEXT, Typeface.NORMAL);
        content.setTextIsSelectable(true);
        content.setLineSpacing(0, 1.25f);
        content.setPadding(0, dp(9), 0, 0);
        card.addView(content);
        return card;
    }

    private void showRemoteImage(String url) {
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setPadding(dp(8), dp(8), dp(8), dp(8));
        frame.setBackground(strokeRound(CARD_2, Color.rgb(50, 56, 78), 20, 1));

        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(CYAN));
        frame.addView(loading, new LinearLayout.LayoutParams(-1, dp(52)));
        resultContainer.addView(frame, new LinearLayout.LayoutParams(-1, -2));

        new Thread(() -> {
            try {
                byte[] bytes = HttpUtil.getBytes(url, 10_000, 60_000, 6_000_000);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) throw new IllegalStateException("Görsel açılamadı");
                runOnUiThread(() -> {
                    frame.removeAllViews();
                    ImageView image = new ImageView(this);
                    image.setAdjustViewBounds(true);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    image.setImageBitmap(bitmap);
                    frame.addView(image, new LinearLayout.LayoutParams(-1, -2));
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    frame.removeAllViews();
                    frame.addView(text("Görsel üretildi fakat önizleme yüklenemedi.", 13, MUTED, Typeface.NORMAL));
                });
            }
        }, "nexus-image-preview").start();
    }

    private void showVideo(String url) {
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setPadding(dp(8), dp(8), dp(8), dp(8));
        frame.setBackground(strokeRound(CARD_2, Color.rgb(50, 56, 78), 20, 1));

        VideoView video = new VideoView(this);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(video);
        video.setMediaController(controls);
        video.setVideoURI(Uri.parse(url));
        video.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            video.seekTo(1);
        });
        frame.addView(video, new LinearLayout.LayoutParams(-1, dp(420)));

        TextView play = smallButton("▶ VİDEOYU OYNAT");
        play.setOnClickListener(v -> video.start());
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(-1, dp(46));
        playLp.setMargins(0, dp(8), 0, 0);
        frame.addView(play, playLp);
        resultContainer.addView(frame);
    }

    private void showWelcome() {
        resultContainer.removeAllViews();
        resultContainer.addView(messageCard("NEXUS", "Hazırım. Yukarıya bir görev yaz. Kod yazabilir, soru sorabilir, görsel veya video ürettirebilir ya da bir metni sesli okutabilirsin.", Color.rgb(25, 24, 45)));
    }

    private void setWorking(boolean value, RequestMode mode) {
        working.setVisibility(value ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!value);
        sendButton.setAlpha(value ? 0.55f : 1f);
        sendButton.setText(value ? "NEXUS ÇALIŞIYOR • " + mode.label.toUpperCase(new Locale("tr", "TR")) : "✦  NEXUS'A GÖNDER");
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("tr", "TR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
            }
        });
    }

    private void speak(String value) {
        String text = value == null || value.trim().isEmpty() ? lastTextResult : value;
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "Okunacak metin yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus-speech");
    }

    private void copyText(String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Nexus AI", value));
        Toast.makeText(this, "Kopyalandı.", Toast.LENGTH_SHORT).show();
    }

    private TextView smallButton(String label) {
        TextView view = text(label, 11, TEXT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(strokeRound(CARD_2, Color.rgb(56, 62, 86), 14, 1));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView actionButton(String label, int color) {
        TextView view = text(label, 14, Color.WHITE, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLetterSpacing(0.04f);
        view.setBackground(round(color, 17));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        return view;
    }

    private View space(int value) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(value)));
        return space;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable strokeRound(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable drawable = round(fill, radiusDp);
        drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
