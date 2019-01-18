package test.myapplication.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Map;

public class MyWebView extends WebView {
    private Map<String, String> additionalHttpHeaders;
    private String curUrl;
    private MyWebViewClient webViewClient;

    public MyWebView(Context context) {
        this(context, null);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSettings(context);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSettings(context);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void initSettings(final Context context) {
        try {
            getSettings().setSupportMultipleWindows(false);

            getSettings().setLoadsImagesAutomatically(true);
            getSettings().setUseWideViewPort(true);
            getSettings().setLoadWithOverviewMode(true);
            getSettings().setGeolocationEnabled(true);
            getSettings().setPluginState(WebSettings.PluginState.ON_DEMAND);
            getSettings().setSaveFormData(true);
            getSettings().setSavePassword(true);
            getSettings().setSupportZoom(true);
            getSettings().setDisplayZoomControls(false);
            getSettings().setBuiltInZoomControls(true);
            getSettings().setSupportMultipleWindows(false);
            getSettings().setEnableSmoothTransition(true);
            // HTML5 API flags
            getSettings().setAppCacheEnabled(true);
            getSettings().setDatabaseEnabled(true);
            getSettings().setDomStorageEnabled(true);

            // HTML5 configuration settings.
            getSettings().setAppCacheMaxSize(10 * 1024 * 1024);
            getSettings().setAppCachePath(context.getDir("appcache", Context.MODE_PRIVATE).getPath());
            getSettings().setDatabasePath(context.getDir("databases", Context.MODE_PRIVATE).getPath());
            getSettings().setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());
            //
            getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            setHorizontalScrollBarEnabled(false);
            setHorizontalScrollbarOverlay(true);

            getSettings().setAllowContentAccess(true);
            getSettings().setAllowFileAccess(true);

            CookieSyncManager.createInstance(context);
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.setAcceptFileSchemeCookies(true);
            if (Build.VERSION.SDK_INT >= 21) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
            }
            getSettings().setJavaScriptEnabled(true);
        } catch (Exception e) {

        }
        setWebViewClient(new MyWebViewClient(this));
        setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (Exception e) {

                }
            }
        });
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        if (client instanceof MyWebViewClient) {
            this.webViewClient = (MyWebViewClient) client;
        }
        super.setWebViewClient(client);
    }

    public MyWebViewClient getMyWebViewClient() {
        if (webViewClient == null) {
            setWebViewClient(new MyWebViewClient(this));
        }
        return webViewClient;
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        super.loadUrl(url, additionalHttpHeaders);
        this.curUrl = url;
        this.additionalHttpHeaders = additionalHttpHeaders;
    }

    public String getCurUrl() {
        return curUrl;
    }

    public Map<String, String> getAdditionalHttpHeaders() {
        return additionalHttpHeaders;
    }
}