package test.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import java.util.Map;

import test.myapplication.BuildConfig;
import test.myapplication.utils.Reflect;

public class ProxySettingUtils {

    private static final String LOG_TAG = "ProxySetting";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public boolean setProxy(WebView webview, String host, int port) {
        if (webview == null) {
            return false;
        }
        if (TextUtils.isEmpty(host) || port == 0) {
            return clearProxy(webview);
        }
        // 3.2 (HC) or lower
        if (Build.VERSION.SDK_INT <= 13) {
            return setProxyBefore14(webview, host, port);
        }
        // ICS: 4.0-4.3 (JB)
        else if (Build.VERSION.SDK_INT <= 18) {
            return setProxy14(webview, host, port);
        }
        // 4.4 (KK) & 5.0 (Lollipop)
        else {
            return setProxy19(webview, host, "" + port);
        }
    }

    public boolean clearProxy(WebView webview) {
        if (webview == null) {
            return false;
        }
        // 3.2 (HC) or lower
        if (Build.VERSION.SDK_INT <= 13) {
            return setProxyBefore14(webview, null);
        }
        // ICS: 4.0 -4.3 (JB)
        else if (Build.VERSION.SDK_INT <= 18) {
            return setProxy14(webview, null);
        }
        // 4.4 (KK) & 5.0 (Lollipop)
        else {
            return setProxy19(webview, null, null);
        }
    }

    /**
     * Set Proxy for Android 3.2 and below.
     */
    private boolean setProxyBefore14(WebView webview, String host, int port) {
        if (DEBUG)
            Log.d(LOG_TAG, "Setting ProxySetting with <= 3.2 API.");
        Object proxy = Reflect.on("org.apache.http.HttpHost").create(host, port).get();
        return setProxyBefore14(webview, proxy);
    }

    private boolean setProxyBefore14(WebView webview, Object proxyServer) {
        // Getting network
        Object network = null;
        Context context = webview.getContext().getApplicationContext();
        try {
            network = Reflect.on("android.webkit.Network").call("getInstance", context).get();
            Object requestQueue = Reflect.on(network).get("mRequestQueue");// getFieldValueSafely(requestQueueField, network);
            Reflect.on(requestQueue).set("mProxyHost", proxyServer);
        } catch (Exception ex) {
            if (DEBUG)
                Log.e(LOG_TAG, "setProxyBefore14", ex);
            return false;
        }
        if (DEBUG)
            Log.d(LOG_TAG, "Setting ProxySetting with <= 3.2 API successful!");
        return true;
    }

    private boolean setProxy14(WebView webview, String host, int port) {
        try {
            Object proxy = Reflect.on("android.net.ProxyProperties").create(host, port, null).get();
            return setProxy14(webview, proxy);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean setProxy14(WebView webview, Object proxy) {
        if (Build.VERSION.SDK_INT < 14) {
            return false;
        }
        try {
            Object webviewcore;
            if (Build.VERSION.SDK_INT <= 15) {
                if (DEBUG)
                    Log.d(LOG_TAG, "Setting ProxySetting with 4.0 - 4.0.3 API.");
                webviewcore = Reflect.on(webview).get("mWebViewCore");
            } else {
                if (DEBUG)
                    Log.d(LOG_TAG, "Setting ProxySetting with 4.1 - 4.3 API.");
                Object webViewClassic = Reflect.on("android.webkit.WebViewClassic")
                        .call("fromWebView", webview).get();
                webviewcore = Reflect.on(webViewClassic).get("mWebViewCore");
            }
            Object mBrowserFrame = Reflect.on(webviewcore).get("mBrowserFrame");
            Object sJavaBridge = Reflect.on(mBrowserFrame).get("sJavaBridge");
            Reflect.on(sJavaBridge).call("updateProxy", proxy);
        } catch (Exception ex) {
            if (DEBUG)
                Log.e(LOG_TAG, "Setting ProxySetting with >= 4.0 API failed with error: ", ex);
            return false;
        }
        if (DEBUG)
            Log.d(LOG_TAG, "Setting ProxySetting with 4.1 - 4.3 API successful:" + proxy);
        return true;
    }

    // from https://stackoverflow.com/questions/19979578/android-webview-set-proxy-programatically-kitkat
    private boolean setProxy19(WebView webView, String host, String port) {
        if (DEBUG)
            Log.d(LOG_TAG, "Setting ProxySetting with >= 4.4 API.");
        Context appContext = webView.getContext().getApplicationContext();
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port);
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port);
        try {
            Object loadedApk = Reflect.on(appContext).get("mLoadedApk");
            Map receivers = Reflect.on(loadedApk).get("mReceivers");
            Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((Map) receiverMap).keySet()) {
                    Class<?> clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        if (DEBUG)
                            Log.d(LOG_TAG, "receiver=" + rec);
                        Reflect.on(rec).call("onReceive", appContext, intent);
                    }
                }
            }
            if (DEBUG)
                Log.d(LOG_TAG, "Setting ProxySetting with >= 4.4 API successful:" + port);
            return true;
        } catch (Exception e) {
            if (DEBUG)
                Log.e(LOG_TAG, "setProxy19", e);
        }
        return false;
    }
}