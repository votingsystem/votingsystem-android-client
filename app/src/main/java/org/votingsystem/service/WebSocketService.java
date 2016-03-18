package org.votingsystem.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.pkcs.PKCS10CertificationRequestHolder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.AppVS;
import org.votingsystem.activity.CMSSignerActivity;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.CurrencyContentProvider;
import org.votingsystem.contentprovider.MessageContentProvider;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.RemoteSignedSessionDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.fragment.PaymentFragment;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();

    public static final String SSL_ENGINE_CONFIGURATOR =
            "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppVS appVS;
    private Session session;
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private CountDownLatch latch = new CountDownLatch(1);

    @Override public void onCreate(){
        appVS = (AppVS) getApplicationContext();
        LOGD(TAG + ".onCreate", "WebSocketService started");
    }

    @Override public void onDestroy(){
        LOGD(TAG + ".onDestroy() ", "onDestroy");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG + ".onStartCommand", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(session == null || !session.isOpen()) {
            latch = new CountDownLatch(1);
            WebSocketListener socketListener = new WebSocketListener(
                    appVS.getCurrencyServer().getWebSocketURL());
            executorService.submit(socketListener);
        }
        try {
            if(latch.getCount() > 0) latch.await();
        } catch(Exception ex) {
            LOGE(TAG + ".onStartCommand", "ERROR CONNECTING TO WEBSOCKET: " + ex.getMessage());
            ex.printStackTrace();
        }
        final Bundle arguments = intent.getExtras();
        final TypeVS operationType = arguments.containsKey(ContextVS.TYPEVS_KEY) ?
                (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY) : TypeVS.FROM_USER;
        if(operationType == TypeVS.PIN) {
            executorService.submit(new Runnable() {
                @Override public void run() {
                    try {
                        String msgUUID = arguments.getString(ContextVS.UUID_KEY);
                        processPIN(msgUUID);
                    } catch (Exception ex) { ex.printStackTrace();}
                }});
        } else {
            final String dtoStr = arguments.getString(ContextVS.DTO_KEY);
            final String message = arguments.getString(ContextVS.MESSAGE_KEY);
            final String broadCastId = arguments.getString(ContextVS.CALLER_KEY);
            if(operationType == TypeVS.WEB_SOCKET_CLOSE && session != null && session.isOpen()) {
                executorService.submit(new Runnable() {
                    @Override public void run() {
                        try {
                            session.close();
                        } catch (Exception ex) { ex.printStackTrace();}
                    }});
            } else if(message != null) {
                executorService.submit(new Runnable() {
                    @Override public void run() {
                        try {
                            LOGD(TAG + ".onStartCommand", "operation: " + operationType +
                                    " - socketMsg: " + message);
                            switch(operationType) {
                                case MESSAGEVS:
                                    List<DeviceDto> targetDevicesDto = JSON.readValue(
                                            dtoStr, new TypeReference<List<DeviceDto>>(){});
                                    for (DeviceDto deviceDto : targetDevicesDto) {
                                        SocketMessageDto messageDto = SocketMessageDto.getMessageVSToDevice(
                                                deviceDto, null, message, broadCastId);
                                        session.getBasicRemote().sendText(
                                                JSON.writeValueAsString(messageDto));
                                    }
                                    break;
                                default:
                                    session.getBasicRemote().sendText(message);
                            }
                        } catch(Exception ex) {
                            ex.printStackTrace();
                            UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, ex.getMessage(),
                                    getString(R.string.error_lbl));
                        }
                    }
                });
            }
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void setWebSocketSession(Session session) {
        this.session = session;
        latch.countDown();
    }

    private void processPIN(String msgUUID) throws Exception {
        LOGD(TAG, "processPIN - msgUUID: " + msgUUID);
        WebSocketSession webSocketSession = appVS.getWSSession(msgUUID);
        SocketMessageDto socketMessageDto = webSocketSession.getLastMessage();
        switch (socketMessageDto.getOperation()) {
            case INIT_REMOTE_SIGNED_SESSION:
                RemoteSignedSessionDto remoteSignedSessionDto =
                        socketMessageDto.getMessage(RemoteSignedSessionDto.class);
                PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(
                        remoteSignedSessionDto.getCsr().getBytes());
                PKCS10CertificationRequestHolder csrHolder =
                        new PKCS10CertificationRequestHolder(csr.getEncoded());
                UserDto userFromCSR = UserDto.getUser(csrHolder.getSubject());
                if(!userFromCSR.checkUserFromCSR(appVS.getX509UserCert())) {
                    UIUtils.launchMessageActivity(ResponseVS.SC_ERROR,
                            MsgUtils.userFromCSRMissmatch(userFromCSR),
                            getString(R.string.error_lbl));
                } else {
                    CMSSignedMessage cmsSignedMessage = AppVS.getInstance().signMessage(
                            JSON.writeValueAsBytes(remoteSignedSessionDto));
                    SocketMessageDto initSessionMessageDto = SocketMessageDto.
                            INIT_REMOTE_SIGNED_SESSION_REQUEST(cmsSignedMessage);
                    session.getBasicRemote().sendText(JSON.writeValueAsString(initSessionMessageDto));
                }
                break;
        }
    }

    private class WebSocketListener implements Runnable {

        private String serviceURL = null;
        final ClientManager client = ClientManager.createClient();

        public WebSocketListener(String serviceURL) {
            this.serviceURL = serviceURL;
            if(serviceURL.startsWith("wss")) {
                LOGD(TAG + ".WebsocketListener", "setting SECURE connection");
                try {
                    KeyStore p12Store = KeyStore.getInstance("PKCS12");
                    p12Store.load(null, null);
                    X509Certificate serverCert = appVS.getSSLServerCert();
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                    ByteArrayOutputStream baos  = new ByteArrayOutputStream();
                    p12Store.store(baos, "".toCharArray());
                    byte[] p12KeyStoreBytes = baos.toByteArray();
                    SSLContextConfigurator sslContext = new SSLContextConfigurator();
                    sslContext.setTrustStoreType("PKCS12");
                    sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                    sslContext.setTrustStorePass("");
                    SSLEngineConfigurator sslEngineConfigurator =
                            new SSLEngineConfigurator(sslContext, true, false, false);
                    //BUG with Android 5.0 and Tyrus client!!!
                    //https://java.net/projects/tyrus/lists/users/archive/2015-01/message/0
                    client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else LOGD(TAG + ".WebsocketListener", "setting INSECURE connection");
        }

        @Override public void run() {
            try {
                LOGD(TAG + ".WebsocketListener", "connecting to '" + serviceURL + "'...");
                final ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().
                        configurator(new ClientEndpointConfig.Configurator() {
                            @Override
                            public void beforeRequest(Map<String, List<String>> headers) {
                                headers.put("Origin", Arrays.asList(appVS.getCurrencyServerURL()));
                            }

                            @Override
                            public void afterResponse(HandshakeResponse handshakeResponse) {
                                //final Map<String, List<String>> headers = handshakeResponse.getHeaders();
                            }
                        }).build();
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig endpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                try {
                                    sendWebSocketBroadcast(JSON.readValue(message, SocketMessageDto.class));
                                } catch (IOException e) { e.printStackTrace(); }
                            }
                        });
                        setWebSocketSession(session);
                    }
                    @Override public void onClose(Session session, CloseReason closeReason) {
                        appVS.setWithSocketConnection(false);
                        try {
                            sendWebSocketBroadcast(new SocketMessageDto(
                                    ResponseVS.SC_OK, null, TypeVS.MESSAGEVS_FROM_VS)
                                    .setMessageType(TypeVS.WEB_SOCKET_CLOSE));
                        } catch (Exception ex) {  ex.printStackTrace(); }
                    }
                }, clientEndpointConfig, URI.create(serviceURL));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendWebSocketBroadcast(SocketMessageDto socketMsg) {
        try {
            Intent intent =  new Intent(ContextVS.WEB_SOCKET_BROADCAST_ID);
            intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, socketMsg);
            WebSocketSession socketSession = appVS.getWSSession(socketMsg.getUUID());
            //check messages from system
            if(socketMsg.getOperation() == TypeVS.MESSAGEVS_FROM_VS) {
                socketMsg.setOperation(socketMsg.getMessageType());
                LOGD(TAG, "MESSAGEVS_FROM_VS - operation: " + socketMsg.getOperation());
                if(socketMsg.getOperation() != null) {
                    switch(socketMsg.getOperation()) {
                        case INIT_SESSION:
                            break;
                        case INIT_SIGNED_SESSION:
                            if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                                appVS.setConnectedDevice(socketMsg.getConnectedDevice());
                                appVS.setWithSocketConnection(true);
                                appVS.setToken(socketMsg.getMessage().toCharArray());
                            } else appVS.setWithSocketConnection(false);
                            break;
                        case TRANSACTION_INFO:
                            break;
                        case WEB_SOCKET_CLOSE:
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            return;
                        default: LOGD(TAG, "MESSAGEVS_FROM_VS - UNPROCESSED - MessageType: " + socketMsg.getMessageType());
                    }
                }
                if(ResponseVS.SC_WS_CONNECTION_NOT_FOUND == socketMsg.getStatusCode() ||
                        ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
                    UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, socketMsg.getMessage(),
                            getString(R.string.error_lbl));
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                return;
            }
            socketMsg.decryptMessage();
            if(socketSession == null) {
                socketSession = new WebSocketSession(socketMsg);
                appVS.putWSSession(socketMsg.getUUID(), socketSession);
            } else socketSession.setLastMessage(socketMsg);
            LOGD(TAG + ".sendWebSocketBroadcast", "statusCode: " + socketMsg.getStatusCode() +
                    " - Operation: " + socketMsg.getOperation() + " - MessageType: " + socketMsg.getMessageType());
            switch(socketMsg.getOperation()) {
                case MESSAGEVS:
                    ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, socketMsg.getMessage());
                    responseVS.setCaption(getString(R.string.msg_lbl)).
                            setNotificationMessage(socketMsg.getMessage());
                    MessageContentProvider.insert(getContentResolver(), socketMsg);
                    Utils.showNewMessageNotification();
                    break;
                case MESSAGEVS_SIGN:
                    intent = new Intent(this, CMSSignerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, socketMsg);
                    startActivity(intent);
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case CURRENCY_WALLET_CHANGE:
                    if(socketMsg.getMessageType() == TypeVS.MSG_TO_DEVICE_BY_TARGET_SESSION_ID) {
                        if(ResponseVS.SC_OK == socketMsg.getStatusCode() && socketSession != null) {
                            for(Currency currency : (Collection<Currency>) socketSession.getData()) {
                                currency.setState(Currency.State.EXPENDED);
                            }
                            Wallet.remove((Collection<Currency>) socketSession.getData());
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        }
                    } else if(socketMsg.getMessageType() == TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID) {
                        MessageContentProvider.insert(getContentResolver(), socketMsg);
                        Utils.showNewMessageNotification();
                    }
                    break;
                case TRANSACTION_INFO:
                    //response received after asking for the details of a QR code
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        TransactionDto dto = socketMsg.getMessage(TransactionDto.class);
                        dto.setSocketMessageDto(socketMsg);
                        dto.setQrMessageDto((QRMessageDto) socketSession.getData());
                        Intent resultIntent = new Intent(this, FragmentContainerActivity.class);
                        resultIntent.putExtra(ContextVS.FRAGMENT_KEY, PaymentFragment.class.getName());
                        resultIntent.putExtra(ContextVS.TRANSACTION_KEY, dto);
                        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(resultIntent);
                    } else {
                        UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                case INIT_REMOTE_SIGNED_SESSION:
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        Utils.getProtectionPasswordForSocketOperation(
                                getString(R.string.allow_remote_device_authenticated_session),
                                socketMsg.getUUID(),
                                socketMsg.getOperationCode(),
                                this);
                    }
                    break;
                case QR_MESSAGE_INFO_RESPONSE:
                    break;
                case QR_MESSAGE_INFO:
                    //the payer has read our QR code and ask for details
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        SocketMessageDto msgDto = null;
                        try {
                            QRMessageDto<TransactionDto> qrDto = AppVS.getInstance().getQRMessage(
                                    socketMsg.getMessage());
                            //qrDto.setHashCertVS(socketMsg.getContent().getHashCertVS());
                            TransactionDto transactionDto = qrDto.getData();

                            Currency currency =  new  Currency(
                                    AppVS.getInstance().getCurrencyServer().getServerURL(),
                                    transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                                    transactionDto.isTimeLimited(),  qrDto.getHashCertVS(),
                                    transactionDto.getTagName());
                            qrDto.setCurrency(currency);
                            CMSSignedMessage simeMessage = AppVS.getInstance().signMessage(
                                    currency.getCertificationRequest().getCsrPEM());
                            transactionDto.setCmsMessagePEM(simeMessage.toPEMStr());
                            msgDto = socketMsg.getResponse(ResponseVS.SC_OK,
                                    JSON.getMapper().writeValueAsString(transactionDto),
                                    simeMessage, TypeVS.TRANSACTION_INFO);
                            socketSession.setData(qrDto);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            msgDto = socketMsg.getResponse(ResponseVS.SC_ERROR,
                                    ex.getMessage(), null, TypeVS.QR_MESSAGE_INFO);
                        } finally {
                            session.getBasicRemote().sendText(JSON.writeValueAsString(msgDto));
                        }
                    } else {
                        UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                case TRANSACTION_RESPONSE:
                    //the payer has completed the payment and send the details
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        try {
                            QRMessageDto<TransactionDto> qrDto =
                                    (QRMessageDto<TransactionDto>) socketSession.getData();
                            CMSSignedMessage cmsMessage = socketMsg.getCMS();
                            TransactionDto dto = cmsMessage.getSignedContent(TransactionDto.class);
                            TypeVS typeVS = dto.getOperation();
                            if(TypeVS.CURRENCY_CHANGE == typeVS) {
                                Currency currency = qrDto.getCurrency();
                                currency.initSigner(socketMsg.getMessage().getBytes());
                                CurrencyStateDto currencyStateDto = HttpHelper.getData(CurrencyStateDto.class,
                                        AppVS.getInstance().getCurrencyServer()
                                        .getCurrencyStateServiceURL(currency.getHashCertVS()),
                                        MediaType.JSON);
                                currency.setState(currencyStateDto.getState());
                                getContentResolver().insert(CurrencyContentProvider.CONTENT_URI,
                                        CurrencyContentProvider.getContentValues(currency));
                                if(currency.getState() != Currency.State.OK) {
                                    throw new ValidationExceptionVS(
                                            "CURRENCY_CHANGE ERROR - Currency state: " + currency.getState());
                                }
                                Wallet.updateWallet(Arrays.asList(currency));
                            }
                            TransactionDto transactionDto = qrDto.getData();
                            String result = transactionDto.validateReceipt(cmsMessage, true);
                            intent.putExtra(ContextVS.MESSAGE_KEY, result);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            AppVS.getInstance().removeQRMessage(qrDto.getUUID());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, ex.getMessage(),
                                    getString(R.string.error_lbl));
                        }
                    } else {
                        UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                default: LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, ex.getMessage(),
                    getString(R.string.exception_lbl));
        }
    }

}
