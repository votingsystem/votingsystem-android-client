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
import org.votingsystem.dto.UserDto;
import org.votingsystem.fragment.ContactFragment;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;

import java.util.Arrays;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactPagerActivity extends AppCompatActivity {

    public static final String TAG = ContactPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        UIUtils.setSupportActionBar(this);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(Constants.CURSOR_POSITION_KEY, -1);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        UserDto user = (UserDto) getIntent().getExtras().getSerializable(Constants.USER_KEY);
        if(user != null) {
            updateActionBarTitle(user.getName());
            ContactPagerAdapter pagerAdapter = new ContactPagerAdapter(
                    getSupportFragmentManager(), Arrays.asList(user));
            mViewPager.setAdapter(pagerAdapter);
        }  else {
            ContactDBPagerAdapter pagerAdapter = new ContactDBPagerAdapter(
                    getSupportFragmentManager());
            mViewPager.setAdapter(pagerAdapter);
            String selection = UserContentProvider.TYPE_COL + " =? ";
            cursor = getContentResolver().query(UserContentProvider.CONTENT_URI, null, selection,
                    new String[]{UserDto.Type.CONTACT.toString()}, null);
            cursor.moveToFirst();
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override public void onPageSelected(int position) {
                    cursor.moveToPosition(position);
                    updateActionBarTitle(cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL)));
                }
            });
            mViewPager.setCurrentItem(cursorPosition);
        }
        getSupportActionBar().setTitle(getString(R.string.contacts_lbl));
    }

    private void updateActionBarTitle(String fullName) {
        getSupportActionBar().setTitle(getString(R.string.contact_lbl));
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

    class ContactDBPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public ContactDBPagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(
                    UserContentProvider.CONTENT_URI, null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".ContactPagerAdapter.getItem", " - item: " + i);
            cursor.moveToPosition(i);
            Long contactId = cursor.getLong(cursor.getColumnIndex(UserContentProvider.ID_COL));
            return ContactFragment.newInstance(contactId);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }
    }

    class ContactPagerAdapter extends FragmentStatePagerAdapter {

        private List<UserDto> itemList;
        public ContactPagerAdapter(FragmentManager fm, List<UserDto> itemList) {
            super(fm);
            this.itemList = itemList;
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".ContactPagerAdapter.getItem", " - item: " + i);
            return ContactFragment.newInstance(itemList.get(i));
        }

        @Override public int getCount() {
            return itemList.size();
        }
    }

}