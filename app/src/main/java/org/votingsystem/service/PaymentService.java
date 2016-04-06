package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
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
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.CurrencyBundle;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.Operation;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
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

    private AppVS appVS;

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        if(appVS.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            appVS.broadcastResponse(ResponseVS.ERROR(getString(R.string.error_lbl),
                    getString(R.string.server_connection_error_msg, appVS.getCurrencyServerURL())));
            return;
        }
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        char[] pin = arguments.getCharArray(ContextVS.PIN_KEY);
        String hashCertVS = arguments.getString(ContextVS.HASH_CERTVS_KEY);
        TransactionDto transactionDto = (TransactionDto) intent.getSerializableExtra(
                ContextVS.TRANSACTION_KEY);
        try {
            switch(operation) {
                case CURRENCY_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case CURRENCY_CHECK:
                    checkCurrency(serviceCaller, hashCertVS);
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
            appVS.broadcastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseVS.EXCEPTION(ex, this), this));
        }
    }

    private void processTransaction(String serviceCaller, TransactionDto transactionDto) {
        LOGD(TAG + ".processTransaction", "operation: " + transactionDto.getOperation());
        ResponseVS responseVS = null;
        if(transactionDto.getDateCreated() != null && DateUtils.inRange(transactionDto.getDateCreated(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionDto.getType()) {
                    case FROM_USER:
                        Operation operation = new Operation(TypeVS.FROM_USER, transactionDto,
                                Operation.State.PENDING);
                        Uri operationUri = getContentResolver().insert(
                                OperationContentProvider.CONTENT_URI,
                                OperationContentProvider.getContentValues(operation));
                        responseVS = sendTransaction(transactionDto.getTransactionFromUser());
                        if(ResponseVS.SC_OK == responseVS.getStatusCode() &&
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
                                        .getResponse(ResponseVS.SC_OK, null,receipt,
                                        TypeVS.TRANSACTION_RESPONSE);
                                socketRespDto.setCmsMessagePEM(base64Receipt);
                                //backup to recover from fails
                                transactionDto.setSocketMessageDto(socketRespDto);
                                AppVS.getInstance().getWSSession(socketRespDto.getUUID()).setData(transactionDto);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                //this is to send the signed receipts to the online service server receptor
                                //of the transaction
                                TransactionResponseDto responseDto = new TransactionResponseDto();
                                responseDto.setOperation(TypeVS.FROM_USER);
                                responseDto.setCMSMessage(base64Receipt);
                                responseVS = HttpHelper.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
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
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            String message = transactionDto.validateReceipt(responseVS.getCMS(), false);
                            if(transactionDto.getSocketMessageDto() != null) {

                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseVS.SC_OK, null, responseVS.getCMS(),
                                        TypeVS.TRANSACTION_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        TypeVS.CURRENCY_SEND, null, responseVS.getCMS());
                                responseVS = HttpHelper.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
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
                        if(!transactionDto.getQrMessageDto().getHashCertVS().equals(
                                currencyDto.getHashCertVS()))
                            throw new ValidationExceptionVS("currency request hash mismatch");
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            String message = transactionDto.validateReceipt(responseVS.getCMS(), false);
                            if(transactionDto.getSocketMessageDto() != null) {
                                String responseMessage = transactionDto.getQrMessageDto() == null?
                                        null: transactionDto.getQrMessageDto().getCurrencyChangeCert();
                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseVS.SC_OK, responseMessage,
                                        responseVS.getCMS(), TypeVS.TRANSACTION_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        TypeVS.CURRENCY_CHANGE,
                                        transactionDto.getQrMessageDto().getCurrencyChangeCert(),
                                        responseVS.getCMS());
                                responseVS = HttpHelper.getInstance().sendData(JSON.writeValueAsBytes(responseDto),
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
                responseVS = ResponseVS.EXCEPTION(ex, this);
            }
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                getString(R.string.payment_session_expired_msg));
        appVS.broadcastResponse(Utils.getBroadcastResponse(transactionDto.getOperation(),
                serviceCaller, responseVS, appVS));
    }

    private void sendSocketMessage(SocketMessageDto socketMessage) {
        LOGD(TAG + ".sendSocketMessage() ", "sendSocketMessage");
        try {
            Intent startIntent = new Intent(this, WebSocketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, socketMessage.getOperation());
            startIntent.putExtra(ContextVS.MESSAGE_KEY,
                    JSON.writeValueAsString(socketMessage));
            startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    private ResponseVS currencyRequest(String serviceCaller, TransactionDto transactionDto,
                           char[] pin){
        CurrencyServerDto currencyServer = appVS.getCurrencyServer();
        ResponseVS responseVS = null;
        try {
            LOGD(TAG + ".currencyRequest", "Amount: " + transactionDto.getAmount());
            String messageSubject = getString(R.string.currency_request_msg_subject);
            CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto,
                    transactionDto.getAmount(), currencyServer.getServerURL());
            byte[] contentToSign =  JSON.writeValueAsBytes(requestDto);
            CMSSignedMessage cmsMessage = appVS.signMessage(contentToSign);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, JSON.writeValueAsBytes(requestDto.getRequestCSRSet()));
            mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, cmsMessage.toPEM());
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ResultListDto<String> resultListDto = (ResultListDto<String>) responseVS.getMessage(
                        new TypeReference<ResultListDto<String>>(){});
                requestDto.loadCurrencyCerts(resultListDto.getResultList());
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                responseVS.setCaption(getString(R.string.currency_request_ok_caption)).setNotificationMessage(
                        getString(R.string.currency_request_ok_msg, requestDto.getTotalAmount(),
                        requestDto.getCurrencyCode()));
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                updateUserInfo(serviceCaller);
            } else responseVS.setCaption(getString(
                    R.string.currency_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            appVS.broadcastResponse(
                    responseVS.setTypeVS(TypeVS.CURRENCY_REQUEST).setServiceCaller(serviceCaller));
            return responseVS;
        }
    }

    private ResponseVS sendTransaction(TransactionDto transactionDto) {
        LOGD(TAG + ".sendTransaction", "transactionDto: " + transactionDto.toString());
        ResponseVS responseVS = null;
        try {
            CurrencyServerDto currencyServer = appVS.getCurrencyServer();
            CMSSignedMessage cmsMessage = appVS.signMessage(JSON.writeValueAsBytes(transactionDto));
            responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(),
                    ContentType.JSON_SIGNED, currencyServer.getTransactionServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS sendCurrencyBatch(TransactionDto transactionDto) {
        LOGD(TAG + ".sendCurrencyBatch", "sendCurrencyBatch");
        ResponseVS responseVS = null;
        try {
            CurrencyServerDto currencyServer = appVS.getCurrencyServer();
            CurrencyBundle currencyBundle = Wallet.getCurrencyBundleForTag(
                    transactionDto.getCurrencyCode(), transactionDto.getTagName());
            CurrencyBatchDto requestDto = null;
            if(TypeVS.CURRENCY_CHANGE == transactionDto.getOperation()) {
                transactionDto.setToUserIBAN(null);
                requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
                CMSSignedMessage cmsMessage = transactionDto.getCMSMessage();
                requestDto.setCurrencyChangeCSR(cmsMessage.getSignedContentStr());
            } else requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            Operation operation = new Operation(transactionDto.getOperation(), requestDto,
                    Operation.State.PENDING);
            Uri operationUri = getContentResolver().insert(OperationContentProvider.CONTENT_URI,
                    OperationContentProvider.getContentValues(operation));
            responseVS = HttpHelper.getInstance().sendData(JSON.writeValueAsBytes(requestDto),
                    ContentType.JSON, currencyServer.getCurrencyTransactionServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
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
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void updateUserInfo(String serviceCaller) {
        LOGD(TAG + ".updateUserInfo", "updateUserInfo");
        if(appVS.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            return;
        }
        if(appVS.getUser() == null) {
            LOGD(TAG + ".updateUserInfo", "missing user data");
            return;
        }
        ResponseVS responseVS = null;
        try {
            String targetService = appVS.getCurrencyServer().getUserBalanceServiceURL();
            responseVS = HttpHelper.getInstance().getData(targetService, ContentType.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                BalancesDto accountsInfo = (BalancesDto) responseVS.getMessage(BalancesDto.class);
                PrefUtils.putBalances(accountsInfo, DateUtils.getCurrentWeekPeriod());
                TransactionContentProvider.updateUserTransactionList(appVS, accountsInfo);
            } else responseVS.setCaption(getString(R.string.error_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                responseVS.setNotificationMessage(getString(R.string.currency_accounts_updated));
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.CURRENCY_ACCOUNTS_INFO);
            appVS.broadcastResponse(responseVS);
        }
    }

    private void checkCurrency(String serviceCaller, String hashCertVS) {
        LOGD(TAG + ".checkCurrency", "checkCurrency");
        ResponseVS responseVS = null;
        try {
            Set<String> hashCertVSSet = null;
            if(hashCertVS != null) hashCertVSSet = new HashSet<>(Arrays.asList(hashCertVS));
            else hashCertVSSet = Wallet.getHashCertVSSet();
            if(hashCertVSSet == null) {
                LOGD(TAG + ".checkCurrency", "empty hashCertVSSet nothing to check");
                return;
            }
            Set<CurrencyStateDto> responseDto =  HttpHelper.getInstance().sendData(
                    new TypeReference<Set<CurrencyStateDto>>() {},
                    JSON.writeValueAsBytes(hashCertVSSet),
                    appVS.getCurrencyServer().getCurrencyBundleStateServiceURL(),
                    MediaType.JSON);
            Set<CurrencyStateDto> currencyWithErrors = new HashSet<>();
            Set<String> currencyOKSet = new HashSet<>();
            for(CurrencyStateDto currencyDto: responseDto) {
                if(Currency.State.OK == currencyDto.getState()) {
                    currencyOKSet.add(currencyDto.getHashCertVS());
                } else currencyWithErrors.add(currencyDto);
            }
            if(!currencyOKSet.isEmpty()) {
                Wallet.updateCurrencyState(currencyOKSet, Currency.State.OK);
            }
            if(!currencyWithErrors.isEmpty()) {
                Set<Currency> removedSet = Wallet.removeErrors(currencyWithErrors);
                if(!removedSet.isEmpty()) {
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                            MsgUtils.getUpdateCurrencyWithErrorMsg(removedSet, appVS));
                    responseVS.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                            serviceCaller).setTypeVS(TypeVS.CURRENCY_CHECK);
                    appVS.broadcastResponse(responseVS);
                }
            } else {
                responseVS = new ResponseVS(ResponseVS.SC_OK).setServiceCaller(serviceCaller)
                        .setTypeVS(TypeVS.CURRENCY_CHECK);
                appVS.broadcastResponse(responseVS);
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

}