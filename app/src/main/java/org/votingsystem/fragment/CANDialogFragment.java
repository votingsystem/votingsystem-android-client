package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CANDialogFragment extends DialogFragment implements DialogInterface.OnKeyListener {

    public static final String TAG = CANDialogFragment.class.getSimpleName();

    private String broadCastId = null;
    private EditText canEditText;

    public static CANDialogFragment showDialog(String broadCastId,
            FragmentManager fragmentManager) {
        CANDialogFragment dialog = new CANDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CALLER_KEY, broadCastId);
        dialog.setArguments(args);
        dialog.show(fragmentManager, CANDialogFragment.TAG);
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        broadCastId = getArguments().getString(ContextVS.CALLER_KEY);
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.can_dialog, null);
        canEditText = (EditText)view.findViewById(R.id.can);
        canEditText.setText(PrefUtils.getDNIeCAN());
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        SpannableStringBuilder aboutBody = new SpannableStringBuilder();
        aboutBody.append(Html.fromHtml(getString(R.string.can_dialog_body)));
        messageTextView.setText(aboutBody);
        messageTextView.setMovementMethod(new LinkMovementMethod());
        this.setCancelable(false);
        Dialog dialog = new AlertDialog.Builder(getActivity()).setView(view).setPositiveButton(R.string.accept_lbl,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setCAN(canEditText.getText().toString().trim());
                    }
                }
        ).create();
        dialog.setOnKeyListener(this);
        return dialog;
    }

    private void setCAN(String can) {
        if(can != null && TextUtils.isEmpty(can)) can = null;
        Intent intent = new Intent(broadCastId);
        intent.putExtra(ContextVS.CAN_KEY, can);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        getDialog().dismiss();
    }

    @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        //OnKey is fire twice: the first time for key down, and the second time for key up,
        //so you have to filter:
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return true;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setCAN(null);
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            LOGD(TAG + ".onKey", "KEYCODE_ENTER");
            setCAN(canEditText.getText().toString().trim());
        }
        return false;//True if the listener has consumed the event, false otherwise.
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.CALLER_KEY, broadCastId);
    }

}