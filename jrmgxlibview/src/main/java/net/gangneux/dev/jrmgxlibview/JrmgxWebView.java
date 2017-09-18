package net.gangneux.dev.jrmgxlibview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressWarnings({"unused", "WeakerAccess"})
public class JrmgxWebView extends JrmgxRenderableView {

    private final static String TAG = JrmgxWebView.class.getCanonicalName();

    private CustomWebView webview = null;
    protected float scale = 0f;

    public JrmgxWebView(final int width, final int height) {
        super(width, height);
        init();
    }

    public JrmgxWebView(final int width, final int heigh, boolean debug, int flags) {
        super(width, heigh, debug, flags);
        init();
    }

    public JrmgxWebView(int width, int height, Activity activity) {
        super(width, height, true, 0);
        setActivity(activity);
        init();
    }

    private void init() {
        getActivity().runOnUiThread(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {

                webview = new CustomWebView(getActivity());

                webview.getSettings().setJavaScriptEnabled(true);
                webview.getSettings().setSupportZoom(false);
                webview.getSettings().setBuiltInZoomControls(false);

                // Set scale on devices that supports it
                webview.setPadding(0, 0, 0, 0);
                webview.setInitialScale(getScale() * 100);

                webview.setWebChromeClient(webChromeClient);
                webview.setWebViewClient(webViewClient);

                setUpView(webview);
            }
        });
    }

    public void setUserAgent(final String userAgent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Set User Agent " + userAgent);
                webview.getSettings().setUserAgentString(userAgent);
            }
        });
    }

    public void loadUrl(final String url) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Load url " + url);
                webview.loadUrl(url);
            }
        });
    }

    public void loadData(final String data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Load data " + data);
                webview.loadData(data, "text/html; charset=utf-8", "UTF-8");
            }
        });
    }

    public void evaluateJavascript(final String script) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Evaluate javascript " + script);
                webview.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        sendMessageToDelegate("onResultEvaluateJavascript:" + value);
                    }
                });
            }
        });
    }

    public void goBack() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webview.goBack();
            }
        });
    }

    public void goForward() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webview.goForward();
            }
        });
    }

    private class CustomWebView extends WebView {

        private int scrollX = 0;
        private int scrollY = 0;

        public CustomWebView(Context context) {
            super(context);
        }

        public CustomWebView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            scrollX = l;
            scrollY = t;
        }

        @Override
        public void draw(Canvas originCanvas) {
            if (surface != null) {
                log("Draw surface");
                Canvas canvasFromSurface;
                try {
                    canvasFromSurface = surface.lockCanvas(null);
                    if (canvasFromSurface == null) {
                        throw new Exception("Canvas from Surface not ready");
                    }

                    if (scale == 0) {
                        scale = (float) canvasFromSurface.getWidth() / (float) originCanvas.getWidth();
                        log("Scale is " + scale + " width " + canvasFromSurface.getWidth() + " / " + originCanvas.getWidth());
                    }

                    canvasFromSurface.scale(scale, scale);
                    canvasFromSurface.translate(-scrollX, -scrollY);
                    super.draw(canvasFromSurface);
                    surface.unlockCanvasAndPost(canvasFromSurface);
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                }
            }
            else {
                log("Draw surface IS NULL");
            }
        }
    }

    private WebChromeClient webChromeClient = new WebChromeClient() { };

    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            sendMessageToDelegate("onPageStarted:" + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            sendMessageToDelegate("onPageFinished:" + url);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            sendMessageToDelegate("onReceivedError:" + error.toString());
            super.onReceivedError(view, request, error);
        }
    };
}
