package org.votingsystem.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.SignerId;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.util.CollectionStore;
import org.votingsystem.AppVS;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.MessageActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.FieldEventVSDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.ui.DialogButton;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * An assortment of UI helpers.
 */
public class UIUtils  {

    private static final String TAG = UIUtils.class.getSimpleName();

    public static final int EMPTY_MESSAGE = 1;
    /**
     * Regex to search for HTML escape sequences.
     *
     * <p></p>Searches for any continuous string of characters starting with an ampersand and ending with a
     * semicolon. (Example: &amp;amp;)
     */
    public static final String TARGET_FORM_FACTOR_HANDSET = "handset";
    public static final String TARGET_FORM_FACTOR_TABLET = "tablet";
    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");
    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";
    private static SimpleDateFormat sDayOfWeekFormat = new SimpleDateFormat("E");
    private static DateFormat sShortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);


    public static void launchMessageActivity(ResponseVS responseVS) {
        Intent intent = new Intent(AppVS.getInstance(), MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        AppVS.getInstance().startActivity(intent);
    }

    public static void launchMessageActivity(Integer statusCode, String message, String caption) {
        ResponseVS responseVS = new ResponseVS(statusCode);
        responseVS.setCaption(caption).setNotificationMessage(message);
        Intent intent = new Intent(AppVS.getInstance(), MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        AppVS.getInstance().startActivity(intent);
    }

    public static void showSignersInfoDialog(Set<UserVSDto> signers, FragmentManager fragmentManager,
             Context context) {
        StringBuilder signersInfo = new StringBuilder(context.getString(R.string.num_signers_lbl,
                signers.size()) + "<br/><br/>");
        for(UserVSDto signer : signers) {
            X509Certificate certificate = signer.getCertificate();
            signersInfo.append(context.getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getDayWeekDateStr(certificate.getNotBefore(), "HH:mm"),
                    DateUtils.getDayWeekDateStr(certificate.getNotAfter(), "HH:mm")) + "<br/>");
        }
        MessageDialogFragment.showDialog(ResponseVS.SC_OK, context.getString(
                R.string.signers_info_lbl), signersInfo.toString(), fragmentManager);
    }

    public static Drawable getEmptyLogo(Context context) {
        Drawable drawable = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
        return drawable;
    }

    public static void showTimeStampInfoDialog(TimeStampToken timeStampToken,
           X509Certificate timeStampServerCert, FragmentManager fragmentManager, Context context) {
        try {
            TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
            String certificateInfo = null;
            SignerId signerId = timeStampToken.getSID();
            String dateInfoStr = DateUtils.getDayWeekDateStr(tsInfo.getGenTime(), "HH:mm");
            CollectionStore store = (CollectionStore) timeStampToken.getCertificates();
            Collection<X509CertificateHolder> matches = store.getMatches(signerId);
            X509CertificateHolder certificateHolder = null;
            if(matches.size() == 0) {
                LOGD(TAG + ".showTimeStampInfoDialog",
                        "no cert matches found, validating with timestamp server cert");
                certificateHolder = new X509CertificateHolder(timeStampServerCert.getEncoded());
                timeStampToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(certificateHolder));
            } else certificateHolder = matches.iterator().next();
            LOGD(TAG + ".showTimeStampInfoDialog", "serial number: '" +
                    certificateHolder.getSerialNumber() + "'");
            X509Certificate certificate = new JcaX509CertificateConverter().
                    getCertificate(certificateHolder);
            certificateInfo = context.getString(R.string.timestamp_info_formated_msg, dateInfoStr,
                    tsInfo.getSerialNumber().toString(),
                    certificate.getSubjectDN(),
                    timeStampToken.getSID().getSerialNumber().toString());
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, context.getString(
                    R.string.timestamp_info_lbl), certificateInfo, fragmentManager);
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, context.getString(
                    R.string.error_lbl), context.getString(R.string.timestamp_error_lbl),
                    fragmentManager);
        }
    }

    public static boolean isSameDayDisplay(long time1, long time2) {
        TimeZone displayTimeZone = PrefUtils.getDisplayTimeZone();
        Calendar cal1 = Calendar.getInstance(displayTimeZone);
        Calendar cal2 = Calendar.getInstance(displayTimeZone);
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    //http://stackoverflow.com/questions/15055458/detect-7-inch-and-10-inch-tablet-programmatically
    public static double getDiagonalInches(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        //float scaleFactor = metrics.density;
        //float widthDp = widthPixels / scaleFactor;
        //float heightDp = heightPixels / scaleFactor;
        float widthDpi = metrics.xdpi;
        float heightDpi = metrics.ydpi;
        float widthInches = widthPixels / widthDpi;
        float heightInches = heightPixels / heightDpi;
        double diagonalInches = Math.sqrt((widthInches * widthInches) +
                (heightInches * heightInches));
        return diagonalInches;
    }

    /**
     * Populate the given {@link android.widget.TextView} with the requested text, formatting
     * through {@link android.text.Html#fromHtml(String)} when applicable. Also sets
     * {@link android.widget.TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    public static String getPollText(final Context context, long start, long end) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (now < start) {
            return "";
        } else if (start <= now && now <= end) {
            return "";
        } else {
            return "";
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);
        // Walk through string, inserting bold snippet spans
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);
            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);
            // Insert bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //builder.setSpan(new ForegroundColorSpan(0xff111111),
            //        startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            delta += 2;
        }
        return builder;
    }

    public static void preferPackageForIntent(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                intent.setPackage(packageName);
                break;
            }
        }
    }

    public static Drawable getLogoIcon(Context context, int iconId) {
        Drawable logoIcon = context.getResources().getDrawable(iconId);
        logoIcon.setBounds(0, 0, 32, 32);
        //logoIcon.setColorFilter(R.color.navdrawer_background, PorterDuff.Mode.MULTIPLY);
        return logoIcon;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void changeSearchIcon(Menu menu, Context context) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.search_item);
        SearchView mSearchView = (SearchView) searchViewMenuItem.getActionView();
        int searchImgId = context.getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.action_search);
    }

    public static EditText addFormField(String label, Integer type, LinearLayout mFormView, int id,
                Context context) {
        TextView textView = new TextView(context);
        textView.setTextSize(context.getResources().getDimension(R.dimen.claim_field_text_size));
        textView.setText(label);
        EditText fieldText = new EditText(context.getApplicationContext());
        fieldText.setLayoutParams(UIUtils.getFormItemParams(false));
        fieldText.setTextColor(Color.BLACK);
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        fieldText.setId(id);
        fieldText.setInputType(type);
        mFormView.addView(textView);
        mFormView.addView(fieldText);
        return fieldText;
    }

    public static LinearLayout.LayoutParams getFormItemParams(boolean isLabel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isLabel) {
            params.bottomMargin = 5;
            params.topMargin = 10;
        }
        params.leftMargin = 20;
        params.rightMargin = 20;
        return params;
    }

    // Whether a feedback notification was fired for a particular session. In the event that a
    // feedback notification has not been fired yet, return false and set the bit.
    public static boolean isFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    // Clear the flag that says a notification was fired for the given session.
    // Typically used to debug notifications.
    public static void unmarkFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        sp.edit().putBoolean(key, false).commit();
    }
    // Shows whether a notification was fired for a particular session time block. In the
    // event that notification has not been fired yet, return false and set the bit.
    public static boolean isNotificationFiredForBlock(Context context, String blockId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("notification_fired_%s", blockId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { android.R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }
        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }
        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }
        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }

    public static Map<Integer, EditText> showDynamicFormDialog(final Set<FieldEventVSDto> fields,
               final View.OnClickListener listener, Activity activity) {
        AlertDialog.Builder builder= new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        ScrollView mScrollView = (ScrollView) inflater.inflate(R.layout.dinamic_form,
                (ViewGroup) activity.getCurrentFocus());
        LinearLayout mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
        final TextView errorMsgTextView = (TextView) mScrollView.findViewById(R.id.errorMsg);
        errorMsgTextView.setVisibility(View.GONE);
        final Map<Integer, EditText> fieldsMap = new HashMap<Integer, EditText>();
        for (FieldEventVSDto field : fields) {
            fieldsMap.put(field.getId().intValue(), UIUtils.addFormField(field.getContent(),
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    mFormView, field.getId().intValue(), activity));
        }
        builder.setTitle(R.string.eventfields_dialog_caption).setView(mScrollView).
                setPositiveButton(activity.getString(R.string.accept_lbl), null).
                setNegativeButton(R.string.cancel_lbl, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();//to get positiveButton this must be called first
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View onClick) {
                for (FieldEventVSDto field : fields) {
                    EditText editText = fieldsMap.get(field.getId().intValue());
                    String fieldValue = editText.getText().toString();
                    if (fieldValue.isEmpty()) {
                        errorMsgTextView.setVisibility(View.VISIBLE);
                        return;
                    } else field.setValue(fieldValue);
                    LOGD(TAG + ".showDynamicFormDialog", "field id: " + field.getId() +
                            " - text: " + fieldValue);
                }
                //listener.onClick();
                dialog.dismiss();
            }
        });
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return fieldsMap;
    }

    public static int setColorAlpha(int color, float alpha) {
        int alpha_int = Math.min(Math.max((int)(alpha * 255.0f), 0), 255);
        return Color.argb(alpha_int, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static boolean hasActionBar(Activity activity) {
        return (((AppCompatActivity)activity).getSupportActionBar() != null);
    }

    public static void setStartPadding(final Context context, View view, int padding) {
        if (isRtl(context)) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), padding, view.getPaddingBottom());
        } else {
            view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else {
            return context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        }
    }

    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public static void launchEmbeddedFragment(String className, Context context) {
        Intent intent = new Intent(context, FragmentContainerActivity.class);
        intent.putExtra(ContextVS.FRAGMENT_KEY, className);
        context.startActivity(intent);
    }


    public static void setImage(ImageView imageView, byte[] imageBytes, final Context context) {
        final Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        imageView.setImageBitmap(bmp);
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                imageView.setImageBitmap(bmp);
                new AlertDialog.Builder(context).setView(imageView).show();
            }
        });
    }

    public static AlertDialog.Builder getMessageDialogBuilder(String caption, String message,
              Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_dialog, null);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        return new AlertDialog.Builder(context).setView(view).setCancelable(false);
    }

    public static void showMessageDialog(AlertDialog.Builder builder) {
        final Dialog dialog = builder.show();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else return false;
            }
        });
    }

    //Mandatory dialog, only process y user click on dialog buttons
    public static Dialog showMessageDialog(String caption, String message, DialogButton positiveButton,
             DialogButton negativeButton, Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_dialog, null);
        ((TextView) view.findViewById(R.id.caption_text)).setText(caption);
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(view)
                .setCancelable(false);
        if(positiveButton != null) builder.setPositiveButton(positiveButton.getLabel(),
                positiveButton.getClickListener());
        if(negativeButton != null) builder.setNegativeButton(negativeButton.getLabel(),
                negativeButton.getClickListener());
        final Dialog dialog = builder.show();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        /*dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else return false;
            }
        });*/
        return dialog;
    }

    public static void fillAddressInfo(LinearLayout linearLayout, Context contex) throws IOException {
        UserVSDto userVS = PrefUtils.getAppUser();
        ((TextView)linearLayout.findViewById(R.id.name)).setText(userVS.getAddress().getName());
        ((TextView)linearLayout.findViewById(R.id.postal_code)).setText(userVS.getAddress().getPostalCode());
        ((TextView)linearLayout.findViewById(R.id.city)).setText(userVS.getAddress().getCity());
        linearLayout.setVisibility(View.VISIBLE);
    }

    public static AlertDialog.Builder getMessageDialogBuilder(ResponseVS responseVS,
            Context context) {
        return getMessageDialogBuilder(responseVS.getCaption(), responseVS.getMessage(), context);
    }

    public static void showPasswordRequiredDialog(final Activity activity) {
        DialogButton positiveButton = new DialogButton(activity.getString(R.string.ok_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        activity.setResult(Activity.RESULT_CANCELED, new Intent());
                        activity.finish();
                    }
                });
        UIUtils.showMessageDialog(activity.getString(R.string.error_lbl),
                activity.getString(R.string.passw_missing_msg), positiveButton, null, activity);
    }

    public static Toolbar setSupportActionBar(AppCompatActivity activity) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_vs);
        activity.setSupportActionBar(toolbar);
        return toolbar;
    }

    public static Toolbar setSupportActionBar(AppCompatActivity activity, String title) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_vs);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(title);
        return toolbar;
    }

    public static void killApp(boolean killSafely) {
        if (killSafely) {
            /*
             * Notify the system to finalize and collect all objects of the app
             * on exit so that the virtual machine running the app can be killed
             * by the system without causing issues. NOTE: If this is set to
             * true then the virtual machine will not be killed until all of its
             * threads have closed.
             */
            System.runFinalizersOnExit(true);

            /*
             * Force the system to close the app down completely instead of
             * retaining it in the background. The virtual machine that runs the
             * app will be killed. The app will be completely created as a new
             * app in a new virtual machine running in a new process if the user
             * starts the app again.
             */
            System.exit(0);
        } else {
            /*
             * Alternatively the process that runs the virtual machine could be
             * abruptly killed. This is the quickest way to remove the app from
             * the device but it could cause problems since resources will not
             * be finalized first. For example, all threads running under the
             * process will be abruptly killed when the process is abruptly
             * killed. If one of those threads was making multiple related
             * changes to the database, then it may have committed some of those
             * changes but not all of those changes when it was abruptly killed.
             */
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}
