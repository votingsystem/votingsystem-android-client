package org.votingsystem.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.votingsystem.AppVS;
import org.votingsystem.activity.CurrencyRequesActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.voting.TagVSInfoDto;
import org.votingsystem.service.PaymentService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAccountsFragment extends Fragment {

	public static final String TAG = CurrencyAccountsFragment.class.getSimpleName();

    private static final int CURRENCY_REQUEST   = 1;

    private TransactionDto transaction;
    private View rootView;
    private String broadCastId = CurrencyAccountsFragment.class.getSimpleName();
    private AppVS appVS;
    private TextView last_request_date;
    private RecyclerView accounts_recycler_view;
    private String IBAN;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        switch(responseVS.getTypeVS()) {
            case CURRENCY_ACCOUNTS_INFO:
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    loadUserInfo(DateUtils.getCurrentWeekPeriod());
                }
                break;
            default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                    responseVS.getCaption(), responseVS.getNotificationMessage(),
                    getFragmentManager());
        }
        setProgressDialogVisible(false, null, null);
        }
    };

    public static Fragment newInstance(Long representativeId) {
        CurrencyAccountsFragment fragment = new CurrencyAccountsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(Bundle args) {
        CurrencyAccountsFragment fragment = new CurrencyAccountsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.currency_accounts, container, false);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        //https://developer.android.com/training/material/lists-cards.html
        accounts_recycler_view = (RecyclerView) rootView.findViewById(R.id.accounts_recycler_view);
        accounts_recycler_view.setHasFixedSize(true);
        accounts_recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));
        setHasOptionsMenu(true);
        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
        if(savedInstanceState != null) {
            transaction = (TransactionDto)savedInstanceState.getSerializable(ContextVS.TRANSACTION_KEY);
        } else {
            Intent intent = getActivity().getIntent();
            if(intent.getBooleanExtra(ContextVS.REFRESH_KEY, false)) updateCurrencyAccountsInfo();
        }
        return rootView;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if(Activity.RESULT_OK == resultCode) {
            loadUserInfo(DateUtils.getCurrentWeekPeriod());
        }
    }

    private void loadUserInfo(TimePeriod timePeriod) {
        Date lastCheckedTime = PrefUtils.getCurrencyAccountsLastCheckDate();
        if(lastCheckedTime == null) {
            updateCurrencyAccountsInfo();
            return;
        }
        try {
            last_request_date.setText(Html.fromHtml(getString(R.string.currency_last_request_info_lbl,
                    DateUtils.getDayWeekDateStr(lastCheckedTime, "HH:mm"))));
            BalancesDto userInfo = PrefUtils.getBalances();
            if(userInfo != null) {
                Map<String, TagVSInfoDto> tagVSBalancesMap = userInfo.getTagVSInfoMap(
                        Currency.getInstance("EUR").getCurrencyCode());
                if(tagVSBalancesMap != null) {
                    String[] tagVSArray = tagVSBalancesMap.keySet().toArray(new String[tagVSBalancesMap.keySet().size()]);
                    AccountVSInfoAdapter accountVSInfoAdapter = new AccountVSInfoAdapter(appVS,
                            tagVSBalancesMap, Currency.getInstance("EUR").getCurrencyCode(), tagVSArray);
                    accounts_recycler_view.setAdapter(accountVSInfoAdapter);
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.currency_accounts, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_currency_accounts:
                updateCurrencyAccountsInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void updateCurrencyAccountsInfo() {
        Toast.makeText(getActivity(), getString(R.string.fetching_user_accounts_info_msg),
                Toast.LENGTH_SHORT).show();
        Intent startIntent = new Intent(getActivity(), PaymentService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CURRENCY_ACCOUNTS_INFO);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.fetching_user_accounts_info_msg));
        getActivity().startService(startIntent);
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

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TRANSACTION_KEY, transaction);
    }

    public class AccountVSInfoAdapter extends RecyclerView.Adapter<AccountVSInfoAdapter.ViewHolder>{

        private final Context context;
        private Map<String, TagVSInfoDto> tagVSListBalances;
        private List<String> tagVSList;
        private String currencyCode;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View accountView;
            public ViewHolder(View accountView) {
                super(accountView);
                this.accountView = accountView;
            }
        }

        public AccountVSInfoAdapter(Context context, Map<String, TagVSInfoDto> tagVSListBalances,
                        String currencyCode, String[] tagArray) {
            this.context = context;
            this.currencyCode = currencyCode;
            this.tagVSListBalances = tagVSListBalances;
            tagVSList = new ArrayList<String>();
            tagVSList.addAll(tagVSListBalances.keySet());
        }

        @Override public AccountVSInfoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
            View accountView = ((LayoutInflater) context.getSystemService(Context.
                    LAYOUT_INFLATER_SERVICE)).inflate(R.layout.currency_account_card, parent, false);
            return new ViewHolder(accountView);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            View accountView = holder.accountView;
            TagVSInfoDto selectedTag = tagVSListBalances.get(tagVSList.get(position));
            final BigDecimal accountBalance = selectedTag.getCash();
            Button request_button = (Button) accountView.findViewById(R.id.cash_button);
            if(TagVSDto.WILDTAG.equals(selectedTag.getName())) {
                if(accountBalance.compareTo(BigDecimal.ZERO) == 1) {
                    request_button.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), CurrencyRequesActivity.class);
                            intent.putExtra(ContextVS.MAX_VALUE_KEY, accountBalance);
                            intent.putExtra(ContextVS.CURRENCY_KEY, currencyCode);
                            intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.cash_dialog_msg,
                                    accountBalance, currencyCode));
                            startActivityForResult(intent, CURRENCY_REQUEST);
                        }
                    });
                    request_button.setVisibility(View.VISIBLE);
                } else request_button.setVisibility(View.GONE);
            } else request_button.setVisibility(View.GONE);

            TextView tag_text = (TextView)accountView.findViewById(R.id.tag_text);
            String tag_text_msg = "'" + MsgUtils.getTagVSMessage(selectedTag.getName()) +
                    "' " + getString(R.string.currency_lbl) + " " + currencyCode;
            tag_text.setText(tag_text_msg);
            TextView cash_info = (TextView)accountView.findViewById(R.id.cash_info);
            cash_info.setText(Html.fromHtml(getString(R.string.account_amount_info_lbl,
                    accountBalance, currencyCode)));
            if(selectedTag.getTimeLimitedRemaining().compareTo(BigDecimal.ZERO) > 0) {
                String timeLimitedMsg = selectedTag.getTimeLimitedRemaining().toPlainString() +
                        " " + currencyCode + " " + getString(R.string.in_lbl) + " " +
                        selectedTag.getName();
                TextView time_limited_text = ((TextView)accountView.findViewById(R.id.time_limited_text));
                time_limited_text.setText(getString(R.string.time_remaining_info_lbl, timeLimitedMsg));
                time_limited_text.setVisibility(View.VISIBLE);
            }
        }

        @Override public int getItemCount() {
            return tagVSListBalances.size();
        }
    }

}