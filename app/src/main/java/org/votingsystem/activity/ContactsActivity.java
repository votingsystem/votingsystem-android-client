package org.votingsystem.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.fragment.ContactsGridFragment;
import org.votingsystem.fragment.ProgressDialogFragment;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactsActivity extends ActivityBase {

	public static final String TAG = ContactsActivity.class.getSimpleName();

    private WeakReference<ContactsGridFragment> contactsGridRef;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle arguments = new Bundle();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            LOGD(TAG + ".ACTION_SEARCH: ", "query: " + query);
            arguments.putString(SearchManager.QUERY, query);
        }
        ContactsGridFragment fragment = new ContactsGridFragment();
        fragment.setArguments(arguments);
        contactsGridRef = new WeakReference<>(fragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object) fragment).getClass().getSimpleName()).commit();
        ((NavigationView) findViewById(R.id.nav_view)).inflateMenu(R.menu.drawer_currency);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    };

}