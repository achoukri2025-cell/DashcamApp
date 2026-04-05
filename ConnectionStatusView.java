package com.dashcam.app.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Vue personnalisée : indicateur de signal WiFi animé (3 barres qui clignotent)
 * Utilisé dans l'overlay de la vue en direct pour indiquer la qualité du flux.
 */
public class ConnectionStatusView extends View {

    public enum Status { DISCONNECTED, CONNECTING, CONNECTED, WEAK }

    private Status status = Status.DISCONNECTED;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float animAlpha = 1f;
    private boolean ascending = false;
    private final android.os.Handler handler = new android.os.Handler();

    // Couleurs
    private static final int COLOR_OFF      = Color.parseColor("#3A3F52");
    private static final int COLOR_GREEN    = Color.parseColor("#4CAF50");
    private static final int COLOR_ORANGE   = Color.parseColor("#FF6B2B");
    private static final int COLOR_RED      = Color.parseColor("#FF1744");

    public ConnectionStatusView(Context context) { this(context, null); }
    public ConnectionStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        startBlink();
    }

    public void setStatus(Status status) {
        this.status = status;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int barW = w / 5;
        int gap  = barW / 3;

        int[] heights = { h / 3, h * 2 / 3, h };
        int   activeColor = (status == Status.CONNECTED) ? COLOR_GREEN
                          : (status == Status.CONNECTING) ? COLOR_ORANGE
                          : (status == Status.WEAK)       ? COLOR_RED
                          : COLOR_OFF;

        for (int i = 0; i < 3; i++) {
            int barH   = heights[i];
            int left   = i * (barW + gap);
            int top    = h - barH;

            boolean active = (status == Status.CONNECTED)
                    || (status == Status.CONNECTING && i == 0)
                    || (status == Status.WEAK && i <= 1);

            if (active && status == Status.CONNECTING) {
                paint.setAlpha((int)(animAlpha * 255));
            } else {
                paint.setAlpha(255);
            }

            paint.setColor(active ? activeColor : COLOR_OFF);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawRoundRect(left, top, left + barW, h, 3, 3, paint);
        }
    }

    private void startBlink() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (status == Status.CONNECTING) {
                    animAlpha += ascending ? 0.08f : -0.08f;
                    if (animAlpha >= 1f) { animAlpha = 1f; ascending = false; }
                    if (animAlpha <= 0.2f) { animAlpha = 0.2f; ascending = true; }
                    invalidate();
                } else {
                    animAlpha = 1f;
                }
                handler.postDelayed(this, 80);
            }
        }, 80);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = (int)(getContext().getResources().getDisplayMetrics().density * 24);
        setMeasuredDimension(size, size);
    }
}
