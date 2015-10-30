package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.activity.CertRequestActivity;
import org.votingsystem.activity.CertResponseActivity;
import org.votingsystem.activity.MessageActivity;
import org.votingsystem.android.R;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PinDialogFragment extends DialogFragment implements OnKeyListener {

    public static final String TAG = PinDialogFragment.class.getSimpleName();

    private static final String NEW_PIN_KEY = "NEW_PIN_KEY";
    private enum PinChangeStep {PIN_REQUEST, NEW_PIN_REQUEST, NEW_PIN_CONFIRM}

    private TypeVS typeVS;
    private TextView msgTextView;
    private EditText userPinEditText;
    private Boolean withPasswordConfirm = null;
    private Boolean withHashValidation = null;
    private String broadCastId = null;
    private String firstPin = null;
    private String newPin = null;
    private PinChangeStep pinChangeStep = PinChangeStep.PIN_REQUEST;


    public static void showPinScreen(FragmentManager fragmentManager, String broadCastId,
             String message, boolean isWithPasswordConfirm, TypeVS type) {
        boolean isWithCertValidation = true;
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, isWithPasswordConfirm, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, PinDialogFragment.TAG);
    }

    public static void showWalletScreen(FragmentManager fragmentManager, String broadCastId,
                 String message, boolean isWithPasswordConfirm, TypeVS type) {
        boolean isWithCertValidation = false;
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, isWithPasswordConfirm, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, PinDialogFragment.TAG);
    }

    public static void showChangePinScreen(FragmentManager fragmentManager) {
        boolean isWithCertValidation = true;
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                null, true, isWithCertValidation, null, TypeVS.PIN_CHANGE);
        pinDialog.show(fragmentManager, PinDialogFragment.TAG);
    }

    public static void showPinScreenWithoutCertValidation(FragmentManager fragmentManager,
            String broadCastId, String message, boolean isWithPasswordConfirm, TypeVS type) {
        boolean isWithCertValidation = false;
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, isWithPasswordConfirm, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, PinDialogFragment.TAG);
    }

    public static void showPinScreenWithoutHashValidation(FragmentManager fragmentManager,
              boolean isWithPasswordConfirm, String broadCastId, String msg, TypeVS typeVS) {
        PinDialogFragment pinDialog = new PinDialogFragment();
        pinDialog.setArguments(getArguments(msg, isWithPasswordConfirm, false, false, broadCastId, typeVS));
        pinDialog.show(fragmentManager, PinDialogFragment.TAG);
    }

    private static Bundle getArguments(String msg, boolean isWithPasswordConfirm,
           boolean isWithCertValidation, boolean isWithHashValidation, String broadCastId, TypeVS type) {
        Bundle result = new Bundle();
        result.putString(ContextVS.MESSAGE_KEY, msg);
        result.putString(ContextVS.CALLER_KEY, broadCastId);
        result.putBoolean(ContextVS.PASSWORD_CONFIRM_KEY, isWithPasswordConfirm);
        result.putBoolean(ContextVS.CERT_VALIDATION_KEY, isWithCertValidation);
        result.putBoolean(ContextVS.HASH_VALIDATION_KEY, isWithHashValidation);
        result.putSerializable(ContextVS.TYPEVS_KEY, type);
        return result;
    }

    public static PinDialogFragment newInstance(String msg, boolean isWithPasswordConfirm,
            boolean isWithCertValidation, String broadCastId, TypeVS type) {
        PinDialogFragment dialog = new PinDialogFragment();
        dialog.setArguments(getArguments(msg, isWithPasswordConfirm, isWithCertValidation, true,
                broadCastId, type));
        return dialog;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) firstPin = savedInstanceState.getString(ContextVS.PIN_KEY);
        this.setCancelable(false);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateDialog", "onCreateDialog");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AppVS appVS = (AppVS) getActivity().getApplicationContext();
        boolean isWithCertValidation = getArguments().getBoolean(ContextVS.CERT_VALIDATION_KEY);
        typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        AlertDialog.Builder builder = null;
        if(!ContextVS.State.WITH_CERTIFICATE.equals(appVS.getState()) && isWithCertValidation) {
            builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.cert_not_found_caption),
                    getString(R.string.cert_not_found_msg), getActivity()).setPositiveButton(
                    R.string.request_certificate_menu, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = null;
                    switch(AppVS.getInstance().getState()) {
                        case WITH_CSR:
                            intent = new Intent(getActivity(), CertResponseActivity.class);
                            break;
                        case WITHOUT_CSR:
                            intent = new Intent(getActivity(), CertRequestActivity.class);
                            break;
                    }
                    if(intent != null) startActivity(intent);
                }
            }).setNegativeButton(R.string.cancel_lbl, null);
            return builder.create();
        } else {
            View view = inflater.inflate(R.layout.pin_dialog, null);
            msgTextView = (TextView) view.findViewById(R.id.msg);
            userPinEditText = (EditText)view.findViewById(R.id.user_pin);
            builder = new AlertDialog.Builder(getActivity());
            if(savedInstanceState != null) {
                pinChangeStep = (PinChangeStep) savedInstanceState.getSerializable(
                        TypeVS.PIN_CHANGE.toString());
                newPin = savedInstanceState.getString(NEW_PIN_KEY);
            } else {
                if(getArguments().getString(ContextVS.MESSAGE_KEY) != null) {
                    msgTextView.setText(Html.fromHtml(getArguments().getString(ContextVS.MESSAGE_KEY)));
                }
                withPasswordConfirm = getArguments().getBoolean(ContextVS.PASSWORD_CONFIRM_KEY);
                withHashValidation = getArguments().getBoolean(ContextVS.HASH_VALIDATION_KEY);
                broadCastId = getArguments().getString(ContextVS.CALLER_KEY);
                builder.setView(view).setOnKeyListener(this);
            }
            if(TypeVS.PIN_CHANGE == typeVS) {
                ((TextView) view.findViewById(R.id.caption_text)).setText(R.string.pin_change_lbl);
                switch (pinChangeStep) {
                    case PIN_REQUEST:
                        msgTextView.setText(getString(R.string.enter_actual_pin_msg));
                        break;
                    case NEW_PIN_REQUEST:
                        msgTextView.setText(getString(R.string.enter_new_pin_msg));
                        break;
                    case NEW_PIN_CONFIRM:
                        msgTextView.setText(getString(R.string.confirm_new_pin_msg));
                        break;
                }
            }
        }
        Dialog dialog = builder.create();
        dialog.setOnKeyListener(this);
        return dialog;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.PIN_KEY, firstPin);
        outState.putSerializable(TypeVS.PIN_CHANGE.toString(), pinChangeStep);
        outState.putString(NEW_PIN_KEY, newPin);
    }

    private void setPin(final String pin) {
        if(typeVS == TypeVS.PIN_CHANGE) {
            switch(pinChangeStep) {
                case PIN_REQUEST:
                    if(isHashOK(pin)) {
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        msgTextView.setText(getString(R.string.enter_new_pin_msg));
                        userPinEditText.setText("");
                        return;
                    } else return;
                case NEW_PIN_REQUEST:
                    newPin = pin;
                    pinChangeStep =  PinChangeStep.NEW_PIN_CONFIRM;
                    msgTextView.setText(getString(R.string.confirm_new_pin_msg));
                    userPinEditText.setText("");
                    return;
                case NEW_PIN_CONFIRM:
                    if(pin.equals(newPin)) {
                        PrefUtils.putPin(pin.toCharArray());
                        userPinEditText.setVisibility(View.GONE);
                        msgTextView.setText(getString(R.string.new_pin_ok_msg));
                        return;
                    } else {
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        msgTextView.setText(getString(R.string.new_pin_error_msg));
                        userPinEditText.setText("");
                        return;
                    }
            }
        } else {
            if(withPasswordConfirm) {
                if(firstPin == null) {
                    firstPin = pin;
                    msgTextView.setText(getString(R.string.repeat_password));
                    userPinEditText.setText("");
                    return;
                } else {
                    if (!firstPin.equals(pin)) {
                        firstPin = null;
                        userPinEditText.setText("");
                        msgTextView.setText(getString(R.string.password_mismatch));
                        return;
                    }
                }
            }
        }
        if(withHashValidation && !isHashOK(pin)) return;
        try {
            InputMethodManager imm = (InputMethodManager)getActivity().
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), 0);
            if(broadCastId != null) {
                Intent intent = new Intent(broadCastId);
                intent.putExtra(ContextVS.PIN_KEY, ContextVS.PIN_KEY);
                ResponseVS responseVS = new ResponseVS(typeVS, pin.toCharArray());
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
            firstPin = null;
            getDialog().dismiss();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isHashOK(String pin) {
        try {
            String expectedHash = PrefUtils.getPinHash();
            String pinHash = StringUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!expectedHash.equals(pinHash)) {
                msgTextView.setText(getString(R.string.pin_error_msg));
                userPinEditText.setText("");
                return false;
            } else return true;
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        //OnKey is fire twice: the first time for key down, and the second time for key up,
        //so you have to filter:
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return true;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ((PinDialogFragment) getFragmentManager().
                    findFragmentByTag(PinDialogFragment.TAG)).dismiss();
        }
        //if (keyCode == KeyEvent.KEYCODE_DEL) { } 
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            LOGD(TAG + ".onKey", "KEYCODE_ENTER");
            String pin = userPinEditText.getText().toString();
            if(pin != null && pin.length() == 4) {
                setPin(pin);
            }
        }
        return false;//True if the listener has consumed the event, false otherwise.
    }

    @Override public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() != null && getActivity() instanceof MessageActivity) {
            getActivity().finish();
        }
    }
}