package org.votingsystem.callable;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<SMIMEMessage> {
    
	public static final String TAG = MessageTimeStamper.class.getSimpleName();
    
    private SMIMEMessage smimeMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private String timeStampServiceURL;
      
    public MessageTimeStamper (SMIMEMessage smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
    }

    public MessageTimeStamper (SMIMEMessage smimeMessage, String timeStampServiceURL) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.timeStampServiceURL = timeStampServiceURL;
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest) throws Exception {
        this.timeStampRequest = timeStampRequest;
    }
        
    public MessageTimeStamper (String timeStampDigestAlgorithm, 
    		byte[] digestToTimeStamp) throws Exception {
    	TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        this.timeStampRequest = reqgen.generate(
        		timeStampDigestAlgorithm, digestToTimeStamp);
    }
        
    @Override public SMIMEMessage call() throws Exception {
        //byte[] base64timeStampRequest = Base64.encode(timeStampRequest.getEncoded());
        if(timeStampServiceURL == null) timeStampServiceURL =
                AppVS.getInstance().getTimeStampServiceURL();
        ResponseVS responseVS = HttpHelper.sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, timeStampServiceURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            timeStampToken= new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
            X509Certificate timeStampCert = AppVS.getInstance().getTimeStampCert();
                /* -> Android project config problem
                 * SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);*/
            timeStampToken.validate(timeStampCert, ContextVS.PROVIDER);/**/
            if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
        } else throw new ValidationExceptionVS(
                AppVS.getInstance().getString(R.string.timestamp_service_error_caption));
        return smimeMessage;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

}