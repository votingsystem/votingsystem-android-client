package org.votingsystem.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ElectionContentProvider;
import org.votingsystem.fragment.ElectionFragment;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.Constants.CURSOR_POSITION_KEY;
import static org.votingsystem.util.Constants.ELECTION_STATE_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionPagerActivity extends AppCompatActivity {

    public static final String TAG = ElectionPagerActivity.class.getSimpleName();

    private Cursor cursor = null;
    private ViewPager mViewPager;
    private ActivityResult activityResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Integer cursorPosition = getIntent().getIntExtra(CURSOR_POSITION_KEY, -1);
        String eventStateStr = getIntent().getStringExtra(ELECTION_STATE_KEY);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        String selection = ElectionContentProvider.STATE_COL + "= ? ";
        cursor = getContentResolver().query(ElectionContentProvider.CONTENT_URI,
                null, selection, new String[]{eventStateStr}, null);
        cursor.moveToPosition(cursorPosition);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager(),
                eventStateStr);
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                cursor.moveToPosition(position);
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        super.onActivityResult(requestCode, resultCode, data);
        this.activityResult = new ActivityResult(requestCode, resultCode, data);
    }

    public ActivityResult getActivityResult() {
        ActivityResult result = this.activityResult;
        this.activityResult = null;
        return result;
    }

    public class EventsPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public EventsPagerAdapter(FragmentManager fm, String eventStateStr) {
            super(fm);
            String selection = ElectionContentProvider.STATE_COL + "= ? ";
            cursor = getContentResolver().query(ElectionContentProvider.CONTENT_URI,
                    null, selection, new String[]{eventStateStr}, null);
        }

        @Override
        public Fragment getItem(int i) {
            LOGD(TAG + ".EventsPagerAdapter.getItem", "item: " + i);
            cursor.moveToPosition(i);
            byte[] serializedElection = cursor.getBlob(cursor.getColumnIndex(
                    ElectionContentProvider.SERIALIZED_OBJECT_COL));
            return ElectionFragment.newInstance(serializedElection);
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

    }

}