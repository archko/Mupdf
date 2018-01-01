package org.vudroid.pdfdroid.codec;

import com.artifex.mupdf.viewer.MuPDFCore;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

public class PdfDocument implements CodecDocument {
    MuPDFCore core;

    public void setCore(MuPDFCore core) {
        this.core = core;
    }

    public MuPDFCore getCore() {
        return core;
    }

    public CodecPage getPage(int pageNumber) {
        return PdfPage.createPage(core, pageNumber);
    }

    public int getPageCount() {
        return core.countPages();
    }

    static PdfDocument openDocument(String fname, String pwd) {
        //return new PdfDocument(open(FITZMEMORY, fname, pwd));
        PdfDocument document = new PdfDocument();
        MuPDFCore core = null;
        System.out.println("Trying to open " + fname);
        try {
            core = new MuPDFCore(fname);
            document.setCore(core);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return document;
    }

    private static native long open(int fitzmemory, String fname, String pwd);

    private static native void free(long handle);

    private static native int getPageCount(long handle);

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (null != core) {
            core.onDestroy();
        }
    }
}
