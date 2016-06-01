package xyz.darkphoenix.pointer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class HeadLayer extends View {

    private static final int RADIUS = 5;
    private static final int JUMP = 1;
    private static final int LINE_WIDTH = 5;
    private static final char HORIZONTAL = 'X';
    private static final char VERTICAL = 'Y';

    private FrameLayout frameLayout;
    private WindowManager windowManager;

    private Handler handler = new Handler();
    private Runnable runnable;

    private Point point;
    private Paint paintLine = new Paint();
    private Paint paintCircle = new Paint();
    private Point resolution = new Point();

    public HeadLayer(Context context) {
        super(context);

        addViewToWindow(context);
        setupDrawingElements();
    }

    private void addViewToWindow(Context context) {
        frameLayout = new FrameLayout(context);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(frameLayout, params);
        frameLayout.addView(this);
    }

    private void setupDrawingElements() {
        paintLine.setColor(Color.BLACK);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(LINE_WIDTH);

        paintCircle.setColor(Color.RED);

        Display display = windowManager.getDefaultDisplay();
        display.getRealSize(resolution);
        resolution = new Point(resolution.x, resolution.y);
        point = new Point(resolution.x / 2, resolution.y / 2);
    }

    public void update(boolean volumeUpPressed, boolean volumeDownPressed) {

        if (!volumeUpPressed || !volumeDownPressed)
            handler.removeCallbacks(runnable);

        if (volumeUpPressed) {
            runnable = new MyRunnable(HORIZONTAL);
            handler.post(runnable);
        } else if (volumeDownPressed) {
            runnable = new MyRunnable(VERTICAL);
            handler.post(runnable);
        }
    }

    public Point getPoint() {
        return this.point;
    }

    public Point getResolution() {
        return this.resolution;
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Horizontal Line
        canvas.drawLine(point.x, 0, point.x, resolution.y, paintLine);
        // Vertical Line
        canvas.drawLine(0, point.y, resolution.x, point.y, paintLine);
        // Point
        canvas.drawCircle(point.x, point.y, RADIUS, paintCircle);
    }

    public void destroy() {
        windowManager.removeView(frameLayout);
    }

    private class MyRunnable implements Runnable {

        char axis;

        public MyRunnable(char axis) {
            this.axis = axis;
        }

        @Override
        public void run() {

            if (axis == HORIZONTAL) {
                if (point.x >= resolution.x) point.x = 0;
                point.x += JUMP;
            } else if (axis == VERTICAL) {
                if (point.y >= resolution.y) point.y = 0;
                point.y += JUMP;
            }

            invalidate();
            handler.postDelayed(runnable, 0);
        }
    }

}
