package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSFormFragment extends Fragment {

    public static final String TAG = TransactionVSFormFragment.class.getSimpleName();

    public enum Type {QR_FORM, TRANSACTIONVS_FORM}

    private Spinner currencySpinner;
    private EditText amount_text;
    private TextView tag_text, subject;
    private TagVSDto tagVS;
    private UserVSDto toUserVS;
    private Button add_tag_btn;
    private String broadCastId = TransactionVSFormFragment.class.getSimpleName();
    private CheckBox from_uservs_checkbox, currency_send_checkbox, currency_change_checkbox;
    private Type formType;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            final ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            TagVSDto tagVS = (TagVSDto) intent.getSerializableExtra(ContextVS.TAG_KEY);
            if(tagVS != null) setTagVS(tagVS);
        }
    };

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        formType = (Type) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        toUserVS = (UserVSDto) getArguments().getSerializable(ContextVS.USER_KEY);
        View rootView = inflater.inflate(R.layout.transactionvs_form_fragment, container, false);
        rootView.findViewById(R.id.request_button).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        generateQR();
                    }
                });
        subject = (EditText) rootView.findViewById(R.id.subject);
        amount_text = (EditText) rootView.findViewById(R.id.amount);
        from_uservs_checkbox = (CheckBox) rootView.findViewById(R.id.from_uservs_checkbox);
        from_uservs_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                    checkBoxSelected(buttonView, isChecked);
                }
            });
        currency_send_checkbox = (CheckBox) rootView.findViewById(R.id.currency_send_checkbox);
        currency_send_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                checkBoxSelected(buttonView, isChecked);
            }
        });
        currency_change_checkbox = (CheckBox) rootView.findViewById(R.id.currency_change_checkbox);
        currency_change_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                checkBoxSelected(buttonView, isChecked);
            }
        });
        Button request_button = (Button) rootView.findViewById(R.id.request_button);
        currencySpinner = (Spinner) rootView.findViewById(R.id.currency_spinner);
        add_tag_btn = (Button)rootView.findViewById(R.id.add_tag_btn);
        add_tag_btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if(tagVS == null) SelectTagVSDialogFragment.showDialog(broadCastId,
                        getFragmentManager(), SelectTagVSDialogFragment.TAG);
                else setTagVS(null);
            }
        });
        tag_text = (TextView)rootView.findViewById(R.id.tag_text);
        switch (formType) {
            case QR_FORM:
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_create_lbl));
                request_button.setText(getString(R.string.qr_create_lbl));
                break;
            case TRANSACTIONVS_FORM:
                if(toUserVS.getConnectedDevices() == null || toUserVS.getConnectedDevices().size() == 0) {
                    currency_change_checkbox.setVisibility(View.GONE);
                }
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.send_money_lbl));
                ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(toUserVS.getFullName());
                request_button.setText(getString(R.string.send_money_lbl));
                break;
        }

        if(savedInstanceState != null)
            setTagVS((TagVSDto) savedInstanceState.getSerializable(ContextVS.TAG_KEY));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return rootView;
    }

    private void checkBoxSelected(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            if(formType == Type.TRANSACTIONVS_FORM) {
                from_uservs_checkbox.setChecked(false);
                currency_send_checkbox.setChecked(false);
                currency_change_checkbox.setChecked(false);
            }
            if(buttonView == from_uservs_checkbox) from_uservs_checkbox.setChecked(true);
            if(buttonView == currency_send_checkbox) currency_send_checkbox.setChecked(true);
            if(buttonView == currency_change_checkbox) currency_change_checkbox.setChecked(true);
        }

    }

    private void setTagVS(TagVSDto tagVS) {
        this.tagVS = tagVS;
        if(tagVS != null) {
            add_tag_btn.setText(getString(R.string.change_lbl));
            tag_text.setText(getString(R.string.selected_tag_lbl,tagVS.getName()));
            tag_text.setVisibility(View.VISIBLE);
        } else {
            add_tag_btn.setText(getString(R.string.add_tag_lbl));
            tag_text.setVisibility(View.GONE);
        }
    }

    private void generateQR() {
        LOGD(TAG + ".generateQR", "generateQR");
        subject.setError(null);
        amount_text.setError(null);
        if(TextUtils.isEmpty(subject.getText().toString())){
            subject.setError(getString(R.string.subject_missing_msg));
            return ;
        }
        Integer selectedAmount = 0;
        try {
            selectedAmount = Integer.valueOf(amount_text.getText().toString());
        } catch (Exception ex) { LOGD(TAG + ".generateQR", "ERROR - amount_text:" + amount_text.getText());}
        if(selectedAmount <= 0) {
            amount_text.setError(getString(R.string.min_withdrawal_msg));
            return;
        }
        List<TransactionVSDto.Type> paymentOptions = new ArrayList<>();
        if(from_uservs_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.FROM_USERVS);
        if(currency_send_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.CURRENCY_SEND);
        if(currency_change_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.CURRENCY_CHANGE);
        if(tagVS == null) tagVS = new TagVSDto(TagVSDto.WILDTAG);
        switch (formType) {
            case TRANSACTIONVS_FORM:
                TransactionVSDto transactionDto = new TransactionVSDto();
                transactionDto.setAmount(new BigDecimal(amount_text.getText().toString()));
                transactionDto.setCurrencyCode(currencySpinner.getSelectedItem().toString());
                transactionDto.setTagVS(tagVS);
                transactionDto.setPaymentOptions(paymentOptions);
                transactionDto.setSubject(subject.getText().toString());
                transactionDto.setToUser(toUserVS.getFullName());
                transactionDto.setToUserIBAN(Utils.asSet(toUserVS.getIBAN()));
                Intent resultIntent = new Intent(getActivity(), FragmentContainerActivity.class);
                resultIntent.putExtra(ContextVS.FRAGMENT_KEY, PaymentFragment.class.getName());
                resultIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionDto);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                break;
            case QR_FORM:
                if(paymentOptions.size() == 0) {
                    MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                            getString(R.string.min_payment_option_msg), getFragmentManager());
                    return;
                }
                TransactionVSDto dto = TransactionVSDto.PAYMENT_REQUEST(
                        AppVS.getInstance().getUserVS().getFullName(), UserVSDto.Type.USER,
                        new BigDecimal(amount_text.getText().toString()),
                        currencySpinner.getSelectedItem().toString(),
                        AppVS.getInstance().getConnectedDevice().getIBAN(), subject.getText().toString(),
                        tagVS.getName());
                dto.setPaymentOptions(paymentOptions);
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.TRANSACTION_KEY, dto);
                intent.putExtra(ContextVS.FRAGMENT_KEY, QRFragment.class.getName());
                startActivity(intent);
                break;
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TAG_KEY, tagVS);
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