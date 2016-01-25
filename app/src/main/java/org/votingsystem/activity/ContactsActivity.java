package org.votingsystem.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.fragment.ContactsGridFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.UIUtils;

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
        ContactsGridFragment fragment = new ContactsGridFragment();
        contactsGridRef = new WeakReference<>(fragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object) fragment).getClass().getSimpleName()).commit();
        getSupportActionBar().setSubtitle(getString(R.string.contacts_lbl));
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

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.eventvs_grid, menu);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ||
        //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { }
        double diagonalInches = UIUtils.getDiagonalInches(getWindowManager().getDefaultDisplay());
        if(diagonalInches < 4) {
            //2 -> index of publish documents menu item on main.xml
            menu.getItem(2).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    };

}