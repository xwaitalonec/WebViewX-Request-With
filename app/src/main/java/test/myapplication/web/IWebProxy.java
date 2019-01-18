package test.myapplication.web;


public interface IWebProxy {
    boolean isProxy();
    boolean setProxy();
    boolean isRunning();
    boolean clearProxy();
    boolean start();
    void stop();
    void setListener(IWebProxyListener listener);
    IWebProxyListener getListener();
}
