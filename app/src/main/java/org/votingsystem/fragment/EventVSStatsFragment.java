package org.votingsystem.fragment;

import android.database.Cursor;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.EventVSContentProvider;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSStatsFragment extends Fragment {
	
	public static final String TAG = EventVSStatsFragment.class.getSimpleName();

    private View rootView;
    private EventVSDto eventVS;
    private AppVS appVS;

    public static EventVSStatsFragment newInstance(Long eventId) {
        EventVSStatsFragment fragment = new EventVSStatsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.ITEM_ID_KEY, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        Long eventId =  getArguments().getLong(ContextVS.ITEM_ID_KEY);
        appVS = (AppVS) getActivity().getApplicationContext();
        Cursor cursor = getActivity().getContentResolver().query(
                EventVSContentProvider.getEventURI(eventId), null, null, null, null);
        cursor.moveToFirst();
        String eventJSONData = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        LOGD(TAG + ".onCreateView", "eventJSONData: " + eventJSONData);
        try {
            eventVS = JSON.readValue(eventJSONData, EventVSDto.class);
            eventVS.setAccessControlVS(appVS.getAccessControl());
        } catch(Exception ex) { ex.printStackTrace(); }
        rootView = inflater.inflate(R.layout.eventvs_stats, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated", "");
        super.onActivityCreated(savedInstanceState);
        loadUrl(eventVS.getStatsServiceURL());
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOGD(TAG + ".onSaveInstanceState", "");
    }

    private void loadUrl(String serverURL) {
    	LOGD(TAG + ".loadUrl", " - serverURL: " + serverURL);
        WebView webview = (WebView) rootView.findViewById(R.id.webview);
        WebSettings webSettings = webview.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webview.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webview.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                setProgressDialogVisible(false);
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();//for SSL self-signed certs
            }
        });
        webview.loadUrl(serverURL);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        }  else ProgressDialogFragment.hide(getFragmentManager());
    }

}