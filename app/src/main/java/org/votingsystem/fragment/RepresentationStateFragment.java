package org.votingsystem.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentationStateFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = RepresentationStateFragment.class.getSimpleName();

    private RepresentationStateDto representation;
    private RepresentativeDelegationDto representativeDelegationDto;
    private View rootView;
    private String broadCastId = RepresentationStateFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) ;
        else {
            setProgressDialogVisible(false);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                switch(responseVS.getTypeVS()) {
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                        MessageDialogFragment.showDialog(responseVS, getFragmentManager());
                        break;
                }
            } else if(ResponseVS.SC_OK != responseVS.getStatusCode()) MessageDialogFragment.showDialog(
                    responseVS, getFragmentManager());
        }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        rootView = inflater.inflate(R.layout.representative_state, container, false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.user_representative_lbl);
        setRepresentationView(PrefUtils.getRepresentationState());
        setHasOptionsMenu(true);
        return rootView;
    }

    private void setRepresentationView(RepresentationStateDto representation) {
        this.representation = representation;
        if(representation == null) {
            if(AppVS.getInstance().getUserVS() != null) launchRepresentativeService(TypeVS.STATE);
            rootView.setVisibility(View.GONE);
            return;
        } else {
            rootView.setVisibility(View.VISIBLE);
            ((TextView)rootView.findViewById(R.id.last_checked_date)).setText(getString(
                    R.string.representation_last_checked_msg,
                    DateUtils.getDayWeekDateStr(representation.getLastCheckedDate(), "HH:mm")));
        }
        switch(representation.getState()) {
            case WITHOUT_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.without_representative_msg));
                break;
            case WITH_PUBLIC_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.with_public_representation_msg));
                break;
            case WITH_ANONYMOUS_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.with_anonymous_representation_msg, DateUtils.getDayWeekDateStr(
                                representation.getDateTo(), "HH:mm")));
                representativeDelegationDto = PrefUtils.getAnonymousDelegation();
                if(representativeDelegationDto == null) {
                    ((TextView)rootView.findViewById(R.id.representative_name)).setText(
                            getString(R.string.missing_anonymous_delegation_cancellation_data));
                }
                break;
            case REPRESENTATIVE:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.representative_msg));
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if(representation == null) return;
        switch(representation.getState()) {
            case WITH_ANONYMOUS_REPRESENTATION:
                if(representativeDelegationDto == null)
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                break;
            default:
                menu.removeItem(R.id.cancel_anonymouys_representation);
        }
    }

    private void launchRepresentativeService(TypeVS operationType) {
        LOGD(TAG + ".launchRepresentativeService", "operation:" + operationType.toString());
        Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true);
        getActivity().startService(startIntent);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onResume() {
        super.onResume();
        PrefUtils.registerPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        PrefUtils.unregisterPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                //getActivity().finish();
                return true;
            case R.id.check_representation_state:
                launchRepresentativeService(TypeVS.STATE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(RepresentationStateDto.class.getSimpleName().equals(key)) {
            LOGD(TAG + ".onSharedPreferenceChanged", "key: " + key);
            setRepresentationView(PrefUtils.getRepresentationState());
        }
    }

}