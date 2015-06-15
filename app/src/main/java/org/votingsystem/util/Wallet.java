package org.votingsystem.util;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
        if(currencySet == null) return null;
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

    public static Set<Currency> getCurrencySet(String pin) throws Exception {
        Set<CurrencyDto> currencyDtoSet = getWallet(pin);
        currencySet = CurrencyDto.deSerializeCollection(currencyDtoSet);
        return new HashSet<>(currencySet);
    }

    public static Currency getCurrency(String hashCertVS) throws Exception {
        for(Currency currency : currencySet) {
            if(currency.getHashCertVS().equals(currency.getHashCertVS())) return currency;
        }
        return null;
    }

    public static void save(Collection<Currency> currencyCollection, String pin)
            throws Exception {
        Set<Currency> newCurrencySet = new HashSet<>();
        if(currencySet != null) newCurrencySet.addAll(currencySet);
        newCurrencySet.addAll(currencyCollection);
        Wallet.saveWallet(newCurrencySet, pin);
    }

    public static void remove(Collection<Currency> currencyCollection) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        for(Currency currency : currencyCollection) {
            if(currencyMap.remove(currency.getHashCertVS()) != null) {
                LOGD(TAG +  ".remove", "removed currency: " + currency.getHashCertVS());
                AppVS.getInstance().updateCurrencyDB(currency);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }


    public static Set<Currency> removeErrors(Collection<String> currencyWithErrors)
            throws Exception {
        Set<Currency> removedSet = new HashSet<>();
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String hashCertVS : currencyWithErrors) {
            Currency removedCurrency = currencyMap.remove(hashCertVS);
            if(removedCurrency != null)  {
                LOGD(TAG +  ".removeErrors", "removed currency: " + hashCertVS);
                removedSet.add(removedCurrency);
                AppVS.getInstance().updateCurrencyDB(removedCurrency);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
        return removedSet;
    }

    public static Map<String, Currency> getCurrencyMap() {
        Map<String, Currency> currencyMap = new HashMap<>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        return currencyMap;
    }

    public static void updateCurrencyState(List<String> currencySetOK, Currency.State state)
            throws Exception {
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String hashCertVS : currencySetOK) {
            Currency currency = currencyMap.get(hashCertVS);
            if(currency != null)  {
                LOGD(TAG + ".updateCurrencyOK", "currency OK: " + hashCertVS);
                currency.setState(Currency.State.OK);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }

    public static Map<String, Map<String, IncomesDto>> getCurrencyTagMap() {
        Map<String, Map<String, IncomesDto>> result = new HashMap<>();
        for(Currency currency : currencySet) {
            if(result.containsKey(currency.getCurrencyCode())) {
                Map<String, IncomesDto> tagMap = result.get(currency.getCurrencyCode());
                if(tagMap.containsKey(currency.getTag())) {
                    IncomesDto incomesDto = tagMap.get(currency.getTag());
                    incomesDto.addTotal(currency.getAmount());
                    if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                } else {
                    IncomesDto incomesDto = new IncomesDto();
                    incomesDto.addTotal(currency.getAmount());
                    if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                    tagMap.put(currency.getTag(), incomesDto);
                }
            } else {
                Map<String, IncomesDto> tagMap = new HashMap<>();
                IncomesDto incomesDto = new IncomesDto();
                incomesDto.addTotal(currency.getAmount());
                if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                tagMap.put(currency.getTag(), incomesDto);
                result.put(currency.getCurrencyCode(), tagMap);
            }
        }
        return result;
    }

    public static Set<CurrencyDto> getWallet(String pin) throws Exception {
        byte[] walletBytes = getWalletBytes(pin);
        if(walletBytes == null) return new HashSet<>();
        else return JSON.readValue(walletBytes, new TypeReference<Set<CurrencyDto>>(){});
    }

    private static byte[] getWalletBytes(String pin) throws Exception {
        if(pin != null) {
            String storedPinHash = PrefUtils.getPinHash(AppVS.getInstance());
            String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!pinHash.equals(storedPinHash)) {
                throw new ExceptionVS(AppVS.getInstance().getString(R.string.pin_error_msg));
            }
        }
        try {
            String walletBase64 = PrefUtils.getWallet(AppVS.getInstance());
            if(walletBase64 == null) return null;
            else return AppVS.getInstance().decryptMessage(walletBase64.getBytes());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void saveWallet(Collection<Currency> currencyCollection, String pin) throws Exception {
        AppVS context = AppVS.getInstance();
        if(pin != null) {
            String storedPinHash = PrefUtils.getPinHash(context);
            String pinHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!pinHash.equals(storedPinHash)) {
                throw new ExceptionVS(context.getString(R.string.pin_error_msg));
            }
        }
        if(currencyCollection != null) {
            Set<CurrencyDto> currencyDtoSet = CurrencyDto.serializeCollection(currencyCollection);
            byte[] walletBytes = JSON.writeValueAsBytes(currencyDtoSet);
            byte[] encryptedWalletBytesBase64 = Encryptor.encryptToCMS(walletBytes, context.getX509UserCert());
            PrefUtils.putWallet(encryptedWalletBytesBase64, context);
            currencySet = new HashSet<>(currencyCollection);
        } else PrefUtils.putWallet(null, context);

    }

    public static void updateWallet(Collection<Currency> currencyCollection) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        for(Currency currency : currencyCollection) {
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        for(Currency currency : currencySet) {
            if(currencyMap.containsKey(currency.getHashCertVS())) throw new ValidationExceptionVS(
                    AppVS.getInstance().getString(R.string.currency_repeated_wallet_error_msg,
                            currency.getAmount().toString() + " " + currency.getCurrencyCode()));
            else currencyMap.put(currency.getHashCertVS(), currency);
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }

    public static BigDecimal getAvailableForTagVS(String currencyCode, String tagVS) {
        Map<String, Map<String, IncomesDto>> balancesCashMap = getCurrencyTagMap();
        BigDecimal cash = BigDecimal.ZERO;
        if(balancesCashMap.containsKey(currencyCode)) {
            Map<String, IncomesDto> currencyMap = balancesCashMap.get(currencyCode);
            if(currencyMap.containsKey(TagVSDto.WILDTAG)) cash = cash.add(
                    currencyMap.get(TagVSDto.WILDTAG).getTotal());
            if(!TagVSDto.WILDTAG.equals(tagVS)) {
                if(currencyMap.containsKey(tagVS)) cash =
                        cash.add(currencyMap.get(tagVS).getTotal());
            }
        }
        return cash;
    }

    public static CurrencyBundle getCurrencyBundleForTag(String currencyCode, String tagVS)
            throws ValidationExceptionVS {
        CurrencyBundle currencyBundle = new CurrencyBundle(currencyCode, tagVS);
        for(Currency currency : currencySet) {
            if(currency.getCurrencyCode().equals(currencyCode)) {
                if(tagVS.equals(currency.getTag()) || TagVSDto.WILDTAG.equals(currency.getTag())) {
                    currencyBundle.addCurrency(currency);
                }
            }
        }
        return currencyBundle;
    }

    public static void updateWallet(CurrencyBatchDto currencyBatchDto) throws Exception {
        Wallet.remove(currencyBatchDto.getCurrencyCollection());
        if(currencyBatchDto.getLeftOverCurrency() != null) Wallet.updateWallet(
                Arrays.asList(currencyBatchDto.getLeftOverCurrency()));
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

}
