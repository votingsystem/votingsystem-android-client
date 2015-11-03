package org.votingsystem.activity;

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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.fragment.EventVSFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;

import java.io.IOException;

import static org.votingsystem.util.LogUtils.LOGD;


public class DNIeVotingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

	public static final String TAG = DNIeVotingActivity.class.getSimpleName();

	private String broadCastId = DNIeVotingActivity.class.getSimpleName();

	private VoteVSHelper voteVSHelper;
	private NfcAdapter myNfcAdapter;
	private NfcA exNfcA;
	private NfcB exNfcB;
	private IsoDep exIsoDep;
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

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) {
			LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
	}

	public class SendVoteTask extends AsyncTask<Void, Integer, ResponseVS> {

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
				Log.d(TAG, "sendVote");
				if(AppVS.getInstance().getControlCenter() == null) {
					ControlCenterDto controlCenter = AppVS.getInstance().getActorVS(ControlCenterDto.class,
							voteVSHelper.getEventVS().getControlCenter().getServerURL());
					AppVS.getInstance().setControlCenter(controlCenter);
				}
				AccessRequestDto requestDto = voteVSHelper.getAccessRequest();
				String subject = AppVS.getInstance().getString(R.string.request_msg_subject,
						voteVSHelper.getEventVS().getId());
				String eventSubject = StringUtils.truncate(voteVSHelper.getSubject(), 50);
				SMIMEMessage accessRequest = AppVS.getInstance().signMessageWithDNIe(tagFromIntent,
						PrefUtils.getDNIeCAN(), DNIeVotingActivity.this,
						AppVS.getInstance().getAccessControl().getName(),
						JSON.writeValueAsString(requestDto),
						subject, AppVS.getInstance().getCurrencyServer().getTimeStampServiceURL());
				responseVS = new VoteSender(voteVSHelper, accessRequest).call();
				if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
					voteVSHelper = (VoteVSHelper)responseVS.getData();
					responseVS.setCaption(getString(R.string.vote_ok_caption)).
							setNotificationMessage(getString(
									R.string.vote_ok_msg, eventSubject,
									voteVSHelper.getVote().getOptionSelected().getContent()));
				} else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
					responseVS.setCaption(getString(R.string.access_request_repeated_caption)).
							setNotificationMessage(getString( R.string.access_request_repeated_msg,
									eventSubject));
				} else {
					responseVS.setCaption(getString(R.string.vote_error_caption)).
							setNotificationMessage(
									Html.fromHtml(responseVS.getMessage()).toString());
				}
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
			if(responseVS != null) {
				try {
					Intent intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
					intent.putExtra(ContextVS.FRAGMENT_KEY, EventVSFragment.class.getName());
					intent.putExtra(ContextVS.VOTE_KEY, voteVSHelper);
					intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
					startActivity(intent);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			ProgressDialogFragment.hide(getSupportFragmentManager());
			myHandler.post(askForRead);
        }
    }
	@Override
	public void onBackPressed() {
		ProgressDialogFragment.hide(getSupportFragmentManager());
		super.onBackPressed();
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
		voteVSHelper = (VoteVSHelper) getIntent().getSerializableExtra(ContextVS.VOTE_KEY);
		String message = getIntent().getExtras().getString(ContextVS.MESSAGE_KEY);
		if(message != null) {
			((TextView)findViewById(R.id.vote)).setText(message);
		}
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
		Bundle options = new Bundle();
		//30 secs to check NFC reader
		//options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
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
			new SendVoteTask().execute();
		}
	}

}