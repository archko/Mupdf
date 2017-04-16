package com.artifex.mupdf.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.PDFDocument;
import com.artifex.mupdf.fitz.PDFObject;
import com.artifex.mupdf.fitz.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DocActivityView extends FrameLayout implements TabHost.OnTabChangeListener, View.OnClickListener, DocView.SelectionChangeListener
{
	/*
	This class displays a DocView, and optionally, a DocListPagesView to its right.
	Both views show the document content, but the DocListPagesView is narrow,
	and always shows one column.

	There is logic here, and in Docview, DocViewBase and DocListPagesView,
	to keep them both in sync, as follows:

	When the DocListPagesView is showing, and the user taps on one of its pages,
	that page is highlighted, and the DocView on the left is auto-scrolled to bring that page into view.
	When the user scrolls the DocListPagesView, nothing more is done.

	When the user scrolls the DocView on the left, a "most visible" page is determined, and the
	DocListPagesView is auto-scrolled to bring that page into view, and that page is
	highlighted.
	*/

	private DocView mDocView;
	private DocListPagesView mDocPagesView;

	private DocReflowView mDocReflowView;

	private boolean mShowUI = true;

	//  tab tags
	private String mTagHidden;
	private String mTagFile;
	private String mTagAnnotate;
	private String mTagPages;

	private Button mReflowButton;
	private Button mFirstPageButton;
	private Button mLastPageButton;

	private ImageButton mSearchButton;
	private EditText mSearchText;
	private Button mSearchNextButton;
	private Button mSearchPreviousButton;
	private ImageButton mBackButton;

	private Button mSaveButton;
	private Button mSaveAsButton;
	private Button mPrintButton;
	private Button mShareButton;
	private Button mOpenInButton;

	private Button mToggleAnnotButton;
	private Button mHighlightButton;
	private Button mDeleteButton;

	private Button mNoteButton;
	private Button mDrawButton;
	private Button mLineColorButton;
	private Button mLineThicknessButton;

	private Button mProofButton;

	private String mEmbeddedProfile = null;

	private Context mContext;

	public DocActivityView(Context context)
	{
		super(context);
		mContext = context;
	}

	public DocActivityView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
	}

	public DocActivityView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mContext = context;
	}

	protected boolean usePagesView()
	{
		return true;
	}

	protected void setupTabs()
	{
		TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();

		//  get the tab tags.
		mTagHidden = getResources().getString(R.string.hidden_tab);
		mTagFile = getResources().getString(R.string.file_tab);
		mTagAnnotate = getResources().getString(R.string.annotate_tab);
		mTagPages = getResources().getString(R.string.pages_tab);

		//  first tab is and stays hidden.
		//  when the search tab is selected, we programmatically "select" this hidden tab
		//  which results in NO tabs appearing selected in this tab host.
		setupTab(tabHost, mTagHidden, R.id.hiddenTab, R.layout.tab);
		tabHost.getTabWidget().getChildTabViewAt(0).setVisibility(View.GONE);

		//  these tabs are shown.
		setupTab(tabHost, mTagFile, R.id.fileTab, R.layout.tab_left);
		setupTab(tabHost, mTagAnnotate, R.id.annotateTab, R.layout.tab);
		setupTab(tabHost, mTagPages, R.id.pagesTab, R.layout.tab_right);

		//  start by showing the edit tab
		tabHost.setCurrentTabByTag(mTagFile);

		tabHost.setOnTabChangedListener(this);
	}

	protected void setupTab(TabHost tabHost, String text, int viewId, int tabId)
	{
		View tabview = LayoutInflater.from(tabHost.getContext()).inflate(tabId, null);
		TextView tv = (TextView) tabview.findViewById(R.id.tabText);
		tv.setText(text);

		TabHost.TabSpec tab = tabHost.newTabSpec(text);
		tab.setIndicator(tabview);
		tab.setContent(viewId);
		tabHost.addTab(tab);
	}

	@Override
	public void onTabChanged(String tabId)
	{
		//  hide the search tab
		findViewById(R.id.searchTab).setVisibility(View.GONE);

		//  show search is not selected
		showSearchSelected(false);

		//  show/hide the pages view
		handlePagesTab(tabId);

		hideKeyboard();
	}

	private void showSearchSelected(boolean selected)
	{
		//  set the view
		mSearchButton.setSelected(selected);

		//  colorize
		if (selected)
			mSearchButton.setColorFilter(0xff000000, PorterDuff.Mode.SRC_IN);
		else
			mSearchButton.setColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN);
	}

	protected void handlePagesTab(String tabId)
	{
		if (tabId.equals(mTagPages))
			showPages();
		else
		{
			hideReflow();
			hidePages();
		}
	}

	protected void showPages()
	{
		LinearLayout pages = (LinearLayout) findViewById(R.id.pages_container);
		if (null == pages)
			return;

		if (pages.getVisibility() == View.VISIBLE)
			return;

		pages.setVisibility(View.VISIBLE);

		final ViewTreeObserver observer = mDocView.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				observer.removeOnGlobalLayoutListener(this);
				mDocView.onShowPages();
			}
		});

		final ViewTreeObserver observer2 = getViewTreeObserver();
		observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				observer2.removeOnGlobalLayoutListener(this);
				mDocPagesView.onOrientationChange();
				int page = mDocView.getMostVisiblePage();
				mDocPagesView.setCurrentPage(page);
				mDocPagesView.scrollToPage(page);
			}
		});
	}

	protected void hidePages()
	{
		LinearLayout pages = (LinearLayout) findViewById(R.id.pages_container);
		if (null == pages)
			return;

		if (pages.getVisibility() == View.GONE)
			return;

		pages.setVisibility(View.GONE);
		ViewTreeObserver observer = mDocView.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				mDocView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				mDocView.onHidePages();
			}
		});
	}

	//  called from the main DocView whenever the pages list needs to be updated.
	public void setCurrentPage(int pageNumber)
	{
		if (usePagesView())
		{
			//  set the new current page and scroll there.
			mDocPagesView.setCurrentPage(pageNumber);
			mDocPagesView.scrollToPage(pageNumber);
		}
	}

	public boolean showKeyboard()
	{
		//  show keyboard
		InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		im.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

		return true;
	}

	public void hideKeyboard()
	{
		//  hide the keyboard
		InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		im.hideSoftInputFromWindow(mDocView.getWindowToken(), 0);
	}

	private boolean started = false;
	public void start(final String path)
	{
		started = false;

		((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

		//  inflate the UI
		final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.doc_view, null);

		final ViewTreeObserver vto = getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				if (!started)
				{
					findViewById(R.id.tabhost).setVisibility(mShowUI?View.VISIBLE:View.GONE);
					findViewById(R.id.footer).setVisibility(mShowUI?View.VISIBLE:View.GONE);
					afterFirstLayoutComplete(path);

					started = true;
				}
			}
		});

		addView(view);
	}

	public void afterFirstLayoutComplete(final String path)
	{
		//  main view
		mDocView = (DocView) findViewById(R.id.doc_view_inner);
		mDocView.setHost(this);
		mDocReflowView = (DocReflowView) findViewById(R.id.doc_reflow_view);

		//  page list
		if (usePagesView())
		{
			mDocPagesView = new DocListPagesView(getContext());
			mDocPagesView.setSelectionListener(new DocListPagesView.SelectionListener()
			{
				@Override
				public void onPageSelected(int pageNumber)
				{
					mDocView.scrollToPage(pageNumber);
				}
			});

			LinearLayout layout2 = (LinearLayout) findViewById(R.id.pages_container);
			layout2.addView(mDocPagesView);
		}

		//  tabs
		setupTabs();

		//  selection handles
		View v = findViewById(R.id.doc_wrapper);
		RelativeLayout layout = (RelativeLayout) v;
		mDocView.setupHandles(layout);

		//  watch for a possible orientation change
		ViewTreeObserver observer3 = getViewTreeObserver();
		observer3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				onPossibleOrientationChange();
			}
		});

		//  connect buttons to functions

		mBackButton = (ImageButton)findViewById(R.id.back_button);
		mBackButton.setOnClickListener(this);

		mReflowButton = (Button)findViewById(R.id.reflow_button);
		mReflowButton.setOnClickListener(this);

		mFirstPageButton = (Button)findViewById(R.id.first_page_button);
		mFirstPageButton.setOnClickListener(this);

		mLastPageButton = (Button)findViewById(R.id.last_page_button);
		mLastPageButton.setOnClickListener(this);

		mSearchButton = (ImageButton)findViewById(R.id.search_button);
		mSearchButton.setOnClickListener(this);
		showSearchSelected(false);
		mSearchText = (EditText) findViewById(R.id.search_text_input);
		mSearchText.setOnClickListener(this);

		mSaveButton = (Button)findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(this);

		mSaveAsButton = (Button)findViewById(R.id.save_as_button);
		mSaveAsButton.setOnClickListener(this);

		mPrintButton = (Button)findViewById(R.id.print_button);
		mPrintButton.setOnClickListener(this);

		mShareButton = (Button)findViewById(R.id.share_button);
		mShareButton.setOnClickListener(this);

		mOpenInButton = (Button)findViewById(R.id.open_in_button);
		mOpenInButton.setOnClickListener(this);

		mProofButton = (Button)findViewById(R.id.proof_button);
		mProofButton.setOnClickListener(this);

		//  this listener will
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_NEXT)
				{
					onSearchNextButton();
					return true;
				}
				return false;
			}
		});

		mSearchNextButton = (Button)findViewById(R.id.search_next_button);
		mSearchNextButton.setOnClickListener(this);

		mSearchPreviousButton = (Button)findViewById(R.id.search_previous_button);
		mSearchPreviousButton.setOnClickListener(this);

		mToggleAnnotButton = (Button)findViewById(R.id.show_annot_button);
		mToggleAnnotButton.setOnClickListener(this);

		mHighlightButton = (Button)findViewById(R.id.highlight_button);
		mHighlightButton.setOnClickListener(this);

		mNoteButton = (Button)findViewById(R.id.note_button);
		mNoteButton.setOnClickListener(this);

		mDrawButton = (Button)findViewById(R.id.draw_button);
		mDrawButton.setOnClickListener(this);

		mLineColorButton = (Button)findViewById(R.id.line_color_button);
		mLineColorButton.setOnClickListener(this);

		mLineThicknessButton = (Button)findViewById(R.id.line_thickness_button);
		mLineThicknessButton.setOnClickListener(this);

		mDeleteButton = (Button)findViewById(R.id.delete_button);
		mDeleteButton.setOnClickListener(this);

		mDoc = new Document(path);

		if (mDoc.needsPassword())
		{
			askForPassword();
		}
		else
		{
			afterPassword();
		}
	}

	private static final int ORIENTATION_PORTAIT = 1;
	private static final int ORIENTATION_LANDSCAPE = 2;
	private int mLastOrientation = 0;
	private void onPossibleOrientationChange()
	{
		//  get current orientation
		Point p = Utilities.getRealScreenSize((Activity)mContext);
		int orientation = ORIENTATION_PORTAIT;
		if (p.x>p.y)
			orientation = ORIENTATION_LANDSCAPE;

		//  see if it's changed
		if (orientation != mLastOrientation)
			onOrientationChange();

		mLastOrientation = orientation;
	}

	private void onOrientationChange()
	{
		mDocView.onOrientationChange();

		if (usePagesView()) {
			mDocPagesView.onOrientationChange();
		}
	}

	private Document mDoc;

	private void askForPassword()
	{
		Utilities.passwordDialog((Activity) getContext(), new Utilities.passwordDialogListener()
		{
			@Override
			public void onOK(String password)
			{
				//  yes
				boolean ok = mDoc.authenticatePassword(password);
				if (ok)
				{
					afterPassword();
					mDocView.requestLayout();
				}
				else
				{
					askForPassword();
				}
			}

			@Override
			public void onCancel()
			{
				mDoc.destroy();
				if (mDoneListener != null)
					mDoneListener.done();
			}
		});
	}

	private void afterPassword()
	{
		//  start the views
		mDocView.start(mDoc);
		if (usePagesView())
		{
			mDocPagesView.clone(mDocView);
		}

		mHighlightButton.setEnabled(false);
		mDocView.setSelectionChangeListener(this);
		onSelectionChanged();
	}

	public void showUI(boolean show)
	{
		mShowUI = show;
	}

	public void stop()
	{
		mDocView.finish();
		if (usePagesView())
		{
			mDocPagesView.finish();
		}
	}

	private void onOutline(final Outline[] outline, int level)
	{
		if (outline == null)
			return;

		for (Outline entry : outline)
		{
			int numberOfSpaces = (level) * 4;
			String spaces = "";
			if (numberOfSpaces > 0)
				spaces = String.format("%" + numberOfSpaces + "s", " ");
			Log.i("example", String.format("%d %s %s %s", entry.page + 1, spaces, entry.title, entry.uri));
			if (entry.down != null)
			{
				//  branch
				onOutline(entry.down, level + 1);
			}
		}
	}

	private void onLinks()
	{
		int numPages = mDocView.getPageCount();
		for (int pageNum = 0; pageNum < numPages; pageNum++)
		{
			DocPageView cv = (DocPageView) mDocView.getOrCreateChild(pageNum);

			Link links[] = cv.getPage().getLinks();
			if (links != null)
			{

				for (int i = 0; i < links.length; i++)
				{
					Link link = links[i];

					Log.i("example", String.format("links for page %d:", pageNum));
					Log.i("example", String.format("     link %d:", i));
					Log.i("example", String.format("          page = %d", link.page));
					Log.i("example", String.format("          uri = %s", link.uri));
					Log.i("example", String.format("          bounds = %f %f %f %f ",
							link.bounds.x0, link.bounds.y0, link.bounds.x1, link.bounds.y1));
				}
			}
			else
			{
				Log.i("example", String.format("no links for page %d", pageNum));
			}

		}
	}

	@Override
	public void onClick(View v)
	{
		if (v == mReflowButton)
			onReflowButton();

		if (v == mFirstPageButton)
			onFirstPageButton();
		if (v == mLastPageButton)
			onLastPageButton();

		if (v == mSearchButton)
			onShowSearch();
		if (v == mSearchText)
			onEditSearchText();
		if (v == mSearchNextButton)
			onSearchNextButton();
		if (v == mSearchPreviousButton)
			onSearchPreviousButton();

		if (v == mBackButton)
			onBackButton();

		if (v == mSaveButton)
			onSaveButton();
		if (v == mSaveAsButton)
			onSaveAsButton();
		if (v == mPrintButton)
			onPrintButton();
		if (v == mShareButton)
			onShareButton();
		if (v == mOpenInButton)
			onOpenInButton();

		if (v == mToggleAnnotButton)
			onToggleAnnotButton();
		if (v == mHighlightButton)
			onHighlightButton();
		if (v == mDeleteButton)
			onDeleteButton();

		if (v == mNoteButton)
			onNoteButton();
		if (v == mDrawButton)
			onDrawButton();
		if (v == mLineColorButton)
			onLineColorButton();
		if (v == mLineThicknessButton)
			onLineThicknessButton();

		if (v == mProofButton)
			onProof();

	}

	public void onSearchNextButton()
	{
		hideKeyboard();
		mDocView.onSearchNext(mSearchText.getText().toString());
	}

	public void onSearchPreviousButton()
	{
		hideKeyboard();
		mDocView.onSearchPrevious(mSearchText.getText().toString());
	}

	public void onEditSearchText()
	{
		mSearchText.requestFocus();
		showKeyboard();
	}

	public void onShowSearch()
	{
		//  "deselect" all the visible tabs by selecting the hidden (first) one
		TabHost tabHost = (TabHost)findViewById(R.id.tabhost);
		tabHost.setCurrentTabByTag("HIDDEN");

		//  show search as selected
		showSearchSelected(true);

		//  hide all the other tabs
		hideAllTabs();

		//  show the search tab
		findViewById(R.id.searchTab).setVisibility(View.VISIBLE);
		mSearchText.getText().clear();
	}

	protected void hideAllTabs()
	{
		//  hide all the other tabs
		findViewById(R.id.fileTab).setVisibility(View.GONE);
		findViewById(R.id.annotateTab).setVisibility(View.GONE);
		findViewById(R.id.pagesTab).setVisibility(View.GONE);
	}

	private void onFirstPageButton()
	{
		goToPage(0);
	}

	private void onLastPageButton()
	{
		int npages = mDocView.getPageCount();
		goToPage(npages-1);
	}

	private void goToPage(int pageNumber)
	{
		mDocView.scrollToPage(pageNumber);

		if (mDocReflowView.getVisibility() == View.VISIBLE)
		{
			setReflowText(pageNumber);
			mDocPagesView.setCurrentPage(pageNumber);
		}
	}

	private void onReflowButton()
	{
		if (mDocView.getVisibility() == View.VISIBLE)
		{
			//  set initial text into reflow view
			setReflowText(mDocPagesView.getMostVisiblePage());

			//  show reflow
			showReflow();
		}
		else
		{
			//  hide reflow
			hideReflow();
		}
	}

	private void showReflow()
	{
		mDocView.setVisibility(View.GONE);
		mDocReflowView.setVisibility(View.VISIBLE);
	}

	private void hideReflow()
	{
		mDocReflowView.setVisibility(View.GONE);
		mDocView.setVisibility(View.VISIBLE);
	}

	private void setReflowText(int pageNumber)
	{
		DocPageView dpv = (DocPageView)mDocView.getAdapter().getView(pageNumber, null, null);
		byte bytes[] = dpv.getPage().textAsHtml();
		mDocReflowView.setHTML(bytes);
	}

	private void onBackButton()
	{
		if (mDoneListener != null)
			mDoneListener.done();
	}

	private void onSaveButton()
	{
		Toast.makeText(getContext(),"onSaveButton", Toast.LENGTH_SHORT).show();
	}

	private void onSaveAsButton()
	{
		Toast.makeText(getContext(),"onSaveAsButton", Toast.LENGTH_SHORT).show();
	}

	private void onPrintButton()
	{
		Toast.makeText(getContext(),"onPrintButton", Toast.LENGTH_SHORT).show();
	}

	private void onShareButton()
	{
		Toast.makeText(getContext(),"onShareButton", Toast.LENGTH_SHORT).show();
	}

	private void onOpenInButton()
	{
		Toast.makeText(getContext(),"onOpenInButton", Toast.LENGTH_SHORT).show();
	}

	private void onToggleAnnotButton()
	{
		mDocView.toggleAnnotations();
	}

	private void onHighlightButton()
	{
		mDocView.onHighlight();
	}

	private void onNoteButton()
	{
		mDocView.onNoteMode();
	}

	private void onDrawButton()
	{
		mDocView.onDrawMode();
	}

	private void onLineColorButton()
	{
		if (mDocView.getDrawMode() || mDocView.hasInkAnnotationSelected())
		{
			ColorDialog dlg = new ColorDialog(ColorDialog.BG_COLORS,
					getContext(), mLineColorButton, new ColorDialog.ColorChangedListener()
			{
				@Override
				public void onColorChanged(String color)
				{
					int icolor = Color.parseColor(color);
					mDocView.setInkLineColor(icolor);
					Drawable drawables[] = mLineColorButton.getCompoundDrawables();
					drawables[1].setColorFilter(icolor, PorterDuff.Mode.SRC_IN);
				}
			}, true);
			dlg.setShowTitle(false);
			dlg.show();
		}
	}

	private void onLineThicknessButton()
	{
		if (mDocView.getDrawMode() || mDocView.hasInkAnnotationSelected())
		{
			float val = mDocView.getInkLineThickness();
			LineWidthDialog.show(getContext(), mLineThicknessButton, val,
					new LineWidthDialog.WidthChangedListener()
					{
						@Override
						public void onWidthChanged(float value)
						{
							mDocView.setInkLineThickness(value);
						}
					});
		}
	}

	private String getEmbeddedProfileName()
	{
		PDFObject outputIntents = mDoc.toPDFDocument().getTrailer().get("Root").get("OutputIntents");
		if (outputIntents == null)
			return null;

		int length = outputIntents.size();
		int i;

		for (i = 0 ; i < length; i++) {
			PDFObject intent = outputIntents.get(i);

			String name = intent.get("S").asName();
			if (!name.equals("GTS_PDFX"))
				continue;

			/* We can't use the embedded profile if it's not CMYK based. */
			if (intent.get("DestOutputProfile").get("N").asInteger() != 4)
				continue;

			PDFObject id = intent.get("Info");
			if (id.isString())
				return id.asString();
			id = intent.get("OutputConditionIdentifier");
			if (id.isString())
				return id.asString();
			id = intent.get("OutputCondition");
			if (id.isString())
				return id.asString();
		}
		return null;
	}

	private void onProof()
	{
		proofSetup();
		if (!proofSupported())
		{
			Utilities.showMessage((Activity)getContext(), "gprf not supported", "gprf not supported");
			return;
		}

		//  show a dialog to collect the resolution and profiles
		final Activity activity = (Activity)getContext();

		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.proof_dialog);

		final Spinner sp1 = (Spinner)(dialog.findViewById(R.id.print_profile_spinner));
		final Spinner sp2 = (Spinner)(dialog.findViewById(R.id.display_profile_spinner));
		final Spinner sp3 = (Spinner)(dialog.findViewById(R.id.resolution_spinner));

		mEmbeddedProfile = getEmbeddedProfileName();
		if (mEmbeddedProfile!=null && !mEmbeddedProfile.isEmpty())
		{
			//  if the doc has an embedded profile, add it to the beginning of the list of print profiles.
			String[] baseList = getResources().getStringArray(R.array.proof_print_profiles);
			ArrayList<String> list = new ArrayList<String>(Arrays.asList(baseList));
			list.add(0, "Output Intent: " + mEmbeddedProfile);
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, list);
			spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp1.setAdapter(spinnerAdapter);
		}

		dialog.findViewById(R.id.cancel_button).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//  Cancel
				dialog.dismiss();

				//  remember the display profile selected
				SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putInt("displayProfileSelected", sp2.getSelectedItemPosition());
				editor.commit();
			}
		});

		dialog.findViewById(R.id.ok_button).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//  OK
				dialog.dismiss();

				//  remember the display profile selected
				SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putInt("displayProfileSelected", sp2.getSelectedItemPosition());
				editor.commit();

				doProof(sp1.getSelectedItemPosition(), sp2.getSelectedItemPosition(), sp3.getSelectedItemPosition());

			}
		});

		//  choose the last-selected display profile.
		SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
		int selected = sharedPref.getInt("displayProfileSelected", 0);
		sp2.setSelection(selected);

		dialog.show();
	}

	private static boolean proofSetupDone = false;
	private static boolean proofCodeSupported = false;
	private static boolean proofGsLibLoaded = false;
	private static void proofSetup()
	{
		if (proofSetupDone)
			return;

		proofCodeSupported = (com.artifex.mupdf.fitz.Context.gprfSupportedNative()==1);

		if (proofCodeSupported)
		{
			try
			{
				System.loadLibrary("gs");
				proofGsLibLoaded = true;
			}
			catch (UnsatisfiedLinkError e)
			{
			}
		}

		proofSetupDone = true;
	}

	private static boolean proofSupported()
	{
		return (proofCodeSupported && proofGsLibLoaded);
	}

	private void doProof(int printProfileIndex, int displayProfileIndex, int resolutionIndex)
	{
		//  get the resolution
		String[] resolutions = getResources().getStringArray(R.array.proof_resolutions);
		String resolutionString = resolutions[resolutionIndex];
		int resolution = Integer.parseInt(resolutionString);

		//  get the print profile
		String printProfilePath;
		String[] printProfiles = getResources().getStringArray(R.array.proof_print_profile_files);
		if (mEmbeddedProfile!=null && !mEmbeddedProfile.isEmpty())
		{
			if (printProfileIndex==0)
			{
				printProfilePath = "<EMBEDDED>";
			}
			else
			{
				printProfilePath   = extractProfileAsset("profiles/CMYK/" + printProfiles[printProfileIndex-1]);
			}
		}
		else
		{
			printProfilePath   = extractProfileAsset("profiles/CMYK/" + printProfiles[printProfileIndex]);
		}

		//  get the display profile
		String[] displayProfiles = getResources().getStringArray(R.array.proof_display_profile_files);
		String displayProfileFile = displayProfiles[displayProfileIndex];
		String displayProfilePath = extractProfileAsset("profiles/RGB/"  + displayProfileFile);

		//  what page are we doing?
		int thePage = mDocView.getMostVisiblePage();

		String proofFile = mDocView.getDoc().makeProof(mDocView.getDoc().getPath(), printProfilePath, displayProfilePath, resolution);

		Uri uri = Uri.parse("file://" + proofFile);
		Intent intent = new Intent(getContext(), ProofActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(uri);
		// add the current page so it can be found when the activity is running
		intent.putExtra("startingPage", thePage);
		(getContext()).startActivity(intent);
	}

	private String extractProfileAsset(String profile)
	{
		try
		{
			InputStream inStream = getContext().getAssets().open(profile);
			String tempfile = getContext().getExternalCacheDir() + "/shared/" + profile;
			new File(tempfile).mkdirs();
			Utilities.deleteFile(tempfile);

			FileOutputStream outStream = new FileOutputStream(tempfile);
			byte[] buffer = new byte[4096]; // To hold file contents
			int bytes_read; // How many bytes in buffer

			// Read a chunk of bytes into the buffer, then write them out,
			// looping until we reach the end of the file (when read() returns
			// -1). Note the combination of assignment and comparison in this
			// while loop. This is a common I/O programming idiom.
			while ((bytes_read = inStream.read(buffer)) != -1)
				// Read until EOF
				outStream.write(buffer, 0, bytes_read); // write

			return tempfile;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return "";
	}

	private void onDeleteButton()
	{
		mDocView.onDelete();
	}

	private DocPageView.NoteAnnotation lastNote = null;

	public void onSelectionChanged()
	{
		boolean hasSel = mDocView.hasSelection();
		boolean hasInkAnnotSel = mDocView.hasInkAnnotationSelected();
		boolean hasAnnotSel = mDocView.hasAnnotationSelected();
		boolean hasNoteAnnotSel = mDocView.hasNoteAnnotationSelected();

		mHighlightButton.setEnabled(hasSel);

		boolean noteMode = mDocView.getNoteMode();
		mNoteButton.setSelected(noteMode);
		findViewById(R.id.note_holder).setSelected(noteMode);

		boolean drawMode = mDocView.getDrawMode();
		mDrawButton.setSelected(drawMode);
		mLineColorButton.setEnabled(drawMode || hasInkAnnotSel);
		mLineThicknessButton.setEnabled(drawMode || hasInkAnnotSel);
		mDeleteButton.setEnabled(!drawMode && hasAnnotSel);

		findViewById(R.id.draw_tools_holder).setSelected(drawMode);

		View v = findViewById(R.id.doc_note_editor);
		mDocView.moveNoteEditor(v);

		DocPageView.NoteAnnotation note = mDocView.getSelectedNoteAnnotation();
		if (note != lastNote)
		{
			TextView tv = (TextView)findViewById(R.id.doc_note_editor_date);
			EditText te = (EditText)findViewById(R.id.doc_note_editor_text);

			//  save the old one
			if (lastNote != null)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm");
				String date = sdf.format(new Date());
				lastNote.setDate(date);
				lastNote.setText(te.getText().toString());
			}

			if (note != null)
			{
				tv.setText(note.getDate());
				te.setText(note.getText());
			}

			lastNote = note;
		}
	}

	private OnDoneListener mDoneListener = null;
	public void setOnDoneListener(OnDoneListener l) {mDoneListener = l;}
	public interface OnDoneListener
	{
		public void done();
	}
}
