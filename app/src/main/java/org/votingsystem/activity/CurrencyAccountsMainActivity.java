package org.votingsystem.activity;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.fragment.CurrencyAccountsFragment;
import org.votingsystem.fragment.TransactionVSGridFragment;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAccountsMainActivity extends ActivityBase {

    public static final String TAG = CurrencyAccountsMainActivity.class.getSimpleName();

    //corresponds to finance section child screens order
    private static final int USER_ACCOUNTS_POS       = 0;
    private static final int TRANSANCTIONVS_LIST_POS = 1;

    private AppVS appVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        setContentView(R.layout.currency_accounts_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) { }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            public void onPageSelected(int position) {
                LOGD(TAG + ".onCreate", "onPageSelected: " + position);
                switch (position) {
                    case USER_ACCOUNTS_POS:
                        getSupportActionBar().setSubtitle(null);
                        break;
                    case TRANSANCTIONVS_LIST_POS:
                        getSupportActionBar().setSubtitle(getString(R.string.movements_lbl));
                        break;
                }

            }
        });
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        CurrencyPagerAdapter pagerAdapter = new CurrencyPagerAdapter(getSupportFragmentManager(),
                getIntent().getExtras());
        mViewPager.setAdapter(pagerAdapter);
        getSupportActionBar().setTitle(getString(R.string.currency_accounts_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            /*case R.id.admin_currency_menu_item:
                Intent intent = new Intent(this, BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, appVS.getCurrencyServer().getMenuAdminURL());
                startActivity(intent);
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_CURRENCY_ACCOUNTS;// we only have a nav drawer if we are in top-level
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
    }

    class CurrencyPagerAdapter extends FragmentStatePagerAdapter {

        final String TAG = CurrencyPagerAdapter.class.getSimpleName();

        private static final int USERVS_MONETARY_INFO = 0;
        private static final int CURRENCY_LIST = 1;

        private String searchQuery = null;
        private Bundle args;

        public CurrencyPagerAdapter(FragmentManager fragmentManager, Bundle args) {
            super(fragmentManager);
            this.args = (args != null)? args:new Bundle();
        }

        @Override public Fragment getItem(int position) {
            Fragment selectedFragment = null;
            switch(position) {
                case USERVS_MONETARY_INFO:
                    selectedFragment = new CurrencyAccountsFragment();
                    break;
                case CURRENCY_LIST:
                    selectedFragment = new TransactionVSGridFragment();
                    break;
            }
            args.putString(SearchManager.QUERY, searchQuery);
            selectedFragment.setArguments(args);
            LOGD(TAG + ".getItem", "position:" + position + " - args: " + args +
                    " - selectedFragment.getClass(): " + ((Object)selectedFragment).getClass());
            return selectedFragment;
        }

        @Override public int getCount() {
            return 2;
        } //CURRENCY_ACCOUNTS_INFO and CURRENCY_LIST

    }

}