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

import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.activity.SMIMESignerActivity;
import org.votingsystem.contentprovider.MessageContentProvider;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.dto.AESParamsDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
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

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppVS appVS;
    private Session session;
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
        if(appVS.getCurrencyServer() == null) {
            SocketMessageDto messageDto = new SocketMessageDto(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                    getString(R.string.missing_server_connection), TypeVS.INIT_SIGNED_SESSION);
            messageDto.setCaption(getString(R.string.connection_error_msg));
            sendWebSocketBroadcast(messageDto);
            return START_STICKY;
        } else {
            if(session == null || !session.isOpen()) {
                if(latch.getCount() == 0) latch = new CountDownLatch(1);
                WebSocketListener socketListener = new WebSocketListener(
                        appVS.getCurrencyServer().getWebSocketURL());
                new Thread(null, socketListener, "websocket_service_thread").start();
            }
            try {
                if(latch.getCount() > 0) {
                    LOGD(TAG + ".onStartCommand", "starting websocket session");
                    latch.await();
                }
            } catch(Exception ex) {
                LOGE(TAG + ".onStartCommand", "ERROR CONNECTING TO WEBSOCKET SERVICE: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        Bundle arguments = intent.getExtras();
        final TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        final String dtoStr = arguments.getString(ContextVS.DTO_KEY);
        final String message = arguments.getString(ContextVS.MESSAGE_KEY);
        if(operationType == TypeVS.WEB_SOCKET_INIT) {
            initValidatedSession();
        } else if(operationType == TypeVS.WEB_SOCKET_CLOSE && session != null && session.isOpen()) {
            new Thread(null, new Runnable() {
                @Override public void run() {
                    try {
                        session.close();
                    } catch (Exception ex) { ex.printStackTrace();}
                }
            }, "websocket_message_proccessor_thread").start();
        } else if(message != null) {
            new Thread(null, new Runnable() {
                @Override public void run() {
                    try {
                        LOGD(TAG + ".onStartCommand", "socketMsg: " + message);
                        switch(operationType) {
                            case MESSAGEVS:
                                List<DeviceVSDto> targetDevicesDto = JSON.getMapper().readValue(
                                        dtoStr, new TypeReference<List<DeviceVSDto>>(){});
                                for (DeviceVSDto deviceVSDto : targetDevicesDto) {
                                    SocketMessageDto messageDto = SocketMessageDto.getMessageVSToDevice(
                                            deviceVSDto, null, message);
                                    session.getBasicRemote().sendText(
                                            JSON.getMapper().writeValueAsString(messageDto));
                                }
                                break;
                            default:
                                session.getBasicRemote().sendText(message);
                        }
                    } catch(Exception ex) {
                        UIUtils.launchMessageActivity(ResponseVS.SC_ERROR, ex.getMessage(),
                                getString(R.string.error_lbl));
                    }
                }
            }, "websocket_message_proccessor_thread").start();
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    private void initValidatedSession() {
        new Thread(null, new Runnable() {
            @Override public void run() {
                try {
                    CurrencyServerDto currencyServer = appVS.getCurrencyServer();
                    SocketMessageDto messageDto = SocketMessageDto.INIT_SESSION_REQUEST();
                    String msgSubject = getString(R.string.init_authenticated_session_msg_subject);
                    SMIMEMessage smimeMessage = appVS.signMessage(currencyServer.getName(),
                            JSON.getMapper().writeValueAsString(messageDto), msgSubject,
                            currencyServer.getTimeStampServiceURL());
                    messageDto.setSMIME(smimeMessage);
                    session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                    LOGD(TAG + ".onStartCommand", "websocket session started");
                } catch(Exception ex) {
                    ex.printStackTrace();
                    appVS.broadcastResponse(ResponseVS.EXCEPTION(ex, appVS)
                            .setServiceCaller(ContextVS.WEB_SOCKET_BROADCAST_ID));
                }
            }
        }, "websocket_message_proccessor_thread").start();
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
                    //BUG with Android 5.0 and Tyrus client!!! Not WSS secured connections for now
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
                //sets the incoming buffer size to 1000000 bytes ~ 900K
                //client.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 1000000);
                //BUG with Android 5.0 and Tyrus client!!! Not WSS secured connections for now
                //https://java.net/projects/tyrus/lists/users/archive/2015-01/message/0
                final ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().
                        configurator(new ClientEndpointConfig.Configurator() {
                            @Override
                            public void beforeRequest(Map<String, List<String>> headers) {
                                //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
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
                                sendWebSocketBroadcast(
                                        JSON.getMapper().readValue(message, SocketMessageDto.class));
                            } catch (IOException e) { e.printStackTrace(); }
                            }
                        });
                        setWebSocketSession(session);
                    }
                    @Override public void onClose(Session session, CloseReason closeReason) {
                        sendWebSocketBroadcast(new SocketMessageDto(
                                ResponseVS.SC_OK, null, TypeVS.WEB_SOCKET_CLOSE));
                    }
                }, clientEndpointConfig, URI.create(serviceURL));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendWebSocketBroadcast(SocketMessageDto socketMsg) {
        LOGD(TAG + ".sendWebSocketBroadcast", "statusCode: " + socketMsg.getStatusCode() +
                " - type: " + socketMsg.getOperation());
        Intent intent =  new Intent(ContextVS.WEB_SOCKET_BROADCAST_ID);
        WebSocketSession socketSession = appVS.getWSSession(socketMsg.getUUID());
        try {
            if(socketSession == null && socketMsg.isEncrypted()) {
                byte[] decryptedBytes = appVS.decryptMessage(socketMsg.getAesParams().getBytes());
                AESParamsDto aesDto = JSON.getMapper().readValue(decryptedBytes, AESParamsDto.class);
                AESParams aesParams = AESParams.load(aesDto);
                socketMsg.decryptMessage(aesParams);
                appVS.putWSSession(socketMsg.getUUID(), new WebSocketSession(socketMsg));
            } else if (socketSession != null && socketMsg.isEncrypted()) {
                socketMsg.decryptMessage(socketSession.getAESParams());
            }
            intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, JSON.getMapper().writeValueAsString(socketMsg));
            switch(socketMsg.getOperation()) {
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        LOGD(TAG , "MESSAGEVS_FROM_VS - TypeVS: " + socketSession.getTypeVS());
                        socketMsg.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_SIGNED_SESSION:
                                if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                                    appVS.setConnectedDevice(socketMsg.getConnectedDevice());
                                    appVS.setWithSocketConnection(true);
                                } else appVS.setWithSocketConnection(false);
                                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                                break;
                            case OPERATION_CANCELED: break;
                            default: sendWebSocketBroadcast(socketMsg);
                        }
                    }
                    break;
                case WEB_SOCKET_CLOSE:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode())
                        appVS.setWithSocketConnection(false);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case MESSAGEVS:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode()) {
                        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, socketMsg.getMessage());
                        responseVS.setCaption(getString(R.string.message_lbl)).
                                setNotificationMessage(socketMsg.getMessage());
                        MessageContentProvider.insert(getContentResolver(), socketMsg);
                        PrefUtils.addNumMessagesNotReaded(appVS, 1);
                        Utils.showNewMessageNotification(appVS);
                    } else LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case MESSAGEVS_SIGN:
                    intent = new Intent(this, SMIMESignerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, JSON.getMapper().writeValueAsString(socketMsg));
                    startActivity(intent);
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case CURRENCY_WALLET_CHANGE:
                    if(socketMsg.getOperation() == TypeVS.MESSAGEVS_FROM_DEVICE) {
                        if(ResponseVS.SC_OK == socketMsg.getStatusCode() && socketSession != null) {
                            Wallet.removeCurrencyCollection((Collection<Currency>) socketSession.getData(), appVS);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        }
                    } else if(socketMsg.getOperation() == TypeVS.MESSAGEVS_TO_DEVICE) {
                        MessageContentProvider.insert(getContentResolver(), socketMsg);
                        PrefUtils.addNumMessagesNotReaded(appVS, 1);
                        Utils.showNewMessageNotification(appVS);
                    }
                    break;
                case OPERATION_CANCELED:
                    socketMsg.setOperation(socketSession.getTypeVS());
                    socketMsg.setStatusCode(ResponseVS.SC_CANCELED);
                    intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY,
                            JSON.getMapper().writeValueAsString(socketMsg));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                default: LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        } catch(Exception ex) {ex.printStackTrace();}
    }

}