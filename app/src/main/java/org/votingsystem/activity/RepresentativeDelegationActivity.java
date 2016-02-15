package org.votingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.ReceiptFragment;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.InputFilterMinMax;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeDelegationActivity extends AppCompatActivity {
	
	public static final String TAG = RepresentativeDelegationActivity.class.getSimpleName();

    public static final int RC_PASSW          = 0;

    public static final String ANONYMOUS_SELECTED_KEY  = "ANONYMOUS_SELECTED_KEY";
    public static final String PUBLIC_SELECTED_KEY     = "PUBLIC_SELECTED_KEY";

    private TypeVS operationType;
    private Button acceptButton;
    private CheckBox anonymousCheckBox;
    private CheckBox publicCheckBox;
    private EditText weeks_delegation;
    private AppVS appVS = null;
    private String broadCastId = RepresentativeDelegationActivity.class.getSimpleName();
    private UserVSDto representative = null;
    private Date anonymousDelegationFromDate;
    private Date anonymousDelegationToDate;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        final ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        setProgressDialogVisible(null, null, false);
        if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.error_lbl),
                    responseVS.getNotificationMessage(), RepresentativeDelegationActivity.this).
                    setPositiveButton(getString(R.string.open_receipt_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent intent = new Intent(getApplicationContext(), FragmentContainerActivity.class);
                                    intent.putExtra(ContextVS.URL_KEY, (String) responseVS.getData());
                                    intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                    startActivity(intent);
                                }
                            });
            UIUtils.showMessageDialog(builder);
        } else {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(responseVS.getCaption(),
                    responseVS.getNotificationMessage(),  RepresentativeDelegationActivity.this).
                    setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent resultIntent = new Intent(
                                            RepresentativeDelegationActivity.this, RepresentativesMainActivity.class);
                                    startActivity(resultIntent);
                                    RepresentativeDelegationActivity.this.finish();
                                }
                            });
            UIUtils.showMessageDialog(builder);
        }
        }
    };

    private void sendDelegation() {
        LOGD(TAG + ".sendDelegation", "sendDelegation");
        Intent startIntent = new Intent(this, RepresentativeService.class);
        RepresentativeDelegationDto delegationDto = new RepresentativeDelegationDto();
        delegationDto.setOperation(operationType);
        delegationDto.setRepresentative(representative);
        if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION == operationType) {
            delegationDto.setWeeksOperationActive(
                    Integer.valueOf(weeks_delegation.getText().toString()));
        }
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        try {
            startIntent.putExtra(ContextVS.DTO_KEY, JSON.writeValueAsString(delegationDto));
        } catch (IOException e) { e.printStackTrace(); }
        setProgressDialogVisible(
                getString(R.string.sending_data_lbl), getString(R.string.wait_msg), true);
        startService(startIntent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        representative = (UserVSDto) getIntent().getSerializableExtra(ContextVS.USER_KEY);
        setContentView(R.layout.representative_delegation);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.representative_delegation_lbl));
        acceptButton = (Button) findViewById(R.id.accept_button);
        anonymousCheckBox = (CheckBox) findViewById(R.id.anonymous_delegation_checkbox);
        publicCheckBox = (CheckBox) findViewById(R.id.public_delegation_checkbox);
        weeks_delegation = (EditText)findViewById(R.id.weeks_delegation);
        EditText et = (EditText) findViewById(R.id.weeks_delegation);
        et.setFilters(new InputFilter[]{
                new InputFilterMinMax(1, ContextVS.MAX_WEEKS_ANONYMOUS_DELEGATION)});
        String fileToLoad = "delegation_message_" +
                Locale.getDefault().getLanguage().toLowerCase() + ".html";
        try {
            if(!Arrays.asList(getResources().getAssets().list("")).contains(fileToLoad)) {
                LOGD(TAG + ".onCreate", "missing fileToLoad: " + fileToLoad);
                fileToLoad = "delegation_message_es.html";
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        WebView webView = (WebView)findViewById(R.id.webview);
        webView.setBackgroundColor(getResources().getColor(R.color.bkg_screen_vs));
        webView.loadUrl("file:///android_asset/" + fileToLoad);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                setProgressDialogVisible(null, null, false);
            }
        });
        setProgressDialogVisible(getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg), true);
        if(savedInstanceState != null) {
            operationType = (TypeVS) savedInstanceState.getSerializable(ContextVS.TYPEVS_KEY);
            int selectedCheckBoxId = -1;
            if(savedInstanceState.getBoolean(ANONYMOUS_SELECTED_KEY, false)) {
                selectedCheckBoxId = R.id.anonymous_delegation_checkbox;
                anonymousCheckBox.setChecked(true);
            } else if(savedInstanceState.getBoolean(PUBLIC_SELECTED_KEY, false))  {
                selectedCheckBoxId = R.id.public_delegation_checkbox;
                publicCheckBox.setChecked(true);
            }
            if(selectedCheckBoxId > 0) onCheckboxClicked(selectedCheckBoxId);
        }
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible) ProgressDialogFragment.showDialog(
                caption, message, getSupportFragmentManager());
        else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    public void onCheckboxClicked(View view) {
        onCheckboxClicked(view.getId());
    }

    public void onCheckboxClicked(int selectedBoxId) {
        switch(selectedBoxId) {
            case R.id.anonymous_delegation_checkbox:
                publicCheckBox.setChecked(false);
                break;
            case R.id.public_delegation_checkbox:
                anonymousCheckBox.setChecked(false);
                break;
        }
        if(anonymousCheckBox.isChecked())
            ((LinearLayout)findViewById(R.id.weeks_delegation_layout)).setVisibility(View.VISIBLE);
        else ((LinearLayout)findViewById(R.id.weeks_delegation_layout)).setVisibility(View.GONE);
        if(anonymousCheckBox.isChecked() || publicCheckBox.isChecked()) {
            acceptButton.setEnabled(true);
        } else acceptButton.setEnabled(false);
        LOGD(TAG + ".onCheckboxClicked", "anonymousCheckBox.isChecked(): " + anonymousCheckBox.isChecked() +
                " - publicCheckBox.isChecked(): " + publicCheckBox.isChecked());
    }

    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.cancel_button:
                onBackPressed();
                break;
            case R.id.accept_button:
                String confirmDialogMsg = null;
                if(anonymousCheckBox.isChecked()) {
                    if(TextUtils.isEmpty(weeks_delegation.getText())) {
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), getString(
                                        R.string.anonymous_delegation_time_msg), getSupportFragmentManager());
                        ((EditText)findViewById(R.id.weeks_delegation)).requestFocus();
                        return;
                    }
                    Calendar calendar = DateUtils.getMonday(DateUtils.addDays(7));//7 -> next week monday
                    anonymousDelegationFromDate = calendar.getTime();
                    Integer weeksDelegation = Integer.valueOf(weeks_delegation.getText().toString());
                    calendar.add(Calendar.DAY_OF_YEAR, weeksDelegation*7);
                    anonymousDelegationToDate = calendar.getTime();
                    confirmDialogMsg = getString(R.string.anonymous_delegation_confirm_msg,
                            representative.getName(),  weeks_delegation.getText().toString(),
                            DateUtils.getDayWeekDateStr(anonymousDelegationFromDate, "HH:mm"),
                            DateUtils.getDayWeekDateStr(anonymousDelegationToDate, "HH:mm"));
                    operationType = TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION;
                }  else {
                    confirmDialogMsg = getString(R.string.public_delegation_confirm_msg,
                             representative.getName());
                    operationType = TypeVS.REPRESENTATIVE_SELECTION;
                }
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.representative_delegation_lbl), confirmDialogMsg, this);
                builder.setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Utils.getCryptoDeviceAccessModePassword(RC_PASSW, null, null,
                                        RepresentativeDelegationActivity.this);
                            }
                        });
                UIUtils.showMessageDialog(builder);
                break;
        }
    }

    private void sendResult(int result, ResponseVS responseVS) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        setResult(result, resultIntent);
        finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    sendDelegation();
                }
                break;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ANONYMOUS_SELECTED_KEY, anonymousCheckBox.isChecked());
        outState.putBoolean(PUBLIC_SELECTED_KEY, publicCheckBox.isChecked());
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}