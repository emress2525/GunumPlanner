package com.sesliasistan.jarvis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 401;
    private static final int REQUEST_ASSISTANT_ROLE = 402;
    private static final String KEY_ROLE_PROMPTED = "assistant_role_prompted";

    private TextView statusText;
    private TextView statusChip;
    private TextView transcriptText;
    private Button primaryButton;
    private AssistantOrbView orbView;
    private boolean receiverRegistered;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!JarvisListeningService.ACTION_STATUS.equals(intent.getAction())) return;
            String status = intent.getStringExtra(JarvisListeningService.EXTRA_STATUS);
            String transcript = intent.getStringExtra(JarvisListeningService.EXTRA_TRANSCRIPT);
            boolean active = intent.getBooleanExtra(JarvisListeningService.EXTRA_ACTIVE, isServiceRequested());
            if (status != null && !status.trim().isEmpty()) statusText.setText(status);
            if (transcript != null && !transcript.trim().isEmpty()) transcriptText.setText(transcript);
            renderActive(active);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(6, 10, 15));
        getWindow().setNavigationBarColor(Color.rgb(6, 10, 15));
        buildUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(JarvisListeningService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerStatusReceiverPre33(filter);
        }
        receiverRegistered = true;
        renderActive(isServiceRequested());
        if (isServiceRequested() && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                startForegroundService(new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_START));
            } catch (RuntimeException ignored) {
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerStatusReceiverPre33(IntentFilter filter) {
        registerReceiver(statusReceiver, filter);
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(6, 10, 15));
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(18), dp(24), dp(30));
        root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(dp(24), dp(18) + top, dp(24), dp(30) + bottom);
            return insets;
        });
        scroll.addView(root, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("JARVIS  /  SESLİ ASİSTAN  v1.1", 13, Color.rgb(77, 235, 255), Typeface.BOLD);
        eyebrow.setLetterSpacing(0.16f);
        root.addView(eyebrow, fullWidthWrap());

        TextView title = text("Söyle.\nBen halledeyim.", 31, Color.rgb(243, 251, 255), Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = fullWidthWrap();
        titleLp.topMargin = dp(14);
        root.addView(title, titleLp);

        statusChip = text("●  BEKLEMEDE", 12, Color.rgb(145, 168, 182), Typeface.BOLD);
        statusChip.setBackgroundResource(R.drawable.bg_status_chip);
        LinearLayout.LayoutParams chipLp = wrap();
        chipLp.topMargin = dp(18);
        chipLp.gravity = Gravity.START;
        root.addView(statusChip, chipLp);

        orbView = new AssistantOrbView(this);
        LinearLayout.LayoutParams orbLp = new LinearLayout.LayoutParams(dp(300), dp(300));
        orbLp.gravity = Gravity.CENTER_HORIZONTAL;
        orbLp.topMargin = dp(12);
        root.addView(orbView, orbLp);

        statusText = text("Asistan kapalı", 21, Color.WHITE, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, fullWidthWrap());

        TextView helper = text("“Jarvis” de; uygulama, cihaz, medya, zaman, harita, telefon ve not komutlarını doğal Türkçe söyle.", 14, Color.rgb(145, 168, 182), Typeface.NORMAL);
        helper.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams helperLp = fullWidthWrap();
        helperLp.topMargin = dp(8);
        root.addView(helper, helperLp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackgroundResource(R.drawable.bg_panel);
        LinearLayout.LayoutParams panelLp = fullWidthWrap();
        panelLp.topMargin = dp(24);
        root.addView(panel, panelLp);

        TextView lastHeardLabel = text("SON DUYULAN", 11, Color.rgb(77, 235, 255), Typeface.BOLD);
        lastHeardLabel.setLetterSpacing(0.14f);
        panel.addView(lastHeardLabel, fullWidthWrap());

        transcriptText = text("Henüz bir şey duymadım.", 16, Color.rgb(243, 251, 255), Typeface.NORMAL);
        LinearLayout.LayoutParams transcriptLp = fullWidthWrap();
        transcriptLp.topMargin = dp(8);
        panel.addView(transcriptText, transcriptLp);

        primaryButton = new Button(this);
        primaryButton.setAllCaps(false);
        primaryButton.setTextSize(16);
        primaryButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        primaryButton.setTextColor(Color.rgb(4, 22, 28));
        primaryButton.setBackgroundResource(R.drawable.bg_primary_button);
        primaryButton.setMinHeight(0);
        primaryButton.setPadding(dp(16), dp(17), dp(16), dp(17));
        primaryButton.setOnClickListener(v -> { if (isServiceRequested()) stopAssistant(); else ensurePermissionsAndStart(); });
        LinearLayout.LayoutParams primaryLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        primaryLp.topMargin = dp(22);
        root.addView(primaryButton, primaryLp);

        Button helpButton = new Button(this);
        helpButton.setAllCaps(false);
        helpButton.setText("Komutları göster");
        helpButton.setTextSize(14);
        helpButton.setTextColor(Color.rgb(206, 237, 244));
        helpButton.setBackgroundResource(R.drawable.bg_secondary_button);
        helpButton.setMinHeight(0);
        helpButton.setPadding(dp(16), dp(14), dp(16), dp(14));
        helpButton.setOnClickListener(v -> showCommandExamples());
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        helpLp.topMargin = dp(12);
        root.addView(helpButton, helpLp);

        TextView privacy = text("Mikrofon yalnızca asistanı başlattığında kullanılır. Kamera izni yalnızca fener kontrolü içindir ve reddedilirse diğer komutlar çalışmaya devam eder.", 12, Color.rgb(104, 130, 142), Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyLp = fullWidthWrap();
        privacyLp.topMargin = dp(20);
        root.addView(privacy, privacyLp);
        setContentView(scroll);
    }

    private void ensurePermissionsAndStart() {
        RoleManager roles = getSystemService(RoleManager.class);
        boolean alreadyPrompted = getPreferences(MODE_PRIVATE).getBoolean(KEY_ROLE_PROMPTED, false);
        if (!alreadyPrompted && roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT) && !roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_ROLE_PROMPTED, true).apply();
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), REQUEST_ASSISTANT_ROLE);
            return;
        }
        ensurePermissionsAndStartService();
    }

    private void ensurePermissionsAndStartService() {
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECORD_AUDIO);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (permissions.isEmpty()) {
            startAssistant();
            return;
        }
        requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ASSISTANT_ROLE) ensurePermissionsAndStartService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAssistant();
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Fener komutu kamera izni verilene kadar kullanılamaz.", Toast.LENGTH_SHORT).show();
            }
        } else {
            statusText.setText("Mikrofon izni gerekli");
            Toast.makeText(this, "Jarvis'in seni duyabilmesi için mikrofon izni gerekli.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestAssistantRole() {
        RoleManager roles = getSystemService(RoleManager.class);
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT) && !roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), REQUEST_ASSISTANT_ROLE);
        } else if (roles != null && roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            Toast.makeText(this, "Jarvis zaten varsayılan asistan.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bu telefonda asistan rolü kullanılamıyor.", Toast.LENGTH_LONG).show();
        }
    }

    private void startAssistant() {
        Intent intent = new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_START);
        try {
            startForegroundService(intent);
            getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).edit().putBoolean(JarvisListeningService.KEY_ACTIVE, true).apply();
            renderActive(true);
            statusText.setText("Başlatılıyor…");
        } catch (RuntimeException error) {
            getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).edit().putBoolean(JarvisListeningService.KEY_ACTIVE, false).apply();
            renderActive(false);
            statusText.setText("Asistan başlatılamadı");
            Toast.makeText(this, "Başlatma hatası: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopAssistant() {
        getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).edit().putBoolean(JarvisListeningService.KEY_ACTIVE, false).apply();
        Intent intent = new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_STOP);
        try { startService(intent); } catch (RuntimeException ignored) { stopService(new Intent(this, JarvisListeningService.class)); }
        renderActive(false);
        statusText.setText("Asistan kapalı");
    }

    private boolean isServiceRequested() {
        return getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).getBoolean(JarvisListeningService.KEY_ACTIVE, false);
    }

    private void renderActive(boolean active) {
        orbView.setActive(active);
        primaryButton.setText(active ? "ASİSTANI DURDUR" : "ASİSTANI BAŞLAT");
        statusChip.setText(active ? "●  DİNLEMEDE" : "●  BEKLEMEDE");
        statusChip.setTextColor(active ? Color.rgb(77, 235, 255) : Color.rgb(145, 168, 182));
    }

    private void showCommandExamples() {
        String examples =
                "UYGULAMALAR\n• Jarvis Spotify aç\n• Jarvis Telegram aç\n\n" +
                "CİHAZ\n• Jarvis feneri aç / kapat\n• Jarvis sesi yükselt / azalt / kapat / fulle\n• Jarvis pil yüzde kaç\n• Jarvis kamerayı aç\n• Jarvis Wi‑Fi / Bluetooth / konum / ekran ayarlarını aç\n\n" +
                "ZAMAN\n• Jarvis saat kaç\n• Jarvis bugün tarih ne\n• Jarvis 07:30 alarm kur\n• Jarvis 5 dakika zamanlayıcı kur\n\n" +
                "MEDYA\n• Jarvis müziği durdur / devam ettir\n• Jarvis sonraki şarkı\n• Jarvis önceki şarkı\n\n" +
                "HARİTA & TELEFON\n• Jarvis Kızılay'a yol tarifi aç\n• Jarvis 0555 123 45 67 numarasını ara\n• Jarvis 0555 123 45 67 numarasına geliyorum diye mesaj yaz\n\n" +
                "NOT & WEB\n• Jarvis not al yarın kaynakçıyla konuş\n• Jarvis son notumu oku\n• Jarvis internette Ankara hava durumu ara";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Jarvis komutları")
                .setMessage(examples)
                .setPositiveButton("Tamam", null)
                .setNeutralButton("Varsayılan asistan yap", (dialog, which) -> requestAssistantRole())
                .show();
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        view.setLineSpacing(0f, 1.12f);
        return view;
    }

    private LinearLayout.LayoutParams fullWidthWrap() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
