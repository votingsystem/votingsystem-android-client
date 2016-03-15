package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.TagVSInfoDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TimePeriod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesDto {

    public static final String TAG = BalancesDto.class.getSimpleName();

    private UserDto user;
    private TimePeriod timePeriod;
    private List<TransactionVSDto> transactionFromList;
    private List<TransactionVSDto> transactionToList;
    private Map<String, Map> balances;
    private Map<String, Map<String, BigDecimal>> balancesFrom = new HashMap<>();
    private Map<String, Map<String, IncomesDto>> balancesTo = new HashMap<>();
    private Map<String, Map<String, BigDecimal>> balancesCash = new HashMap<>();
    private Map<String, Map<String, TagVSInfoDto>> balancesInfo;

    public BalancesDto() {}


    public static BalancesDto TO(List<TransactionVSDto> transactionList, Map<String, Map<String, IncomesDto>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionToList(transactionList);
        dto.setBalancesTo(balances);
        return dto;
    }

    public static BalancesDto FROM(List<TransactionVSDto> transactionList,Map<String, Map<String, BigDecimal>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionFromList(transactionList);
        dto.setBalancesFrom(balances);
        return dto;
    }

    public void setTo(List<TransactionVSDto> transactionList, Map<String, Map<String, IncomesDto>> balances) {
        setTransactionToList(transactionList);
        setBalancesTo(balances);
    }

    public void setTo(BalancesDto balancesToDto) {
        setTransactionToList(balancesToDto.getTransactionToList());
        setBalancesTo(balancesToDto.getBalancesTo());
    }


    public void setFrom(List<TransactionVSDto> transactionList, Map<String, Map<String, BigDecimal>> balances) {
        setTransactionFromList(transactionList);
        setBalancesFrom(balances);
    }

    public void calculateCash() {
        setBalancesCash(filterBalanceTo(balancesTo));
        for(String currency: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currency)) {
                for(String tag : getBalancesFrom().get(currency).keySet()) {
                    if(getBalancesCash().get(currency).containsKey(tag)) {
                        BigDecimal newAmount = getBalancesCash().get(currency).get(tag).subtract(getBalancesFrom().get(currency).get(tag));
                        if(newAmount.compareTo(BigDecimal.ZERO) < 0) {
                            getBalancesCash().get(currency).put(TagVSDto.WILDTAG, getBalancesCash().get(currency).
                                    get(TagVSDto.WILDTAG).add(newAmount));
                            getBalancesCash().get(currency).put(tag, BigDecimal.ZERO);
                        } else  getBalancesCash().get(currency).put(tag, newAmount);
                    } else {
                        getBalancesCash().get(currency).put(TagVSDto.WILDTAG,  getBalancesCash().get(currency).get(TagVSDto.WILDTAG)
                                .subtract(getBalancesFrom().get(currency).get(tag)));
                    }
                }
            } else {
                Map<String, BigDecimal> tagData = new HashMap<String, BigDecimal>(getBalancesFrom().get(currency));
                for(String tag: tagData.keySet()) {
                    tagData.put(tag, tagData.get(tag).negate());
                }
            }
        }
    }

    public static Map<String, Map<String, BigDecimal>> filterBalanceTo(Map<String, Map<String, IncomesDto>> balanceTo) {
        Map result = new HashMap<>();
        for(String currency : balanceTo.keySet()) {
            Map<String, BigDecimal> currencyMap = new HashMap<>();
            for(String tag : balanceTo.get(currency).keySet()) {
                currencyMap.put(tag, balanceTo.get(currency).get(tag).getTotal());
            }
            result.put(currency, currencyMap);
        }
        return result;
    }

    public BigDecimal getAvailableForTagVS(String currencyCode, String tagStr) throws ExceptionVS {
        BigDecimal cash = BigDecimal.ZERO;
        if(balancesCash.containsKey(currencyCode)) {
            Map<String, BigDecimal> currencyMap = balancesCash.get(currencyCode);
            if(currencyMap.containsKey(TagVSDto.WILDTAG)) cash = cash.add(
                    currencyMap.get(TagVSDto.WILDTAG));
            if(!TagVSDto.WILDTAG.equals(tagStr)) {
                if(currencyMap.containsKey(tagStr)) cash = cash.add(currencyMap.get(tagStr));
            }
        }
        return cash;
    }

    public Map<String, TagVSInfoDto> getTagVSInfoMap(String currencyCode) throws ExceptionVS {
        balancesInfo = new HashMap<String, Map<String, TagVSInfoDto>>();
        Set<String> currencySet = balancesTo.keySet();
        BigDecimal wildTagExpendedInTags = BigDecimal.ZERO;
        for(String currency : currencySet) {
            Map<String, TagVSInfoDto> currencyInfoMap = new HashMap<String, TagVSInfoDto>();
            Map<String, IncomesDto> currencyMap = balancesTo.get(currency);
            for(String tagVS: currencyMap.keySet()) {
                TagVSInfoDto tagVSInfo = new TagVSInfoDto(tagVS, currency);
                tagVSInfo.setTotal(balancesCash.get(currency).get(tagVS));
                tagVSInfo.setTimeLimited(currencyMap.get(tagVS).getTimeLimited());
                /*tagVSInfo.setTotal(tagVS.getTotal());
                if(balancesFromMap != null && balancesFromMap.get(currency) != null &&
                        balancesFromMap.get(currency).get(tagVS.getName()) != null) {
                    tagVSInfo.setFrom(balancesFromMap.get(currency).get(tagVS.getName()).getTotal());
                }
                tagVSInfo.checkResult(balancesCashMap.get(currency).get(tagVS.getName()).getTotal());*/
                currencyInfoMap.put(tagVS, tagVSInfo);
            }
            balancesInfo.put(currency, currencyInfoMap);
        }
        return balancesInfo.get(currencyCode);
    }

    public Map<String, BigDecimal> getTagVSBalancesMap(String currencyCode) throws ExceptionVS {
        if(balancesCash.containsKey(currencyCode)) {
            return balancesCash.get(currencyCode);
        } else {
            LOGD(TAG + ".getTagVSBalancesMap", "user has not accounts for currency '" +
                    currencyCode + "'");
            return null;
        }
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public List<TransactionVSDto> getTransactionList() {
        List<TransactionVSDto> result = new ArrayList<>(transactionToList);
        result.addAll(transactionFromList);
        return result;
    }

    public Map<String, Map> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, Map> balances) {
        this.balances = balances;
    }

    public List<TransactionVSDto> getTransactionFromList() {
        return transactionFromList;
    }

    public void setTransactionFromList(List<TransactionVSDto> transactionFromList) {
        this.transactionFromList = transactionFromList;
    }

    public List<TransactionVSDto> getTransactionToList() {
        return transactionToList;
    }

    public void setTransactionToList(List<TransactionVSDto> transactionToList) {
        this.transactionToList = transactionToList;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesCash() {
        if(balancesCash == null) calculateCash();
        return balancesCash;
    }

    public void setBalancesCash(Map<String, Map<String, BigDecimal>> balancesCash) {
        this.balancesCash = balancesCash;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesFrom() {
        return balancesFrom;
    }

    public void setBalancesFrom(Map<String, Map<String, BigDecimal>> balancesFrom) {
        this.balancesFrom = balancesFrom;
    }

    public Map<String, Map<String, IncomesDto>> getBalancesTo() {
        return balancesTo;
    }

    public void setBalancesTo(Map<String, Map<String, IncomesDto>> balancesTo) {
        this.balancesTo = balancesTo;
    }
}
