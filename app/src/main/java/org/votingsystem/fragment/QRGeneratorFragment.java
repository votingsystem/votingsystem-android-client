package org.votingsystem.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRGeneratorFragment extends Fragment {

    public static final String TAG = QRGeneratorFragment.class.getSimpleName();

    private Spinner currencySpinner;
    private EditText amount_text;
    private TextView tag_text, subject;
    private TagVSDto tagVS;
    private Button add_tag_btn;
    private String broadCastId = QRGeneratorFragment.class.getSimpleName();
    private CheckBox from_uservs_checkbox, currency_send_checkbox, currency_change_checkbox;

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
        View rootView = inflater.inflate(R.layout.qr_generator_fragment, container, false);
        rootView.findViewById(R.id.request_button).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        generateQR();
                    }
                });
        subject = (EditText) rootView.findViewById(R.id.subject);
        amount_text = (EditText) rootView.findViewById(R.id.amount);
        from_uservs_checkbox = (CheckBox) rootView.findViewById(R.id.from_uservs_checkbox);
        currency_send_checkbox = (CheckBox) rootView.findViewById(R.id.currency_send_checkbox);
        currency_change_checkbox = (CheckBox) rootView.findViewById(R.id.currency_change_checkbox);
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
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_create_lbl));
        if(savedInstanceState != null)
            setTagVS((TagVSDto) savedInstanceState.getSerializable(ContextVS.TAG_KEY));
        return rootView;
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
        if(TextUtils.isEmpty(subject.getText().toString())){
            MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    getString(R.string.subject_missing_msg), getFragmentManager());
            return ;
        }
        Integer selectedAmount = 0;
        try {
            selectedAmount = Integer.valueOf(amount_text.getText().toString());
        } catch (Exception ex) { LOGD(TAG + ".generateQR", "ERROR - amount_text:" + amount_text.getText());}
        if(selectedAmount <= 0) {
            MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    getString(R.string.min_withdrawal_msg), getFragmentManager());
            return;
        }
        List<TransactionVSDto.Type> paymentOptions = new ArrayList<>();
        if(from_uservs_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.FROM_USERVS);
        if(currency_send_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.CURRENCY_SEND);
        if(currency_change_checkbox.isChecked()) paymentOptions.add(TransactionVSDto.Type.CURRENCY_CHANGE);
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
                (tagVS == null ? TagVSDto.WILDTAG : tagVS.getName()));
        dto.setPaymentOptions(paymentOptions);

        Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
        intent.putExtra(ContextVS.TRANSACTION_KEY, dto);
        intent.putExtra(ContextVS.FRAGMENT_KEY, QRFragment.class.getName());
        startActivity(intent);
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