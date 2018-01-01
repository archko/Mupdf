package cn.archko.pdf.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @description:
 * @author: archko 13-12-16 :上午11:13
 */
public class DateUtil {

    /**
     * yyyy-MM-dd
     */
    public static final String TIME_FORMAT_ONE = "yyyy-MM-dd";
    /**
     * yyyy-MM-dd HH:mm
     */
    public static final String TIME_FORMAT_TWO = "yyyy-MM-dd HH:mm";
    /**
     * yyyy-MM-dd HH:mmZ
     */
    public static final String TIME_FORMAT_THREE = "yyyy-MM-dd HH:mmZ";
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    public static final String TIME_FORMAT_FOUR = "yyyy-MM-dd HH:mm:ss";
    /**
     * yyyy-MM-dd HH:mm:ss.SSSZ
     */
    public static final String TIME_FORMAT_FIVE = "yyyy-MM-dd HH:mm:ss.SSSZ";
    /**
     * yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static final String TIME_FORMAT_SIX = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * HH:mm:ss
     */
    public static final String TIME_FORMAT_SEVEN = "HH:mm:ss";
    /**
     * HH:mm:ss.SS
     */
    public static final String TIME_FORMAT_EIGHT = "HH:mm:ss.SS";
    /**
     * yyyy.MM.dd
     */
    public static final String TIME_FORMAT_9 = "yyyy.MM.dd";
    /**
     * MM月dd日
     */
    public static final String TIME_FORMAT_10 = "MM月dd日";
    public static final String TIME_FORMAT_11 = "MM-dd HH:mm";
    public static final String TIME_FORMAT_12 = "yyMM";
    /**
     * HH:mm
     */
    public static final String FORMAT13 = "HH:mm";
    public static final String TIME_FORMAT_14 = "M月d日";
    public static final String TIME_FORMAT_15 = "yyyy.MM.dd HH:mm:ss";

    /**
     * 日期转换
     */
    public static long parseTime(String dateString, String format) {
        if (dateString == null || dateString.length() == 0) {
            return -1;
        }
        try {
            return new SimpleDateFormat(format).parse(dateString).getTime();
        } catch (ParseException e) {

        }
        return -1;
    }

    /**
     * 格式化时间
     */
    public static String formatTime(long time, String format) {
        return new SimpleDateFormat(format).format(new Date(time));
    }

    public static String getSLFriendlyTime(long time) {
        return getSLFriendlyTime(time, "MM-dd");
    }

    public static String getSLFriendlyTime(long time, String format) {
        long now = System.currentTimeMillis();
        long elapse = now - time;
        if (elapse < 60 * 1000) {
            return "1分钟前";
        } else if (elapse < 3600 * 1000) {
            return (elapse / 1000 / 60) + "分钟前";
        } else if (elapse < 24 * 3600 * 1000) {
            return (elapse / 1000 / 3600) + "小时前";
        } /*else if (elapse<2*24*3600*1000) {
            return "昨天"+formatTime(time, FORMAT13);
        } else if (elapse<3*24*3600*1000) {
            return "前天"+formatTime(time, FORMAT13);
        }*/ else {
            return formatTime(time, format);
        }
    }

    //---------------------------

    public static String MD5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes());
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
        }
        return data;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
