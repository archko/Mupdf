package org.vudroid.pdfdroid.codec;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.viewer.OutlineActivity;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

import java.util.ArrayList;

public class PdfDocument implements CodecDocument {
    Document core;
    private Outline[] outline;
    ArrayList<OutlineActivity.Item> items;

    public void setCore(Document core) {
        this.core = core;
    }

    public Document getCore() {
        return core;
    }

    public CodecPage getPage(int pageNumber) {
        return PdfPage.createPage(core, pageNumber);
    }

    public int getPageCount() {
        return core.countPages();
    }

    public static PdfDocument openDocument(String fname, String pwd) {
        //return new PdfDocument(open(FITZMEMORY, fname, pwd));
        PdfDocument document = new PdfDocument();
        Document core = null;
        System.out.println("Trying to open " + fname);
        try {
            core = Document.openDocument(fname);
            document.setCore(core);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return document;
    }

    public boolean hasOutline() {
        if (outline == null)
            outline = core.loadOutline();
        return outline != null;
    }

    public ArrayList<OutlineActivity.Item> getOutline() {
        if (null != items) {
            return items;
        } else {
            items = new ArrayList<>();
            flattenOutlineNodes(items, outline, "");
        }
        return items;
    }

    private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline list[], String indent) {
        for (Outline node : list) {
            if (node.title != null)
                result.add(new OutlineActivity.Item(indent + node.title, node.page));
            if (node.down != null)
                flattenOutlineNodes(result, node.down, indent + "    ");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (null != core) {
            core.destroy();
            outline = null;
            items = null;
        }
    }
}
