package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.WalletActivity;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.util.CurrencyBundle;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyIssuedDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.model.CurrencyRequestBatch;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
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

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        if(contextVS.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                    contextVS.getCurrencyServerURL()), Toast.LENGTH_LONG).show();
            return;
        }
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String pin = arguments.getString(ContextVS.PIN_KEY);
        String hashCertVS = arguments.getString(ContextVS.HASH_CERTVS_KEY);
        String dtoStr = intent.getStringExtra(ContextVS.TRANSACTION_KEY);
        TransactionVSDto transactionDto = null;
        try {
            if(dtoStr != null) transactionDto = JSON.getMapper().readValue(dtoStr, TransactionVSDto.class);
        } catch(Exception ex) { ex.printStackTrace();}
        try {
            switch(transactionDto.getOperation()) {
                case CURRENCY_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case CURRENCY_CHECK:
                    checkCurrency(serviceCaller, hashCertVS);
                    break;
                case CURRENCY_REQUEST:
                    currencyRequest(serviceCaller, transactionDto, pin);
                    break;
                case SIGNED_TRANSACTION:
                case CURRENCY_BATCH:
                    processTransaction(serviceCaller, transactionDto);
                    break;
                case CASH_SEND:
                    break;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            contextVS.broadcastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseVS.EXCEPTION(ex, this), this));
        }
    }

    private void processTransaction(String serviceCaller, TransactionVSDto transactionDto) {
        LOGD(TAG + ".processTransaction", "processTransaction - operation: " + transactionDto.getOperation());
        UserVSDto userVS = PrefUtils.getSessionUserVS(this);
        ResponseVS responseVS = null;
        if(transactionDto.getDate() != null && DateUtils.inRange(transactionDto.getDate(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionDto.getPaymentMethod()) {
                    case SIGNED_TRANSACTION:
                        responseVS = sendTransactionVS(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionDto.getPaymentConfirmURL());
                        }
                        break;
                    case CURRENCY_BATCH:
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionDto.getPaymentConfirmURL());
                            responseVS.setMessage(MsgUtils.getAnonymousSignedTransactionOKMsg(
                                    transactionDto, this));
                        }
                        break;
                    case CASH_SEND:
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, this);
            }
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                getString(R.string.payment_session_expired_msg));
        contextVS.broadcastResponse(Utils.getBroadcastResponse(transactionDto.getOperation(),
                serviceCaller, responseVS, contextVS));
    }

    private ResponseVS currencyRequest(String serviceCaller, TransactionVSDto transactionDto,
                           String pin){
        CurrencyServerDto currencyServer = contextVS.getCurrencyServer();
        ResponseVS responseVS = null;
        try {
            LOGD(TAG + ".currencyRequest", "Amount: " + transactionDto.getAmount());
            String messageSubject = getString(R.string.currency_request_msg_subject);
            String fromUser = contextVS.getUserVS().getNIF();
            String requestDataFileName = ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME + ":" +
                    MediaTypeVS.JSON_SIGNED;
            CurrencyRequestBatch requestBatch = CurrencyRequestBatch.createRequest(
                    transactionDto, currencyServer.getServerURL());
            byte[] requestBytes = JSON.getMapper().writeValueAsString(
                    requestBatch.getCurrencyCSRList()).getBytes();
            String signatureContent =  JSON.getMapper().writeValueAsString(requestBatch.getRequestDto());
            SMIMEMessage smimeMessage = contextVS.signMessage(currencyServer.getName(),
                    signatureContent, messageSubject);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
            mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CurrencyIssuedDto currencyIssued = (CurrencyIssuedDto) responseVS.getMessage(
                        CurrencyIssuedDto.class);
                requestBatch.loadIssuedCurrency(currencyIssued.getIssuedCurrency());
                Wallet.saveCurrencyCollection(requestBatch.getCurrencyMap().values(), pin, contextVS);
                responseVS.setCaption(getString(R.string.currency_request_ok_caption)).setNotificationMessage(
                        getString(R.string.currency_request_ok_msg, requestBatch.getRequestAmount(),
                                requestBatch.getCurrencyCode()));
                Wallet.saveCurrencyCollection(requestBatch.getCurrencyMap().values(), pin, contextVS);
                updateUserInfo(serviceCaller);
            } else responseVS.setCaption(getString(
                    R.string.currency_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            contextVS.broadcastResponse(
                    responseVS.setTypeVS(TypeVS.CURRENCY_REQUEST).setServiceCaller(serviceCaller));
            return responseVS;
        }
    }

    private ResponseVS sendTransactionVS(TransactionVSDto transactionDto) {
        LOGD(TAG + ".sendTransactionVS", "transactionDto: " + transactionDto.toString());
        ResponseVS responseVS = null;
        try {
            CurrencyServerDto currencyServer = contextVS.getCurrencyServer();
            SMIMEMessage smimeMessage = contextVS.signMessage(transactionDto.getToUserIBAN().get(0),
                    JSON.getMapper().writeValueAsString(transactionDto), getString(R.string.FROM_USERVS_msg_subject));
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
            CurrencyServerDto currencyServer = contextVS.getCurrencyServer();
            CurrencyBundle currencyBundle = Wallet.getCurrencyBundleForTransaction(transactionDto);
            CurrencyBatchDto requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            responseVS = HttpHelper.sendData(JSON.getMapper().writeValueAsBytes(requestDto),
                    ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CurrencyBatchResponseDto responseDto = (CurrencyBatchResponseDto) responseVS.getMessage(CurrencyBatchDto.class);
                Currency leftOverCurrency = null;
                if(currencyBundle.getLeftOverCurrency() != null && responseDto.getLeftOverCoin() != null) {
                    leftOverCurrency = currencyBundle.getLeftOverCurrency();
                    leftOverCurrency.initSigner(responseDto.getLeftOverCoin().getBytes());
                }
                SMIMEMessage receipt = new SMIMEMessage(new ByteArrayInputStream(
                        Base64.decode(responseDto.getReceipt())));
                transactionDto.setReceipt(responseDto.getReceipt());
                Set<Currency> currencyToRemove = currencyBundle.getCurrencySet();
                Wallet.removeCurrencyCollection(currencyToRemove, contextVS);
                if(leftOverCurrency != null) Wallet.updateWallet(Arrays.asList(leftOverCurrency), contextVS);
                responseVS.setSMIME(receipt);
            } else if(ResponseVS.SC_CURRENCY_EXPENDED == responseVS.getStatusCode()) {
                Currency expendedCurrency = Wallet.removeExpendedCurrency(responseVS.getMessage(), contextVS);
                responseVS.setMessage(getString(R.string.expended_currency_error_msg, expendedCurrency.
                        getAmount().toString() + " " + expendedCurrency.getCurrencyCode()));
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
        if(contextVS.getCurrencyServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            return;
        }
        ResponseVS responseVS = null;
        try {
            String targetService = contextVS.getCurrencyServer().getUserInfoServiceURL(
                    contextVS.getUserVS().getNIF());
            responseVS = HttpHelper.getData(targetService, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                BalancesDto accountsInfo = (BalancesDto) responseVS.getMessage(BalancesDto.class);
                PrefUtils.putBalances(accountsInfo, DateUtils.getCurrentWeekPeriod(),
                        contextVS);
                TransactionVSContentProvider.updateUserVSTransactionVSList(contextVS, accountsInfo);
            } else responseVS.setCaption(getString(R.string.error_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                responseVS.setNotificationMessage(getString(R.string.currency_accounts_updated));
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.CURRENCY_ACCOUNTS_INFO);
            contextVS.broadcastResponse(responseVS);
            Utils.showAccountsUpdatedNotification(contextVS);
        }
    }

    private void checkCurrency(String serviceCaller, String hashCertVS) {
        LOGD(TAG + ".checkCurrency", "checkCurrency");
        ResponseVS responseVS = null;
        try {
            Set<String> hashCertVSList = null;
            if(hashCertVS != null) hashCertVSList = new HashSet<>(Arrays.asList(hashCertVS));
            else hashCertVSList = Wallet.getHashCertVSSet();
            if(hashCertVSList == null) {
                LOGD(TAG + ".checkCurrency", "empty hashCertVSList");
                return;
            }
            Map<String, Currency.State> responseDto =  HttpHelper.sendData(
                    new TypeReference<Map<String, Currency.State>>() {},
                    JSON.getMapper().writeValueAsBytes(hashCertVSList),
                    contextVS.getCurrencyServer().getCurrencyBundleStateServiceURL(),
                    MediaTypeVS.JSON);
            List<String> currencyWithErrorList = new ArrayList<>();
            List<String> currencyOKList = new ArrayList<>();
            Set<Currency> currencyFromWalletWithErrors = null;
            for(String responseHashCertVS: responseDto.keySet()) {
                if(Currency.State.OK == responseDto.get(responseHashCertVS)) {
                    currencyOKList.add(responseHashCertVS);
                } else currencyWithErrorList.add(responseHashCertVS);
            }
            if(currencyWithErrorList.size() > 0) {
                currencyFromWalletWithErrors = Wallet.updateCurrencyWithErrors(currencyWithErrorList, contextVS);
            }
            if(currencyFromWalletWithErrors != null && !currencyFromWalletWithErrors.isEmpty()) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, MsgUtils.getUpdateCurrencyWithErrorMsg(
                        currencyFromWalletWithErrors, contextVS));
                responseVS.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                        serviceCaller).setTypeVS(TypeVS.CURRENCY_CHECK);
                Intent intent = new Intent(this, WalletActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                startActivity(intent);
            } else if(!currencyOKList.isEmpty()) {
                Wallet.updateCurrencyState(currencyOKList, Currency.State.OK, contextVS);
                responseVS = new ResponseVS(ResponseVS.SC_OK);
                responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.CURRENCY_CHECK);
                contextVS.broadcastResponse(responseVS);
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

}