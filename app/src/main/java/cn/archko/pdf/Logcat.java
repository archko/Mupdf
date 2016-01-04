package cn.archko.pdf;

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
     * Writes the current app logcat to a file.
     *
     * @param filename The filename to save it as
     * @throws java.io.IOException
     */
    public static void writeLogcat(String filename) throws IOException {
        String[] args = { "logcat", "-v", "time", "-d" };

        Process process = Runtime.getRuntime().exec(args);

        InputStreamReader input = new InputStreamReader(process.getInputStream());

        FileOutputStream fileStream;
        try {
            fileStream = new FileOutputStream(filename);
        } catch( FileNotFoundException e) {
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
        }catch(Exception e) {}
        finally {
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
        String[] args = { "logcat", "-v", "time", "-d", "-t", "500" };

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
