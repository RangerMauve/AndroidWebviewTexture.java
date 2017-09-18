package net.gangneux.dev.jrmgxlibview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.unity3d.player.UnityPlayer;

import java.util.LinkedList;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class JrmgxRenderableView {

    public static final int ACTION_DOWN   = 1;
    public static final int ACTION_UP     = 2;
    public static final int ACTION_MOVE   = 3;
    public static final int ACTION_CANCEL = 4;

    // Feature flags

    public static final int FLAG_HARDWARE_RENDERER = 0b001;

    protected boolean flagHardwareRender = false;

    protected Activity activity;

    protected View view               = null;
    protected FrameLayout frameLayout = null;
    protected Surface surface         = null;

    protected boolean debug = false;

    protected int width  = -1;
    protected int height = -1;

    protected LinkedList<MotionEvent> eventQueue = new LinkedList<>();
    protected long lastDownTime = 0;

    protected String delegateGameObjectName = null;
    protected String delegateMethodName     = null;

    protected JrmgxRenderableViewDelegate delegate = null;

    public JrmgxRenderableView(final int width, final int height) {
        log("Constructor with width + height = " + width + " + " + height);

        this.width  = width;
        this.height = height;
    }

    public JrmgxRenderableView(final int width, final int height, boolean debug, int flags) {
        this.debug = debug;

        log("Constructor with width/height: " + width + "/" + height);

        this.width  = width;
        this.height = height;

        setFlags(flags);
    }

    protected void setFlags(int flags) {
        flagHardwareRender = (flags & FLAG_HARDWARE_RENDERER) == FLAG_HARDWARE_RENDERER;

        log("Flag FLAG_HARDWARE_RENDERER is " + (flagHardwareRender ? "ON" : "OFF"));
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Activity getActivity() {
        if (activity == null) {
            return UnityPlayer.currentActivity;
        }

        return activity;
    }

    protected int getScale() {
        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        // VR is horizontal
        return (int) ((double) point.y / (double) width);
    }

    void resolveFrameLayout() {
        try {
            frameLayout = (FrameLayout) getActivity().getWindow().getDecorView().getRootView();
            log("Got framelayout from window");
        }
        catch (Exception e) {
            frameLayout = null;
        }

        if (frameLayout == null) {
            frameLayout = new FrameLayout(getActivity());
            getActivity().addContentView(frameLayout, new FrameLayout.LayoutParams(width, height));
            log("Made framelayout from code");
        }
    }

    void setUpView(View view) {
        resolveFrameLayout();

        // No hardware acceleration (it causes different types of bugs)
        if (!flagHardwareRender) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.NO_GRAVITY);
        params.leftMargin = -width  + 1; // This is needed to get the draw rendered
        params.topMargin  = -height + 1; // if the view is fully outside the screen it is not rendered
        frameLayout.addView(view, params);

        this.view = view;
    }

    public void setSurface(Surface surface) {
        log("Set surface with " + surface);
        this.surface = surface;
    }

    public void setBackgroundColor(final String color) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setBackgroundColor(Color.parseColor(color));
            }
        });
    }

    // Pseudo delegation

    public void setDelegate(final String gameObjectName, final String methodName) {
        delegateGameObjectName = gameObjectName;
        delegateMethodName     = methodName;
    }

    public void setDelegate(JrmgxRenderableViewDelegate delegate) {
        this.delegate = delegate;
    }

    protected void sendMessageToDelegate(final String message) {
        if (delegateGameObjectName != null && delegateMethodName != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UnityPlayer.UnitySendMessage(delegateGameObjectName, delegateMethodName, message);
                }
            });
        }
        else if (delegate != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    delegate.sendMessage(message);
                }
            });
        }
        else {
            log("Delegate is not defined // discard message " + message);
        }
    }

    // Touch

    public static String ActionTypeToString(int actionType) {
        switch (actionType) {
            case ACTION_DOWN:   return "DOWN";
            case ACTION_UP:     return "UP";
            case ACTION_CANCEL: return "CANCEL";
            case ACTION_MOVE:   return "MOVE";
        }
        return "UNKNOW(" + actionType + ")";
    }

    public void eventAdd(int x, int y, int actionType) {
        log("Add event to queue: " + x + "," + y + " " + ActionTypeToString(actionType));
        lastDownTime = SystemClock.uptimeMillis();
        int motionEventType = actionType;
        switch (actionType) {
            case ACTION_DOWN:   motionEventType = MotionEvent.ACTION_DOWN;   break;
            case ACTION_UP:     motionEventType = MotionEvent.ACTION_UP;     break;
            case ACTION_CANCEL: motionEventType = MotionEvent.ACTION_CANCEL; break;
            case ACTION_MOVE:   motionEventType = MotionEvent.ACTION_MOVE;   break;
        }
        eventQueue.add(MotionEvent.obtain(lastDownTime, lastDownTime, motionEventType, x, y, 0));
    }

    public void eventPoll() {
        if (eventQueue.size() <= 0) {
            return;
        }

        if (view == null) {
            log("WARNING: EventPoll view is null");
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MotionEvent e = eventQueue.poll();
                if (e == null) {
                    return;
                }

                log("Dispatch event from queue: " + e.getX() + "," + e.getY() + " " + ActionTypeToString(e.getAction()));
                if (!view.dispatchTouchEvent(e)) {
                    log("WARNING: Failed to dispatch event");
                }
            }
        });
    }

    // Helpers

    void log(String message) {
        if (debug) {
            Log.i(this.getClass().getCanonicalName(), message);
        }
    }
}
