package org.vudroid.djvudroid;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.djvudroid.codec.DjvuContext;

public class DjvuViewerActivity extends BaseViewerActivity {
    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new DjvuContext());
    }
}
