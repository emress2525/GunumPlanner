package com.sesliasistan.jarvis;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class AssistantOrbView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float phase;
    private boolean active;

    public AssistantOrbView(Context context) {
        super(context);
        init();
    }

    public AssistantOrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);
        setContentDescription("Jarvis dinleme göstergesi");
    }

    public void setActive(boolean active) {
        this.active = active;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimator();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    private void startAnimator() {
        if (animator != null) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3200L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.29f;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2));
        float activeBoost = active ? 1f : 0.45f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        for (int i = 0; i < 4; i++) {
            float ringRadius = radius * (1.23f + i * 0.17f) + pulse * dp(4f + i * 1.5f);
            int alpha = (int) ((72 - i * 13) * activeBoost);
            paint.setColor(Color.argb(Math.max(alpha, 10), 77, 235, 255));
            canvas.drawCircle(cx, cy, ringRadius, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        int coreAlpha = active ? 255 : 190;
        paint.setShader(new RadialGradient(
                cx, cy, radius,
                new int[]{Color.argb(coreAlpha, 150, 250, 255), Color.rgb(44, 178, 208), Color.rgb(7, 40, 55)},
                new float[]{0f, 0.48f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius + pulse * dp(active ? 5f : 2f), paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2f));
        paint.setColor(Color.argb(active ? 210 : 110, 190, 251, 255));
        canvas.drawCircle(cx, cy, radius * 1.03f, paint);

        textPaint.setTextSize(radius * 0.82f);
        textPaint.setAlpha(active ? 255 : 205);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText("J", cx, baseline, textPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
