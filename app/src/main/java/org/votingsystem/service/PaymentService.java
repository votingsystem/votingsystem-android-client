package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.OperationContentProvider;
import org.votingsystem.contentprovider.TransactionContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.currency.TransactionResponseDto;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.CurrencyBundle;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.Operation;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;
import org.votingsystem.util.crypto.PEMUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PaymentService extends IntentService {

    public static final String TAG = PaymentService.class.getSimpleName();

    public static final long FOUR_MINUTES = 60 * 4 * 1000;

    public PaymentService() { super(TAG); }

    private App app;

    @Override protected void onHandleIntent(Intent intent) {
        app = (App) getApplicationContext();
        if(app.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            app.broadcastResponse(ResponseDto.ERROR(getString(R.string.error_lbl),
                    getString(R.string.server_connection_error_msg, app.getCurrencyServerURL())));
            return;
        }
        final Bundle arguments = intent.getExtras();
        OperationType operation = (OperationType)arguments.getSerializable(Constants.TYPEVS_KEY);
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        char[] pin = arguments.getCharArray(Constants.PIN_KEY);
        String revocationHash = arguments.getString(Constants.HASH_CERTVS_KEY);
        TransactionDto transactionDto = (TransactionDto) intent.getSerializableExtra(
                Constants.TRANSACTION_KEY);
        try {
            switch(operation) {
                case CURRENCY_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case CURRENCY_CHECK:
                    checkCurrency(serviceCaller, revocationHash);
                    break;
                case CURRENCY_REQUEST:
                    currencyRequest(serviceCaller, transactionDto, pin);
                    break;
                case CURRENCY_CHANGE:
                case FROM_USER:
                case CURRENCY_SEND:
                    processTransaction(serviceCaller, transactionDto);
                    break;
                default:
                    LOGD(TAG + ".onHandleIntent", "unknown operation: " + operation);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            app.broadcastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseDto.EXCEPTION(ex, this), this));
        }
    }

    private void processTransaction(String serviceCaller, TransactionDto transactionDto) {
        LOGD(TAG + ".processTransaction", "operation: " + transactionDto.getOperation());
        ResponseDto responseVS = null;
        if(transactionDto.getDateCreated() != null && DateUtils.inRange(transactionDto.getDateCreated(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionDto.getType()) {
                    case FROM_USER:
                        Operation operation = new Operation(OperationType.FROM_USER, transactionDto,
                                Operation.State.PENDING);
                        Uri operationUri = getContentResolver().insert(
                                OperationContentProvider.CONTENT_URI,
                                OperationContentProvider.getContentValues(operation));
                        responseVS = sendTransaction(transactionDto.getTransactionFromUser());
                        if(ResponseDto.SC_OK == responseVS.getStatusCode() &&
                                transactionDto.getPaymentConfirmURL() != null) {
                            ResultListDto<TransactionDto> resultList =
                                    (ResultListDto<TransactionDto>) responseVS.getMessage(
                                    new TypeReference<ResultListDto<TransactionDto>>() {});
                            String base64Receipt = resultList.getResultList().iterator().next().getCmsMessagePEM();
                            CMSSignedMessage receipt = new CMSSignedMessage(Base64.decode(base64Receipt, Base64.NO_WRAP));
                            String message = transactionDto.validateReceipt(receipt, false);
                            receipt.isValidSignature();
                            if(transactionDto.getSocketMessageDto() != null) {
                                //this is to send the signed receipts to the user that showed the QR code
                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseDto.SC_OK, null,receipt,
                                        OperationType.TRANSACTION_RESPONSE);
                                socketRespDto.setCmsMessagePEM(base64Receipt);
                                //backup to recover from fails
                                transactionDto.setSocketMessageDto(socketRespDto);
                                App.getInstance().getWSSession(socketRespDto.getUUID()).setData(transactionDto);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                //this is to send the signed receipts to the online service server receptor
                                //of the transaction
                                TransactionResponseDto responseDto = new TransactionResponseDto();
                                responseDto.setOperation(OperationType.FROM_USER);
                                responseDto.setCMSMessage(base64Receipt);
                                responseVS = HttpConnection.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentType.JSON, transactionDto.getPaymentConfirmURL());
                            }
                            operation.setState(Operation.State.FINISHED);
                            getContentResolver().delete(operationUri, null, null);
                        } else {
                            operation.setState(Operation.State.ERROR);
                            getContentResolver().update(operationUri, OperationContentProvider
                                    .getContentValues(operation), null, null);
                        }
                        break;
                    case CURRENCY_SEND:
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseDto.SC_OK == responseVS.getStatusCode()) {
                            String message = transactionDto.validateReceipt(responseVS.getCMS(), false);
                            if(transactionDto.getSocketMessageDto() != null) {

                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseDto.SC_OK, null, responseVS.getCMS(),
                                        OperationType.TRANSACTION_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        OperationType.CURRENCY_SEND, null, responseVS.getCMS());
                                responseVS = HttpConnection.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentType.JSON_SIGNED, transactionDto.getPaymentConfirmURL());
                            }
                            responseVS.setMessage(message);
                        }
                        break;
                    case CURRENCY_CHANGE:
                        CMSSignedMessage currencyRequest = CMSSignedMessage.FROM_PEM(transactionDto.getCmsMessagePEM());
                        CurrencyDto currencyDto = new CurrencyDto(
                                PEMUtils.fromPEMToPKCS10CertificationRequest(
                                currencyRequest.getSignedContentStr().getBytes()));
                        if(!transactionDto.getTagName().equals(currencyDto.getTag()))
                            throw new ValidationExceptionVS("Transaction tag: " + transactionDto.getTagName() +
                            " doesn't match currency request tag: " + currencyDto.getTag());
                        if(!transactionDto.getCurrencyCode().equals(currencyDto.getCurrencyCode()))
                            throw new ValidationExceptionVS("Transaction CurrencyCode: " +
                                    transactionDto.getCurrencyCode() +
                                    " doesn't match currency CurrencyCode " + currencyDto.getCurrencyCode());
                        if(transactionDto.getAmount().compareTo(currencyDto.getAmount()) != 0)
                            throw new ValidationExceptionVS("Transaction amount: " +
                                    transactionDto.getAmount() +
                                    " doesn't match currency amount " + currencyDto.getAmount());
                        if(!transactionDto.getQrMessageDto().getRevocationHash().equals(
                                currencyDto.getRevocationHash()))
                            throw new ValidationExceptionVS("currency request hash mismatch");
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseDto.SC_OK == responseVS.getStatusCode()) {
                            String message = transactionDto.validateReceipt(responseVS.getCMS(), false);
                            if(transactionDto.getSocketMessageDto() != null) {
                                String responseMessage = transactionDto.getQrMessageDto() == null?
                                        null: transactionDto.getQrMessageDto().getCurrencyChangeCert();
                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseDto.SC_OK, responseMessage,
                                        responseVS.getCMS(), OperationType.TRANSACTION_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        OperationType.CURRENCY_CHANGE,
                                        transactionDto.getQrMessageDto().getCurrencyChangeCert(),
                                        responseVS.getCMS());
                                responseVS = HttpConnection.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentType.JSON_SIGNED, transactionDto.getPaymentConfirmURL());
                            }
                            responseVS.setMessage(message);
                        }
                        break;
                    default:
                        LOGD(TAG + ".processTransaction", "unknown operation: " + transactionDto.getType());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseDto.EXCEPTION(ex, this);
            }
        } else responseVS = new ResponseDto(ResponseDto.SC_ERROR,
                getString(R.string.session_expired_msg));
        app.broadcastResponse(Utils.getBroadcastResponse(transactionDto.getOperation(),
                serviceCaller, responseVS, app));
    }

    private void sendSocketMessage(SocketMessageDto socketMessage) {
        LOGD(TAG + ".sendSocketMessage() ", "sendSocketMessage");
        try {
            Intent startIntent = new Intent(this, WebSocketService.class);
            startIntent.putExtra(Constants.TYPEVS_KEY, socketMessage.getOperation());
            startIntent.putExtra(Constants.MESSAGE_KEY,
                    JSON.writeValueAsString(socketMessage));
            startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    private ResponseDto currencyRequest(String serviceCaller, TransactionDto transactionDto,
                                        char[] pin){
        CurrencyServerDto currencyServer = app.getCurrencyServer();
        ResponseDto responseDto = null;
        try {
            LOGD(TAG + ".currencyRequest", "Amount: " + transactionDto.getAmount());
            String messageSubject = getString(R.string.currency_request_msg_subject);
            CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto,
                    transactionDto.getAmount(), currencyServer.getServerURL());
            byte[] contentToSign =  JSON.writeValueAsBytes(requestDto);
            CMSSignedMessage cmsMessage = app.signMessage(contentToSign);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(Constants.CSR_FILE_NAME, JSON.writeValueAsBytes(requestDto.getRequestCSRSet()));
            mapToSend.put(Constants.CURRENCY_REQUEST_DATA_FILE_NAME, cmsMessage.toPEM());
            responseDto = HttpConnection.getInstance().sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                ResultListDto<String> resultListDto = (ResultListDto<String>) responseDto.getMessage(
                        new TypeReference<ResultListDto<String>>(){});
                requestDto.loadCurrencyCerts(resultListDto.getResultList());
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                responseDto.setCaption(getString(R.string.currency_request_ok_caption)).setNotificationMessage(
                        getString(R.string.currency_request_ok_msg, requestDto.getTotalAmount(),
                        requestDto.getCurrencyCode()));
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                updateUserInfo(serviceCaller);
            } else responseDto.setCaption(getString(
                    R.string.currency_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            app.broadcastResponse(
                    responseDto.setOperationType(OperationType.CURRENCY_REQUEST).setServiceCaller(serviceCaller));
            return responseDto;
        }
    }

    private ResponseDto sendTransaction(TransactionDto transactionDto) {
        LOGD(TAG + ".sendTransaction", "transactionDto: " + transactionDto.toString());
        ResponseDto responseDto = null;
        try {
            CurrencyServerDto currencyServer = app.getCurrencyServer();
            CMSSignedMessage cmsMessage = app.signMessage(JSON.writeValueAsBytes(transactionDto));
            responseDto = HttpConnection.getInstance().sendData(cmsMessage.toPEM(),
                    ContentType.JSON_SIGNED, currencyServer.getTransactionServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            return responseDto;
        }
    }

    private ResponseDto sendCurrencyBatch(TransactionDto transactionDto) {
        LOGD(TAG + ".sendCurrencyBatch", "sendCurrencyBatch");
        ResponseDto responseVS = null;
        try {
            CurrencyServerDto currencyServer = app.getCurrencyServer();
            CurrencyBundle currencyBundle = Wallet.getCurrencyBundleForTag(
                    transactionDto.getCurrencyCode(), transactionDto.getTagName());
            CurrencyBatchDto requestDto = null;
            if(OperationType.CURRENCY_CHANGE == transactionDto.getOperation()) {
                transactionDto.setToUserIBAN(null);
                requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
                CMSSignedMessage cmsMessage = transactionDto.getCMSMessage();
                requestDto.setCurrencyChangeCSR(cmsMessage.getSignedContentStr());
            } else requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            Operation operation = new Operation(transactionDto.getOperation(), requestDto,
                    Operation.State.PENDING);
            Uri operationUri = getContentResolver().insert(OperationContentProvider.CONTENT_URI,
                    OperationContentProvider.getContentValues(operation));
            responseVS = HttpConnection.getInstance().sendData(JSON.writeValueAsBytes(requestDto),
                    ContentType.JSON, currencyServer.getCurrencyTransactionServiceURL());
            if(ResponseDto.SC_OK == responseVS.getStatusCode()) {
                CurrencyBatchResponseDto responseDto = (CurrencyBatchResponseDto)
                        responseVS.getMessage(CurrencyBatchResponseDto.class);
                if(transactionDto.getQrMessageDto() != null) transactionDto.getQrMessageDto().
                        setCurrencyChangeCert(responseDto.getCurrencyChangeCert());
                CMSSignedMessage cmsMessage = requestDto.validateResponse(responseDto,
                        currencyServer.getTrustAnchors());
                responseVS.setCMS(cmsMessage);
                Wallet.updateWallet(requestDto);
                operation.setState(Operation.State.FINISHED);
                getContentResolver().delete(operationUri, null, null);
            } else {
                operation.setState(Operation.State.ERROR).setStatusCode(responseVS.getStatusCode())
                        .setMessage(responseVS.getMessage());
                getContentResolver().update(operationUri, OperationContentProvider
                        .getContentValues(operation), null, null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseDto.EXCEPTION(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void updateUserInfo(String serviceCaller) {
        LOGD(TAG + ".updateUserInfo", "updateUserInfo");
        if(app.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            return;
        }
        if(app.getUser() == null) {
            LOGD(TAG + ".updateUserInfo", "missing user data");
            return;
        }
        ResponseDto responseDto = null;
        try {
            String targetService = app.getCurrencyServer().getUserBalanceServiceURL();
            responseDto = HttpConnection.getInstance().getData(targetService, ContentType.JSON);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                BalancesDto accountsInfo = (BalancesDto) responseDto.getMessage(BalancesDto.class);
                PrefUtils.putBalances(accountsInfo);
                TransactionContentProvider.updateUserTransactionList(app, accountsInfo);
            } else responseDto.setCaption(getString(R.string.error_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            if(ResponseDto.SC_OK == responseDto.getStatusCode())
                responseDto.setNotificationMessage(getString(R.string.currency_accounts_updated));
            responseDto.setServiceCaller(serviceCaller).setOperationType(OperationType.CURRENCY_ACCOUNTS_INFO);
            app.broadcastResponse(responseDto);
        }
    }

    private void checkCurrency(String serviceCaller, String revocationHash) {
        LOGD(TAG + ".checkCurrency", "checkCurrency");
        ResponseDto responseVS = null;
        try {
            Set<String> revocationHashSet = null;
            if(revocationHash != null) revocationHashSet = new HashSet<>(Arrays.asList(revocationHash));
            else revocationHashSet = Wallet.getRevocationHashSet();
            if(revocationHashSet == null) {
                LOGD(TAG + ".checkCurrency", "empty revocationHashSet nothing to check");
                return;
            }
            Set<CurrencyStateDto> responseDto =  HttpConnection.getInstance().sendData(
                    new TypeReference<Set<CurrencyStateDto>>() {},
                    JSON.writeValueAsBytes(revocationHashSet),
                    app.getCurrencyServer().getCurrencyBundleStateServiceURL(),
                    MediaType.JSON);
            Set<CurrencyStateDto> currencyWithErrors = new HashSet<>();
            Set<String> currencyOKSet = new HashSet<>();
            for(CurrencyStateDto currencyDto: responseDto) {
                if(Currency.State.OK == currencyDto.getState()) {
                    currencyOKSet.add(currencyDto.getRevocationHash());
                } else currencyWithErrors.add(currencyDto);
            }
            if(!currencyOKSet.isEmpty()) {
                Wallet.updateCurrencyState(currencyOKSet, Currency.State.OK);
            }
            if(!currencyWithErrors.isEmpty()) {
                Set<Currency> removedSet = Wallet.removeErrors(currencyWithErrors);
                if(!removedSet.isEmpty()) {
                    responseVS = new ResponseDto(ResponseDto.SC_ERROR,
                            MsgUtils.getUpdateCurrencyWithErrorMsg(removedSet, app));
                    responseVS.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                            serviceCaller).setOperationType(OperationType.CURRENCY_CHECK);
                    app.broadcastResponse(responseVS);
                }
            } else {
                responseVS = new ResponseDto(ResponseDto.SC_OK).setServiceCaller(serviceCaller)
                        .setOperationType(OperationType.CURRENCY_CHECK);
                app.broadcastResponse(responseVS);
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

}