package test.myapplication.web;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import rabbit.DefaultConfig;
import rabbit.IProxyBinder;
import rabbit.ProxyService;
import rabbit.cache.NCache;
import rabbit.proxy.HttpProxy;
import rabbit.util.Config;
import test.myapplication.utils.ProxySettingUtils;
import test.myapplication.web.IWebProxy;
import test.myapplication.web.IWebProxyListener;


public class RabbitProxy implements IWebProxy, Runnable {

    private static final String TAG = "System";

    private HttpProxy httpProxy;
    private WebView webView;
    private Context context;
    private boolean setproxy;
    private volatile int port;

    private int remotePort;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                IProxyBinder binder = IProxyBinder.Stub.asInterface(service);
                synchronized (RabbitProxy.this) {
                    remotePort = binder.getProxyPort();
                    if (httpProxy == null) {
                        port = remotePort;
                        setProxy();
                        if (listener != null) {
                            listener.onProxyChange(setproxy, port);
                        }
                    }
                    Log.i(TAG, "remotePort:" + remotePort);
                }
            } catch (Exception e) {
                Log.e(TAG, "onServiceConnected", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            context.startService(new Intent(context, ProxyService.class));
        }
    };

    public RabbitProxy(WebView webView) {
        this.webView = webView;
        this.context = webView.getContext().getApplicationContext();
        this.context.bindService(new Intent(context, ProxyService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private Config getConfig() {
        Config config = null;
        try {
            config = new Config(context.getAssets().open("rabbit.conf"));
        } catch (Exception e) {
            Log.w(TAG, "no find aseets config,use default");
            try {
                config = new Config(new ByteArrayInputStream(DefaultConfig.CONF.getBytes()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if (config != null) {
            config.setProperty("rabbit.cache.NCache", "directory", new File(context.getCacheDir(), "rcache").getAbsolutePath());
        }
        return config;
    }

    @Override
    public boolean isProxy() {
        return setproxy;
    }

    @Override
    public boolean setProxy() {
        if (port <= 0) {
            return false;
        }
        setproxy = new ProxySettingUtils().setProxy(this.webView, "127.0.0.1", port);
        if (listener != null) {
            listener.onProxyChange(setproxy, port);
        }
        return setproxy;
    }

    @Override
    public boolean isRunning() {
        return port > 0;
    }

    @Override
    public boolean clearProxy() {
        boolean clear = new ProxySettingUtils().clearProxy(this.webView);
        setproxy = !clear;
        if (listener != null) {
            listener.onProxyChange(setproxy, port);
        }
        return clear;
    }

    @Override
    public boolean start() {
        new Thread(this).start();
        return true;
    }

    @Override
    public void run() {
        try {
            int times = 5;
            while (times > 0 && port <= 0) {
                synchronized (RabbitProxy.this) {
                    port = remotePort;
                }
                Thread.sleep(200);
                times--;
            }
            if (remotePort > 0) {
                port = remotePort;
                Log.i(TAG, "proxy remote ok:" + port);
            } else {
                if (httpProxy == null) {
                    httpProxy = new HttpProxy();
                }
                httpProxy.setConfig(getConfig());
                httpProxy.start();
                port = httpProxy.getPort();
                Log.i(TAG, "proxy start ok:" + port);
                setProxy();
                if (listener != null) {
                    listener.onProxyChange(setproxy, port);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "proxy start fail", e);
            port = 0;
        }
    }

    @Override
    public void stop() {
        httpProxy.stop();
    }

    @Override
    public void setListener(IWebProxyListener listener) {
        this.listener = listener;
    }

    @Override
    public IWebProxyListener getListener() {
        return listener;
    }

    private IWebProxyListener listener;
}
