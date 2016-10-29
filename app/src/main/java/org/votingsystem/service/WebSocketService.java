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
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.pkcs.PKCS10CertificationRequestHolder;
import org.votingsystem.App;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.OperationSignerActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.CurrencyContentProvider;
import org.votingsystem.contentprovider.MessageContentProvider;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.RemoteSignedSessionDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.voting.AccessControlDto;
import org.votingsystem.fragment.PaymentFragment;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.NaiveSSLContext;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();


    private App app;
    private WebSocket currencySession;
    private WebSocket votingSystemSession;
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private CountDownLatch latch = new CountDownLatch(1);

    @Override public void onCreate(){
        app = (App) getApplicationContext();
        LOGD(TAG + ".onCreate", "WebSocketService started");
    }

    @Override public void onDestroy(){
        LOGD(TAG + ".onDestroy() ", "onDestroy");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        try {
            final Bundle arguments = intent.getExtras();
            final OperationType sessionType = arguments.containsKey(Constants.SESSION_KEY) ?
                    (OperationType)arguments.getSerializable(Constants.SESSION_KEY) : OperationType.CURRENCY_SYSTEM;
            if(sessionType == OperationType.CURRENCY_SYSTEM) {
                if(currencySession == null || !currencySession.isOpen()) {
                    latch = new CountDownLatch(1);
                    WebSocketListener socketListener = new WebSocketListener(sessionType);
                    executorService.submit(socketListener);
                }
            } else {
                if(votingSystemSession == null || !votingSystemSession.isOpen()) {
                    latch = new CountDownLatch(1);
                    WebSocketListener socketListener = new WebSocketListener(sessionType);
                    executorService.submit(socketListener);
                }
            }
            try {
                if(latch.getCount() > 0) latch.await();
            } catch(Exception ex) {
                LOGE(TAG + ".onStartCommand", "ERROR CONNECTING TO WEBSOCKET: " + ex.getMessage());
                ex.printStackTrace();
            }
            final OperationType operationType = arguments.containsKey(Constants.TYPEVS_KEY) ?
                    (OperationType)arguments.getSerializable(Constants.TYPEVS_KEY) : OperationType.FROM_USER;
            if(operationType == OperationType.WEB_SOCKET_INIT) {
            } else if(operationType == OperationType.PIN) {
                executorService.submit(new Runnable() {
                    @Override public void run() {
                        try {
                            String msgUUID = arguments.getString(Constants.UUID_KEY);
                            processPIN(msgUUID);
                        } catch (Exception ex) { ex.printStackTrace();}
                    }});
            } else {
                final String dtoStr = arguments.getString(Constants.DTO_KEY);
                final String message = arguments.getString(Constants.MESSAGE_KEY);
                final String broadCastId = arguments.getString(Constants.CALLER_KEY);
                final WebSocket session = sessionType == OperationType.CURRENCY_SYSTEM? currencySession : votingSystemSession;
                if(operationType == OperationType.WEB_SOCKET_CLOSE && session != null && session.isOpen()) {
                    executorService.submit(new Runnable() {
                        @Override public void run() {
                            try {
                                session.disconnect();
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
                                            session.sendText(JSON.writeValueAsString(messageDto));
                                        }
                                        break;
                                    default:
                                        session.sendText(message);
                                }
                            } catch(Exception ex) {
                                ex.printStackTrace();
                                UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, ex.getMessage(),
                                        getString(R.string.error_lbl));
                            }
                        }
                    });
                }
            }
        } catch (Exception ex) { ex.printStackTrace();}
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

    private void setWebSocketSession(WebSocket session, OperationType sessionType) {
        LOGD(TAG + ".setWebSocketSession", "sessionType: " + sessionType);
        if(OperationType.CURRENCY_SYSTEM == sessionType) this.currencySession = session;
        else this.votingSystemSession = session;
        latch.countDown();
    }

    private void processPIN(String msgUUID) throws Exception {
        LOGD(TAG, "processPIN - msgUUID: " + msgUUID);
        WebSocketSession webSocketSession = app.getWSSession(msgUUID);
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
                if(!userFromCSR.checkUserFromCSR(app.getX509UserCert())) {
                    UIUtils.launchMessageActivity(ResponseDto.SC_ERROR,
                            MsgUtils.userFromCSRMissmatch(userFromCSR),
                            getString(R.string.error_lbl));
                } else {
                    CMSSignedMessage cmsSignedMessage = App.getInstance().signMessage(
                            JSON.writeValueAsBytes(remoteSignedSessionDto));
                    SocketMessageDto initSessionMessageDto = SocketMessageDto.
                            INIT_REMOTE_SIGNED_SESSION_REQUEST(cmsSignedMessage);
                    currencySession.sendText(JSON.writeValueAsString(initSessionMessageDto));
                }
                break;
        }
    }

    private class WebSocketListener implements Runnable {

        private OperationType sessionType;
        private String serviceURL = null;

        public WebSocketListener(OperationType sessionType) {
            this.sessionType = sessionType;
        }

        @Override public void run() {
            try {
                if(OperationType.CURRENCY_SYSTEM == sessionType) {
                    CurrencyServerDto currencyServer = app.getActor(CurrencyServerDto.class,
                            app.getCurrencyServerURL());
                    serviceURL = currencyServer.getWebSocketURL();
                } else {
                    AccessControlDto accessControl = app.getActor(AccessControlDto.class,
                            app.getAccessControlURL());
                    serviceURL = accessControl.getWebSocketURL();
                }
                LOGD(TAG + ".WebsocketListener", "connecting to '" + serviceURL + "'...");
                KeyStore p12Store = KeyStore.getInstance("PKCS12");
                p12Store.load(null, null);
                X509Certificate serverCert = app.getSSLServerCert();
                p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                ByteArrayOutputStream baos  = new ByteArrayOutputStream();
                p12Store.store(baos, "".toCharArray());
                byte[] p12KeyStoreBytes = baos.toByteArray();


                WebSocketFactory factory = new WebSocketFactory();
                SSLContext context = NaiveSSLContext.getInstance("TLS");
                factory.setSSLContext(context);
                WebSocket webSocket = factory.createSocket(serviceURL);
                webSocket.addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket, String message) throws Exception {
                        sendWebSocketBroadcast(JSON.readValue(message, SocketMessageDto.class));
                    }
                    @Override
                    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                        setWebSocketSession(websocket, sessionType);
                    }
                    @Override
                    public void onDisconnected(WebSocket websocket,
                                               WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                               boolean closedByServer) throws Exception {
                        sendWebSocketBroadcast(new SocketMessageDto(
                                ResponseDto.SC_WS_CONNECTION_CLOSED, null,
                                OperationType.MESSAGEVS_FROM_VS).setMessageType(OperationType.WEB_SOCKET_CLOSE));
                    }
                });
                webSocket.connect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendWebSocketBroadcast(SocketMessageDto socketMsg) {
        try {
            Intent intent =  new Intent(Constants.WEB_SOCKET_BROADCAST_ID);
            intent.putExtra(Constants.WEBSOCKET_MSG_KEY, socketMsg);
            WebSocketSession socketSession = app.getWSSession(socketMsg.getUUID());
            //check messages from system
            if(socketMsg.getOperation() == OperationType.MESSAGEVS_FROM_VS) {
                LOGD(TAG, "MESSAGEVS_FROM_VS - messageType: " + socketMsg.getMessageType());
                if(socketMsg.getMessageType() != null) {
                    switch(socketMsg.getMessageType()) {
                        case INIT_SESSION:
                            if(ResponseDto.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                                app.setConnectedDevice(new DeviceDto().setId(socketMsg.getDeviceToId()));
                            }
                            break;
                        case INIT_SIGNED_SESSION:
                            if(ResponseDto.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                                app.setWithSocketConnection(true);
                                app.setConnectedDevice(socketMsg.getConnectedDevice());
                                if(socketMsg.getMessage() != null) {
                                    app.setToken(socketMsg.getMessage().toCharArray());
                                } else LOGD(TAG, "socket session without token");
                            }
                            break;
                        case TRANSACTION_INFO:
                            break;
                        case WEB_SOCKET_CLOSE:
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            app.setWithSocketConnection(false);
                            return;
                        default: LOGD(TAG, "MESSAGEVS_FROM_VS - UNPROCESSED - MessageType: " + socketMsg.getMessageType());
                    }
                }
                if(ResponseDto.SC_WS_CONNECTION_NOT_FOUND == socketMsg.getStatusCode() ||
                        ResponseDto.SC_ERROR == socketMsg.getStatusCode() ||
                        ResponseDto.SC_WS_CONNECTION_INIT_ERROR == socketMsg.getStatusCode()) {
                    UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, socketMsg.getMessage(),
                            getString(R.string.error_lbl));
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                return;
            }
            if(socketMsg.getMessageType() == OperationType.OPERATION_PROCESS) {
                QRMessageDto qrMessageDto = socketSession.getQrMessage();
                if(socketMsg.getOperationCode().equals(qrMessageDto.getOperationCode())) {
                    intent = new Intent(this, OperationSignerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    socketMsg.setQrMessage(qrMessageDto);
                    intent.putExtra(Constants.WEBSOCKET_MSG_KEY, socketMsg);
                    startActivity(intent);
                } else LOGE(TAG, "OPERATION_PROCESS ERROR - unexpected operation code");
                return;
            }
            socketMsg.decryptMessage();
            if(socketSession == null) {
                socketSession = new WebSocketSession(socketMsg);
                app.putWSSession(socketMsg.getUUID(), socketSession);
            } else socketSession.setLastMessage(socketMsg);
            LOGD(TAG + ".sendWebSocketBroadcast", "statusCode: " + socketMsg.getStatusCode() +
                    " - OperationDto: " + socketMsg.getOperation() + " - MessageType: " + socketMsg.getMessageType());
            switch(socketMsg.getOperation()) {
                case MESSAGEVS:
                    ResponseDto responseDto = new ResponseDto(ResponseDto.SC_OK, socketMsg.getMessage());
                    responseDto.setCaption(getString(R.string.msg_lbl)).
                            setNotificationMessage(socketMsg.getMessage());
                    MessageContentProvider.insert(getContentResolver(), socketMsg);
                    Utils.showNewMessageNotification();
                    break;
                case CURRENCY_WALLET_CHANGE:
                    if(socketMsg.getStep() == OperationType.CURRENCY_CHANGE) {
                        if(ResponseDto.SC_OK == socketMsg.getStatusCode() && socketSession != null) {
                            for(Currency currency : (Collection<Currency>) socketSession.getData()) {
                                currency.setState(Currency.State.EXPENDED);
                            }
                            Wallet.remove((Collection<Currency>) socketSession.getData());
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        }
                    } else {
                        MessageContentProvider.insert(getContentResolver(), socketMsg);
                        Utils.showNewMessageNotification();
                    }
                    break;
                case TRANSACTION_INFO:
                    //response received after asking for the details of a QR code
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    if(ResponseDto.SC_ERROR != socketMsg.getStatusCode()) {
                        TransactionDto dto = socketMsg.getMessage(TransactionDto.class);
                        dto.setSocketMessageDto(socketMsg);
                        dto.setQrMessageDto(socketSession.getQrMessage());
                        Intent resultIntent = new Intent(this, FragmentContainerActivity.class);
                        resultIntent.putExtra(Constants.FRAGMENT_KEY, PaymentFragment.class.getName());
                        resultIntent.putExtra(Constants.TRANSACTION_KEY, dto);
                        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(resultIntent);
                    } else {
                        UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                case INIT_REMOTE_SIGNED_SESSION:
                    if(ResponseDto.SC_ERROR != socketMsg.getStatusCode()) {
                        Utils.getProtectionPasswordForSocketOperation(
                                getString(R.string.allow_remote_device_authenticated_session),
                                socketMsg.getUUID(),
                                socketMsg.getOperationCode(),
                                this);
                    }
                    break;
                case MESSAGE_INFO_RESPONSE:
                    break;
                case MESSAGE_INFO:
                    //the payer has read our QR code and ask for details
                    if(ResponseDto.SC_ERROR != socketMsg.getStatusCode()) {
                        SocketMessageDto msgDto = null;
                        try {
                            QRMessageDto<TransactionDto> qrDto = App.getInstance().getQRMessage(
                                    socketMsg.getMessage());
                            //qrDto.setRevocationHash(socketMsg.getContent().getRevocationHash());
                            TransactionDto transactionDto = qrDto.getData();

                            Currency currency =  new  Currency(
                                    App.getInstance().getCurrencyServer().getServerURL(),
                                    transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                                    transactionDto.isTimeLimited(),  qrDto.getRevocationHash(),
                                    transactionDto.getTagName());
                            qrDto.setCurrency(currency);
                            CMSSignedMessage simeMessage = App.getInstance().signMessage(
                                    currency.getCertificationRequest().getCsrPEM());
                            transactionDto.setCmsMessagePEM(simeMessage.toPEMStr());
                            msgDto = socketMsg.getResponse(ResponseDto.SC_OK,
                                    JSON.getMapper().writeValueAsString(transactionDto),
                                    simeMessage, OperationType.TRANSACTION_INFO);
                            socketSession.setData(qrDto);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            msgDto = socketMsg.getResponse(ResponseDto.SC_ERROR,
                                    ex.getMessage(), null, OperationType.MESSAGE_INFO);
                        } finally {
                            currencySession.sendText(JSON.writeValueAsString(msgDto));
                        }
                    } else {
                        UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                case TRANSACTION_RESPONSE:
                    //the payer has completed the payment and send the details
                    if(ResponseDto.SC_ERROR != socketMsg.getStatusCode()) {
                        try {
                            QRMessageDto<TransactionDto> qrDto = socketSession.getQrMessage();
                            CMSSignedMessage cmsMessage = socketMsg.getCMS();
                            TransactionDto dto = cmsMessage.getSignedContent(TransactionDto.class);
                            OperationType operationType = dto.getOperation();
                            if(OperationType.CURRENCY_CHANGE == operationType) {
                                Currency currency = qrDto.getCurrency();
                                currency.initSigner(socketMsg.getMessage().getBytes());
                                CurrencyStateDto currencyStateDto = HttpConnection.getInstance().getData(CurrencyStateDto.class,
                                        App.getInstance().getCurrencyServer()
                                        .getCurrencyStateServiceURL(currency.getRevocationHash()),
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
                            intent.putExtra(Constants.MESSAGE_KEY, result);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            App.getInstance().removeQRMessage(qrDto.getUUID());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, ex.getMessage(),
                                    getString(R.string.error_lbl));
                        }
                    } else {
                        UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, socketMsg.getMessage(),
                                getString(R.string.error_lbl));
                    }
                    break;
                default: LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, ex.getMessage(),
                    getString(R.string.exception_lbl));
        }
    }

}
