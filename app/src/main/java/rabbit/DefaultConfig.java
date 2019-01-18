package rabbit;

public class DefaultConfig {
    public static final String CONF="[rabbit.proxy.HttpProxy]\n" +
            "port=0\n" +
            "proxyhost=\n" +
            "proxyport=\n" +
            "proxyauth=\n" +
            "logo=http://$proxy/FileSender/public/smallRabbIT3.png\n" +
            "StrictHTTP=false\n" +
            "[logging]\n" +
            "loglevel=ERROR\n" +
            "[dns]\n" +
            "dnsHandler=rabbit.proxy.DNSSunHandler\n" +
            "[rabbit.proxy.DNSJavaHandler]\n" +
            "dnscachetime=8\n" +
            "[rabbit.io.ConnectionHandler]\n" +
            "keepalivetime=15000\n" +
            "usepipelining=false\n" +
            "[Handlers]\n" +
            "image/gif=rabbit.handler.ImageHandler\n" +
            "image/jpeg=rabbit.handler.ImageHandler\n" +
            "image/png=rabbit.handler.ImageHandler\n" +
            "text/html=rabbit.handler.FilterHandler\n" +
            "text/html; charset\\=iso-8859-1=rabbit.handler.FilterHandler\n" +
            "text/html;charset\\=iso-8859-1=rabbit.handler.FilterHandler\n" +
            "text/html; charset\\=iso8859_1=rabbit.handler.FilterHandler\n" +
            "text/html;charset\\=iso8859_1=rabbit.handler.FilterHandler\n" +
            "text/html; charset\\=gbk=rabbit.handler.FilterHandler\n" +
            "text/html;charset\\=gbk=rabbit.handler.FilterHandler\n" +
            "text/html; charset\\=gb2312=rabbit.handler.FilterHandler\n" +
            "text/html;charset\\=gb2312=rabbit.handler.FilterHandler\n" +
            "text/plain=rabbit.handler.GZipHandler\n" +
            "text/plain; charset\\=iso-8859-1=rabbit.handler.GZipHandler\n" +
            "text/plain; charset\\=UTF-8=rabbit.handler.GZipHandler\n" +
            "text/xml=rabbit.handler.GZipHandler\n" +
            "text/xml; charset\\=iso-8859-1=rabbit.handler.GZipHandler\n" +
            "text/xml; charset\\=utf-8=rabbit.handler.GZipHandler\n" +
            "text/xml; charset\\=utf8=rabbit.handler.GZipHandler\n" +
            "application/xml=rabbit.handler.GZipHandler\n" +
            "application/xml; charset\\=utf-8=rabbit.handler.GZipHandler\n" +
            "application/xml; charset\\=utf8=rabbit.handler.GZipHandler\n" +
            "application/postscript=rabbit.handler.GZipHandler\n" +
            "application/postscript; charset\\=utf-8=rabbit.handler.GZipHandler\n" +
            "application/postscript; charset\\=utf8=rabbit.handler.GZipHandler\n" +
            "text/css=rabbit.handler.GZipHandler\n" +
            "text/css; charset\\=utf-8=rabbit.handler.GZipHandler\n" +
            "text/css; charset\\=utf8=rabbit.handler.GZipHandler\n" +
            "[CacheHandlers]\n" +
            "[rabbit.cache.NCache]\n" +
            "directory=/tmp/rcache\n" +
            "cachetime=24\n" +
            "maxsize=10000\n" +
            "cleanloop=60\n" +
            "[Filters]\n" +
            "httpinfilters=rabbit.filter.HttpBaseFilter,rabbit.filter.DontFilterFilter,rabbit.filter.BlockFilter\n" +
            "httpoutfilters=rabbit.filter.HttpBaseFilter\n" +
            "[sslhandler]\n" +
            "allowSSL=yes          # allow to all ports.\n" +
            "[rabbit.filter.AccessFilter]\n" +
            "[rabbit.filter.HttpBaseFilter]\n" +
            "remove=Connection,Proxy-Connection,Keep-Alive,Public,Transfer-Encoding,Upgrade,Proxy-Authorization,TE,Proxy-Authenticate,Trailer\n" +
            "cookieid=false\n" +
            "[rabbit.filter.DontFilterFilter]\n" +
            "dontFilterURLmatching=(login\\.passport\\.com|\\.jar|www\\.ureach\\.com)\n" +
            "dontFilterAgentsMatching=Java\n" +
            "[rabbit.filter.DontCacheFilter]\n" +
            "[rabbit.filter.BlockFilter]\n" +
            "blockURLmatching=(\\.sex\\.|[-.]ad(s?)\\.|/ad\\.|adserving\\.|ad101com-|pagead/imgad)\n" +
            "[rabbit.filter.HttpSnoop]\n" +
            "[rabbit.filter.ProxyAuth]   \n" +
            "[rabbit.filter.SQLProxyAuth]   \n" +
            "driver=\n" +
            "url=\n" +
            "user=\n" +
            "password=\n" +
            "select=select password from rabbit_users where username=?\n" +
            "[rabbit.filter.ReverseProxy]\n" +
            "transformMatch=^/(.*)\n" +
            "transformTo=http://www.khelekore.org/$1\n" +
            "deny=^http(s?)://.*\n" +
            "allowMeta=true\n" +
            "[rabbit.handler.GZipHandler]\n" +
            "compress=true\n" +
            "[rabbit.handler.ImageHandler]\n" +
            "convert=/usr/bin/convert\n" +
            "convertargs=-quality 10 -flatten $filename +profile \"*\" jpeg:$filename.c\n" +
            "[rabbit.handler.FilterHandler]\n" +
            "filters=rabbit.filter.BodyFilter,rabbit.filter.BlinkFilter,rabbit.filter.LowresImageFilter\n" +
            "[rabbit.filter.BodyFilter]\n" +
            "prefix=<center><font size=-2><a href=\"http://www.khelekore.org/rabbit/\" target=\"_top\">RabbIT</a> filtered this page.\n" +
            "postfix=</font></center><br>\n" +
            "unfilteredlink=true\n" +
            "link=unfiltered page\n" +
            "[rabbit.filter.BackgroundFilter]\n" +
            "adlinks=[/.]((c|net|ns|surf|page|imag)?ad([svq]|fu|srv|[sz]erver|log|bannercenter|_?click|verts|finity|force|click)?\\d*|banner|linkexchange|acc_clickthru)[/.]|gen_addframe|event.ng|/m=|/ad(num|vert|name)?=|/site_id=|support.net|/redir\\.|\\?assoc=\n" +
            "adreplacer=http://$proxy/FileSender/public/NoAd.gif\n" +
            "[rabbit.filter.LowresImageFilter]\n";
}
