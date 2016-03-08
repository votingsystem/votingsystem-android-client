package org.votingsystem.callable;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
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
public class MessageTimeStamper implements Callable<CMSSignedMessage> {
    
	public static final String TAG = MessageTimeStamper.class.getSimpleName();
    
    private CMSSignedMessage cmsMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private String timeStampServiceURL;
      
    public MessageTimeStamper (CMSSignedMessage cmsMessage) throws Exception {
        this.cmsMessage = cmsMessage;
        this.timeStampRequest = cmsMessage.getTimeStampRequest();
    }

    public MessageTimeStamper (CMSSignedMessage cmsMessage, String timeStampServiceURL) throws Exception {
        this.cmsMessage = cmsMessage;
        this.timeStampRequest = cmsMessage.getTimeStampRequest();
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
        
    @Override public CMSSignedMessage call() throws Exception {
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
            if(cmsMessage != null) cmsMessage = cmsMessage.addTimeStamp(cmsMessage, timeStampToken);
        } else throw new ValidationExceptionVS(
                AppVS.getInstance().getString(R.string.timestamp_service_error_caption));
        return cmsMessage;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public CMSSignedMessage getCMS() {
        return cmsMessage;
    }

}