package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.votingsystem.android.R;
import org.votingsystem.dto.CertRequestDto;
import org.votingsystem.service.UserCertRequestService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.UIUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.util.ContextVS.CALLER_KEY;
import static org.votingsystem.util.ContextVS.DTO_KEY;
import static org.votingsystem.util.ContextVS.PIN_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertRequestFormFragment extends Fragment {

	public static final String TAG = CertRequestFormFragment.class.getSimpleName();

    private String broadCastId = CertRequestFormFragment.class.getSimpleName();
    private View progressView;
    private View formView;



    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String givenname = null;
    private String surname = null;
    private String email = null;
    private String nif = null;
    private String phone = null;
    private String deviceId = null;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    private EditText phoneText;
    private EditText mailText;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            if(ResponseVS.SC_CANCELED == responseVS.getStatusCode()) return;
            launchUserCertRequestService((char[]) responseVS.getData());
        } else {
            showProgress(false, true);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.operation_ok_msg),
                        getString(R.string.request_cert_verification_msg),
                        getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                CertRequestFormFragment.this.getActivity().finish();
                            }
                        });
                UIUtils.showMessageDialog(builder);
            } else showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                    responseVS.getMessage());
        }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.cert_request_form, container, false);
        progressView = rootView.findViewById(R.id.progress_view);
        formView = rootView.findViewById(R.id.form_view);

        getActivity().setTitle(getString(R.string.request_certificate_form_lbl));
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        givennameText = (EditText)rootView.findViewById(R.id.given_name_edit);
        surnameText = (EditText)rootView.findViewById(R.id.surname_edit);
        mailText = (EditText)rootView.findViewById(R.id.mail_edit);
        phoneText = (EditText)rootView.findViewById(R.id.phone_edit);
        nifText = (EditText)rootView.findViewById(R.id.nif_edit);
        nifText.setOnEditorActionListener(new OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }});

        nifText.setOnKeyListener(new OnKeyListener() {
            // android:imeOptions="actionDone" doesn't work
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                //LOGD(TAG + ".onKey", " - keyCode: " + keyCode);
                if (event != null && keyCode == KeyEvent.KEYCODE_ENTER) {
                    submitForm();
                    return true;
                } else return false;
            }
        });
        Button requestButton = (Button) rootView.findViewById(R.id.request_button);
        requestButton.setOnClickListener(new OnClickListener() {
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
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
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
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(nifText.getWindowToken(), 0);
      	if (validateForm ()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.request_certificate_form_lbl),
                    getString(R.string.cert_data_confirm_msg, givenname,
                            surname, nif, phone, email), getActivity());
            builder.setPositiveButton(getString(R.string.continue_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            PinDialogFragment.showPinScreenWithoutHashValidation(getFragmentManager(), true,
                                    broadCastId, getString(
                                    R.string.pin_for_new_cert_msg), null);
                        }
                    }).setNegativeButton(getString(R.string.cancel_lbl), null);
            UIUtils.showMessageDialog(builder);
      	}
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private boolean validateForm () {
        givennameText.setError(null);
        surnameText.setError(null);
        nifText.setError(null);
        phoneText.setError(null);
        mailText.setError(null);
    	if(TextUtils.isEmpty(givennameText.getText().toString())){
            givennameText.setError(getString(R.string.givenname_missing_msg));
    		return false;
    	} else {
            givenname = StringUtils.normalize(givennameText.getText().toString()).toUpperCase();
        }
    	if(TextUtils.isEmpty(surnameText.getText().toString())){
            surnameText.setError(getString(R.string.surname_missing_msg));
    		return false;
    	} else {
            surname = StringUtils.normalize(surnameText.getText().toString()).toUpperCase();
        }
        try {
            nif = NifUtils.validate(nifText.getText().toString().toUpperCase(), getActivity());
        } catch(Exception ex) {
            nifText.setError(ex.getMessage());
            return false;
        }
        if(TextUtils.isEmpty(phoneText.getText().toString())){
            phoneText.setError(getString(R.string.phone_missing_msg));
            return false;
        } else {
            phone = phoneText.getText().toString();
        }
        if(TextUtils.isEmpty(mailText.getText().toString())){
            mailText.setError(getString(R.string.mail_missing_msg));
            return false;
        } else {
            email =  StringUtils.normalize(mailText.getText().toString()).toUpperCase();
        }
    	deviceId = PrefUtils.getApplicationId();
		LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
    	return true;
    }

    private void launchUserCertRequestService(char[] pin) {
        LOGD(TAG + ".launchUserCertRequestService() ", "launchUserCertRequestService");
        CertRequestDto dto = new CertRequestDto();
        dto.setDeviceId(deviceId);
        dto.setPhone(phone);
        dto.setGivenName(givenname);
        dto.setSurname(surname);
        dto.setNif(nif);
        dto.setEmail(email);
        Intent startIntent = new Intent(getActivity(), UserCertRequestService.class);
        startIntent.putExtra(PIN_KEY, pin);
        try {
            startIntent.putExtra(DTO_KEY, JSON.writeValueAsString(dto));
        } catch (IOException e) {  e.printStackTrace();  }
        startIntent.putExtra(CALLER_KEY, broadCastId);
        getActivity().startService(startIntent);
        showProgress(true, true);
    }

    public void showProgress(boolean show, boolean animate) {
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        formView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

}