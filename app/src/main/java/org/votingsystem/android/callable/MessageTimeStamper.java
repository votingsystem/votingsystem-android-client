package org.votingsystem.android.callable;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.votingsystem.android.AppVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<ResponseVS> {
    
	public static final String TAG = MessageTimeStamper.class.getSimpleName();
    
    private SMIMEMessage smimeMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private AppVS appVS;
    private String timeStampServiceURL;
      
    public MessageTimeStamper (SMIMEMessage smimeMessage,
            AppVS context) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.appVS = context;
    }

    public MessageTimeStamper (SMIMEMessage smimeMessage, String timeStampServiceURL,
               AppVS context) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.appVS = context;
        this.timeStampServiceURL = timeStampServiceURL;
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest,
            AppVS context) throws Exception {
        this.timeStampRequest = timeStampRequest;
        this.appVS = context;
    }
        
    public MessageTimeStamper (String timeStampDigestAlgorithm, 
    		byte[] digestToTimeStamp, AppVS context) throws Exception {
    	TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        this.timeStampRequest = reqgen.generate(
        		timeStampDigestAlgorithm, digestToTimeStamp);
        this.appVS = context;
    }
    
        
    @Override public ResponseVS call() throws Exception {
        //byte[] base64timeStampRequest = Base64.encode(timeStampRequest.getEncoded());
        if(timeStampServiceURL == null) timeStampServiceURL =
                appVS.getTimeStampServiceURL();
        ResponseVS responseVS = HttpHelper.sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, timeStampServiceURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            timeStampToken= new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
            X509Certificate timeStampCert = appVS.getTimeStampCert();
                /* -> Android project config problem
                 * SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);*/
            timeStampToken.validate(timeStampCert, ContextVS.PROVIDER);/**/
            if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
        }
        return responseVS;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

}