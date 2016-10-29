package org.votingsystem.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FragmentContainerActivity extends AppCompatActivity {

	public static final String TAG = FragmentContainerActivity.class.getSimpleName();

    private WeakReference<Fragment> fragmentRef;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        String fragmentClass = getIntent().getStringExtra(Constants.FRAGMENT_KEY);
        try {
            Class clazz = Class.forName(fragmentClass);
            Fragment fragment = (Fragment)clazz.newInstance();
            fragmentRef = new WeakReference<>(fragment);
            fragment.setArguments(Utils.intentToFragmentArguments(getIntent()));
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment,
                            ((Object)fragment).getClass().getSimpleName()).commit();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        getSupportActionBar().setTitle(title);
        if(subTitle != null) getSupportActionBar().setSubtitle(subTitle);
        if(iconId != null) getSupportActionBar().setLogo(iconId);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode:  " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        ConnectionUtils.onActivityResult(requestCode, resultCode, data, this);
        if(fragmentRef.get() != null) {
            fragmentRef.get().onActivityResult(requestCode, resultCode, data);
        }
    }
}