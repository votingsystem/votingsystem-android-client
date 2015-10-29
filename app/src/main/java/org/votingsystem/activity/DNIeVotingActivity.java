package org.votingsystem.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.ui.DNIePasswordDialog;
import org.votingsystem.util.ContextVS;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;

import de.tsenger.androsmex.data.CANSpecDO;
import de.tsenger.androsmex.data.CANSpecDOStore;
import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.ui.passwordcallback.DNIeDialogManager;

import static org.votingsystem.util.LogUtils.LOGD;


public class DNIeVotingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

	public static final String TAG = DNIeVotingActivity.class.getSimpleName();

	private String broadCastId = DNIeVotingActivity.class.getSimpleName();

	private NfcAdapter myNfcAdapter = null;
	private NfcA exNfcA;
	private NfcB exNfcB;
	private IsoDep exIsoDep;
	private Tag tagFromIntent;
	
	private CANSpecDOStore cansDO;
	private CANSpecDO canDnie;

	private ProgressDialog progressBar;
	private final Handler myHandler = new Handler();

	private static final String AUTH_CERT_ALIAS = "CertAutenticacion";
	private static final String SIGN_CERT_ALIAS = "CertFirmaDigital";

	final Runnable newRead = new Runnable() {
		public void run() {
			// Cambiamos el fondo de pantalla
			findViewById(R.id.fondopantalla).setBackgroundResource(R.drawable.fondo_secundario);
			((TextView)findViewById(R.id.result1)).setText(R.string.process_msg_lectura);
			findViewById(R.id.result1).setVisibility(TextView.VISIBLE);
			((ImageView)findViewById(R.id.resultimg)).setImageResource(R.drawable.dni30_peq);
			findViewById(R.id.resultimg).setVisibility(ImageView.VISIBLE);
			findViewById(R.id.result2).setVisibility(TextView.INVISIBLE);
			findViewById(R.id.resultinfo).setVisibility(TextView.INVISIBLE);
			findViewById(R.id.fab).setVisibility(FloatingActionButton.INVISIBLE);
		}
	};


	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) {
			LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		if( (exNfcA!=null) || (exNfcB!=null) || (exIsoDep!=null) ) {
			new SendVoteTask().execute();
		}
	}

	public class SendVoteTask extends AsyncTask<Void, Integer, Void> {
	    @Override
	    protected void onPreExecute()  {
	    	// Lanzamos el diálogo con el progreso
			myHandler.post(newRead);
		    progressBar = new ProgressDialog(DNIeVotingActivity.this);
		    progressBar.setIndeterminate(true);
		    progressBar.setCancelable(false);
		    progressBar.setTitle("DNIe version 3.0");
		    progressBar.setMessage("Estableciendo conexión...");
			progressBar.show();
	    }
	    
		@Override
	    protected Void doInBackground(Void... arg0) {
			try  {
				//TODO
				LOGD(TAG, "sendVote");
				// Se instancia el proveedor y se añade
				final DnieProvider p = new DnieProvider();
				p.setProviderTag(tagFromIntent);
				// Tag discovered by the activity
				p.setProviderCan("*****");
				Security.insertProviderAt(p, 1);

				// Desactivamos el modod rápido
				System.setProperty("es.gob.jmulticard.fastmode", "false");
				DNIePasswordDialog myFragment = new DNIePasswordDialog(DNIeVotingActivity.this, true);
				DNIeDialogManager.setDialogUIHandler(myFragment);
				KeyStore ksUserDNIe = KeyStore.getInstance("MRTD");
				ksUserDNIe.load(null,null);

				// Actualizamos la BBDD de los CAN para añadir estos datos si no los tuviéramos
				/*if(canDnie.getUserNif().length()==0){
					String certSubject = ((X509Certificate) ksUserDNIe.getCertificate(SIGN_CERT_ALIAS)).getSubjectDN().toString();
					CANSpecDO newCan = new CANSpecDO(canDnie.getCanNumber(),
							certSubject.substring((certSubject.indexOf("CN=") + 3)),
							certSubject.substring(certSubject.indexOf("NIF ") + 4));
					cansDO.delete(canDnie);
					cansDO.save(newCan);
				}*/
				//Forzamos a cargar los certificados reales pidiendo la clave
				ksUserDNIe.getKey(SIGN_CERT_ALIAS, null);
				ksUserDNIe.getCertificate(SIGN_CERT_ALIAS).getEncoded();

				PrivateKey privateKey = (PrivateKey) ksUserDNIe.getKey(SIGN_CERT_ALIAS, null);
				Certificate[] chain = ksUserDNIe.getCertificateChain(SIGN_CERT_ALIAS);

				Signature signature = Signature.getInstance("SHA1withRSA", "DNIeJCAProvider");
				signature.initSign(privateKey);
				signature.update("CertVoteRequest".getBytes());
				byte[] signatureBytes = signature.sign();
				LOGD(TAG, "signatureBytes: " + new String(signatureBytes));
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
        }

	    @Override
	    protected void onPostExecute(Void result) {
			LOGD(TAG, "onPostExecute");
			progressBar.dismiss();
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.nfc_signature);
		cansDO 	= new CANSpecDOStore(this);
		myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		myNfcAdapter.setNdefPushMessage(null, this);
		myNfcAdapter.setNdefPushMessageCallback(null, this);
		VoteVSHelper voteVSHelper = (VoteVSHelper) getIntent().getSerializableExtra(ContextVS.VOTE_KEY);
		LOGD(TAG, "onCreate - voteVSHelper: " + voteVSHelper);
    }

	@Override public void onResume() {
		super.onResume();
		enableNFCReaderMode();
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
				broadcastReceiver, new IntentFilter(broadCastId));
	}

	@Override public void onPause() {
		super.onPause();
		disableNFCReaderMode();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	private boolean enableNFCReaderMode () {
		Log.d(TAG, "enableNFCReaderMode");
		// Ponemos en 30 segundos el tiempo de espera para comprobar presencia de lectores NFC
		Bundle options = new Bundle();
		options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
		myNfcAdapter.enableReaderMode(DNIeVotingActivity.this,
				this,
				NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
						NfcAdapter.FLAG_READER_NFC_A |
						NfcAdapter.FLAG_READER_NFC_B,
				options);
		return true;
	}

	private boolean disableNFCReaderMode() {
		Log.d(TAG, "disableNFCReaderMode");
		myNfcAdapter.disableReaderMode(DNIeVotingActivity.this);
		return true;
	}

	@Override
	public void onTagDiscovered(Tag tag) {
		Log.d(TAG, "onTagDiscovered");
		tagFromIntent = tag;
		exNfcA	 = NfcA.get(tagFromIntent);
		exNfcB	 = NfcB.get(tagFromIntent);
		exIsoDep = IsoDep.get(tagFromIntent);
		if( (exNfcA!=null) || (exNfcB!=null) || (exIsoDep!=null)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					SendVoteTask newTask = new SendVoteTask();
					newTask.execute();
				}
			});
		}
	}

}