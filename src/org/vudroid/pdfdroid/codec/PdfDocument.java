package org.vudroid.pdfdroid.codec;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.OutlineActivityData;
import cx.hell.android.pdfviewpro.APVApplication;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

public class PdfDocument implements CodecDocument
{
    private long docHandle;
    private static final int FITZMEMORY = 512 * 1024;
    MuPDFCore core;

    public void setCore(MuPDFCore core) {
        this.core=core;
    }

    private PdfDocument(long docHandle)
    {
        this.docHandle = docHandle;
    }

    public CodecPage getPage(int pageNumber)
    {
        //return PdfPage.createPage(docHandle, pageNumber + 1);
        return PdfPage.createPage(core, pageNumber+1);
    }

    public int getPageCount()
    {
        //return getPageCount(docHandle);
        return core.countPages();
    }

    static PdfDocument openDocument(String fname, String pwd)
    {
        //return new PdfDocument(open(FITZMEMORY, fname, pwd));
        PdfDocument document= new PdfDocument(0);
        MuPDFCore core=null;
        System.out.println("Trying to open "+fname);
        try {
            core=new MuPDFCore(APVApplication.getInstance(), fname);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
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
    protected void finalize() throws Throwable
    {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        /*if (docHandle != 0) {
            free(docHandle);
            docHandle = 0;
        }*/
        if (null!=core){
            core.onDestroy();
        }
    }
}
