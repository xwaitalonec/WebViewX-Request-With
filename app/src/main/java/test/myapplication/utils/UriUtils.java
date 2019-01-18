package test.myapplication.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UriUtils {
    private static final String UTF_8 = "UTF-8";

    /**
     * 数据转query
     *
     * @param list 数据
     * @return query字符串，不带?
     */
    public static String toQueryString(Map<String, String> list) {
        if (list == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> e : list.entrySet()) {
            sb.append(e.getKey() + "=" + encode(e.getValue()));
            sb.append("&");
        }
        String args = sb.toString();
        if (args.endsWith("&")) {
            args = args.substring(0, args.length() - 1);
        }
        return args;
    }

    /***
     * 从连接获取query数据
     *
     * @param uri 链接
     * @return 数据集合
     */
    public static Map<String, String> getQuerys(String uri) {
        Map<String, String> args = new HashMap<String, String>();
        if (uri != null) {
            int index = uri.indexOf('?');
            if (index > 0 && index < uri.length()) {
                uri = uri.substring(index + 1);
                String[] tmps = uri.split("[\\?|&]");
                for (String tmp : tmps) {
                    String[] m = tmp.split("=");
                    if (m.length == 2) {
                        args.put(m[0], decode(m[1]));
                    } else {
                        args.put(m[0], "");
                    }
                }
            }
        }
        return args;
    }

    /**
     * @param uri 网址
     * @return 移除?以及后面的数据
     */
    public static String removeQuery(String uri) {
        if (uri != null) {
            int index = uri.indexOf('?');
            if (index > 0 && index < uri.length()) {
                uri = uri.substring(0, index);
            }
        }
        return uri;
    }

    /***
     * url解码
     *
     * @param str 字符串
     * @return 解码后字符串
     */
    public static String decode(String str) {
        String nstr = str;
        try {
            nstr = URLDecoder.decode(str, UTF_8);
        } catch (Exception e) {

        }
        return nstr;
    }

    /***
     * 获取当前路径
     *
     * @param url url
     * @return
     */
    public static String getCurPath(String url) {
        if (url == null) return "";
        int i = url.lastIndexOf("/");
        if (i > 0) {
            return url.substring(0, i);
        }
        return url;
    }

    /***
     * 获取链接的域名
     *
     * @param url url
     * @return
     */
    public static String getHostName(String url) {
        if (url == null) return "";
        int i = url.indexOf("://");
        if (i > 0) {
            int j = url.indexOf("/", i + 3);
            if (j >= 0) {
                return url.substring(0, j);
            } else {
                return url;
            }
        }
        return url;
    }

    public static String removeHost(String url) {
        if (url == null) return "";
        int i = url.indexOf("://");
        if (i > 0) {
            //http://127.0.0.1/index.html
            int j = url.indexOf("/", i + 3);
            if (j >= 0) {
                return url.substring(j);
            } else {
                return url;
            }
        }
        int j = url.indexOf("/");
        if (j >= 0) {
            return url.substring(j);
        }
        return url;
    }

    /***
     * 拼接2个网址，处理重复/
     *
     * @param url1
     * @param url2 如果url2以http开头，则不处理
     * @return
     */
    public static String join(String url1, String url2) {
        if (url1 == null) {
            return url2;
        }
        url1 = removeQuery(url1);
        if (url2.startsWith("http")) {
            return url2;
        }
        if (url2.startsWith("./")) {
            url1 = getHostName(url1);
            url2 = url2.substring(1);
        } else {
            url1 = getCurPath(url1);
        }
        if (!url1.endsWith("/")) {
            url1 += "/";
        }
        if (url2.startsWith("/")) {
            url2 = url2.substring(1);
        }
        return url1 + url2;
    }

    /***
     * 手动重定向
     *
     * @param baseUrl 链接
     * @param rs      内容
     * @return
     */
    public static String findRedirect(String baseUrl, String rs) {
        String html = rs.replace(" ", "");
        Pattern js = Pattern.compile("<script[^>]*?>[\\s\\S]*?</script>", Pattern.MULTILINE | Pattern.DOTALL);
        String[] regs = {"[self.|top.|.window.]*?location[.href]*=['|\"]([\\S]+?)['|\"]",
                         "<meta[\\s\\S]*?http-equiv=\"refresh\"[\\s\\S]*?url=\"([\\S]+)\""
        };
        Matcher mjs = js.matcher(rs);
        String url = null;
        while (url == null && mjs.find()) {
            Matcher m;
            String str = mjs.group();
            //去除行注释
            str = str.replaceAll("\\n(\\s*?//[^\\n]*)?\\n", "");
            //去除多行注释
            str = str.replaceAll("/\\*[\\s\\S]*?\\*/", "");
            for (String reg : regs) {
                Pattern pattern = Pattern.compile(reg, Pattern.MULTILINE | Pattern.DOTALL);
                m = pattern.matcher(str);
                if (m.find()) {
                    url = join(baseUrl, m.groupCount() > 0 ? m.group(1) : m.group());
                    break;
                }
            }
        }
        if (url != null) {
            //补额外url参数
            final Map<String, String> data = UriUtils.getQuerys(url);
            final Map<String, String> args = UriUtils.getQuerys(baseUrl);
            String k = findKey(html, "location.href");
            if (k != null) {
                data.put(k, baseUrl);
            }
            if (html.contains("+location.search") || html.contains("+window.location.search")) {
                data.putAll(args);
            }
            return UriUtils.removeQuery(url) + "?" + toQueryString(data);
        }
        return null;
    }

    private static String findKey(String html, String key) {
        int i = html.indexOf(key);
        if (i > 0) {
            i = html.indexOf(key, i + key.length());
            if (i > 0) {
                int s = html.lastIndexOf("&", i);
                if (s >= 0) {
                    int e = html.indexOf("=", s);
                    if (e >= 0) {
                        return html.substring(s + 1, e);
                    }
                }
            }
        }
        return null;
    }

    /***
     * url加密
     *
     * @param str 字符串
     * @return 加密后
     */
    public static String encode(String str) {
        String nstr = str;
        try {
            nstr = URLEncoder.encode(str, UTF_8);
        } catch (Exception e) {

        }
        return nstr;
    }
}
