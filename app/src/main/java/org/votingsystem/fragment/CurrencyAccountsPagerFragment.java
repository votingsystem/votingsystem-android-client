package org.votingsystem.fragment;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.votingsystem.android.R;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAccountsPagerFragment extends Fragment {

    public static final String TAG = CurrencyAccountsPagerFragment.class.getSimpleName();

    //corresponds to finance section child screens order
    private static final int USER_ACCOUNTS_POS       = 0;
    private static final int TRANSANCTIONVS_LIST_POS = 1;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.currency_accounts_main, container, false);
        ViewPager mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) { }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            public void onPageSelected(int position) {
                LOGD(TAG + ".onCreate", "onPageSelected: " + position);
                switch (position) {
                    case USER_ACCOUNTS_POS:
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(null);
                        break;
                    case TRANSANCTIONVS_LIST_POS:
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                                getString(R.string.movements_lbl));
                        break;
                }

            }
        });
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        CurrencyPagerAdapter pagerAdapter = new CurrencyPagerAdapter(getFragmentManager(),
                getActivity().getIntent().getExtras());
        mViewPager.setAdapter(pagerAdapter);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.currency_accounts_lbl));
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            /*case R.id.admin_currency_menu_item:
                Intent intent = new Intent(this, BrowserActivity.class);
                intent.putExtra(ContextVS.URL_KEY, appVS.getCurrencyServer().getMenuAdminURL());
                startActivity(intent);
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class CurrencyPagerAdapter extends FragmentStatePagerAdapter {

        final String TAG = CurrencyPagerAdapter.class.getSimpleName();

        private static final int USER_MONETARY_INFO = 0;
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
                case USER_MONETARY_INFO:
                    selectedFragment = new CurrencyAccountsFragment();
                    break;
                case CURRENCY_LIST:
                    selectedFragment = new TransactionGridFragment();
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