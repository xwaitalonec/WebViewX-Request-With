package test.myapplication.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class SSLUtils {
    private static SSLSocketFactory trustAllSSlSocketFactory;
    private static SSLSocketFactory sslSocketFactory;

    public static SSLSocketFactory getAllSSLSocketFactory() {
        if (sslSocketFactory == null) {
            synchronized (SSLUtils.class) {
                if (sslSocketFactory == null) {
                    X509TrustManager xtm = new X509TrustManager(){

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    };
                    TrustManager mytm[] = { xtm };
                    try {
                        // 得到上下文
                        SSLContext ctx = SSLContext.getInstance("SSL");
                        // 初始化
                        ctx.init(null, mytm, null);
                        // 获得工厂
                        sslSocketFactory = ctx.getSocketFactory();
                    }catch (Exception e){

                    }
                }
            }
        }
        return sslSocketFactory;
    }


    public static SSLSocketFactory getTrustAllSSLSocketFactory() {
        if (trustAllSSlSocketFactory == null) {
            synchronized (SSLUtils.class) {
                if (trustAllSSlSocketFactory == null) {

                    // 信任所有证书
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                    };
                    try {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, trustAllCerts, null);
                        trustAllSSlSocketFactory = sslContext.getSocketFactory();
                    } catch (Throwable ex) {
                    }
                }
            }
        }

        return trustAllSSlSocketFactory;
    }
}
