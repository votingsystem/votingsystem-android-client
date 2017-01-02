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
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.fragment.ReceiptFragment;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptPagerActivity extends AppCompatActivity {

    public static final String TAG = ReceiptPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(Constants.CURSOR_POSITION_KEY, 0);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        ReceiptPagerAdapter pagerAdapter = new ReceiptPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        cursor = getContentResolver().query(ReceiptContentProvider.CONTENT_URI, null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        mViewPager.setCurrentItem(cursorPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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

    class ReceiptPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public ReceiptPagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(ReceiptContentProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override
        public Fragment getItem(int i) {
            LOGD(TAG + ".ReceiptPagerAdapter.getItem", " - item: " + i);
            cursor.moveToPosition(i);
            return ReceiptFragment.newInstance(cursor.getPosition());
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

    }

}