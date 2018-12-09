package cn.archko.pdf.common;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Logcat {
    public final static String TAG = "Logcat";

    /**
     * 是否允许输出日志
     */
    public static boolean loggable = true;

    public static void v(String tag, String msg) {
        if (loggable) {
            Log.v(tag, msg);
        }
    }

    public static void d(String msg) {
        if (loggable) {
            Log.d(TAG, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (loggable) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (loggable) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (loggable) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (loggable) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, Throwable throwable) {
        if (loggable) {
            Log.e(tag, throwable.getMessage(), throwable);
        }
    }

    public static void e(String tag, String msg, Throwable throwable) {
        if (loggable) {
            Log.e(tag, msg, throwable);
        }
    }

    /**
     * 长日志，以便显示全部的信息
     *
     * @param tag
     * @param tempData 日志内容
     */
    public static void longLog(String tag, String tempData) {
        if (!loggable) {
            return;
        }

        tempData = tempData;
        final int len = tempData.length();
        final int div = 2000;
        int count = len / div;
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                Log.d(tag, tempData.substring(i * div, (i + 1) * div));
            }
            int mode = len % div;
            if (mode > 0) {
                Log.d(tag, tempData.substring(div * count, len));
            }
        } else {
            Log.d(tag, tempData);
        }
    }

    /**
     * Writes the current app logcat to a file.
     *
     * @param filename The filename to save it as
     * @throws java.io.IOException
     */
    public static void writeLogcat(String filename) throws IOException {
        String[] args = {"logcat", "-v", "time", "-d"};

        Process process = Runtime.getRuntime().exec(args);

        InputStreamReader input = new InputStreamReader(process.getInputStream());

        FileOutputStream fileStream;
        try {
            fileStream = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            return;
        }

        OutputStreamWriter output = new OutputStreamWriter(fileStream);
        BufferedReader br = new BufferedReader(input);
        BufferedWriter bw = new BufferedWriter(output);

        try {
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
        } catch (Exception e) {
        } finally {
            bw.close();
            output.close();
            br.close();
            input.close();
        }
    }

    /**
     * Get the last 500 lines of the application logcat.
     *
     * @return the log string.
     * @throws java.io.IOException
     */
    public static String getLogcat() throws IOException {
        String[] args = {"logcat", "-v", "time", "-d", "-t", "500"};

        Process process = Runtime.getRuntime().exec(args);
        InputStreamReader input = new InputStreamReader(
                process.getInputStream());
        BufferedReader br = new BufferedReader(input);
        StringBuilder log = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null)
            log.append(line + "\n");

        br.close();
        input.close();

        return log.toString();
    }

}
