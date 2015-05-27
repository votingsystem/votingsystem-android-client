package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.AppVS;
import org.votingsystem.activity.WalletActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.TransactionVSContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.CurrencyBundle;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.MsgUtils;
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
        String pin = arguments.getString(ContextVS.PIN_KEY);
        String hashCertVS = arguments.getString(ContextVS.HASH_CERTVS_KEY);
        String dtoStr = intent.getStringExtra(ContextVS.TRANSACTION_KEY);
        TransactionVSDto transactionDto = null;
        try {
            if(dtoStr != null) {
                transactionDto = JSON.readValue(dtoStr, TransactionVSDto.class);
                operation = transactionDto.getOperation();
            }
        } catch(Exception ex) { ex.printStackTrace();}
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
                case SIGNED_TRANSACTION:
                case CURRENCY_BATCH:
                    processTransaction(serviceCaller, transactionDto);
                    break;
                case CASH_SEND:
                    break;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            appVS.broadcastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
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
                switch (transactionDto.getType()) {
                    case FROM_USERVS:
                        responseVS = sendTransactionVS(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionDto.getPaymentConfirmURL());
                        }
                        break;
                    case CURRENCY_SEND:
                        responseVS = sendCurrencyBatch(transactionDto);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionDto.getPaymentConfirmURL());
                            responseVS.setMessage(MsgUtils.getAnonymousSignedTransactionOKMsg(
                                    transactionDto, this));
                        }
                        break;
                    case CURRENCY_CHANGE:
                        break;
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

    private ResponseVS currencyRequest(String serviceCaller, TransactionVSDto transactionDto,
                           String pin){
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
                Wallet.saveCurrencyCollection(requestDto.getCurrencyMap().values(), pin, appVS);
                responseVS.setCaption(getString(R.string.currency_request_ok_caption)).setNotificationMessage(
                        getString(R.string.currency_request_ok_msg, requestDto.getTotalAmount(),
                        requestDto.getCurrencyCode()));
                Wallet.saveCurrencyCollection(requestDto.getCurrencyMap().values(), pin, appVS);
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
            SMIMEMessage smimeMessage = appVS.signMessage(transactionDto.getToUserIBAN().get(0),
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
            CurrencyBundle currencyBundle = Wallet.getCurrencyBundleForTransaction(transactionDto);
            CurrencyBatchDto requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            responseVS = HttpHelper.sendData(JSON.writeValueAsBytes(requestDto),
                    ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CurrencyBatchResponseDto responseDto = (CurrencyBatchResponseDto) responseVS.getMessage(CurrencyBatchDto.class);
                Currency leftOverCurrency = null;
                if(currencyBundle.getLeftOverCurrency() != null && responseDto.getLeftOverCoin() != null) {
                    leftOverCurrency = currencyBundle.getLeftOverCurrency();
                    leftOverCurrency.initSigner(responseDto.getLeftOverCoin().getBytes());
                }
                SMIMEMessage receipt = new SMIMEMessage(Base64.decode(responseDto.getReceipt()));
                transactionDto.setReceipt(responseDto.getReceipt());
                Set<Currency> currencyToRemove = currencyBundle.getCurrencySet();
                Wallet.removeCurrencyCollection(currencyToRemove, appVS);
                if(leftOverCurrency != null) Wallet.updateWallet(Arrays.asList(leftOverCurrency), appVS);
                responseVS.setSMIME(receipt);
            } else if(ResponseVS.SC_CURRENCY_EXPENDED == responseVS.getStatusCode()) {
                Currency expendedCurrency = Wallet.removeExpendedCurrency(responseVS.getMessage(), appVS);
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
                PrefUtils.putBalances(accountsInfo, DateUtils.getCurrentWeekPeriod(),
                        appVS);
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
            Set<String> hashCertVSList = null;
            if(hashCertVS != null) hashCertVSList = new HashSet<>(Arrays.asList(hashCertVS));
            else hashCertVSList = Wallet.getHashCertVSSet();
            if(hashCertVSList == null) {
                LOGD(TAG + ".checkCurrency", "empty hashCertVSList");
                return;
            }
            Map<String, Currency.State> responseDto =  HttpHelper.sendData(
                    new TypeReference<Map<String, Currency.State>>() {},
                    JSON.writeValueAsBytes(hashCertVSList),
                    appVS.getCurrencyServer().getCurrencyBundleStateServiceURL(),
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
                currencyFromWalletWithErrors = Wallet.updateCurrencyWithErrors(currencyWithErrorList, appVS);
            }
            if(currencyFromWalletWithErrors != null && !currencyFromWalletWithErrors.isEmpty()) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, MsgUtils.getUpdateCurrencyWithErrorMsg(
                        currencyFromWalletWithErrors, appVS));
                responseVS.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                        serviceCaller).setTypeVS(TypeVS.CURRENCY_CHECK);
                Intent intent = new Intent(this, WalletActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                startActivity(intent);
            } else if(!currencyOKList.isEmpty()) {
                Wallet.updateCurrencyState(currencyOKList, Currency.State.OK, appVS);
                responseVS = new ResponseVS(ResponseVS.SC_OK);
                responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.CURRENCY_CHECK);
                appVS.broadcastResponse(responseVS);
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

}