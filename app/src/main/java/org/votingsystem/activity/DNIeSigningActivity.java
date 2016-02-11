package org.votingsystem.activity;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.util.Store;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.signature.smime.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.ui.DNIePasswordDialog;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.ui.passwordcallback.DNIeDialogManager;

import static org.votingsystem.util.LogUtils.LOGD;


public class DNIeSigningActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

	public static final String TAG = DNIeSigningActivity.class.getSimpleName();

	public static final String CERT_AUTENTICATION = "CertAutenticacion";
	public static final String CERT_SIGN = "CertFirmaDigital";


	public static final int MODE_PASSWORD_REQUEST  = 0;
	public static final int MODE_SIGN_DOCUMENT     = 1;


    private TypeVS operation;
	private String textToSign;
	private String toUser;
	private String msgSubject;
	private NfcAdapter myNfcAdapter;
	private Tag tagFromIntent;
	private int acitivityMode;
    private char[] patternLock;

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


	public class SignWithDNIeTask extends AsyncTask<Void, Integer, ResponseVS> {

		private char[] password;

		public SignWithDNIeTask(char[] password) {
			this.password = password;
		}

		@Override
	    protected void onPreExecute()  {
			myHandler.post(newRead);
			ProgressDialogFragment.showDialog(getString(R.string.dnie_sign_progress_caption),
					getString(R.string.dnie_sign_connecting_msg),
					getSupportFragmentManager());
	    }
	    
		@Override
	    protected ResponseVS doInBackground(Void... arg0) {
			ResponseVS responseVS = null;
			try  {
				DnieProvider p = new DnieProvider();
				p.setProviderTag(tagFromIntent);
				p.setProviderCan(PrefUtils.getDNIeCAN());
				Security.insertProviderAt(p, 1);
				//Deactivate fastmode
				System.setProperty("es.gob.jmulticard.fastmode", "false");
				DNIePasswordDialog myFragment = new DNIePasswordDialog(DNIeSigningActivity.this, password, true);
				DNIeDialogManager.setDialogUIHandler(myFragment);
				KeyStore ksUserDNIe = KeyStore.getInstance("MRTD");
				ksUserDNIe.load(null, null);
				//force load real certs
				ksUserDNIe.getKey(CERT_SIGN, null);
				X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);
				if(MODE_PASSWORD_REQUEST == acitivityMode) {
					return ResponseVS.OK().setMessageBytes(new String(myFragment.getPassword()).getBytes());
				}
				PrivateKey privateKey = (PrivateKey) ksUserDNIe.getKey(CERT_SIGN, null);
				Certificate[] chain = ksUserDNIe.getCertificateChain(CERT_SIGN);
				Store cerStore = new JcaCertStore(Arrays.asList(chain));

				SMIMEMessage smimeMessage = DNIeContentSigner.getSMIME(privateKey, userCert, cerStore,
						toUser, textToSign, msgSubject);
				SMIMEMessage accessRequest = new MessageTimeStamper(smimeMessage,
						AppVS.getInstance().getTimeStampServiceURL()).call();

                responseVS = new ResponseVS(ResponseVS.SC_OK, accessRequest);
                responseVS.setTypeVS(operation);
			} catch (IOException ex) {
				ex.printStackTrace();
				showMessageDialog(getString(R.string.error_lbl), getString(R.string.dnie_connection_error_msg));
			} catch (Exception ex) {
				ex.printStackTrace();
				showMessageDialog(getString(R.string.error_lbl), ex.getMessage());
			}
			return responseVS;
        }

	    @Override
	    protected void onPostExecute(ResponseVS responseVS) {
			LOGD(TAG, "onPostExecute");
            myHandler.post(askForRead);
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if(responseVS != null) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
        }
    }

	private void showMessageDialog(String caption, String message) {
		ProgressDialogFragment.hide(getSupportFragmentManager());
		MessageDialogFragment.showDialog(caption, message, getSupportFragmentManager());
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dnie_voting_activity);
		myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		myNfcAdapter.setNdefPushMessage(null, this);
		myNfcAdapter.setNdefPushMessageCallback(null, this);
        operation = (TypeVS) getIntent().getExtras().getSerializable(ContextVS.OPERATIONVS_KEY);
        textToSign = getIntent().getExtras().getString(ContextVS.MESSAGE_CONTENT_KEY);
        toUser = getIntent().getExtras().getString(ContextVS.USER_KEY);
        msgSubject = getIntent().getExtras().getString(ContextVS.MESSAGE_SUBJECT_KEY);
		acitivityMode = getIntent().getExtras().getInt(ContextVS.MODE_KEY, -1);
		String message = getIntent().getExtras().getString(ContextVS.MESSAGE_KEY);
		if(message != null) {
			((TextView)findViewById(R.id.topMsg)).setText(message);
		} else findViewById(R.id.topMsg).setVisibility(View.GONE);
        patternLock = (char[]) getIntent().getExtras().getSerializable(ContextVS.LOCK_PATTERN_KEY);
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

	@Override
	public void onTagDiscovered(Tag tag) {
		LOGD(TAG, "onTagDiscovered");
		tagFromIntent = tag;
        NfcA exNfcA	 = NfcA.get(tagFromIntent);
        NfcB exNfcB	 = NfcB.get(tagFromIntent);
        IsoDep exIsoDep = IsoDep.get(tagFromIntent);
		if( (exNfcA != null) || (exNfcB != null) || (exIsoDep != null)) {
            if(patternLock != null) {
                char[] password = PrefUtils.getLockPatterProtectedPassword(new String(patternLock));
                new SignWithDNIeTask(password).execute();
            } else new SignWithDNIeTask(null).execute();
		}
	}

}