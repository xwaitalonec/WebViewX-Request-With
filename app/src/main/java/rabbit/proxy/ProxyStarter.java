package rabbit.proxy;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import rabbit.util.Config;

/**
 * A class that starts up proxies.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class ProxyStarter {

    private static final String DEFAULT_CONFIG = "conf/rabbit.conf";

    public void startProxy(Config conf) {
        try {
            HttpProxy p = new HttpProxy();
            p.setConfig(conf);
            p.start();
        } catch (IOException e) {
            System.err.println("failed to configure proxy, ignoring: " + e);
            e.printStackTrace();
        }
    }
}
