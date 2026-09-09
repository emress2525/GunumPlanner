package com.emre.gunumplanner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ModernUi {
    static final int BG = 0xFFF5F7FB;
    static final int SURFACE = 0xFFFFFFFF;
    static final int SURFACE_ALT = 0xFFF0F3FA;
    static final int PRIMARY = 0xFF3559E0;
    static final int PRIMARY_SOFT = 0xFFE8EEFF;
    static final int TEXT = 0xFF16181D;
    static final int SUB = 0xFF667085;
    static final int LINE = 0xFFE3E8F2;
    static final int SUCCESS = 0xFF1F9D55;
    static final int WARNING = 0xFFF59E0B;
    static final int PURPLE = 0xFF7C3AED;
    static final int DANGER = 0xFFDC2626;

    private ModernUi() {}

    static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    static GradientDrawable rounded(Context c, int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    static LinearLayout card(Context c, boolean hero) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(c, SURFACE, hero ? 24 : 18, LINE, 1));
        if (Build.VERSION.SDK_INT >= 21) box.setElevation(dp(c, hero ? 3 : 1));
        return box;
    }

    static TextView label(Context c, String text, int size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0f, 1.12f);
        return t;
    }

    static TextView chip(Context c, String text, int bg, int fg) {
        TextView t = label(c, text, 12, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        t.setBackground(rounded(c, bg, 999, bg, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 0, dp(c, 8), 0);
        t.setLayoutParams(lp);
        return t;
    }

    static Button primaryButton(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(c, PRIMARY, 18, PRIMARY, 1));
        b.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        if (Build.VERSION.SDK_INT >= 21) b.setElevation(dp(c, 2));
        return b;
    }

    static Button softButton(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(TEXT);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(c, SURFACE_ALT, 14, LINE, 1));
        b.setPadding(dp(c, 10), dp(c, 8), dp(c, 10), dp(c, 8));
        return b;
    }

    static Button navButton(Context c, String text, boolean active) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        styleNav(c, b, active);
        return b;
    }

    static void styleNav(Context c, Button b, boolean active) {
        b.setTextColor(active ? PRIMARY : SUB);
        b.setBackground(rounded(c, active ? PRIMARY_SOFT : SURFACE_ALT, 14,
                active ? PRIMARY_SOFT : SURFACE_ALT, 0));
        b.setPadding(dp(c, 8), dp(c, 9), dp(c, 8), dp(c, 9));
    }

    static LinearLayout.LayoutParams weight(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(c, 4), 0, dp(c, 4), 0);
        return lp;
    }

    static LinearLayout.LayoutParams margin(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return lp;
    }

    static View accent(Context c, int color) {
        View v = new View(c);
        v.setBackground(rounded(c, color, 999, color, 0));
        return v;
    }
}
