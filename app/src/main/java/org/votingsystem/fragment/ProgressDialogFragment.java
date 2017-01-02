package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.util.Constants;

import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ProgressDialogFragment extends DialogFragment {

    public static final String TAG = ProgressDialogFragment.class.getSimpleName();

    private TextView progress_text;
    private String progressMessage = null;
    private String caption = null;
    private String dialogTag = null;

    public static ProgressDialogFragment showDialog(String caption, String progressMessage,
                                                    FragmentManager fragmentManager) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        if (fragmentManager == null) {
            LOGD(TAG, "fragmentManager null");
            return dialog;
        }
        Bundle args = new Bundle();
        args.putString(Constants.MESSAGE_KEY, progressMessage);
        args.putString(Constants.CAPTION_KEY, caption);
        dialog.setArguments(args);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(dialog, ProgressDialogFragment.TAG);
        ft.commitAllowingStateLoss();
        return dialog;
    }

    public static ProgressDialogFragment showDialog(String caption, String progressMessage,
                                                    String dialogTag, FragmentManager fragmentManager) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        if (fragmentManager == null) {
            LOGD(TAG, "fragmentManager null");
            return dialog;
        }
        Bundle args = new Bundle();
        args.putString(Constants.MESSAGE_KEY, progressMessage);
        args.putString(Constants.CAPTION_KEY, caption);
        args.putString(Constants.TAG_KEY, dialogTag);
        dialog.setArguments(args);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(dialog, ProgressDialogFragment.TAG + dialogTag);
        ft.commitAllowingStateLoss();
        return dialog;
    }

    public static void hide(String dialogTag, FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            LOGD(TAG, "fragmentManager null");
            return;
        }
        if (fragmentManager != null && fragmentManager.findFragmentByTag(
                ProgressDialogFragment.TAG + dialogTag) != null) {
            ((ProgressDialogFragment) fragmentManager.
                    findFragmentByTag(ProgressDialogFragment.TAG + dialogTag)).dismiss();
        } else LOGD(TAG + ".hide", TAG + " not found");
    }

    public static void hide(FragmentManager fragmentManager) {
        try {
            if (fragmentManager == null) {
                LOGD(TAG, "fragmentManager null");
                return;
            }
            List<Fragment> fragmentList = fragmentManager.getFragments();
            if (fragmentList != null) {
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ProgressDialogFragment)
                        ((ProgressDialogFragment) fragment).dismiss();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        progressMessage = getArguments().getString(Constants.MESSAGE_KEY);
        caption = getArguments().getString(Constants.CAPTION_KEY);
        dialogTag = getArguments().getString(Constants.TAG_KEY);
        if (dialogTag == null) dialogTag = ProgressDialogFragment.TAG;
        else dialogTag = ProgressDialogFragment.TAG + dialogTag;
        View view = getActivity().getLayoutInflater().inflate(R.layout.progress_dialog, null);
        progress_text = (TextView) view.findViewById(R.id.progress_text);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        progress_text.setText(progressMessage);
        this.setCancelable(false);
        Dialog dialog = new AlertDialog.Builder(getActivity()).setView(view).create();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ((ProgressDialogFragment) getFragmentManager().
                            findFragmentByTag(dialogTag)).dismiss();
                    return true;
                } else return false;
            }
        });
        return dialog;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
        progress_text.setText(progressMessage);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.MESSAGE_KEY, progressMessage);
        outState.putString(Constants.CAPTION_KEY, caption);
    }

}