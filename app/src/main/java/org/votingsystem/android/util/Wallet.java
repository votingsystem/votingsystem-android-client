package org.votingsystem.android.util;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static final String TAG = Wallet.class.getSimpleName();

    private static Set<Currency> currencySet = null;

    public static Set<Currency> getCurrencySet() {
        if(currencySet == null) return new HashSet<>();
        else return new HashSet<>(currencySet);
    }

    public static Set<String> getHashCertVSSet() {
        if(currencySet == null) return null;
        Set<String> result = new HashSet<>();
        for(Currency currency : currencySet) {
            result.add(currency.getHashCertVS());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(String pin, AppContextVS context) throws Exception {
        Set<CurrencyDto> currencyDtoList = getWallet(pin, context);
        currencySet = CurrencyDto.deSerializeCollection(currencyDtoList);
        return new HashSet<Currency>(currencySet);
    }

    public static void saveCurrencyCollection(Collection<Currency> currencyCollection, String pin,
             AppContextVS context) throws Exception {
        Set<Currency> newCurrencySet = new HashSet<>(currencySet);
        newCurrencySet.addAll(currencyCollection);
        Wallet.saveWallet(newCurrencySet, pin, context);
    }

    public static void removeCurrencyCollection(
            Collection<Currency> currencyCollection, AppContextVS context) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        for(Currency currency : currencyCollection) {
            if(currencyMap.remove(currency.getHashCertVS()) != null)  LOGD(
                    TAG +  ".removeCurrencyCollection", "removed currency: " + currency.getHashCertVS());
        }
        currencySet = new HashSet<>(currencyMap.values());
        Wallet.saveWallet(currencySet, null, context);
    }


    public static Currency removeExpendedCurrency(String hashCertVS, AppContextVS context) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        Currency expendedCurrency = null;
        if((expendedCurrency = currencyMap.remove(hashCertVS)) != null)  LOGD(TAG +  ".removeCurrencyList",
                "removed currency: " + hashCertVS);
        Wallet.saveWallet(currencySet, null, context);
        return expendedCurrency;
    }

    public static Map<String, Currency> getCurrencyMap() {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        return currencyMap;
    }

    public static Set<Currency> updateCurrencyWithErrors(List<String> currencySetToRemove,
            AppContextVS contextVS) throws Exception {
        Set<Currency> errorList = new HashSet<>();
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String hashCertVS : currencySetToRemove) {
            Currency removedCurrency = currencyMap.remove(hashCertVS);
            if(removedCurrency != null)  {
                LOGD(TAG +  ".updateCurrencyWithErrors", "removed currency: " + hashCertVS);
                errorList.add(removedCurrency);
            }
        }
        Wallet.saveWallet(currencySet, null, contextVS);
        return errorList;
    }

    public static void updateCurrencyState(List<String> currencySetOK, Currency.State state,
                AppContextVS contextVS) throws Exception {
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String hashCertVS : currencySetOK) {
            Currency currency = currencyMap.get(hashCertVS);
            if(currency != null)  {
                LOGD(TAG + ".updateCurrencyOK", "currency OK: " + hashCertVS);
                currency.setState(Currency.State.OK);
            }
        }
        Wallet.saveWallet(currencySet, null, contextVS);
    }

    public static Map<String, Map<String, IncomesDto>> getCurrencyTagMap() {
        Map<String, Map<String, IncomesDto>> result = new HashMap<>();
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        for(Currency currency : currencySet) {
            if(result.containsKey(currency.getCurrencyCode())) {
                Map<String, IncomesDto> tagMap = result.get(currency.getCurrencyCode());
                if(tagMap.containsKey(currency.getSignedTagVS())) {
                    IncomesDto incomesDto = tagMap.get(currency.getSignedTagVS());
                    incomesDto.addTotal(currency.getAmount());
                    incomesDto.addTimeLimited(currency.getAmount());
                } else {
                    IncomesDto incomesDto = new IncomesDto();
                    incomesDto.addTotal(currency.getAmount());
                    incomesDto.addTimeLimited(currency.getAmount());
                    tagMap.put(currency.getSignedTagVS(), incomesDto);
                }
            } else {
                Map<String, IncomesDto> tagMap = new HashMap<>();
                IncomesDto incomesDto = new IncomesDto();
                incomesDto.addTotal(currency.getAmount());
                incomesDto.addTimeLimited(currency.getAmount());
                tagMap.put(currency.getSignedTagVS(), incomesDto);
                result.put(currency.getCurrencyCode(), tagMap);
            }
        }
        return result;
    }

    public static Set<CurrencyDto> getWallet(String pin, AppContextVS context) throws Exception {
        byte[] walletBytes = getWalletBytes(pin, context);
        if(walletBytes == null) return new HashSet<>();
        else return JSON.getMapper().readValue(walletBytes, new TypeReference<Set<CurrencyDto>>(){});
    }

    private static byte[] getWalletBytes(String pin, AppContextVS context) throws Exception {
        if(pin != null) {
            String storedPinHash = PrefUtils.getPinHash(context);
            String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!pinHash.equals(storedPinHash)) {
                throw new ExceptionVS(context.getString(R.string.pin_error_msg));
            }
        }
        try {
            String walletBase64 = PrefUtils.getWallet(context);
            if(walletBase64 == null) return null;
            else return context.decryptMessage(walletBase64.getBytes());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void saveWallet(Collection<Currency> currencyCollection, String pin,
                                  AppContextVS context) throws Exception {
        if(pin != null) {
            String storedPinHash = PrefUtils.getPinHash(context);
            String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!pinHash.equals(storedPinHash)) {
                throw new ExceptionVS(context.getString(R.string.pin_error_msg));
            }
        }
        if(currencyCollection != null) {
            Set<CurrencyDto> currencyDtoSet = CurrencyDto.serializeCollection(currencyCollection);
            byte[] walletBytes = JSON.getMapper().writeValueAsBytes(currencyDtoSet);
            byte[] encryptedWalletBytes = Encryptor.encryptToCMS(walletBytes, context.getX509UserCert());
            PrefUtils.putWallet(encryptedWalletBytes, context);
            currencySet = new HashSet<>(currencyCollection);
        } else PrefUtils.putWallet(null, context);

    }

    public static void updateWallet(Collection<Currency> currencyCollection,
                                    AppContextVS contextVS) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        for(Currency currency : currencyCollection) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        for(Currency currency : currencySet) {
            if(currencyMap.containsKey(currency.getHashCertVS())) throw new ValidationExceptionVS(
                    contextVS.getString(R.string.currency_repeated_wallet_error_msg, currency.getAmount().toString() +
                    " " + currency.getCurrencyCode()));
            else currencyMap.put(currency.getHashCertVS(), currency);
        }
        Wallet.saveWallet(currencyMap.values(), null, contextVS);
    }

    public static BigDecimal getAvailableForTagVS(String currencyCode, String tagVS) {
        Map<String, Map<String, IncomesDto>> balancesCashMap = getCurrencyTagMap();
        BigDecimal cash = BigDecimal.ZERO;
        if(balancesCashMap.containsKey(currencyCode)) {
            Map<String, IncomesDto> currencyMap = balancesCashMap.get(currencyCode);
            if(currencyMap.containsKey(TagVSDto.WILDTAG)) cash = cash.add(
                    (BigDecimal) currencyMap.get(TagVSDto.WILDTAG).getTotal());
            if(!TagVSDto.WILDTAG.equals(tagVS)) {
                if(currencyMap.containsKey(tagVS)) cash =
                        cash.add((BigDecimal) currencyMap.get(tagVS).getTotal());
            }
        }
        return cash;
    }

    public static CurrencyBundle getCurrencyBundleForTag(String currencyCode, String tagVS) {
        BigDecimal sumTotal = BigDecimal.ZERO;
        List<Currency> result = new ArrayList<>();
        for(Currency currency : currencySet) {
            if(currency.getCurrencyCode().equals(currencyCode) && tagVS.equals(currency.getSignedTagVS())) {
                result.add(currency);
                sumTotal = sumTotal.add(currency.getAmount());
            }
        }
        return new CurrencyBundle(sumTotal, currencyCode, result, tagVS);
    }

    public static CurrencyBundle getCurrencyBundleForTransaction(
            TransactionVSDto transactionDto) throws ExceptionVS {
        return getCurrencyBundleForTransaction(transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.getTagVS().getName());
    }

    public static CurrencyBundle getCurrencyBundleForTransaction(BigDecimal requestAmount,
            String currencyCode, String tagVS) throws ExceptionVS {
        CurrencyBundle tagBundle = getCurrencyBundleForTag(currencyCode, tagVS);
        CurrencyBundle result = null;
        BigDecimal remaining = null;
        if(tagBundle.getAmount().compareTo(requestAmount) < 0) {
            result = tagBundle;
            remaining = requestAmount.subtract(result.getAmount());
            BigDecimal wildtagAccumulated = BigDecimal.ZERO;
            CurrencyBundle wildtagBundle =  getCurrencyBundleForTag(currencyCode, TagVSDto.WILDTAG);
            if(wildtagBundle.getAmount().compareTo(remaining) < 0) throw new ExceptionVS(
                "insufficient cash for request: " + requestAmount + " " + currencyCode + " - " +
                tagVS);
            List<Currency> wildtagCurrencies = new ArrayList<>();
            while(wildtagAccumulated.compareTo(remaining) < 0) {
                Currency newCurrency = wildtagBundle.getTagCurrencyList().remove(0);
                wildtagAccumulated = wildtagAccumulated.add(newCurrency.getAmount());
                wildtagCurrencies.add(newCurrency);
            }
            if(wildtagAccumulated.compareTo(remaining) > 0) {
                Currency lastRemoved = null;
                while(wildtagAccumulated.compareTo(remaining) > 0) {
                    lastRemoved = wildtagCurrencies.remove(0);
                    wildtagAccumulated = wildtagAccumulated.subtract(lastRemoved.getAmount());
                }
                if(wildtagAccumulated.compareTo(remaining) < 0) {
                    wildtagCurrencies.add(0, lastRemoved);
                    wildtagAccumulated = wildtagAccumulated.add(lastRemoved.getAmount());
                }
                result.setWildTagAmount(wildtagAccumulated);
                result.setWildTagCurrencyList(wildtagCurrencies);
            }
        } else {
            BigDecimal accumulated = BigDecimal.ZERO;
            List<Currency> tagCurrencies = new ArrayList<>();
            while(accumulated.compareTo(requestAmount) < 0) {
                Currency newCurrency = tagBundle.getTagCurrencyList().remove(0);
                accumulated = accumulated.add(newCurrency.getAmount());
                tagCurrencies.add(newCurrency);
            }
            if(accumulated.compareTo(requestAmount) > 0) {
                Currency lastRemoved = null;
                while(accumulated.compareTo(requestAmount) > 0) {
                    lastRemoved = tagCurrencies.remove(0);
                    accumulated = accumulated.subtract(lastRemoved.getAmount());
                }
                if(accumulated.compareTo(requestAmount) < 0) {
                    tagCurrencies.add(0, lastRemoved);
                    accumulated = accumulated.add(lastRemoved.getAmount());
                }
            }
            result = new CurrencyBundle(accumulated, tagCurrencies, currencyCode, tagVS);
        }
        return result;
    }

}
