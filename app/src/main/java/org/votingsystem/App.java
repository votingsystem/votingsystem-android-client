package org.votingsystem;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.votingsystem.activity.MessageActivity;
import org.votingsystem.android.R;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.RootUtils;
import org.votingsystem.xades.SignatureValidator;
import org.votingsystem.xades.XmlSignature;
import org.votingsystem.xml.XmlReader;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.votingsystem.util.Constants.ALGORITHM_RNG;
import static org.votingsystem.util.Constants.KEY_SIZE;
import static org.votingsystem.util.Constants.PROVIDER;
import static org.votingsystem.util.Constants.SIG_NAME;
import static org.votingsystem.util.Constants.USER_CERT_ALIAS;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class App extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = App.class.getSimpleName();

    private static final Map<String, MetadataDto> systemEntityMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private DeviceDto connectedDevice;
    private Set<ElectionDto.State> electionStateLoaded = new HashSet<>();
    private AtomicInteger notificationId = new AtomicInteger(1);
    private boolean isRootedPhone = false;
    private SystemEntityDto votingServiceProvider;
    private SystemEntityDto idProvider;

    private static App INSTANCE;

    public static App getInstance() {
        return INSTANCE;
    }

    public void putSystemEntity(MetadataDto systemEntity) {
        systemEntityMap.put(systemEntity.getEntity().getId(), systemEntity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            INSTANCE = this;
            KeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            /*if (!PrefUtils.isEulaAccepted(this)) {//Check if the EULA has been accepted; if not, show it.
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();}*/
            PrefUtils.registerPreferenceChangeListener(this);
            if (!Constants.ALLOW_ROOTED_PHONES && RootUtils.isDeviceRooted()) {
                isRootedPhone = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        HttpConn.getInstance().shutdown();
        PrefUtils.unregisterPreferenceChangeListener(this);
    }

    public Integer getNotificationId() {
        return notificationId.get();
    }

    public KeyStore.PrivateKeyEntry getUserPrivateKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                USER_CERT_ALIAS, null);
        return keyEntry;
    }

    public X509Certificate getX509UserCert() throws CertificateException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        return (X509Certificate) keyEntry.getCertificateChain()[0];
    }

    public byte[] decryptMessage(byte[] encryptedPEM) throws Exception {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        //X509Certificate cryptoTokenCert = (X509Certificate) keyEntry.getCertificateChain()[0];
        PrivateKey privateKey = keyEntry.getPrivateKey();
        return Encryptor.decryptCMS(encryptedPEM, privateKey);
    }

    public MetadataDto getSystemEntity(final String systemEntityId, boolean forceHTTPLoad) {
        MetadataDto result = systemEntityMap.get(systemEntityId);
        if(result == null && forceHTTPLoad) {
            try {
                result = getSystemEntityFromURL(OperationType.METADATA.getUrl(systemEntityId));
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private MetadataDto getSystemEntityFromURL(String entityURL) throws Exception {
        /*if(Looper.getMainLooper().getThread() != Thread.currentThread()) { //not in main thread,
            //if invoked from main thread -> android.os.NetworkOnMainThreadException
        } else {  }*/
        ResponseDto responseDto = HttpConn.getInstance().doGetRequest(entityURL, ContentType.XML);
        Set<XmlSignature> signatures = new SignatureValidator(responseDto.getMessageBytes()).validate();
        X509Certificate systemEntityCert = signatures.iterator().next().getSigningCertificate();
        MetadataDto systemEntity = XmlReader.readMetadata(responseDto.getMessageBytes());
        systemEntity.setSigningCertificate(systemEntityCert);
        putSystemEntity(systemEntity);
        return systemEntity;
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

    public void showNotification(ResponseDto responseDto) {
        final NotificationManager mgr = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(Constants.RESPONSE_KEY, responseDto);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId.getAndIncrement(), clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(responseDto.getCaption())
                .setContentText(responseDto.getNotificationMessage()).setSound(soundUri);
        if (responseDto.getStatusCode() == ResponseDto.SC_ERROR)
            builder.setSmallIcon(R.drawable.ic_close_24px);
        else if (responseDto.getStatusCode() == ResponseDto.SC_OK)
            builder.setSmallIcon(R.drawable.ic_check_24px);
        else builder.setSmallIcon(R.drawable.ic_mail_outline_24px);
        mgr.notify(notificationId.getAndIncrement(), builder.build());
    }

    public void broadcastResponse(ResponseDto responseDto) {
        LOGD(TAG + ".broadcastResponse", "statusCode: " + responseDto.getStatusCode() +
                " - type: " + responseDto.getOperationType() + " - serviceCaller: " +
                responseDto.getServiceCaller());
        Intent intent = new Intent(responseDto.getServiceCaller());
        intent.putExtra(Constants.RESPONSE_KEY, responseDto);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    public DeviceDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public boolean isElectionStateLoaded(ElectionDto.State eventsState) {
        return electionStateLoaded.contains(eventsState);
    }

    public void addElectiontateLoaded(ElectionDto.State eventsState) {
        this.electionStateLoaded.add(eventsState);
    }

    public boolean isRootedPhone() {
        return isRootedPhone;
    }

    public SystemEntityDto getVotingServiceProvider() {
        return votingServiceProvider;
    }

    public List<TrustedEntitiesDto.EntityDto> getTrustedEntityList(
            String entityId, SystemEntityType entityType) {
        List<TrustedEntitiesDto.EntityDto> result = new ArrayList<>();
        MetadataDto metadata = systemEntityMap.get(entityId);
        if(metadata != null) {
            for(TrustedEntitiesDto.EntityDto trustedEntity: metadata.getTrustedEntities().getEntities()) {
                if(trustedEntity.getType() == entityType)
                    result.add(trustedEntity);
            }
        }
        return result;
    }


    public void setVotingServiceProvider(SystemEntityDto votingServiceProvider) {
        this.votingServiceProvider = votingServiceProvider;
    }

    public SystemEntityDto getIdProvider() {
        return idProvider;
    }

    public void setIdProvider(SystemEntityDto idProvider) {
        this.idProvider = idProvider;
    }

}