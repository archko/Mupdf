package com.artifex.mupdfdemo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toolbar;

public class OutlineActivity extends Activity {
	OutlineItem mItems[];

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.outline_list);

		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
			//toolbar.setLogo(R.drawable.icon);
			setActionBar(toolbar);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			/*toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});*/
		} else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowHomeEnabled(true);
        } else {
            setTheme(R.style.AppFullscreen);
        }

		final ListView listView=(ListView) findViewById(R.id.list);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListItemClick((ListView) parent, view, position, id);
			}
		});
		mItems = OutlineActivityData.get().items;
		listView.setAdapter(new OutlineAdapter(getLayoutInflater(), mItems));
		// Restore the position within the list from last viewing

		//getListView().setDividerHeight(0);
		//listView.setCacheColorHint(getResources().getColor(android.R.color.transparent));
		setResult(-1);
		listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				listView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				setSelection(listView);
			}
		});
	}

	private void setSelection(ListView listView) {
		int pos = OutlineActivityData.get().position;
		if (null != getIntent()) {
            int cp = getIntent().getIntExtra("cp", -1);
            if (cp != -1) {
                pos = cp;
            }
        }
		OutlineItem[] items = OutlineActivityData.get().items;
		//System.out.println("pos:"+pos+" items:"+items.length);
		OutlineItem item;
		int idx=0;
		for (int i = items.length - 1; i >= 0; i--) {
            item = items[i];
            //System.out.println("pos page:"+item.page);
            if (item.page <= pos) {
                //pos = item.page;
                idx=i;
                break;
            }
        }

		//System.out.println("pos:"+pos+" idx:"+idx);
		if (idx < listView.getAdapter().getCount()) {
            listView.setSelection(idx);
        }
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		OutlineActivityData.get().position = l.getFirstVisiblePosition();
		setResult(mItems[position].page);
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId=item.getItemId();
		if (itemId==android.R.id.home) {
			finish();
		}

		return super.onOptionsItemSelected(item);
	}
}
