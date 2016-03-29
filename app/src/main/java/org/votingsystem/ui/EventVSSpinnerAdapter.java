package org.votingsystem.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.votingsystem.android.R;

import java.util.ArrayList;

/** Adapter that provides views for our top-level Action Bar spinner. */
public class EventVSSpinnerAdapter  extends BaseAdapter {

    private class EventVSSpinnerItem {
        boolean isHeader;
        String tag, title;
        int color;
        boolean indented;

        EventVSSpinnerItem(boolean isHeader, String tag, String title, boolean indented, int color) {
            this.isHeader = isHeader;
            this.tag = tag;
            this.title = title;
            this.indented = indented;
            this.color = color;
        }
    }

    private boolean mTopLevel;
    private LayoutInflater inflater;

    public EventVSSpinnerAdapter(boolean topLevel, LayoutInflater inflater) {
        this.mTopLevel = topLevel;
        this.inflater = inflater;
    }

    // pairs of (tag, title)
    private ArrayList<EventVSSpinnerItem> mItems = new ArrayList<EventVSSpinnerItem>();

    public void clear() {
        mItems.clear();
    }

    public void addItem(String tag, String title, boolean indented, int color) {
        mItems.add(new EventVSSpinnerItem(false, tag, title, indented, color));
    }

    public void addHeader(String title) {
        mItems.add(new EventVSSpinnerItem(true, "", title, false, 0));
    }

    @Override public int getCount() {
        return mItems.size();
    }

    @Override public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    private boolean isHeader(int position) {
        return position >= 0 && position < mItems.size()
                && mItems.get(position).isHeader;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
            view = inflater.inflate(R.layout.spinner_eventvs_item_dropdown,
                    parent, false);
            view.setTag("DROPDOWN");
        }

        TextView headerTextView = (TextView) view.findViewById(R.id.header_text);
        View dividerView = view.findViewById(R.id.divider_view);
        TextView normalTextView = (TextView) view.findViewById(R.id.normal_text);

        if (isHeader(position)) {
            headerTextView.setText(getTitle(position));
            headerTextView.setVisibility(View.VISIBLE);
            normalTextView.setVisibility(View.GONE);
            dividerView.setVisibility(View.VISIBLE);
        } else {
            headerTextView.setVisibility(View.GONE);
            normalTextView.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.GONE);
            normalTextView.setText(getTitle(position));
        }
        return view;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
            view = inflater.inflate(mTopLevel
                            ? R.layout.spinner_eventvs_item_actionbar
                            : R.layout.spinner_eventvs_item,
                    parent, false);
            view.setTag("NON_DROPDOWN");
        }
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));
        return view;
    }

    private String getTitle(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).title : "";
    }

    private int getColor(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).color : R.color.bkg_vs;
    }

    private String getTag(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position).tag : "";
    }

    @Override
    public boolean isEnabled(int position) {
        return !isHeader(position);
    }

    @Override public int getItemViewType(int position) {
        return 0;
    }

    @Override  public int getViewTypeCount() {
        return 1;
    }

    @Override public boolean areAllItemsEnabled() {
        return false;
    }
}
