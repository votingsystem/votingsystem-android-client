package org.votingsystem.util.debug;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.util.debug.actions.BrowserVSAction;
import org.votingsystem.util.debug.actions.CurrencyConsumedAction;
import org.votingsystem.util.debug.actions.DeleteDBAction;
import org.votingsystem.util.debug.actions.ForceSyncNowAction;
import org.votingsystem.util.debug.actions.NFCActivityAction;
import org.votingsystem.util.debug.actions.PatternLockAction;
import org.votingsystem.util.debug.actions.PrefsAction;

import static org.votingsystem.util.LogUtils.LOGD;


public class DebugActionRunnerFragment extends Fragment {

    private static final String TAG = DebugActionRunnerFragment.class.getSimpleName();

    private TextView mLogArea;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.debug_action_runner, null);
        mLogArea = (TextView) rootView.findViewById(R.id.logArea);
        ViewGroup tests = (ViewGroup) rootView.findViewById(R.id.debug_action_list);
        tests.addView(createTestAction(new ForceSyncNowAction(
                (App) getActivity().getApplicationContext())));
        tests.addView(createTestAction(new PatternLockAction()));
        tests.addView(createTestAction(new PrefsAction()));
        tests.addView(createTestAction(new DeleteDBAction()));
        tests.addView(createTestAction(new CurrencyConsumedAction()));
        tests.addView(createTestAction(new BrowserVSAction()));
        tests.addView(createTestAction(new NFCActivityAction((App) getActivity().getApplicationContext())));
        setHasOptionsMenu(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                getString(R.string.debug_tests));
        return rootView;
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

    protected View createTestAction(final DebugAction test) {
        Button testButton = new Button(this.getActivity());
        testButton.setText(test.getLabel());
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                final long start = System.currentTimeMillis();
                mLogArea.setText("");
                test.run(view.getContext(), new DebugAction.Callback() {
                    @Override public void done(boolean success, String message) {
                        logTimed((System.currentTimeMillis() - start),
                        (success ? "[OK] " : "[FAIL] ") + message);
                    }
                });
            }
        });
        return testButton;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            String contents = result.getContents();
            LOGD(TAG + ".onActivityResult", "contents: " + contents);
        }
    }

    protected void logTimed(long time, String message) {
        message = "["+time+"ms] "+message;
        LOGD(TAG, message);
        mLogArea.append(message + "\n");
    }

}
