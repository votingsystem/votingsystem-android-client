package org.votingsystem.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.activity.CurrencyActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

public class WalletFragment extends Fragment {

    public static final String TAG = WalletFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private CurrencyListAdapter adapter = null;
    private List<Currency> currencyList;
    private String broadCastId = WalletFragment.class.getSimpleName();
    private Menu menu;
    private boolean walletLoaded = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case CURRENCY:
                    try {
                        currencyList = new ArrayList<>(Wallet.getCurrencySet((String) responseVS.getData()));
                        Utils.launchCurrencyStatusCheck(broadCastId, null);
                        printSummary();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                    }
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case CURRENCY_CHECK:
                    currencyList = new ArrayList<>(Wallet.getCurrencySet());
                    printSummary();
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), responseVS.getMessage(),
                                getFragmentManager());
                    }
                    break;
                case CURRENCY_ACCOUNTS_INFO:
                    break;
            }
        }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.wallet_lbl));
        rootView = inflater.inflate(R.layout.wallet_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(getActivity(), CurrencyActivity.class);
                intent.putExtra(ContextVS.CURRENCY_KEY, currencyList.get(position));
                startActivity(intent);
            }
        });
        if(Wallet.getCurrencySet() == null) {
            PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                    getString(R.string.enter_wallet_pin_msg), false, TypeVS.CURRENCY);
            currencyList = new ArrayList<>();
        } else {
            currencyList = new ArrayList<>(Wallet.getCurrencySet());
            printSummary();
        }
        setHasOptionsMenu(true);
        return rootView;
    }

    private void printSummary() {
        adapter = new CurrencyListAdapter(currencyList, getActivity());
        gridView.setAdapter(adapter);
        adapter.setItemList(currencyList);
        adapter.notifyDataSetChanged();
        if(menu != null) menu.removeItem(R.id.open_wallet);
        Map<String, Map<String, IncomesDto>> currencyMap = Wallet.getCurrencyTagMap();
        ((LinearLayout)rootView.findViewById(R.id.summary)).removeAllViews();
        for(String currency : currencyMap.keySet()) {
            LinearLayout currencyData = (LinearLayout) getActivity().getLayoutInflater().inflate(
                    R.layout.wallet_currency_summary, null);
            Map<String, IncomesDto> tagInfoMap = currencyMap.get(currency);
            for(String tag : tagInfoMap.keySet()) {
                String contentFormatted = getString(R.string.tag_info,
                        (tagInfoMap.get(tag).getTotal()).toPlainString(), currency,
                        MsgUtils.getTagVSMessage(tag));
                if((tagInfoMap.get(tag).getTimeLimited()).compareTo(BigDecimal.ZERO) > 0) {
                        contentFormatted = contentFormatted + " - " + getString(R.string.tag_info_time_limited,
                        (tagInfoMap.get(tag).getTimeLimited()).toPlainString(), currency);
                }
                TextView tagDataTextView = new TextView(getActivity());
                tagDataTextView.setGravity(Gravity.CENTER);
                tagDataTextView.setText(Html.fromHtml(contentFormatted));
                ((LinearLayout)currencyData.findViewById(R.id.tag_info)).addView(tagDataTextView);
            }
            ((LinearLayout)rootView.findViewById(R.id.summary)).addView(currencyData);
        }
        walletLoaded = true;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if(!walletLoaded) menuInflater.inflate(R.menu.wallet, menu);
        this.menu = menu;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_wallet:
                PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                        getString(R.string.enter_wallet_pin_msg), false, TypeVS.CURRENCY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void isProgressDialogVisible(boolean isVisible) {
        if(isVisible) ProgressDialogFragment.showDialog(
                getString(R.string.unlocking_wallet_msg), getString(R.string.wait_msg), getFragmentManager());
        else ProgressDialogFragment.hide(getFragmentManager());
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    public class CurrencyListAdapter  extends ArrayAdapter<Currency> {

        private List<Currency> itemList;
        private Context context;

        public CurrencyListAdapter(List<Currency> itemList, Context ctx) {
            super(ctx, R.layout.currency_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public Currency getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        @Override public View getView(int position, View view, ViewGroup parent) {
            Currency currency = itemList.get(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.currency_card, null);
            }
            //Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
            //Calendar weekLapseCalendar = Calendar.getInstance();
            //weekLapseCalendar.setTime(weekLapse);
            LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            linearLayout.setBackgroundColor(Color.WHITE);
            TextView date_data = (TextView)view.findViewById(R.id.date_data);
            date_data.setText(DateUtils.getDayWeekDateSimpleStr(currency.getDateFrom()));

            TextView currency_state = (TextView) view.findViewById(R.id.currency_state);
            currency_state.setText(currency.getStateMsg(getActivity()));
            currency_state.setTextColor(currency.getStateColor(getActivity()));

            TextView amount = (TextView) view.findViewById(R.id.amount);
            amount.setText(currency.getAmount().toPlainString());
            amount.setTextColor(currency.getStateColor(getActivity()));
            TextView currencyTextView = (TextView) view.findViewById(R.id.currencyCode);
            currencyTextView.setText(currency.getCurrencyCode().toString());
            currencyTextView.setTextColor(currency.getStateColor(getActivity()));

            if(DateUtils.getCurrentWeekPeriod().inRange(currency.getDateTo())) {
                TextView time_limited_msg = (TextView) view.findViewById(R.id.time_limited_msg);
                time_limited_msg.setText(getString(R.string.lapse_lbl,
                        DateUtils.getDayWeekDateStr(currency.getDateTo())));
            }
            ((TextView) view.findViewById(R.id.tag_data)).setText(MsgUtils.getTagVSMessage(
                    currency.getTag()));
            return view;
        }

        public List<Currency> getItemList() {
            return itemList;
        }

        public void setItemList(List<Currency> itemList) {
            this.itemList = itemList;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

}