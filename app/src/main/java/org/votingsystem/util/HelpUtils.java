package org.votingsystem.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import org.votingsystem.android.R;


/**
 * This is a set of helper methods for showing contextual help information in the app.
 */
public class HelpUtils {
    public static void showAbout(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_about");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        new AboutDialog().show(ft, "dialog_about");
    }

    public static class AboutDialog extends DialogFragment {

        private static final String VERSION_UNAVAILABLE = "N/A";

        public AboutDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            PackageManager pm = getActivity().getPackageManager();
            String packageName = getActivity().getPackageName();
            String versionName;
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                versionName = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = VERSION_UNAVAILABLE;
            }
            SpannableStringBuilder aboutBody = new SpannableStringBuilder();
            aboutBody.append(Html.fromHtml(getString(R.string.about_body, versionName)));
            SpannableString licensesLink = new SpannableString(getString(R.string.about_licenses));
            licensesLink.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    HelpUtils.showOpenSourceLicenses(getActivity());
                }
            }, 0, licensesLink.length(), 0);
            aboutBody.append("\n\n");
            aboutBody.append(licensesLink);
            SpannableString eulaLink = new SpannableString(getString(R.string.about_eula));
            eulaLink.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    HelpUtils.showEula(getActivity());
                }
            }, 0, eulaLink.length(), 0);
            aboutBody.append("\n\n");
            aboutBody.append(eulaLink);
            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.message_dialog, null);
            TextView messageTextView = (TextView) view.findViewById(R.id.message);
            ((TextView) view.findViewById(R.id.caption_text)).setText(R.string.title_about);
            messageTextView.setText(aboutBody);
            messageTextView.setMovementMethod(new LinkMovementMethod());
            return new AlertDialog.Builder(getActivity()).setView(view)
                    .setPositiveButton(R.string.accept_lbl,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    ).create();
        }
    }

    public static void showOpenSourceLicenses(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_licenses");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new OpenSourceLicensesDialog().show(ft, "dialog_licenses");
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {

        public OpenSourceLicensesDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about_licenses)
                    .setView(webView)
                    .setPositiveButton(R.string.ok_lbl,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

    public static void showEula(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_eula");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new EulaDialog().show(ft, "dialog_eula");
    }

    public static class EulaDialog extends DialogFragment {

        public EulaDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_normal);

            TextView eulaTextView = new TextView(getActivity());
            eulaTextView.setText(Html.fromHtml(getString(R.string.eula_legal_text)));
            eulaTextView.setMovementMethod(LinkMovementMethod.getInstance());
            eulaTextView.setPadding(padding, padding, padding, padding);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about_eula)
                    .setView(eulaTextView)
                    .setPositiveButton(R.string.ok_lbl,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
