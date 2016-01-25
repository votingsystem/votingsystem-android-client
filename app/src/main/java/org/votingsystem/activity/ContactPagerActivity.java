package org.votingsystem.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.fragment.ContactFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.util.ArrayList;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        String dtoStr = getIntent().getExtras().getString(ContextVS.DTO_KEY);
        List<UserVSDto> userVSListDto = null;
        try {
            if(dtoStr != null) userVSListDto = JSON.readValue(dtoStr, new TypeReference<List<UserVSDto>>(){});
        } catch (Exception ex) { ex.printStackTrace();}
        UserVSDto userVS = (UserVSDto) getIntent().getExtras().getSerializable(ContextVS.USER_KEY);
        if(userVS != null) userVSListDto = Arrays.asList(userVS);
        if(userVSListDto != null || userVS != null) {
            try {
                final List<UserVSDto> userVSList = new ArrayList<>(userVSListDto);
                updateActionBarTitle(userVSListDto.get(0).getName());
                ContactPagerAdapter pagerAdapter = new ContactPagerAdapter(
                        getSupportFragmentManager(), userVSList);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override public void onPageSelected(int position) {
                        updateActionBarTitle(userVSList.get(position).getName());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }  else {
            ContactDBPagerAdapter pagerAdapter = new ContactDBPagerAdapter(
                    getSupportFragmentManager());
            mViewPager.setAdapter(pagerAdapter);
            String selection = UserContentProvider.TYPE_COL + " =? ";
            cursor = getContentResolver().query(UserContentProvider.CONTENT_URI, null, selection,
                    new String[]{UserVSDto.Type.CONTACT.toString()}, null);
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

        private List<UserVSDto> itemList;
        public ContactPagerAdapter(FragmentManager fm, List<UserVSDto> itemList) {
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