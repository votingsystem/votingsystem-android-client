package org.votingsystem.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.TransactionContentProvider;
import org.votingsystem.fragment.TransactionFragment;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionPagerActivity extends AppCompatActivity {

    public static final String TAG = TransactionPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        App app = (App) getApplicationContext();
        setContentView(R.layout.pager_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(Constants.CURSOR_POSITION_KEY, -1);
        String selection = TransactionContentProvider.WEEK_LAPSE_COL + " =? ";
        cursor = getContentResolver().query(TransactionContentProvider.CONTENT_URI, null, selection,
                new String[]{app.getCurrentWeekLapseId()}, null);
        TransactionPagerAdapter pagerAdapter = new TransactionPagerAdapter(
                getSupportFragmentManager(), cursor.getCount());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        //cursor.moveToFirst();
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState + " - count: " + cursor.getCount());
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    class TransactionPagerAdapter extends FragmentStatePagerAdapter {

        private int numPages;
        public TransactionPagerAdapter(FragmentManager fm, int numPages) {
            super(fm);
            this.numPages = numPages;
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".TransactionPagerAdapter.getItem", "item: " + i);
            return TransactionFragment.newInstance(i);
        }

        @Override public int getCount() {
            return numPages;
        }

    }

}