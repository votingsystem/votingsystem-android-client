package org.votingsystem.callable;

import android.os.AsyncTask;

import org.votingsystem.signature.smime.SMIMEMessage;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * To avoid NetworkOnMainThreadException exception timestamping the signature
 */
public class SignerTask  extends AsyncTask<String, String, SMIMEMessage> {

    public static interface Listener {
        public SMIMEMessage sign();
        public void processResult(SMIMEMessage smimeMessage);
    }

    Listener listener;

    public SignerTask(Listener listener) {
        this.listener = listener;
    }


    @Override protected SMIMEMessage doInBackground(String... urls) {
        return listener.sign();
    }

    @Override protected void onPostExecute(SMIMEMessage smimeMessage) {
        listener.processResult(smimeMessage);
    }
}