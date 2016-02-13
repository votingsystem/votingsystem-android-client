package org.votingsystem.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.fragment.RepresentativeFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativePagerActivity extends AppCompatActivity {

    public static final String TAG = RepresentativePagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        UIUtils.setSupportActionBar(this);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        RepresentativePagerAdapter pagerAdapter = new RepresentativePagerAdapter(
                getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        String selection = UserContentProvider.TYPE_COL + " =? ";
        cursor = getContentResolver().query(UserContentProvider.CONTENT_URI, null, selection,
                new String[]{UserVSDto.Type.REPRESENTATIVE.toString()}, null);
        cursor.moveToFirst();
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                updateActionBarTitle();
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
        updateActionBarTitle();
    }

    private void updateActionBarTitle() {
        getSupportActionBar().setTitle(getString(R.string.representative_lbl));
        String fullName = cursor.getString(cursor.getColumnIndex(UserContentProvider.FULL_NAME_COL));
        getSupportActionBar().setSubtitle(fullName);
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

    class RepresentativePagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public RepresentativePagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(
                    UserContentProvider.CONTENT_URI, null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".RepresentativePagerAdapter.getItem", " - item: " + i);
            cursor.moveToPosition(i);
            Long representativeId = cursor.getLong(cursor.getColumnIndex(
                    UserContentProvider.ID_COL));
            return RepresentativeFragment.newInstance(representativeId);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}