package org.votingsystem.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.model.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyFragment extends Fragment {

    public static final String TAG = CurrencyFragment.class.getSimpleName();

    private AppVS appVS;
    private Currency currency;
    private TextView currency_amount, currency_state, currency_currency, date_info, hash_cert;


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.currency, container, false);
        currency_amount = (TextView)rootView.findViewById(R.id.currency_amount);
        currency_state = (TextView)rootView.findViewById(R.id.currency_state);
        currency_currency = (TextView)rootView.findViewById(R.id.currency_currency);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        hash_cert = (TextView)rootView.findViewById(R.id.hash_cert);
        if(getArguments() != null && getArguments().containsKey(ContextVS.CURRENCY_KEY)) {
            currency = (Currency) getArguments().getSerializable(ContextVS.CURRENCY_KEY);
            initCurrencyScreen(currency);
        }
        return rootView;
    }

    public void initCurrencyScreen(Currency currency) {
        try {
            hash_cert.setText(currency.getHashCertVS());
            currency_amount.setText(currency.getAmount().toPlainString());
            currency_currency.setText(currency.getCurrencyCode());
            getActivity().setTitle(MsgUtils.getCurrencyDescriptionMessage(currency, getActivity()));
            date_info.setText(getString(R.string.currency_date_info,
                    DateUtils.getDateStr(currency.getDateFrom(), "dd MMM yyyy' 'HH:mm"),
                    DateUtils.getDateStr(currency.getDateTo(), "dd MMM yyyy' 'HH:mm")));
            if(currency.getState() != null && Currency.State.OK != currency.getState()) {
                currency_state.setText(MsgUtils.getCurrencyStateMessage(currency, getActivity()));
                currency_state.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}