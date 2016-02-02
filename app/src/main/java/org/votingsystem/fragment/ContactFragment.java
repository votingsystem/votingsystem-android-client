package org.votingsystem.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;

import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactFragment extends Fragment {

	public static final String TAG = ContactFragment.class.getSimpleName();

    private String broadCastId = null;
    private Button toggle_contact_button;
    private UserVSDto userVS;
    private Menu menu;
    

    public static Fragment newInstance(Long contactId) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.CURSOR_POSITION_KEY, contactId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(UserVSDto userVS) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.USER_KEY, userVS);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.contact, container, false);
        toggle_contact_button = (Button) rootView.findViewById(R.id.toggle_contact_button);
        TextView user_name_text = (TextView) rootView.findViewById(R.id.user_name_text);
        Long contactId =  getArguments().getLong(ContextVS.CURSOR_POSITION_KEY, -1);
        if(contactId > 0) {
            userVS = UserContentProvider.loadUser(contactId, getActivity());
        } else {
            userVS =  (UserVSDto) getArguments().getSerializable(ContextVS.USER_KEY);
            UserVSDto contactDB = UserContentProvider.loadUser(userVS, getActivity());
            if(contactDB != null) userVS = contactDB;
        }
        user_name_text.setText(userVS.getFullName());
        setContactButtonState();
        setHasOptionsMenu(true);
        broadCastId = ContactFragment.class.getSimpleName() + "_" + (contactId != null? contactId:
                UUID.randomUUID().toString());
        return rootView;
    }

    private void deleteContact() {
        UserContentProvider.deleteById(userVS.getId(), getActivity());
        getActivity().onBackPressed();
    }

    private void addContact() {
        getActivity().getContentResolver().insert(UserContentProvider.CONTENT_URI,
                UserContentProvider.getContentValues(userVS.setType(UserVSDto.Type.CONTACT)));
        setContactButtonState();
    }

    private void setContactButtonState() {
        if(UserVSDto.Type.CONTACT == userVS.getType()) toggle_contact_button.setVisibility(View.GONE);
        else {
            toggle_contact_button.setVisibility(View.VISIBLE);
            toggle_contact_button.setText(getString(R.string.add_contact_lbl));
            toggle_contact_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { addContact(); }
            });
        }
        if(menu != null) {
            menu.removeItem(R.id.send_message); //to avoid duplicated items
            menu.removeItem(R.id.delete_item);
            menu.add(R.id.general_items, R.id.send_message, 1,
                        getString(R.string.send_message_lbl));
            if(UserVSDto.Type.CONTACT == userVS.getType()) menu.add(R.id.general_items, R.id.delete_item, 3,
                    getString(R.string.remove_contact_lbl));
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(R.string.operation_ok_msg),
                    message, getFragmentManager());
        } else if(message != null) MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                    getString(R.string.operation_error_msg), message, getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contact, menu);
        this.menu = menu;
        setContactButtonState();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.send_message:
                Intent resultIntent = new Intent(getActivity(), FragmentContainerActivity.class);
                resultIntent.putExtra(ContextVS.FRAGMENT_KEY, MessageFormFragment.class.getName());
                resultIntent.putExtra(ContextVS.USER_KEY, userVS);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                return true;
            case R.id.send_money:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, TransactionVSFormFragment.class.getName());
                intent.putExtra(ContextVS.TYPEVS_KEY, TransactionVSFormFragment.Type.TRANSACTIONVS_FORM);
                intent.putExtra(ContextVS.USER_KEY, userVS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.delete_item:
                deleteContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}