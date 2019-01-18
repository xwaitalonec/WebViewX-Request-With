package test.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import java.util.HashMap;
import java.util.Map;

import test.myapplication.web.RabbitProxy;
import test.myapplication.web.IWebProxy;
import test.myapplication.web.IWebProxyListener;
import test.myapplication.web.MyWebView;

public class MainActivity extends Activity {
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final MyWebView mWebView2 = (MyWebView) findViewById(R.id.webview2);
        IWebProxy webProxy = new RabbitProxy(mWebView2);
        webProxy.setListener(new IWebProxyListener() {
            @Override
            public void onProxyChange(boolean proxy, int port) {
                if (proxy) {
                    final Map<String, String> extraHeaders = new HashMap<String, String>();
                    extraHeaders.put("requestMethod", "GET");
                    mWebView2.loadUrl(SSL_URL, extraHeaders);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mWebView2.loadUrl(SSL_URL, extraHeaders);
                        }
                    }, 5000);
                    //貌似有点延迟
                }
            }
        });
        mWebView2.loadUrl(SSL_URL);
        mWebView2.getMyWebViewClient().setProxy(webProxy);
        webProxy.start();
    }

    private static final String TEST_URL = "http://192.168.1.110/header.php";
    private static final String SSL_URL = "https://www.baidu.com/";
}
