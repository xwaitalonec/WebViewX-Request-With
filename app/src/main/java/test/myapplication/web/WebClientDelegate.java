package test.myapplication.web;


import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import test.myapplication.BuildConfig;
import test.myapplication.utils.Reflect;
import test.myapplication.utils.SSLUtils;
import test.myapplication.utils.UriUtils;

class WebClientDelegate {
    private String[] others = {"/", ".php", ".jsp", ".asp", ".aspx", ".html", ".xhtml", ".htm"};
    //不处理
    private String[] res = {".js", ".css", ".ico", ".png", ".jpg", ".pdf", ".zip", ".gif", ".bmp", ".rar", ".7z", ".gz"};
    private String[] images = {".ico", ".png", ".jpg", ".gif", ".bmp", ".webp"};
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "ProxySetting";
    private MyWebViewClient parent;
    private boolean ignoreImage = true;
    private WebSettings settings;

    WebClientDelegate(MyWebViewClient parent) {
        this.parent = parent;
        settings = parent.getWebView().getSettings();
        HttpsURLConnection.setDefaultSSLSocketFactory(SSLUtils.getTrustAllSSLSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(DO_NOT_VERIFY);
    }

    void setIgnoreImage(boolean ignoreImage) {
        this.ignoreImage = ignoreImage;
    }

    boolean checkUrl(String url) {
        if (url != null) {
            if (!url.startsWith("http")) {
                return false;
            }
            if (ignoreImage) {
//            图片资源不处理
                for (String r : images) {
                    if (url.endsWith(r)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private void dealHeaders(HttpURLConnection connection, String url, Map<String, String> headers) {
        boolean hasAccpetEncoding = false;
        boolean hasCookies = false;
        Log.d(TAG, "shouldInterceptRequest:headers:" + headers.size());
        for (Map.Entry<String, String> e : headers.entrySet()) {
            Log.d(TAG, "shouldInterceptRequest:header:" + e.getKey() + ":" + e.getValue());
            if ("X-Requested-With".equalsIgnoreCase(e.getKey())) {
                continue;
            } else if ("Accept-Charset".equalsIgnoreCase(e.getKey())) {
                hasAccpetEncoding = true;
            } else if ("Cookie".equalsIgnoreCase(e.getKey())) {
                hasCookies = true;
            }
            connection.setRequestProperty(e.getKey(), e.getValue());
        }
        if (!hasCookies) {
            connection.setRequestProperty("Cookie", CookieManager.getInstance().getCookie(url));
        }
        if (!hasAccpetEncoding) {
            // Log.d(TAG, "shouldInterceptRequest:addheader:Accept-Charset");
            // connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
        }
    }

    WebResourceResponse shouldInterceptRequest2(String method, final WebView view, String url, Map<String, String> headers) {
        try {
            URL uri = new URL(url);
            boolean ssl = uri.getProtocol().toLowerCase().equals("https");
            if (ssl) {
                if (DEBUG) {
                    Log.v(TAG, method + ":ignore:" + url);
                }
                return null;
            }
            if (DEBUG) {
                Log.d(TAG, method + ":load:" + url);
            }
            HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
            if (headers != null) {
                dealHeaders(connection, url, headers);
            }
            int code = 0;
            try {
                code = connection.getResponseCode();
            } catch (Exception e) {
                if (!ssl) {
                    if (DEBUG)
                        Log.w(TAG, "getResponseCode", e);
                }
                return null;
            }
            String type = connection.getContentType();
            //cookies保存
            try {
                String cookies = connection.getHeaderField("Cookie");
                if (!TextUtils.isEmpty(cookies)) {
                    CookieManager.getInstance().setCookie(url, cookies);
                }
            } catch (Exception e) {
                if (DEBUG)
                    Log.e(TAG, "setcookies", e);
            }
            if (code >= 200 && code <= 299 && (type != null)) {
                //加载网页内容
                type = type.toLowerCase(Locale.US);
                ByteArrayOutputStream outputStream = toString(connection.getInputStream());
                String html = outputStream.toString();
                if (type.contains("text/html") && isHtml(html)) {
                    loadHtml(view, outputStream, connection, url);
                    return new WebResourceResponse(null, null, null);
                } else if (DEBUG) {
                    Log.w(TAG, "shouldInterceptRequest:" + type);
                }
            }
            return new WebResourceResponse(connection.getContentType(), connection.getContentEncoding(), connection.getInputStream());
        } catch (IOException e) {
            if (DEBUG)
                Log.e(TAG, "shouldInterceptRequest", e);
        }
        return null;
    }

    private ByteArrayOutputStream toString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int len = 0;
        while ((len = inputStream.read(data)) != -1) {
            outputStream.write(data, 0, len);
        }
        return outputStream;
    }

    private boolean isHtml(String html) {
        Pattern pattern = Pattern.compile("<html", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(html);
        return m.find();
    }

    private String getEncodingBytype(String type) {
        if (!TextUtils.isEmpty(type)) {
            String[] ts = type.split(";");
            for (String t : ts) {
                t = t.trim().toLowerCase(Locale.US);
                if (t.contains("charset=")) {
                    return t.split("=")[1];
                }
            }
        }
        return null;
    }

    private String getEncodingByHtml(String defhtml) {
        Pattern pattern = Pattern.compile("<meta[^>]+?content=\"[^\"]*?charset=([a-z0-9A-Z_-]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(defhtml);
        if (matcher.find()) {
            if (DEBUG)
                Log.d(TAG, "find cht 1 " + matcher.group());
            if (matcher.groupCount() > 0) {
                String defEncode = matcher.group(1);
                if (DEBUG)
                    Log.d(TAG, "find cht 2 " + defEncode);
                return defEncode;
            }
        } else if (DEBUG) {
            Log.e(TAG, "no find cht");
        }
        return null;
    }

    private void loadHtml(final WebView view, ByteArrayOutputStream outputStream, HttpURLConnection connection, final String url) throws IOException {
        int code = connection.getResponseCode();
        final String type = connection.getContentType();
        String defhtml = outputStream.toString();
        String defEncode = connection.getContentEncoding();
        if (TextUtils.isEmpty(defEncode)) {
            //text/html; charset=gb2312
            defEncode = getEncodingBytype(type);
        }
        if (DEBUG)
            Log.i(TAG, "shouldInterceptRequest:" + code + ":" + defEncode + ":" + type + ":" + url);
        if (TextUtils.isEmpty(defEncode)) {
//            Log.i(TAG, "no find "+defhtml);
            defEncode = getEncodingByHtml(defhtml);
        }
        if (DEBUG) {
            int htmllen = defhtml.length();
            htmllen = htmllen > 20 ? 20 : htmllen;
            Log.d(TAG, "html\n" + defhtml.substring(0, htmllen));
        }
        if (!TextUtils.isEmpty(defEncode)) {
            try {
                defhtml = outputStream.toString(defEncode);
            } catch (Exception e) {
                if (DEBUG)
                    Log.e(TAG, "toString(" + defEncode + ")", e);
            }
        }
        if (!TextUtils.isEmpty(defhtml)) {
            show(view, url, defhtml, defEncode, type);
        }
    }

    private void show(final WebView view, final String url, final String html, final String _encode, final String type) {
        parent.post(new Runnable() {
            @Override
            public void run() {
                String encode = _encode;
                String _type = type;
                if (Build.VERSION.SDK_INT <= 17) {
                    encode = "utf-8";
                    if (type.contains(";")) {
                        String[] rs = type.split(";");
                        for (String r : rs) {
                            if (r.contains("charset")) {
                                continue;
                            }
                            _type = r;
                            break;
                        }
                    }
                }
                if (DEBUG)
                    Log.i(TAG, "shouldInterceptRequest:load:" + _type + ";encode=" + encode);
                if (getEncodingBytype(_type) != null) {
                    view.loadDataWithBaseURL(UriUtils.getCurPath(url), html, _type + ";charset=" + encode, encode, url);
                } else {
                    view.loadDataWithBaseURL(UriUtils.getCurPath(url), html, _type, encode, url);
                }
            }
        });
    }

    Map<String, String> makeDefaultHeaders(WebView view, String url, String curUrl) {
        Map<String, String> defHeaders = new HashMap<>();
        defHeaders.put("User-Agent", getWebSettings(view).getUserAgentString());
        //defHeaders.put("Accept-Encoding", "gzip,deflate");
        if (!TextUtils.isEmpty(curUrl) && curUrl.contains("://") && !TextUtils.equals(url, curUrl)) {
            //防盗链
            defHeaders.put("Referer", UriUtils.getHostName(curUrl));
        }
        if (view instanceof MyWebView) {
            MyWebView myWebView = (MyWebView) view;
            if (TextUtils.equals(url, myWebView.getCurUrl())) {
                Map<String, String> args = myWebView.getAdditionalHttpHeaders();
                if (args != null) {
                    defHeaders.putAll(args);
                }
            }
        }
        return defHeaders;
    }

    private WebSettings getWebSettings(WebView webView) {
        Object webViewProvider = null;
        try {
            webViewProvider = Reflect.on(webView).call("getWebViewProvider").get();
        } catch (Exception e) {
            try {
                webViewProvider = Reflect.on(webView).get("mProvider");
            } catch (Exception ex) {

            }
        }
        if (webViewProvider != null) {
            return Reflect.on(webViewProvider).call("getSettings").get();
        }
        return settings;
    }

}
