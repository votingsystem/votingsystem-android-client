package org.votingsystem.activity;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.jce.PrincipalUtil;
import org.bouncycastle2.util.Store;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.cms.DNIeContentSigner;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.UserCertificationRequestDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.ui.DNIePasswordDialog;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.crypto.CMSUtils;
import org.votingsystem.util.crypto.CertificationRequest;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.ui.passwordcallback.DNIeDialogManager;

import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ID_CardNFCReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

	public static final String TAG = ID_CardNFCReaderActivity.class.getSimpleName();

	public static final String CERT_AUTENTICATION = "CertAutenticacion";
	public static final String CERT_SIGN          = "CertFirmaDigital";

	public static final int MODE_UPDATE_USER_DATA = 0;
	public static final int MODE_PASSWORD_REQUEST = 1;

	private String textToSign;
	private String toUser;
	private String dnieCAN;
	private NfcAdapter myNfcAdapter;
	private Tag tagFromIntent;
	private boolean requestCsr;
	private int activityMode;
    private char[] accessModePassw;

	private Handler myHandler = new Handler();

	final Runnable newRead = new Runnable() {
		public void run() {
			((TextView)findViewById(R.id.msg)).setText(R.string.process_msg_lectura);
			((ImageView)findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_peq);
		}
	};

	final Runnable askForRead = new Runnable() {
		public void run() {
			((ImageView)findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_grey_peq);
			((TextView)findViewById(R.id.msg)).setText(R.string.op_dgtinit);
		}
	};

	private void showMessageDialog(String caption, String message) {
		ProgressDialogFragment.hide(getSupportFragmentManager());
		MessageDialogFragment.showDialog(caption, message, getSupportFragmentManager());
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.idcard_nfc_reader_activity);
		myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		myNfcAdapter.setNdefPushMessage(null, this);
		myNfcAdapter.setNdefPushMessageCallback(null, this);
        textToSign = getIntent().getExtras().getString(ContextVS.MESSAGE_CONTENT_KEY);
        toUser = getIntent().getExtras().getString(ContextVS.USER_KEY);
		if(toUser == null) toUser = getString(R.string.voting_system_lbl);
		activityMode = getIntent().getExtras().getInt(ContextVS.MODE_KEY, -1);
		String message = getIntent().getExtras().getString(ContextVS.MESSAGE_KEY);
		requestCsr = getIntent().getExtras().getBoolean(ContextVS.CSR_KEY, false);
		if(message != null) {
			((TextView)findViewById(R.id.topMsg)).setText(message);
		} else findViewById(R.id.topMsg).setVisibility(View.GONE);
        accessModePassw = (char[]) getIntent().getExtras().getSerializable(ContextVS.PASSWORD_KEY);
		dnieCAN = PrefUtils.getDNIeCAN();
    }

	@Override public void onResume() {
		super.onResume();
        enableNFCReaderMode();
	}

	@Override public void onPause() {
		super.onPause();
        LOGD(TAG, "disableNFCReaderMode");
        disableNFCReaderMode();
	}

    private void enableNFCReaderMode () {
        LOGD(TAG, "enableNFCReaderMode");
        Bundle options = new Bundle();
        //30 secs to check NFC reader
        //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
        myNfcAdapter.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B,
                options);
    }

    private void disableNFCReaderMode() {
        LOGD(TAG, "disableNFCReaderMode");
        myNfcAdapter.disableReaderMode(this);
    }

	Runnable cardOperation = new Runnable() {
		@Override public void run() {
			char[] password = null;
			if(accessModePassw != null) password = PrefUtils.getProtectedPassword(accessModePassw);
			ResponseVS responseVS = null;
			try  {
				DnieProvider p = new DnieProvider();
				p.setProviderTag(tagFromIntent);
				p.setProviderCan(dnieCAN);
				Security.insertProviderAt(p, 1);
				//Deactivate fastmode
				//System.setProperty("es.gob.jmulticard.fastmode", "false");
				DNIePasswordDialog passwordDialog = new DNIePasswordDialog(ID_CardNFCReaderActivity.this, password, true);
				DNIeDialogManager.setDialogUIHandler(passwordDialog);
				KeyStore ksUserDNIe = KeyStore.getInstance("MRTD");
				ksUserDNIe.load(null, null);
				//force load real certs
				ksUserDNIe.getKey(CERT_SIGN, null);
				X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);

				if(MODE_PASSWORD_REQUEST == activityMode) {
					responseVS = ResponseVS.OK().setMessageBytes(new String(passwordDialog.getPassword()).getBytes());
				} else {
					UserDto appUser = null;
					if(MODE_UPDATE_USER_DATA == activityMode) {
						UserDto userFromCert = UserDto.getUser(PrincipalUtil.getSubjectX509Principal(userCert));
						appUser = PrefUtils.getAppUser();
						appUser.setNIF(userFromCert.getNIF()).setX509Certificate(userCert);
						appUser.setFirstName(userFromCert.getFirstName());
						appUser.setLastName(userFromCert.getLastName());
						if(userFromCert.getCountry() != null) appUser.setCountry(userFromCert.getCountry());
						if(userFromCert.getCn() != null) appUser.setCn(userFromCert.getCn());
						if(requestCsr) {
							CertificationRequest certificationRequest = CertificationRequest.getUserRequest(
									SIGNATURE_ALGORITHM, PROVIDER, appUser.getNIF(), appUser.getEmail(),
									appUser.getPhone(), PrefUtils.getDeviceId(), appUser.getFirstName(),
									appUser.getLastName(), DeviceDto.Type.MOBILE);
							byte[] csrBytes = certificationRequest.getCsrPEM();
							UserCertificationRequestDto userCertificationRequestDto =
									new UserCertificationRequestDto(appUser.getAddress(), csrBytes);
							certificationRequest.setUserCertificationRequestDto(userCertificationRequestDto);
							PrefUtils.putCsrRequest(certificationRequest);
							textToSign = JSON.writeValueAsString(userCertificationRequestDto);
						} else textToSign = JSON.writeValueAsString(appUser);
					}
					PrivateKey privateKey = (PrivateKey) ksUserDNIe.getKey(CERT_SIGN, null);
					Certificate[] chain = ksUserDNIe.getCertificateChain(CERT_SIGN);
					Store cerStore = new JcaCertStore(Arrays.asList(chain));

					byte[] contentToSign = textToSign.getBytes();
					TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(SIGNATURE_ALGORITHM,
							contentToSign);
					CMSSignedData cmsSignedData = DNIeContentSigner.signData(privateKey, userCert,
							cerStore, contentToSign, timeStampToken);
					responseVS = new ResponseVS(ResponseVS.SC_OK, new CMSSignedMessage(cmsSignedData));
					if(MODE_UPDATE_USER_DATA == activityMode) {
						PrefUtils.putAppUser(appUser);
						responseVS.setMessageBytes(new String(passwordDialog.getPassword()).getBytes());
					}
					if(accessModePassw != null && password == null) {
						CryptoDeviceAccessMode accessMode = PrefUtils.getCryptoDeviceAccessMode();
						PrefUtils.putProtectedPassword(accessMode.getMode(), accessModePassw,
								passwordDialog.getPassword());
					}
				}
				myHandler.post(askForRead);
				ProgressDialogFragment.hide(getSupportFragmentManager());
				if(responseVS != null) {
					Intent resultIntent = new Intent();
					resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
					setResult(Activity.RESULT_OK, resultIntent);
					finish();
				}
			} catch (NullPointerException ex) {
				ex.printStackTrace();
				showMessageDialog(getString(R.string.error_lbl),
						getString(R.string.dnie_connection_null_error_msg));
			} catch (Exception ex) {
				ex.printStackTrace();
				showMessageDialog(getString(R.string.error_lbl),
						getString(R.string.dnie_connection_error_msg));
			}
		}
	};

	@Override
	public void onTagDiscovered(Tag tag) {
		LOGD(TAG, "onTagDiscovered");
		tagFromIntent = tag;
        NfcA exNfcA	 = NfcA.get(tagFromIntent);
        NfcB exNfcB	 = NfcB.get(tagFromIntent);
        IsoDep exIsoDep = IsoDep.get(tagFromIntent);
		if( (exNfcA != null) || (exNfcB != null) || (exIsoDep != null)) {
			myHandler.post(newRead);
			ProgressDialogFragment.showDialog(getString(R.string.dnie_sign_progress_caption),
					getString(R.string.dnie_sign_connecting_msg),
					getSupportFragmentManager());
			new Thread(cardOperation).start();
		}
	}

}