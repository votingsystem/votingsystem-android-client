package org.votingsystem.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.votingsystem.android.R;
import org.votingsystem.dto.AddressVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.Country;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDataFormFragment extends Fragment {

	public static final String TAG = UserDataFormFragment.class.getSimpleName();

    private EditText phoneText;
    private EditText mailText;
    private EditText address;
    private EditText postal_code;
    private EditText city;
    private EditText province;
    private Spinner country_spinner;
    private UserVSDto userVSDto;


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "progressVisible: ");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.user_data_form, container, false);
        getActivity().setTitle(getString(R.string.user_data_lbl));
        mailText = (EditText)rootView.findViewById(R.id.mail_edit);
        phoneText = (EditText)rootView.findViewById(R.id.phone_edit);
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
            userVSDto = PrefUtils.getSessionUserVS();
            AddressVS addressVS = userVSDto.getAddress();
            if(addressVS != null) {
                address.setText(addressVS.getName());
                postal_code.setText(addressVS.getPostalCode());
                city.setText(addressVS.getCity());
                province.setText(addressVS.getProvince());
                country_spinner.setSelection(addressVS.getCountry().getPosition());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootView;
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        super.onPause();
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
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(city.getWindowToken(), 0);
      	if (validateForm ()) {
            String deviceId = PrefUtils.getApplicationId();
            LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
            userVSDto = new UserVSDto();
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
      	}
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getActivity().getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getActivity().getSupportFragmentManager());
    }

    private boolean validateForm () {
        address.setError(null);
        postal_code.setError(null);
        city.setError(null);
        province.setError(null);
        phoneText.setError(null);
        mailText.setError(null);
        if(TextUtils.isEmpty(phoneText.getText().toString())){
            phoneText.setError(getString(R.string.phone_missing_msg));
            return false;
        }
        if(TextUtils.isEmpty(mailText.getText().toString())){
            mailText.setError(getString(R.string.mail_missing_msg));
            return false;
        }
        return true;
    }

    public class DataSender extends AsyncTask<String, String, ResponseVS> {

        public DataSender() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.connecting_caption), getString(R.string.wait_msg)); }

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