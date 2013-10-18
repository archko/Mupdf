package cx.hell.android.pdfviewpro;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;
import android.widget.ProgressBar;

public class StreamUtils {
	
	public static byte[] readBytesFully(InputStream i) throws IOException {
		return StreamUtils.readBytesFully(i, 0, null);
	}
	
	public static byte[] readBytesFully(InputStream i, int max, ProgressBar progressBar) throws IOException {
		byte buf[] = new byte[4096];
		int totalReadBytes = 0;
		while(true) {
			int readBytes = 0;
			readBytes = i.read(buf, totalReadBytes, buf.length-totalReadBytes);
			if (readBytes < 0) {
				// end of stream
				break;
			}
			totalReadBytes += readBytes;
			if (progressBar != null) progressBar.setProgress(totalReadBytes);
			if (max > 0 && totalReadBytes > max) {
				throw new IOException("Remote file is too big");
			}
			if (totalReadBytes == buf.length) {
				// grow buf
				Log.d("cx.hell.android.pdfviewpro", "readBytesFully: growing buffer from " + buf.length + " to " + (buf.length*2));
				byte newbuf[] = new byte[buf.length*2];
				System.arraycopy(buf, 0, newbuf, 0, totalReadBytes);
				buf = newbuf;
			}
		}
		byte result[] = new byte[totalReadBytes];
		System.arraycopy(buf, 0, result, 0, totalReadBytes);
		return result;
	}
	
	public static String readStringFully(InputStream i) throws IOException {
		byte[] b = StreamUtils.readBytesFully(i);
		return new String(b, "utf-8");
	}
	
}
