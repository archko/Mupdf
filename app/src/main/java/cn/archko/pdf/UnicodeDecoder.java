package cn.archko.pdf;

import java.io.UnsupportedEncodingException;

/**
 * @author: wushuyong 2018/11/30 :9:24 AM
 */
public class UnicodeDecoder {

    public static String escape(String src) {
        int i;
        char j;
        StringBuffer tmp = new StringBuffer();
        tmp.ensureCapacity(src.length() * 6);
        for (i = 0; i < src.length(); i++) {
            j = src.charAt(i);
            if (Character.isDigit(j) || Character.isLowerCase(j) || Character.isUpperCase(j))
                tmp.append(j);
            else if (j < 256) {
                tmp.append("%");
                if (j < 16)
                    tmp.append("0");
                tmp.append(Integer.toString(j, 16));
            } else {
                tmp.append("%u");
                tmp.append(Integer.toString(j, 16));
            }
        }
        return tmp.toString();
    }

    public static String unescape(String src) {
        StringBuffer tmp = new StringBuffer();
        tmp.ensureCapacity(src.length());
        int lastPos = 0, pos = 0;
        char ch;
        while (lastPos < src.length()) {
            pos = src.indexOf("%", lastPos);
            if (pos == lastPos) {
                if (src.charAt(pos + 1) == 'u') {
                    ch = (char) Integer.parseInt(src.substring(pos + 2, pos + 6), 16);
                    tmp.append(ch);
                    lastPos = pos + 6;
                } else {
                    ch = (char) Integer.parseInt(src.substring(pos + 1, pos + 3), 16);
                    tmp.append(ch);
                    lastPos = pos + 3;
                }
            } else {
                if (pos == -1) {
                    tmp.append(src.substring(lastPos));
                    lastPos = src.length();
                } else {
                    tmp.append(src.substring(lastPos, pos));
                    lastPos = pos;
                }
            }
        }
        return tmp.toString();
    }

    //&#x编码转换成汉字（java）
    public static String unescape2(String src) {
        StringBuilder tmp = new StringBuilder();
        try {
            tmp.ensureCapacity(src.length());
            int lastPos = 0, pos = 0;
            char ch;
            src = src.replace("&#x", "%u").replace(";", "");
            while (lastPos < src.length()) {
                pos = src.indexOf("%", lastPos);
                if (pos == lastPos) {
                    if (src.charAt(pos + 1) == 'u') {
                        ch = (char) Integer.parseInt(src.substring(pos + 2, pos + 6), 16);
                        tmp.append(ch);
                        lastPos = pos + 6;
                    } else {
                        ch = (char) Integer.parseInt(src.substring(pos + 1, pos + 3), 16);
                        tmp.append(ch);
                        lastPos = pos + 3;
                    }
                } else {

                    if (pos == -1) {
                        tmp.append(src.substring(lastPos));
                        lastPos = src.length();
                    } else {
                        tmp.append(src.substring(lastPos, pos));
                        lastPos = pos;
                    }
                }
            }
            return tmp.toString();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return src;
        }
    }


    public static void main(String[] args) throws UnsupportedEncodingException {
        String str = "<p>&#x53c2;&#x8003;</p>=====block'=====\n" +
                "    <p>&#x63d0;&#x4f9b;&#x5173;&#x4e8e; Kotlin &#x8bed;&#x8a00;&#x548c;&#x6807;&#x51c6;&#x5e93;&#x7684;&#x5b8c;&#x6574;&#x53c2;&#x8003;&#x3002;</p>=====block'=====\n" +
                "    <p>&#x4ece;&#x54ea;&#x5f00;&#x59cb;</p>=====block'=====";
        String name = str.replace("&#x", "%u");
        String name2 = name.replace(";", "");
        String str2 = unescape2(str);
        System.out.println("newStr: " + str2);
    }
}
