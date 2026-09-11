package com.promptnet.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText input;
    private TextView result;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        String lastInput = getPreferences(MODE_PRIVATE).getString("last_input", "");
        String lastOutput = getPreferences(MODE_PRIVATE).getString("last_output", "");
        input.setText(lastInput);
        result.setText(lastOutput);
        if (!lastOutput.isEmpty()) status.setText("Son prompt geri yüklendi");
    }

    private View buildUi() {
        int pad = dp(20);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 244, 245));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(26), pad, dp(36));
        scroll.addView(root);

        TextView title = text("PromptNet", 30, true);
        root.addView(title);

        TextView subtitle = text("Bir kere anlat. Sistem eksikleri tamamlayıp doğrulanabilir bir görev promptuna çevirsin.", 15, false);
        subtitle.setTextColor(Color.rgb(82, 82, 91));
        subtitle.setPadding(0, dp(8), 0, dp(20));
        root.addView(subtitle);

        input = new EditText(this);
        input.setHint("Örn: Inventor için delik kaçıklıklarını bulan program yap");
        input.setTextSize(16);
        input.setMinLines(5);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        input.setBackgroundColor(Color.WHITE);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        Button generate = new Button(this);
        generate.setText("PROMPTU OLUŞTUR");
        generate.setAllCaps(false);
        generate.setTextSize(16);
        generate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(-1, dp(54));
        buttonLp.topMargin = dp(14);
        root.addView(generate, buttonLp);

        status = text("Hazır", 13, false);
        status.setTextColor(Color.rgb(82, 82, 91));
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        result = text("", 14, false);
        result.setTextIsSelectable(true);
        result.setPadding(dp(16), dp(16), dp(16), dp(16));
        result.setBackgroundColor(Color.WHITE);
        root.addView(result, new LinearLayout.LayoutParams(-1, -2));

        Button copy = new Button(this);
        copy.setText("Promptu Kopyala");
        copy.setAllCaps(false);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(-1, dp(50));
        copyLp.topMargin = dp(12);
        root.addView(copy, copyLp);

        generate.setOnClickListener(v -> generatePrompt());
        copy.setOnClickListener(v -> copyPrompt());
        return scroll;
    }

    private void generatePrompt() {
        String raw = input.getText().toString();
        try {
            PromptEngine.Result compiled = PromptEngine.compile(raw);
            result.setText(compiled.prompt);
            status.setText("✓ " + compiled.requirementCount + " gereksinim işlendi • " + compiled.domain);
            getPreferences(MODE_PRIVATE).edit()
                    .putString("last_input", raw)
                    .putString("last_output", compiled.prompt)
                    .apply();
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPrompt() {
        String text = result.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Önce bir prompt oluştur.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("PromptNet", text));
        Toast.makeText(this, "Prompt kopyalandı.", Toast.LENGTH_SHORT).show();
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.rgb(24, 24, 27));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
