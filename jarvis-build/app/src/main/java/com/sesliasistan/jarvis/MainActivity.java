package com.sesliasistan.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(statusReceiver, filter);
        receiverRegistered = true;
        renderActive(isServiceRequested());
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

        TextView eyebrow = text("JARVIS  /  SESLİ ASİSTAN", 13, Color.rgb(77, 235, 255), Typeface.BOLD);
        eyebrow.setLetterSpacing(0.18f);
        root.addView(eyebrow, fullWidthWrap());

        TextView title = text("Hazır olduğunda\nsadece adımı söyle.", 31, Color.rgb(243, 251, 255), Typeface.BOLD);
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

        TextView helper = text("Aktifken “Jarvis” de. Ardından komutunu söyle veya tek cümlede “Jarvis Spotify aç” de.", 14, Color.rgb(145, 168, 182), Typeface.NORMAL);
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
        primaryButton.setOnClickListener(v -> {
            if (isServiceRequested()) stopAssistant(); else ensurePermissionsAndStart();
        });
        LinearLayout.LayoutParams primaryLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        primaryLp.topMargin = dp(22);
        root.addView(primaryButton, primaryLp);

        Button helpButton = new Button(this);
        helpButton.setAllCaps(false);
        helpButton.setText("Neler söyleyebilirim?");
        helpButton.setTextSize(14);
        helpButton.setTextColor(Color.rgb(206, 237, 244));
        helpButton.setBackgroundResource(R.drawable.bg_secondary_button);
        helpButton.setMinHeight(0);
        helpButton.setPadding(dp(16), dp(14), dp(16), dp(14));
        helpButton.setOnClickListener(v -> showCommandExamples());
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        helpLp.topMargin = dp(12);
        root.addView(helpButton, helpLp);

        TextView privacy = text("Mikrofon yalnızca asistanı sen başlattığında foreground servis içinde kullanılır. Bildirimden istediğin an durdurabilirsin.", 12, Color.rgb(104, 130, 142), Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyLp = fullWidthWrap();
        privacyLp.topMargin = dp(20);
        root.addView(privacy, privacyLp);

        setContentView(scroll);
    }

    private void ensurePermissionsAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            startAssistant();
            return;
        }
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startAssistant();
        else {
            statusText.setText("Mikrofon izni gerekli");
            Toast.makeText(this, "Jarvis'in seni duyabilmesi için mikrofon izni gerekli.", Toast.LENGTH_LONG).show();
        }
    }

    private void startAssistant() {
        Intent intent = new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_START);
        try {
            startForegroundService(intent);
            getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).edit().putBoolean(JarvisListeningService.KEY_ACTIVE, true).apply();
            renderActive(true);
            statusText.setText("Başlatılıyor…");
        } catch (Exception error) {
            renderActive(false);
            statusText.setText("Asistan başlatılamadı");
            Toast.makeText(this, "Başlatma hatası: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopAssistant() {
        getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE).edit().putBoolean(JarvisListeningService.KEY_ACTIVE, false).apply();
        Intent intent = new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_STOP);
        try { startService(intent); } catch (Exception ignored) { stopService(new Intent(this, JarvisListeningService.class)); }
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
        String examples = "Örnekler:\n\n• Jarvis Spotify aç\n• Jarvis saat kaç\n• Jarvis bugün tarih ne\n• Jarvis internette Ankara hava durumu ara\n• Jarvis 07:30 alarm kur\n• Jarvis Bluetooth ayarlarını aç";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Jarvis komutları")
                .setMessage(examples)
                .setPositiveButton("Tamam", null)
                .setNeutralButton("Uygulama ayarları", (dialog, which) -> {
                    Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    settings.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(settings);
                })
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
