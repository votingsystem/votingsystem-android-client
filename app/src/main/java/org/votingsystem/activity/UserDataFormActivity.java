package org.votingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.Address;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.UserDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.model.Currency;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.Country;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.crypto.CertificationRequest;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserDataFormActivity extends AppCompatActivity {

	public static final String TAG = UserDataFormActivity.class.getSimpleName();

    public static final int RC_SIGN_USER_DATA            = 0;
    public static final int RC_REQUEST_ACCESS_MODE_PASSW = 1;

    private EditText canText;
    private EditText phoneText;
    private EditText mailText;
    private EditText addressText;
    private EditText postal_code;
    private EditText city;
    private EditText province;
    private Spinner country_spinner;
    private UserDto userDto;
    private char[] password;
    private CryptoDeviceAccessMode.Mode accessMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreate", "onCreate");
        setContentView(R.layout.user_data_form);
        setTitle(getString(R.string.user_data_lbl));
        TextView messageTextView = (TextView)findViewById(R.id.message);
        SpannableStringBuilder aboutBody = new SpannableStringBuilder();
        aboutBody.append(Html.fromHtml(getString(R.string.can_dialog_body)));
        messageTextView.setText(aboutBody);
        messageTextView.setMovementMethod(new LinkMovementMethod());
        mailText = (EditText)findViewById(R.id.mail_edit);
        phoneText = (EditText)findViewById(R.id.phone_edit);
        canText = (EditText)findViewById(R.id.can);
        addressText = (EditText)findViewById(R.id.address);
        postal_code = (EditText)findViewById(R.id.postal_code);
        city = (EditText)findViewById(R.id.location);
        province = (EditText)findViewById(R.id.province);
        country_spinner = (Spinner)findViewById(R.id.country_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Country.getListValues());
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        country_spinner.setAdapter(dataAdapter);
        Country country = Country.valueOf(getResources().getConfiguration().locale.getLanguage().
                toUpperCase());
        if(country != null) country_spinner.setSelection(country.getPosition());
        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        try {
            String can = PrefUtils.getDNIeCAN();
            if(can != null) canText.setText(can);
            userDto = PrefUtils.getAppUser();
            if(userDto == null) {//first run
                if(savedInstanceState == null) MessageDialogFragment.showDialog(ResponseVS.SC_OK,
                        getString(R.string.msg_lbl), getString(R.string.first_run_msg), getSupportFragmentManager());

            } else {
                phoneText.setText(userDto.getPhone());
                mailText.setText(userDto.getEmail());
                Address address = userDto.getAddress();
                if(address != null) {
                    addressText.setText(address.getName());
                    postal_code.setText(address.getPostalCode());
                    city.setText(address.getCity());
                    province.setText(address.getProvince());
                    country_spinner.setSelection(address.getCountry().getPosition());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(savedInstanceState != null) {
            password = savedInstanceState.getCharArray(ContextVS.PASSWORD_KEY);
            accessMode = (CryptoDeviceAccessMode.Mode) savedInstanceState.getSerializable(ContextVS.MODE_KEY);
        }
    }


    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
      	if (validateForm ()) {
            String deviceId = PrefUtils.getDeviceId();
            LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
            if(userDto == null) userDto = new UserDto();
            userDto.setPhone(phoneText.getText().toString());
            userDto.setEmail(mailText.getText().toString());
            Address address = new Address();
            if(!TextUtils.isEmpty(addressText.getText().toString()))
                address.setName(addressText.getText().toString());
            if(!TextUtils.isEmpty(postal_code.getText().toString()))
                address.setPostalCode(postal_code.getText().toString());
            if(!TextUtils.isEmpty(city.getText().toString()))
                address.setCity(city.getText().toString());
            if(!TextUtils.isEmpty(province.getText().toString()))
                address.setProvince(province.getText().toString());
            address.setCountry(Country.getByPosition(country_spinner.getSelectedItemPosition()));

            userDto.setAddress(address);
            PrefUtils.putAppUser(userDto);

            String addressStr = address.getName() == null ? "":address.getName();
            String postalCode = address.getPostalCode() == null ? "":address.getPostalCode();
            String city = address.getCity() == null ? "":address.getCity();
            String province = address.getProvince() == null ? "":address.getProvince();
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.user_data_form_lbl),
                    getString(R.string.user_data_confirm_msg, canText.getText().toString(),
                            userDto.getPhone(),
                            userDto.getEmail(), addressStr, postalCode, city, province,
                            address.getCountry().getName()), this);
            builder.setPositiveButton(getString(R.string.continue_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            PrefUtils.putDNIeCAN(canText.getText().toString());
                            Intent intent = null;
                            CryptoDeviceAccessMode passwordAccessMode = PrefUtils.getCryptoDeviceAccessMode();
                            if(passwordAccessMode != null) {
                                switch (passwordAccessMode.getMode()) {
                                    case PATTER_LOCK:
                                        intent = new Intent(UserDataFormActivity.this, PatternLockActivity.class);
                                        break;
                                    case PIN:
                                        intent = new Intent(UserDataFormActivity.this, PinActivity.class);
                                        break;
                                }
                                intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_VALIDATE_INPUT);
                                startActivityForResult(intent, RC_REQUEST_ACCESS_MODE_PASSW);
                            } else {
                                intent = new Intent(UserDataFormActivity.this,
                                        CryptoDeviceAccessModeSelectorActivity.class);
                                startActivityForResult(intent, RC_REQUEST_ACCESS_MODE_PASSW);
                            }
                        }
                    });
            UIUtils.showMessageDialog(builder);
      	}
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    private boolean validateForm () {
        canText.setError(null);
        phoneText.setError(null);
        mailText.setError(null);
        addressText.setError(null);
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
        if(TextUtils.isEmpty(addressText.getText().toString())){
            addressText.setError(getString(R.string.enter_address_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(postal_code.getText().toString())){
            postal_code.setError(getString(R.string.enter_postal_code_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(city.getText().toString())){
            city.setError(getString(R.string.enter_city_error_lbl));
            return false;
        }
        if(TextUtils.isEmpty(province.getText().toString())){
            province.setError(getString(R.string.enter_province_error_lbl));
            return false;
        }
        if(!StringUtils.isValidEmail(mailText.getText())) {
            mailText.setError(getString(R.string.mail_missing_msg));
            return false;
        }
        return true;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray(ContextVS.PASSWORD_KEY, password);
        outState.putSerializable(ContextVS.MODE_KEY, accessMode);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_REQUEST_ACCESS_MODE_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    this.password = new String(responseVS.getMessageBytes()).toCharArray();
                    accessMode = (CryptoDeviceAccessMode.Mode) data.getSerializableExtra(ContextVS.MODE_KEY);
                    Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                    if(PrefUtils.getCryptoDeviceAccessMode() == null) {
                        intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_msg));
                        PrefUtils.putCryptoDeviceAccessMode(new CryptoDeviceAccessMode(accessMode, password));
                    }
                    intent.putExtra(ContextVS.MODE_KEY, ID_CardNFCReaderActivity.MODE_UPDATE_USER_DATA);
                    intent.putExtra(ContextVS.CSR_KEY, true);
                    intent.putExtra(ContextVS.PASSWORD_KEY, password);
                    startActivityForResult(intent, RC_SIGN_USER_DATA);
                }
                break;
            case RC_SIGN_USER_DATA:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    new DataSender(responseVS.getCMS(),
                            new String(responseVS.getMessageBytes()).toCharArray()).execute();
                }
                break;
        }

    }

    public class DataSender extends AsyncTask<String, String, ResponseVS> {

        private CMSSignedMessage cmsMessage;
        private char[] protectedPassword;

        public DataSender(CMSSignedMessage cmsMessage, char[] idCardPassw) {
            this.cmsMessage = cmsMessage;
            this.protectedPassword = idCardPassw;
        }

        @Override protected void onPreExecute() {
                setProgressDialogVisible(true,
                    getString(R.string.connecting_caption), getString(R.string.wait_msg));
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            ResponseVS responseVS = null;
            try {
                responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                        AppVS.getInstance().getCurrencyServer().getCSRSignedWithIDCardServiceURL());
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                CertificationRequest certificationRequest = (CertificationRequest)
                        ObjectUtils.deSerializeObject(PrefUtils.getCsrRequest().getBytes());
                PrivateKey privateKey = certificationRequest.getPrivateKey();
                Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(
                        responseVS.getMessageBytes());
                X509Certificate x509Cert = certificates.iterator().next();
                UserDto user = UserDto.getUser(x509Cert);
                LOGD(TAG, "updateKeyStore - user: " + user.getNIF() +
                        " - certificates.size(): " + certificates.size());
                X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                certificates.toArray(certsArray);
                keyStore.setKeyEntry(ContextVS.USER_CERT_ALIAS, privateKey, null, certsArray);

                char[] newToken = new String(certificationRequest.getUserCertificationRequestDto()
                        .getPlainToken()).toCharArray();
                try {
                    Set<Currency> wallet = PrefUtils.getWallet(password, AppVS.getInstance().getToken());
                    PrefUtils.putWallet(wallet, password, newToken);
                } catch (Exception ex) { ex.printStackTrace();}
                AppVS.getInstance().setToken(newToken);
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, UserDataFormActivity.this);
            } finally {
                PrefUtils.putCsrRequest(null);
                return responseVS;
            }
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            setProgressDialogVisible(false, null, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PrefUtils.putDNIeEnabled(true);
                finish();
            } else {
                MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
            }
        }
    }

}