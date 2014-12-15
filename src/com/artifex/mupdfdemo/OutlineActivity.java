package com.artifex.mupdfdemo;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ListView;

public class OutlineActivity extends ListActivity {
	OutlineItem mItems[];

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mItems = OutlineActivityData.get().items;
		setListAdapter(new OutlineAdapter(getLayoutInflater(),mItems));
		// Restore the position within the list from last viewing

		//getListView().setDividerHeight(0);
		getListView().setCacheColorHint(getResources().getColor(android.R.color.transparent));
		setResult(-1);
		getListView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
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
				if (idx < getListView().getAdapter().getCount()) {
					getListView().setSelection(idx);
				}
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		OutlineActivityData.get().position = getListView().getFirstVisiblePosition();
		setResult(mItems[position].page);
		finish();
	}
}
