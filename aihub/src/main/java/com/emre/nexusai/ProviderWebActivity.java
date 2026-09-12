package com.emre.nexusai;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ProviderWebActivity extends Activity {
    private static final int BG = Color.rgb(9, 11, 18);
    private static final int PANEL = Color.rgb(17, 20, 30);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(153, 162, 187);
    private static final int PURPLE = Color.rgb(139, 92, 246);

    private WebView webView;
    private ProgressBar progress;
    private String prompt = "";
    private ProviderRegistry.Provider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        String key = getIntent().getStringExtra("providerKey");
        prompt = safe(getIntent().getStringExtra("prompt"));
        provider = ProviderRegistry.get(key);
        if (provider == null) {
            finish();
            return;
        }

        if (!prompt.isEmpty()) copyPrompt(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(buildTopBar(), new LinearLayout.LayoutParams(-1, dp(60)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        webView = new WebView(this);
        configureWebView();
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        webView.loadUrl(provider.url);
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackgroundColor(PANEL);

        TextView back = button("‹", 28, dp(46));
        back.setOnClickListener(v -> {
            if (webView != null && webView.canGoBack()) webView.goBack();
            else finish();
        });
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), -1));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(provider.name, 15, TEXT, Typeface.BOLD);
        TextView sub = text("Nexus AI içinde güvenli web oturumu", 11, MUTED, Typeface.NORMAL);
        info.addView(name);
        info.addView(sub);
        bar.addView(info, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView paste = button(prompt.isEmpty() ? "YENİLE" : "PROMPT", 11, dp(76));
        paste.setOnClickListener(v -> {
            if (prompt.isEmpty()) {
                webView.reload();
            } else {
                copyPrompt(true);
                injectPrompt();
            }
        });
        bar.addView(paste, new LinearLayout.LayoutParams(dp(78), dp(40)));

        TextView external = button("↗", 18, dp(44));
        external.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView == null ? provider.url : webView.getUrl()));
            startActivity(intent);
        });
        bar.addView(external, new LinearLayout.LayoutParams(dp(44), dp(40)));
        return bar;
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setBackgroundColor(BG);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!prompt.isEmpty()) view.postDelayed(() -> injectPrompt(), 700);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void injectPrompt() {
        if (webView == null || prompt.isEmpty()) return;
        String quoted = JSONObject.quote(prompt);
        String js = "(function(){" +
                "var p=" + quoted + ";" +
                "var candidates=[].slice.call(document.querySelectorAll('textarea,[contenteditable=\\\"true\\\"],input[type=\\\"text\\\"]'));" +
                "var el=candidates.find(function(x){var r=x.getBoundingClientRect();return r.width>80&&r.height>20;})||candidates[0];" +
                "if(!el)return 'missing';" +
                "el.focus();" +
                "if(el.tagName==='TEXTAREA'||el.tagName==='INPUT'){" +
                "var proto=el.tagName==='TEXTAREA'?window.HTMLTextAreaElement.prototype:window.HTMLInputElement.prototype;" +
                "var setter=Object.getOwnPropertyDescriptor(proto,'value').set;setter.call(el,p);" +
                "}else{el.textContent=p;}" +
                "['input','change'].forEach(function(n){el.dispatchEvent(new Event(n,{bubbles:true}));});" +
                "return 'ok';})();";
        webView.evaluateJavascript(js, value -> {
            if (value != null && value.contains("missing")) {
                Toast.makeText(this, "Prompt panoda hazır. Giriş alanına basılı tutup yapıştırabilirsin.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyPrompt(boolean notify) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Nexus AI prompt", prompt));
        if (notify) Toast.makeText(this, "Prompt panoya kopyalandı.", Toast.LENGTH_SHORT).show();
    }

    private TextView button(String label, float sp, int minWidth) {
        TextView v = text(label, sp, TEXT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setMinWidth(minWidth);
        v.setBackground(strokeRound(Color.rgb(24, 28, 42), Color.rgb(55, 61, 84), 13, 1));
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

    private GradientDrawable strokeRound(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
