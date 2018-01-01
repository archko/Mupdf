package org.vudroid.pdfdroid;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.artifex.mini.OutlineActivity;

import org.vudroid.core.AKDecodeService;
import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.HistoryFragment;

public class PdfViewerActivity extends BaseViewerActivity {

    @Override
    protected DecodeService createDecodeService() {
        return new AKDecodeService(new PdfContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HistoryFragment.
                ACTION_STOPPED));
    }

    public void openOutline() {
        AKDecodeService service = (AKDecodeService) getDecodeService();
        PdfDocument document = (PdfDocument) service.getDocument();
        if (document.getCore().hasOutline() && null != document.getCore().getOutline()) {
            Intent intent = new Intent(this, OutlineActivity.class);
            intent.putExtra("cp", getDocumentView().getCurrentPage());
            intent.putExtra("POSITION", getDocumentView().getCurrentPage());
            intent.putExtra("OUTLINE", document.getCore().getOutline());
            startActivityForResult(intent, OUTLINE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    getDocumentView().goToPage(resultCode - RESULT_FIRST_USER);
                getPageSeekBarControls().hide();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
