package test.myapplication;

import android.app.Application;
import android.content.Context;

//import com.umeng.PluginUtilsss;

/**
 * Created by Administrator on 2016/10/9.
 */

public class App extends Application {
    public static boolean DEBUG = true;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
