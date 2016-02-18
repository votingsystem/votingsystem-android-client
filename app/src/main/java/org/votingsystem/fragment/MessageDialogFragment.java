package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageDialogFragment extends DialogFragment {

    public static final String TAG = MessageDialogFragment.class.getSimpleName();


    public static void showDialog(Integer statusCode, String caption, String message,
            FragmentManager fragmentManager) {
        hide(fragmentManager);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        if(fragmentManager == null) {
            LOGD(TAG, "fragmentManager null");
            return;
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(newFragment, MessageDialogFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public static void showDialog(String caption, String message, FragmentManager fragmentManager) {
        hide(fragmentManager);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(ResponseVS.SC_OK,
                caption, message);
        if(fragmentManager == null) {
            LOGD(TAG, "fragmentManager null");
            return;
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(newFragment, MessageDialogFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public static void showDialog(ResponseVS responseVS,  FragmentManager fragmentManager) {
        showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                responseVS.getNotificationMessage(), fragmentManager);
    }

    public static void hide(FragmentManager fragmentManager) {
        if(fragmentManager != null && fragmentManager.findFragmentByTag(
                MessageDialogFragment.TAG) != null) {
            ((MessageDialogFragment) fragmentManager
                    .findFragmentByTag(MessageDialogFragment.TAG)).dismiss();
        }
    }

    public static MessageDialogFragment newInstance(Integer statusCode, String caption,
                    String message){
        MessageDialogFragment fragment = new MessageDialogFragment();
        Bundle args = new Bundle();
        if(statusCode != null) args.putInt(ContextVS.RESPONSE_STATUS_KEY, statusCode);
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_dialog, null);
        int statusCode = getArguments().getInt(ContextVS.RESPONSE_STATUS_KEY, -1);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        String message = getArguments().getString(ContextVS.MESSAGE_KEY);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity()).setPositiveButton(
            getString(R.string.accept_lbl),  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    MessageDialogFragment.this.dismiss();
                }
            });
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        TextView captionTextView = (TextView) view.findViewById(R.id.caption_text);
        if(caption != null) captionTextView.setText(caption);
        else {
            captionTextView.setVisibility(View.GONE);
            view.findViewById(R.id.separator).setVisibility(View.GONE);
        }
        if(message != null) messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog dialog = builder.create();
        dialog.setView(view);
        if(statusCode > 0) {
            if(ResponseVS.SC_OK == statusCode) dialog.setIcon(R.drawable.fa_check_32);
            else dialog.setIcon(R.drawable.fa_times_32);
        }
        this.setCancelable(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ((MessageDialogFragment) getFragmentManager().
                            findFragmentByTag(MessageDialogFragment.TAG)).dismiss();
                    return true;
                } else return false;
            }
        });
        return dialog;
    }

}
