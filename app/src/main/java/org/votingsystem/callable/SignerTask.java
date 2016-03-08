package org.votingsystem.callable;

import android.os.AsyncTask;

import org.votingsystem.cms.CMSSignedMessage;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * To avoid NetworkOnMainThreadException exception timestamping the signature
 */
public class SignerTask  extends AsyncTask<String, String, CMSSignedMessage> {

    public static interface Listener {
        public CMSSignedMessage sign();
        public void processResult(CMSSignedMessage cmsMessage);
    }

    Listener listener;

    public SignerTask(Listener listener) {
        this.listener = listener;
    }


    @Override protected CMSSignedMessage doInBackground(String... urls) {
        return listener.sign();
    }

    @Override protected void onPostExecute(CMSSignedMessage cmsMessage) {
        listener.processResult(cmsMessage);
    }
}