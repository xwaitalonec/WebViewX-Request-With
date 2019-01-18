package rabbit;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import rabbit.proxy.HttpProxy;
import rabbit.util.Config;


public class ProxyService extends Service {
    private HttpProxy httpProxy;
    private int port;
    private static final String TAG = "System";
    @Override
    public IBinder onBind(Intent intent) {
        return new IProxyBinder.Stub(){

            @Override
            public int getProxyPort() throws RemoteException {
                synchronized (ProxyService.class) {
                    return port;
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Notification notification = new Notification();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            startForeground(0, notification);
        } catch (Throwable e) {
            // Ignore
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (httpProxy == null) {
                        httpProxy = new HttpProxy();
                    }
                    httpProxy.setConfig(getConfig());
                    httpProxy.start();
                    synchronized (ProxyService.class) {
                        port = httpProxy.getPort();
                    }
                    Log.i(TAG, "proxyservice start ok:" + port);
                } catch (Exception e) {
                    Log.e(TAG, "proxyservice start fail", e);
                    port = 0;
                }
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        synchronized (this) {
            if (httpProxy != null) {
                httpProxy.stop();
                httpProxy = null;
            }
        }
        super.onDestroy();
        startup(this);
    }

    private Config getConfig() {
        Config config = null;
        try {
            config = new Config(getAssets().open("rabbit.conf"));
        } catch (Exception e) {
            Log.w(TAG, "no find aseets config,use default");
            try {
                config = new Config(new ByteArrayInputStream(DefaultConfig.CONF.getBytes()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if (config != null) {
            config.setProperty("rabbit.cache.NCache", "directory", new File(getCacheDir(), "rcache").getAbsolutePath());
        }
        return config;
    }

    public void startup(Context context) {
        context.startService(new Intent(context, getClass()));
    }
}
