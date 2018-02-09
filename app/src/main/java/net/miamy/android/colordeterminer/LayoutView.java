package net.miamy.android.colordeterminer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Vadim on 08.02.2018.
 */

public class LayoutView extends View
{
    private int delta;

    public LayoutView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();// - (int) getY();
        DrawFocusRect(canvas, width / 2 - delta, height / 2 - delta,
                width / 2 + delta, height / 2 + delta, Color.WHITE);
    }

    private void DrawFocusRect(Canvas canvas, float RectLeft, float RectTop, float RectRight, float RectBottom, int color)
    {
        if (canvas != null)
        {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(color);
            paint.setStrokeWidth(4);
            canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);
        }
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}

