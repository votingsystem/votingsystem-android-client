package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.activity.DNIeSigningActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.AddressVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.Country;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.UIUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDataFormFragment extends Fragment {

	public static final String TAG = UserDataFormFragment.class.getSimpleName();

    public static final int DNIE_PASSWORD_REQUEST = 0;
    public static final int ACCESS_MODE_SELECT = 0;

    private EditText canText;
    private EditText phoneText;
    private EditText mailText;
    private EditText address;
    private EditText postal_code;
    private EditText city;
    private EditText province;
    private Spinner country_spinner;
    private UserVSDto userVSDto;
    private char[] password;


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.user_data_form, container, false);
        getActivity().setTitle(getString(R.string.user_data_lbl));
        TextView messageTextView = (TextView)rootView.findViewById(R.id.message);
        SpannableStringBuilder aboutBody = new SpannableStringBuilder();
        aboutBody.append(Html.fromHtml(getString(R.string.can_dialog_body)));
        messageTextView.setText(aboutBody);
        messageTextView.setMovementMethod(new LinkMovementMethod());
        mailText = (EditText)rootView.findViewById(R.id.mail_edit);
        phoneText = (EditText)rootView.findViewById(R.id.phone_edit);
        canText = (EditText)rootView.findViewById(R.id.can);
        address = (EditText)rootView.findViewById(R.id.address);
        postal_code = (EditText)rootView.findViewById(R.id.postal_code);
        city = (EditText)rootView.findViewById(R.id.location);
        province = (EditText)rootView.findViewById(R.id.province);
        country_spinner = (Spinner)rootView.findViewById(R.id.country_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, Country.getListValues());
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        country_spinner.setAdapter(dataAdapter);
        Country country = Country.valueOf(getResources().getConfiguration().locale.getLanguage().
                toUpperCase());
        if(country != null) country_spinner.setSelection(country.getPosition());
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        try {
            String can = PrefUtils.getDNIeCAN();
            if(can != null) canText.setText(can);
            userVSDto = PrefUtils.getAppUser();
            if(userVSDto == null) {//first run
                if(savedInstanceState == null) MessageDialogFragment.showDialog(ResponseVS.SC_OK,
                        getString(R.string.msg_lbl), getString(R.string.first_run_msg), getFragmentManager());

            } else {
                phoneText.setText(userVSDto.getPhone());
                mailText.setText(userVSDto.getEmail());
                AddressVS addressVS = userVSDto.getAddress();
                if(addressVS != null) {
                    address.setText(addressVS.getName());
                    postal_code.setText(addressVS.getPostalCode());
                    city.setText(addressVS.getCity());
                    province.setText(addressVS.getProvince());
                    country_spinner.setSelection(addressVS.getCountry().getPosition());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
      	if (validateForm ()) {
            String deviceId = PrefUtils.getApplicationId();
            LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
            if(userVSDto == null) userVSDto = new UserVSDto();
            userVSDto.setPhone(phoneText.getText().toString());
            userVSDto.setEmail(mailText.getText().toString());
            AddressVS addressVS = new AddressVS();
            if(!TextUtils.isEmpty(address.getText().toString()))
                addressVS.setName(address.getText().toString());
            if(!TextUtils.isEmpty(postal_code.getText().toString()))
                addressVS.setPostalCode(postal_code.getText().toString());
            if(!TextUtils.isEmpty(city.getText().toString()))
                addressVS.setCity(city.getText().toString());
            if(!TextUtils.isEmpty(province.getText().toString()))
                addressVS.setProvince(province.getText().toString());
            addressVS.setCountry(Country.getByPosition(country_spinner.getSelectedItemPosition()));

            userVSDto.setAddress(addressVS);
            PrefUtils.putAppUser(userVSDto);

            String address = addressVS.getName() == null ? "":addressVS.getName();
            String postalCode = addressVS.getPostalCode() == null ? "":addressVS.getPostalCode();
            String city = addressVS.getCity() == null ? "":addressVS.getCity();
            String province = addressVS.getProvince() == null ? "":addressVS.getProvince();
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.user_data_form_lbl),
                    getString(R.string.user_data_confirm_msg, canText.getText().toString(),
                            userVSDto.getPhone(),
                            userVSDto.getEmail(), address, postalCode, city, province,
                            addressVS.getCountry().getName()), getActivity());
            builder.setPositiveButton(getString(R.string.continue_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            PrefUtils.putDNIeCAN(canText.getText().toString());
                            Intent intent = new Intent(getActivity(), DNIeSigningActivity.class);
                            intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_msg));
                            intent.putExtra(ContextVS.MODE_KEY, DNIeSigningActivity.MODE_PASSWORD_REQUEST);
                            startActivityForResult(intent, DNIE_PASSWORD_REQUEST);
                        }
                    });
            UIUtils.showMessageDialog(builder);
      	}
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getActivity().getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getActivity().getSupportFragmentManager());
    }

    private boolean validateForm () {
        canText.setError(null);
        phoneText.setError(null);
        mailText.setError(null);
        address.setError(null);
        postal_code.setError(null);
        city.setError(null);
        province.setError(null);
        if(TextUtils.isEmpty(canText.getText().toString())){
            canText.setError(getString(R.string.can_dialog_caption));
            return false;
        }
        if(TextUtils.isEmpty(phoneText.getText().toString())){
            phoneText.setError(getString(R.string.phone_missing_msg));
            return false;
        }
        if(TextUtils.isEmpty(mailText.getText().toString())){
            mailText.setError(getString(R.string.mail_missing_msg));
            return false;
        }
        try {
            InternetAddress emailAddr = new InternetAddress(mailText.getText().toString());
            emailAddr.validate();
        } catch (AddressException ex) {
            mailText.setError(getString(R.string.mail_missing_msg));
            return false;
        }
        return true;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case DNIE_PASSWORD_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    PrefUtils.putDNIeEnabled(true);
                    getActivity().finish();
                }
                break;
        }

    }

    public class DataSender extends AsyncTask<String, String, ResponseVS> {

        public DataSender() { }

        @Override protected void onPreExecute() {
                setProgressDialogVisible(true,
                    getString(R.string.connecting_caption), getString(R.string.wait_msg));
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            String currencyURL = urls[0];
            return HttpHelper.getData(currencyURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {

            setProgressDialogVisible(false, null, null);
        }
    }
}