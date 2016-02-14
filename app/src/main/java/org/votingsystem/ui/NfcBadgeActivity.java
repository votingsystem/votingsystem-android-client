package org.votingsystem.ui;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

import java.util.Arrays;

import static org.votingsystem.util.LogUtils.LOGD;


public class NfcBadgeActivity extends Activity {

    private static final String TAG = NfcBadgeActivity.class.getSimpleName();
    private static final String URL_PREFIX = "votingsystem.org";
    // For debug purposes
    public static final String ACTION_SIMULATE = "org.votingsystem.currency.ACTION_SIMULATE";

    @Override
    public void onStart() {
        super.onStart();
        // Check for NFC data
        Intent i = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            LOGD(TAG, "Badge detected");
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Scan another attendee's badge.
             * CATEGORY:  'NFC'
             * ACTION:    'Read'
             * LABEL:     'Badge'. Badge info IS NOT collected.
             * [/ANALYTICS]
             */

            readTag((Tag) i.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        } else if (ACTION_SIMULATE.equals(i.getAction())) {
            String simulatedUrl = i.getDataString();
            LOGD(TAG, "Simulating badge scanning with URL " + simulatedUrl);
            // replace https by Unicode character 4, as per normal badge encoding rules
            recordBadge(simulatedUrl.replace("https://", "\u0004"));
        } else {
            LOGD(TAG, "Invalid action in Intent to NfcBadgeActivity: " + i.getAction());
        }
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void readTag(Tag t) {
        byte[] id = t.getId();

        // get NDEF tag details
        Ndef ndefTag = Ndef.get(t);

        // get NDEF message details
        NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
        if (ndefMesg == null) {
            return;
        }
        NdefRecord[] ndefRecords = ndefMesg.getRecords();
        if (ndefRecords == null) {
            return;
        }
        for (NdefRecord record : ndefRecords) {
            short tnf = record.getTnf();
            String type = new String(record.getType());
            if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type.getBytes(), NdefRecord.RTD_URI)) {
                String url = new String(record.getPayload());
                recordBadge(url);
            }
        }
    }

    private void recordBadge(String url) {
        LOGD(TAG, "Recording badge, URL " + url);
    }



}
