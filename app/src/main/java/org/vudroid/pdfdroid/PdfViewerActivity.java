package org.vudroid.pdfdroid;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.vudroid.core.AKDecodeService;
import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.pdfdroid.codec.PdfContext;

import cn.archko.pdf.HistoryFragment;

public class PdfViewerActivity extends BaseViewerActivity
{
    @Override
    protected DecodeService createDecodeService()
    {
        return new AKDecodeService(new PdfContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(HistoryFragment.
                ACTION_STOPPED));
    }
}
