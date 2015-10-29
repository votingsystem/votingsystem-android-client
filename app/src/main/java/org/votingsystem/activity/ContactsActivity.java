/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Bundle args = getIntent().getExtras();
        ContactsGridFragment fragment = new ContactsGridFragment();
        contactsGridRef = new WeakReference<ContactsGridFragment>(fragment);
        if(args == null) args = new Bundle();
        args.putSerializable(ContextVS.EVENT_STATE_KEY, EventVSDto.State.ACTIVE);
        fragment.setArguments(args);
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