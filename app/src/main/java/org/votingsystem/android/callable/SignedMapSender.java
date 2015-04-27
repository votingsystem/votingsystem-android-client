package org.votingsystem.android.callable;

import org.votingsystem.android.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;

import static org.votingsystem.util.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedMapSender implements Callable<ResponseVS> {

    public static final String TAG = SignedMapSender.class.getSimpleName();

    private SMIMEMessage smimeMessage = null;
    private AppVS appVS = null;
    private String fromUser = null;
    private String toUser = null;
    private String subject = null;
    private String signedFileName = null;
    private String textToSign = null;
    private Map<String, Object> mapToSend;
    private String serviceURL = null;

    public SignedMapSender(String fromUser, String toUser, String textToSign,
            Map<String, Object> mapToSend, String subject, Header header, String serviceURL,
            String signedFileName, AppVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.mapToSend = mapToSend;
        this.subject = subject;
        this.appVS = context;
        this.serviceURL = serviceURL;
        this.signedFileName = signedFileName;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "serviceURL: " + serviceURL);
        ResponseVS responseVS = null;
        try {
            KeyStore.PrivateKeyEntry keyEntry = appVS.getUserPrivateKey();
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyEntry.getPrivateKey(),
                    keyEntry.getCertificateChain(), SIGNATURE_ALGORITHM, ANDROID_PROVIDER);
            smimeMessage = signedMailGenerator.getSMIME(fromUser, toUser,textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, appVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                responseVS.setCaption(appVS.getString(R.string.timestamp_service_error_caption));
                responseVS.setNotificationMessage(responseVS.getMessage());
                return responseVS;
            }
            mapToSend.put(signedFileName, timeStamper.getSMIME().getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend, serviceURL);
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(appVS.getString(R.string.exception_lbl),
                    appVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, appVS);
        } finally {return responseVS;}
    }

}
