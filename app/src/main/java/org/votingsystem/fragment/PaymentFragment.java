package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.activity.CurrencyRequesActivity;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.service.PaymentService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import static org.votingsystem.util.ContextVS.CALLER_KEY;
import static org.votingsystem.util.ContextVS.PIN_KEY;
import static org.votingsystem.util.ContextVS.TYPEVS_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PaymentFragment extends Fragment {

	public static final String TAG = PaymentFragment.class.getSimpleName();

    private static final int RC_OPEN_WALLET       = 0;
    private static final int RC_CURRENCY_REQUEST = 1;
    private static final int RC_SEND_TRANSACTION  = 2;

    private String broadCastId = PaymentFragment.class.getSimpleName();
    private TextView receptor;
    private TextView subject;
    private TextView amount;
    private TextView tagvs;
    private TransactionVSDto transactionDto;
    private Spinner payment_method_spinner;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            String pin = intent.getStringExtra(PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            setProgressDialogVisible(false);
            String caption = ResponseVS.SC_OK == responseVS.getStatusCode()? getString(
                    R.string.payment_ok_caption):getString(R.string.error_lbl);
            getActivity().finish();
            UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, responseVS.getMessage(), caption);
        }
    };

    private void launchTransactionVS(TransactionVSDto.Type transactionType) {
        LOGD(TAG + ".launchTransactionVS() ", "launchTransactionVS");
        Intent startIntent = new Intent(getActivity(), PaymentService.class);
        try {
            transactionDto.setType(transactionType);
            transactionDto.setOperation(TypeVS.valueOf(transactionType.toString()));
            startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionDto);
            startIntent.putExtra(CALLER_KEY, broadCastId);
            startIntent.putExtra(TYPEVS_KEY, transactionDto.getOperation());
            getActivity().startService(startIntent);
            setProgressDialogVisible(true);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.payment_fragment, container, false);
        getActivity().setTitle(getString(R.string.payment_lbl));
        receptor = (TextView)rootView.findViewById(R.id.receptor);
        subject = (TextView)rootView.findViewById(R.id.subject);
        amount= (TextView)rootView.findViewById(R.id.amount);
        tagvs= (TextView)rootView.findViewById(R.id.tagvs);
        payment_method_spinner = (Spinner)rootView.findViewById(R.id.payment_method_spinner);
        try {
            transactionDto = (TransactionVSDto) getArguments().getSerializable(ContextVS.TRANSACTION_KEY);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.payment_spinner_item,
                    TransactionVSDto.getPaymentMethods(transactionDto.getPaymentOptions()));
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            payment_method_spinner.setAdapter(dataAdapter);
            receptor.setText(transactionDto.getToUser());
            subject.setText(transactionDto.getSubject());
            amount.setText(transactionDto.getAmount().toString() + " " + transactionDto.getCurrencyCode());
            String tagvsInfo = getString(R.string.selected_tag_lbl,
                    MsgUtils.getTagVSMessage(transactionDto.getTagName()));
            if(transactionDto.isTimeLimited()) tagvsInfo = tagvsInfo + " " +
                    getString(R.string.time_remaining_tagvs_info_lbl);
            tagvs.setText(tagvsInfo);
            if(transactionDto.getOperation() != null) {
                switch(transactionDto.getOperation()) {
                    case DELIVERY_WITH_PAYMENT:
                    case DELIVERY_WITHOUT_PAYMENT:
                        UIUtils.fillAddressInfo((LinearLayout)rootView.findViewById(R.id.address_info),
                                getActivity());
                        break;
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
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

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
        try {
            transactionDto.setType(TransactionVSDto.getByDescription(
                    (String) payment_method_spinner.getSelectedItem()));
            BalancesDto userInfo = PrefUtils.getBalances();
            final BigDecimal availableForTagVS = userInfo.getAvailableForTagVS(
                    transactionDto.getCurrencyCode(), transactionDto.getTagVS().getName());
            switch (transactionDto.getType()) {
                case FROM_USERVS:
                    try {
                        if(availableForTagVS.compareTo(transactionDto.getAmount()) < 0) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_cash_msg,
                                            transactionDto.getCurrencyCode(),
                                            transactionDto.getAmount().toString(),
                                            availableForTagVS.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.check_available_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(ContextVS.REFRESH_KEY, true);
                                            intent.putExtra(ContextVS.FRAGMENT_KEY,
                                                    CurrencyAccountsFragment.class.getName());
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                            return;
                        } else {
                            Utils.getCryptoDeviceAccessModePassword(RC_SEND_TRANSACTION,
                                    MsgUtils.getTransactionVSConfirmMessage(transactionDto, getActivity()),
                                    null, (AppCompatActivity)getActivity());
                        }
                    } catch(Exception ex) { ex.printStackTrace();}
                    break;
                case CURRENCY_CHANGE:
                case CURRENCY_SEND:
                    if(Wallet.getCurrencySet() == null) {
                        Utils.getCryptoDeviceAccessModePassword(RC_OPEN_WALLET,
                                getString(R.string.enter_wallet_password_msg),
                                null, (AppCompatActivity)getActivity());
                        return;
                    }
                    final BigDecimal availableForTagVSWallet = Wallet.getAvailableForTagVS(
                            transactionDto.getCurrencyCode(), transactionDto.getTagVS().getName());
                    if(availableForTagVSWallet.compareTo(transactionDto.getAmount()) < 0) {
                        final BigDecimal amountToRequest = transactionDto.getAmount().subtract(
                                availableForTagVSWallet);
                        if(availableForTagVSWallet.compareTo(amountToRequest) < 0) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_anonymous_money_msg,
                                            transactionDto.getCurrencyCode(),
                                            availableForTagVSWallet.toString(),
                                            amountToRequest.toString(),
                                            availableForTagVSWallet.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.check_available_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(ContextVS.REFRESH_KEY, true);
                                            intent.putExtra(ContextVS.FRAGMENT_KEY,
                                                    CurrencyAccountsFragment.class.getName());
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                            return;
                        } else {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_anonymous_cash_msg,
                                            transactionDto.getCurrencyCode(),
                                            transactionDto.getAmount().toString(),
                                            availableForTagVSWallet.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.request_cash_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    CurrencyRequesActivity.class);
                                            intent.putExtra(ContextVS.MAX_VALUE_KEY,
                                                    availableForTagVS);
                                            intent.putExtra(ContextVS.DEFAULT_VALUE_KEY,
                                                    amountToRequest);
                                            intent.putExtra(ContextVS.CURRENCY_KEY,
                                                    transactionDto.getCurrencyCode());
                                            intent.putExtra(ContextVS.MESSAGE_KEY,
                                                    getString(R.string.cash_for_payment_dialog_msg,
                                                    transactionDto.getCurrencyCode(),
                                                    amountToRequest.toString(),
                                                    availableForTagVS.toString()));
                                            startActivityForResult(intent, RC_CURRENCY_REQUEST);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                        }
                    } else {
                        launchTransactionVS(transactionDto.getType());
                    }
                    break;
            }
        } catch(Exception ex) { ex.printStackTrace();}
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.sending_payment_lbl),
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_OPEN_WALLET:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    try {
                        Set<Currency> currencySet = Wallet.getCurrencySet(
                                new String(responseVS.getMessageBytes()).toCharArray());
                        submitForm();
                    } catch(Exception ex) { ex.printStackTrace(); }
                }
                break;
            case RC_CURRENCY_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    submitForm();
                }
                break;
            case RC_SEND_TRANSACTION:
                if(Activity.RESULT_OK == resultCode) {
                    launchTransactionVS(transactionDto.getType());
                }
                break;
        }

    }

}