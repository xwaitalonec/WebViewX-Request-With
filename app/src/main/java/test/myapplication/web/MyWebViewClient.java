package test.myapplication.web;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Map;

public class MyWebViewClient extends WebViewClient {
    private static final String TAG = "ProxySetting";
    private Handler handler;
    private String curUrl;
    private WebClientDelegate webClientDelegate;
    private boolean useTransfer = true;
    private IWebProxy proxy;
    private WebView webView;

    public MyWebViewClient(WebView webView) {
        this.webView = webView;
        handler = new Handler(webView.getContext().getMainLooper());
        webClientDelegate = new WebClientDelegate(this);
    }

    public void setUseTransfer(boolean useTransfer) {
        this.useTransfer = useTransfer;
    }

    public boolean isUseTransfer() {
        return useTransfer;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setProxy(IWebProxy proxy) {
        this.proxy = proxy;
        if (proxy != null) {
            if (proxy.getListener() != null) {
                setListener(proxy.getListener());
            }
        }
    }

    public void setListener(final IWebProxyListener listener) {
        if (this.proxy != null) {
            if ((listener instanceof MainIWebProxyListener)) {
                this.proxy.setListener(listener);
            } else {
                this.proxy.setListener(new MainIWebProxyListener(listener));
            }
        }
    }

    private class MainIWebProxyListener implements IWebProxyListener {
        private IWebProxyListener listener;

        private MainIWebProxyListener(IWebProxyListener listener) {
            this.listener = listener;
        }

        @Override
        public void onProxyChange(final boolean proxy, final int port) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onProxyChange(proxy, port);
                    }
                }
            });
        }
    }

    public boolean isProxy() {
        return proxy != null && proxy.isRunning() && proxy.isProxy();
    }

    public void setProxy() {
        if (proxy != null && proxy.isRunning()) {
            if (!proxy.isProxy()) {
                proxy.setProxy();
            }
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//        setProxy();
        return false;//super.shouldOverrideUrlLoading(view, url);
    }

    public String getCurUrl() {
        return curUrl;
    }

    void post(Runnable runnable) {
        handler.post(runnable);
    }

    void postDelayed(Runnable runnable, long dealy) {
        handler.postDelayed(runnable, dealy);
    }

    public void setIgnoreImage(boolean ignoreImage) {
        webClientDelegate.setIgnoreImage(ignoreImage);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        curUrl = url;
    }

    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        //<=4.4
        if (!isProxy() && isUseTransfer() && webClientDelegate.checkUrl(url)) {
            Map<String, String> defHeaders = webClientDelegate.makeDefaultHeaders(view, url, curUrl);
            WebResourceResponse resourceResponse = webClientDelegate.shouldInterceptRequest2("shouldInterceptRequest", view, url, defHeaders);
            if (resourceResponse != null) {
                return resourceResponse;
            }
        }
        return super.shouldInterceptRequest(view, url);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        //>=5.0
        if (!isProxy() && request != null) {
            String url = "" + request.getUrl();
            if (isUseTransfer() && webClientDelegate.checkUrl(url)) {
                Map<String, String> defHeaders = request.getRequestHeaders();
                if (defHeaders == null) {
                    defHeaders = webClientDelegate.makeDefaultHeaders(view, url, curUrl);
                }
                //defHeaders.put("Accept-Encoding", "gzip, deflate");
                WebResourceResponse resourceResponse = webClientDelegate.shouldInterceptRequest2("shouldInterceptRequest", view, url, defHeaders);
                if (resourceResponse != null) {
                    return resourceResponse;
                }
            }
        }
        return super.shouldInterceptRequest(view, request);
    }
}