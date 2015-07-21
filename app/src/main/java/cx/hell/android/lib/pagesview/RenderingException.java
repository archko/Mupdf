package cx.hell.android.lib.pagesview;

import java.io.Serializable;

public class RenderingException extends Exception implements Serializable{
	private static final long serialVersionUID = 1010978161527539002L;
	
	public RenderingException(String message) {
		super(message);
	}
}
