package org.votingsystem.activity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import static org.votingsystem.util.LogUtils.LOGD;


public class DNIeSigningActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

	public static final String TAG = DNIeSigningActivity.class.getSimpleName();

    private String serviceCaller;
    private TypeVS operation;
	private String textToSign;
	private String toUser;
	private String msgSubject;
	private NfcAdapter myNfcAdapter;
	private Tag tagFromIntent;

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
				SMIMEMessage accessRequest = AppVS.getInstance().signMessageWithDNIe(tagFromIntent,
                        PrefUtils.getDNIeCAN(), DNIeSigningActivity.this,
                        toUser, textToSign, msgSubject, AppVS.getInstance().getTimeStampServiceURL());
                responseVS = new ResponseVS(ResponseVS.SC_OK, accessRequest);
                responseVS.setTypeVS(operation);
                AppVS.getInstance().setDNIeSignature(serviceCaller,responseVS);
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
            if(responseVS != null) finish();
        }
    }

	private void showMessageDialog(String caption, String message) {
		ProgressDialogFragment.hide(getSupportFragmentManager());
		MessageDialogFragment.showDialog(caption, message, getSupportFragmentManager());
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dnie_voting_activity);
		myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		myNfcAdapter.setNdefPushMessage(null, this);
		myNfcAdapter.setNdefPushMessageCallback(null, this);
        serviceCaller = getIntent().getExtras().getString(ContextVS.CALLER_KEY);
        operation = (TypeVS) getIntent().getExtras().getSerializable(ContextVS.TYPEVS_KEY);
        textToSign = getIntent().getExtras().getString(ContextVS.MESSAGE_CONTENT_KEY);
        toUser = getIntent().getExtras().getString(ContextVS.USER_KEY);
        msgSubject = getIntent().getExtras().getString(ContextVS.MESSAGE_SUBJECT_KEY);
		String message = getIntent().getExtras().getString(ContextVS.MESSAGE_KEY);
		if(message != null) {
			((TextView)findViewById(R.id.vote)).setText(message);
		}
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
			new SignWithDNIeTask().execute();
		}
	}

}