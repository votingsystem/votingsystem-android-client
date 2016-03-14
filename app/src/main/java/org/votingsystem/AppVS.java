package org.votingsystem;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedDataGenerator;
import org.bouncycastle2.cms.CMSTypedData;
import org.bouncycastle2.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle2.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle2.util.Store;
import org.votingsystem.activity.MessageActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSGenerator;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.CurrencyContentProvider;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.voting.AccessControlDto;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.service.BootStrapService;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.BuildConfig;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.RootUtil;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.util.crypto.AESParams;
import org.votingsystem.util.crypto.CMSUtils;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.KeyGeneratorVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.votingsystem.util.ContextVS.ALGORITHM_RNG;
import static org.votingsystem.util.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.util.ContextVS.KEY_SIZE;
import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.ContextVS.SIG_NAME;
import static org.votingsystem.util.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.util.ContextVS.USER_KEY;
import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AppVS extends MultiDexApplication implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = AppVS.class.getSimpleName();

    private boolean withSocketConnection;
    private static final Map<String, ActorDto> serverMap = new HashMap<>();
    private static final Map<String, WebSocketSession> sessionsMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private AccessControlDto accessControl;
    private String accessControlURL;
    private ControlCenterDto controlCenter;
    private CurrencyServerDto currencyServer;
    private String currencyServerURL;
    private UserVSDto userVS;
    private DeviceVSDto connectedDevice;
    private X509Certificate sslServerCert;
    private Map<String, X509Certificate> certsMap = new HashMap<>();
    private Set<EventVSDto.State> eventsStateLoaded = new HashSet<>();
    private AtomicInteger notificationId = new AtomicInteger(1);
    private boolean isRootedPhone = false;
    private char[] token;

    private static AppVS INSTANCE;

    public static AppVS getInstance() {
        return INSTANCE;
    }

    @Override public void onCreate() {
        super.onCreate();
        try {
            INSTANCE = this;
            KeyGeneratorVS.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            PrefUtils.init();
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            currencyServerURL = props.getProperty(ContextVS.CURRENCY_SERVER_URL);
            accessControlURL = props.getProperty(ContextVS.ACCESS_CONTROL_URL_KEY);
            LOGD(TAG + ".onCreate", "accessControlURL: " + accessControlURL +
                    " - currencyServerURL: " + currencyServerURL);
            /*if (!PrefUtils.isEulaAccepted(this)) {//Check if the EULA has been accepted; if not, show it.
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();}*/
            if(accessControl == null || currencyServer == null) {
                Intent startIntent = new Intent(this, BootStrapService.class);
                startIntent.putExtra(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL);
                startIntent.putExtra(ContextVS.CURRENCY_SERVER_URL, currencyServerURL);
                startService(startIntent);
            }
            PrefUtils.registerPreferenceChangeListener(this);
            userVS = PrefUtils.getAppUser();
            byte[] certBytes = FileUtils.getBytesFromInputStream(getAssets().open(
                    "VotingSystemSSLCert.pem"));
            Collection<X509Certificate> votingSystemSSLCerts =
                    PEMUtils.fromPEMToX509CertCollection(certBytes);
            sslServerCert = votingSystemSSLCerts.iterator().next();
            HttpHelper.init(sslServerCert);
            if(!BuildConfig.ALLOW_ROOTED_PHONES && RootUtil.isDeviceRooted()) {
                isRootedPhone = true;
            }
        } catch(Exception ex) { ex.printStackTrace(); }
	}

    public X509Certificate getSSLServerCert() {
        return sslServerCert;
    }

    public  String getAccessControlURL() {
        return  accessControlURL;
    }

    public  String getCurrencyServerURL() {
        return  currencyServerURL;
    }

    public void finish() {
        LOGD(TAG, "finish");
        stopService(new Intent(getApplicationContext(), WebSocketService.class));
        HttpHelper.shutdown();
        UIUtils.killApp(true);
    }

    public void setServer(ActorDto actorDto) {
        if(serverMap.get(actorDto.getServerURL()) == null) {
            serverMap.put(actorDto.getServerURL(), actorDto);
        } else LOGD(TAG + ".setServer", "server with URL '" + actorDto.getServerURL() +
                "' already in context");
    }

    public ActorDto getServer(String serverURL) {
        return serverMap.get(serverURL);
    }

    @Override public void onTerminate() {
        super.onTerminate();
        PrefUtils.unregisterPreferenceChangeListener(this);
    }

    public Integer getNotificationId() {
        return notificationId.get();
    }

    public void putCert(String serverURL, X509Certificate cert) {
        LOGD(TAG + ".putCert", "serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public X509Certificate getTimeStampCert() throws Exception {
        if(accessControl != null) return accessControl.getTimeStampCert();
        if(currencyServer != null) return currencyServer.getTimeStampCert();
        return null;
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public AccessControlDto getAccessControl() {
        if(accessControl == null) {
            accessControl = getActorVS(AccessControlDto.class, accessControlURL);
        }
        return accessControl;
    }

    public String getTimeStampServiceURL() {
        if(accessControl != null) return accessControl.getTimeStampServiceURL();
        if(currencyServer != null) return currencyServer.getTimeStampServiceURL();
        return null;
    }

    public void setAccessControlVS(AccessControlDto accessControl) {
        LOGD(TAG + ".setAccessControlVS", "serverURL: " + accessControl.getServerURL());
        this.accessControl = accessControl;
        serverMap.put(accessControl.getServerURL(), accessControl);
        PrefUtils.markAccessControlLoaded(accessControl.getServerURL());
    }

    public String getCurrentWeekLapseId() {
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        return DateUtils.getPath(currentLapseCalendar.getTime());
    }

    public KeyStore.PrivateKeyEntry getUserPrivateKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                USER_CERT_ALIAS, null);
        return keyEntry;
    }

    public X509Certificate getX509UserCert() throws CertificateException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        return (X509Certificate) keyEntry.getCertificateChain()[0];
    }

    public byte[] decryptMessage(byte[] base64EncryptedData) throws Exception {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        //X509Certificate cryptoTokenCert = (X509Certificate) keyEntry.getCertificateChain()[0];
        PrivateKey privateKey = keyEntry.getPrivateKey();
        return Encryptor.decryptCMS(base64EncryptedData, privateKey);
    }

    public <T> T getActorVS(final Class<T> type, final String serverURL) {
        T targetServer = (T) getServer(serverURL);
        if(targetServer == null) {
            if(Looper.getMainLooper().getThread() != Thread.currentThread()) { //not in main thread,
                //if invoked from main thread -> android.os.NetworkOnMainThreadException
                targetServer = getActorDtoFromURL(type, serverURL);
            } else {
                LOGD(TAG + ".getActorVS", "FROM MAIN THREAD - CREATING NEW THREAD - " + serverURL);
                new Thread(new Runnable() {
                    @Override public void run() {  getActorDtoFromURL(type, serverURL);  }
                }).start();
            }
        }
        return (T) targetServer;
    }

    private <T> T getActorDtoFromURL(Class<T> type, String serverURL) {
        T targetServer = null;
        try {
            targetServer = HttpHelper.getData(type, ActorDto.getServerInfoURL(serverURL), MediaTypeVS.JSON);
            setServer((ActorDto) targetServer);
        } catch(Exception ex) {
            LOGE(TAG + ".getActorDtoFromURL", "ERROR fetching: " + serverURL);
        } finally { return targetServer; }
    }

    public void putQRMessage(QRMessageDto messageDto) {
        qrMessagesMap.put(messageDto.getUUID(), messageDto);
    }

    public QRMessageDto getQRMessage(String uuid) {
        return qrMessagesMap.get(uuid);
    }

    public QRMessageDto removeQRMessage(String uuid) {
        return qrMessagesMap.remove(uuid);
    }

    public void putWSSession(String UUID, WebSocketSession session) {
        sessionsMap.put(UUID, session.setUUID(UUID));
    }

    public WebSocketSession getWSSession(String UUID) {
        return sessionsMap.get(UUID);
    }

    public WebSocketSession getWSSession(Long deviceID) {
        WebSocketSession socketSession = null;
        for(String sessionUUID : sessionsMap.keySet()) {
            socketSession = sessionsMap.get(sessionUUID);
            if(socketSession != null && socketSession.getDeviceVS() != null && socketSession.getDeviceVS().
                    getId() == deviceID) {
                return socketSession;
            }
        }
        return null;
    }

    public AESParams getSessionKey(String UUID) {
        return sessionsMap.get(UUID).getAESParams();
    }

    //method with http connections, if invoked from main thread -> android.os.NetworkOnMainThreadException
    public CMSSignedMessage signMessage(byte[] contentToSign) throws Exception {
        LOGD(TAG + ".signMessage", "signMessage - user NIF: " +  getUserVS().getNIF());
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        CMSGenerator cmsGenerator = new CMSGenerator(keyEntry.getPrivateKey(),
                Arrays.asList(keyEntry.getCertificateChain()[0]),
                SIGNATURE_ALGORITHM, ANDROID_PROVIDER);
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(
                SIGNATURE_ALGORITHM, contentToSign);
        return cmsGenerator.signData(contentToSign, timeStampToken);
    }

    public void setControlCenter(ControlCenterDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterDto getControlCenter() {
        return controlCenter;
    }

    public void setCurrencyServerDto(CurrencyServerDto currencyServer) {
        serverMap.put(currencyServer.getServerURL(), currencyServer);
        this.currencyServer = currencyServer;
    }

    public CurrencyServerDto getCurrencyServer() {
        if(currencyServer == null) currencyServer = getActorVS(
                CurrencyServerDto.class, currencyServerURL);
        return currencyServer;
    }

    public void showNotification(ResponseVS responseVS){
        final NotificationManager mgr = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId.getAndIncrement(), clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(responseVS.getCaption())
                .setContentText(responseVS.getNotificationMessage()).setSound(soundUri);
        if(responseVS.getStatusCode() == ResponseVS.SC_ERROR)
            builder.setSmallIcon(R.drawable.cancel_22);
        else if(responseVS.getStatusCode() == ResponseVS.SC_OK)
            builder.setSmallIcon(R.drawable.fa_check_32);
        else builder.setSmallIcon(R.drawable.mail_mark_unread_32);
        mgr.notify(notificationId.getAndIncrement(), builder.build());
    }

    public void broadcastResponse(ResponseVS responseVS) {
        LOGD(TAG + ".broadcastResponse", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void updateCurrencyDB(Currency currency) throws Exception {
        Uri uri = getContentResolver().insert(CurrencyContentProvider.CONTENT_URI,
                CurrencyContentProvider.getContentValues(currency));
        LOGD(TAG + ".updateCurrencyDB", "uri: " + uri + " - state: " + currency.getState());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(USER_KEY.equals(key)) {
            userVS = PrefUtils.getAppUser();
        }
    }

    public DeviceVSDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceVSDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public boolean isWithSocketConnection() {
        return withSocketConnection;
    }

    public void setWithSocketConnection(boolean withSocketConnection) {
        this.withSocketConnection = withSocketConnection;
    }

    public boolean isEventsStateLoaded(EventVSDto.State eventsState) {
        return eventsStateLoaded.contains(eventsState);
    }

    public void addEventsStateLoaded(EventVSDto.State eventsState) {
        this.eventsStateLoaded.add(eventsState);
    }

    public char[] getToken() {
        return token;
    }

    public void setToken(char[] token) {
        this.token = token;
    }

    public boolean isRootedPhone() {
        return isRootedPhone;
    }


    public CMSSignedMessage signCMSData(String signatureContent) throws Exception {
        List certList = new ArrayList();
        CMSTypedData msg = new CMSProcessableByteArray(signatureContent.getBytes());

        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();

        certList.add((X509Certificate) keyEntry.getCertificateChain()[0]);
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();


        /*SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();

        signerInfoGeneratorBuilder.setProvider(ANDROID_PROVIDER);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                SIGNATURE_ALGORITHM, key, x509Cert);*/


        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(ANDROID_PROVIDER).build(keyEntry.getPrivateKey());
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC").build()).build(signer, (X509Certificate) keyEntry.getCertificateChain()[0]));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        //validatePKCS7SignedData(pemEncoded);
        return  new CMSSignedMessage(signedData);
    }
}