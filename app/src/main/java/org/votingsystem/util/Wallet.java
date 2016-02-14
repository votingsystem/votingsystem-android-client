package org.votingsystem.util;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
        return new HashSet<>(currencySet);
    }

    public static Set<String> getHashCertVSSet() {
        Set<String> result = new HashSet<>();
        for(Currency currency : currencySet) {
            result.add(currency.getHashCertVS());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(char[] pin) throws Exception {
        currencySet = PrefUtils.getWallet(pin);
        return new HashSet<>(currencySet);
    }

    public static Currency getCurrency(String hashCertVS) throws Exception {
        for(Currency currency : currencySet) {
            if(currency.getHashCertVS().equals(hashCertVS)) return currency;
        }
        return null;
    }

    public static void save(Collection<Currency> currencyCollection, char[] pin)
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


    public static Set<Currency> removeErrors(Collection<CurrencyStateDto> currencyWithErrors)
            throws Exception {
        Set<Currency> removedSet = new HashSet<>();
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(CurrencyStateDto currencyStateDto : currencyWithErrors) {
            Currency removedCurrency = currencyMap.remove(currencyStateDto.getHashCertVS());
            if(removedCurrency != null)  {
                LOGD(TAG +  ".removeErrors", "removed currency: " + currencyStateDto.getHashCertVS());
                AppVS.getInstance().updateCurrencyDB(removedCurrency.setState(currencyStateDto.getState()));
                removedSet.add(removedCurrency);
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

    public static void updateCurrencyState(Set<String> currencySet, Currency.State state)
            throws Exception {
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String hashCertVS : currencySet) {
            Currency currency = currencyMap.get(hashCertVS);
            if(currency != null)  {
                LOGD(TAG + ".updateCurrencyState", "hash: " + hashCertVS + " - state:" + state);
                currency.setState(state);
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

    public static void saveWallet(Collection<Currency> currencyCollection, char[] passw) throws Exception {
        PrefUtils.putWallet(currencyCollection, passw);
        currencySet = new HashSet<>(currencyCollection);
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
        for(Currency currency : currencyBatchDto.getCurrencyCollection()) {
            currency.setState(Currency.State.EXPENDED);
        }
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
