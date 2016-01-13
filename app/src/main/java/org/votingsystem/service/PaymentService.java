package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.OperationVSContentProvider;
import org.votingsystem.contentprovider.TransactionVSContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.TransactionResponseDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.CurrencyBundle;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.OperationVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.Utils;
import org.votingsystem.util.Wallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        TransactionVSDto transactionDto = (TransactionVSDto) intent.getSerializableExtra(
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
                case FROM_USERVS:
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

    private void processTransaction(String serviceCaller, TransactionVSDto transactionDto) {
        LOGD(TAG + ".processTransaction", "operation: " + transactionDto.getOperation());
        ResponseVS responseVS = null;
        if(transactionDto.getDateCreated() != null && DateUtils.inRange(transactionDto.getDateCreated(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionDto.getType()) {
                    case FROM_USERVS:
                        OperationVS operationVS = new OperationVS(TypeVS.FROM_USERVS, transactionDto,
                                OperationVS.State.PENDING);
                        Uri operationUri = getContentResolver().insert(
                                OperationVSContentProvider.CONTENT_URI,
                                OperationVSContentProvider.getContentValues(operationVS));
                        responseVS = sendTransactionVS(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode() &&
                                transactionDto.getPaymentConfirmURL() != null) {
                            ResultListDto<TransactionVSDto> resultList =
                                    (ResultListDto<TransactionVSDto>) responseVS.getMessage(
                                    new TypeReference<ResultListDto<TransactionVSDto>>() {});
                            String base64Receipt = resultList.getResultList().iterator().next().getMessageSMIME();
                            SMIMEMessage receipt = new SMIMEMessage(Base64.decode(base64Receipt));
                            String message = transactionDto.validateReceipt(receipt, false);
                            receipt.isValidSignature();
                            if(transactionDto.getSocketMessageDto() != null) {
                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseVS.SC_OK, null,
                                        AppVS.getInstance().getConnectedDevice().getId(),
                                        TypeVS.TRANSACTIONVS_RESPONSE);
                                socketRespDto.setSmimeMessage(base64Receipt);
                                //backup to recover from fails
                                transactionDto.setSocketMessageDto(socketRespDto);
                                AppVS.getInstance().getWSSession(socketRespDto.getUUID()).setData(transactionDto);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto();
                                responseDto.setOperation(TypeVS.FROM_USERVS);
                                responseDto.setSmimeMessage(base64Receipt);
                                responseVS = HttpHelper.sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentTypeVS.JSON, transactionDto.getPaymentConfirmURL());
                            }
                            operationVS.setState(OperationVS.State.FINISHED);
                            getContentResolver().delete(operationUri, null, null);
                        } else {
                            operationVS.setState(OperationVS.State.ERROR);
                            getContentResolver().update(operationUri, OperationVSContentProvider
                                    .getContentValues(operationVS), null, null);
                        }
                        break;
                    case CURRENCY_SEND:
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            String message = transactionDto.validateReceipt(responseVS.getSMIME(), false);
                            if(transactionDto.getSocketMessageDto() != null) {

                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseVS.SC_OK, null,
                                        AppVS.getInstance().getConnectedDevice().getId(),
                                        responseVS.getSMIME(),
                                        TypeVS.TRANSACTIONVS_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        TypeVS.CURRENCY_SEND, null, responseVS.getSMIME());
                                responseVS = HttpHelper.sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentTypeVS.JSON_SIGNED, transactionDto.getPaymentConfirmURL());
                            }
                            responseVS.setMessage(message);
                        }
                        break;
                    case CURRENCY_CHANGE:
                        SMIMEMessage currencyRequest = new SMIMEMessage(Base64.decode(
                                transactionDto.getMessageSMIME()));
                        CurrencyDto currencyDto = new CurrencyDto(
                                CertUtils.fromPEMToPKCS10CertificationRequest(
                                currencyRequest.getSignedContent().getBytes()));
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
                            String message = transactionDto.validateReceipt(responseVS.getSMIME(), false);
                            if(transactionDto.getSocketMessageDto() != null) {
                                String responseMessage = transactionDto.getQrMessageDto() == null?
                                        null: transactionDto.getQrMessageDto().getCurrencyChangeCert();
                                SocketMessageDto socketRespDto = transactionDto.getSocketMessageDto()
                                        .getResponse(ResponseVS.SC_OK, responseMessage,
                                        AppVS.getInstance().getConnectedDevice().getId(),
                                        responseVS.getSMIME(), TypeVS.TRANSACTIONVS_RESPONSE);
                                sendSocketMessage(socketRespDto);
                                responseVS.setMessage(message);
                            } else {
                                TransactionResponseDto responseDto = new TransactionResponseDto(
                                        TypeVS.CURRENCY_CHANGE,
                                        transactionDto.getQrMessageDto().getCurrencyChangeCert(),
                                        responseVS.getSMIME());
                                responseVS = HttpHelper.sendData(JSON.writeValueAsBytes(responseDto),
                                        ContentTypeVS.JSON_SIGNED, transactionDto.getPaymentConfirmURL());
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

    private ResponseVS currencyRequest(String serviceCaller, TransactionVSDto transactionDto,
                           char[] pin){
        CurrencyServerDto currencyServer = appVS.getCurrencyServer();
        ResponseVS responseVS = null;
        try {
            LOGD(TAG + ".currencyRequest", "Amount: " + transactionDto.getAmount());
            String messageSubject = getString(R.string.currency_request_msg_subject);
            CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto,
                    transactionDto.getAmount(), currencyServer.getServerURL());
            String signatureContent =  JSON.writeValueAsString(requestDto);
            SMIMEMessage smimeMessage = appVS.signMessage(currencyServer.getName(),
                    signatureContent, messageSubject);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, JSON.writeValueAsBytes(requestDto.getRequestCSRSet()));
            mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
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

    private ResponseVS sendTransactionVS(TransactionVSDto transactionDto) {
        LOGD(TAG + ".sendTransactionVS", "transactionDto: " + transactionDto.toString());
        ResponseVS responseVS = null;
        try {
            CurrencyServerDto currencyServer = appVS.getCurrencyServer();
            SMIMEMessage smimeMessage = appVS.signMessage(transactionDto.getToUserIBAN().iterator().next(),
                    JSON.writeValueAsString(transactionDto), getString(R.string.FROM_USERVS_msg_subject));
            responseVS = HttpHelper.sendData(smimeMessage.getBytes(),
                    ContentTypeVS.JSON_SIGNED, currencyServer.getTransactionVSServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS sendCurrencyBatch(TransactionVSDto transactionDto) {
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
                SMIMEMessage smimeMessage = transactionDto.getSmimeMessage();
                requestDto.setCurrencyChangeCSR(smimeMessage.getSignedContent());
            } else requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            OperationVS operationVS = new OperationVS(transactionDto.getOperation(), requestDto,
                    OperationVS.State.PENDING);
            Uri operationUri = getContentResolver().insert(OperationVSContentProvider.CONTENT_URI,
                    OperationVSContentProvider.getContentValues(operationVS));
            responseVS = HttpHelper.sendData(JSON.writeValueAsBytes(requestDto),
                    ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CurrencyBatchResponseDto responseDto = (CurrencyBatchResponseDto)
                        responseVS.getMessage(CurrencyBatchResponseDto.class);
                if(transactionDto.getQrMessageDto() != null) transactionDto.getQrMessageDto().
                        setCurrencyChangeCert(responseDto.getCurrencyChangeCert());
                SMIMEMessage smimeMessage = requestDto.validateResponse(responseDto,
                        currencyServer.getTrustAnchors());
                responseVS.setSMIME(smimeMessage);
                Wallet.updateWallet(requestDto);
                operationVS.setState(OperationVS.State.FINISHED);
                getContentResolver().delete(operationUri, null, null);
            } else {
                operationVS.setState(OperationVS.State.ERROR).setStatusCode(responseVS.getStatusCode())
                        .setMessage(responseVS.getMessage());
                getContentResolver().update(operationUri, OperationVSContentProvider
                        .getContentValues(operationVS), null, null);
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
        ResponseVS responseVS = null;
        try {
            String targetService = appVS.getCurrencyServer().getUserInfoServiceURL(
                    appVS.getUserVS().getNIF());
            responseVS = HttpHelper.getData(targetService, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                BalancesDto accountsInfo = (BalancesDto) responseVS.getMessage(BalancesDto.class);
                PrefUtils.putBalances(accountsInfo, DateUtils.getCurrentWeekPeriod());
                TransactionVSContentProvider.updateUserVSTransactionVSList(appVS, accountsInfo);
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
            Set<CurrencyStateDto> responseDto =  HttpHelper.sendData(
                    new TypeReference<Map<String, CurrencyStateDto>>() {},
                    JSON.writeValueAsBytes(hashCertVSSet),
                    appVS.getCurrencyServer().getCurrencyBundleStateServiceURL(),
                    MediaTypeVS.JSON);
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