package org.votingsystem.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.Currency;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    public static String formatEventVSSubtitle(Context context,String params) {
        return null;
    }

    public static String formatIntervalTimeString(long intervalStart, long intervalEnd,
              StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, intervalStart, intervalEnd, TIME_FLAGS,
                PrefUtils.getDisplayTimeZone(context).getID()).toString();
    }

    public static String getHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }
        }
        return null;
    }

    public static String getMessagesDrawerItemMessage(Context context) {
        Integer numMessagesNotreaded = PrefUtils.getNumMessagesNotReaded(context);
        String prefix = numMessagesNotreaded == 0 ? "": numMessagesNotreaded + "  ";
        return prefix + context.getString(R.string.messages_lbl);
    }

    public static String getTagVSMessage(String tag) {
        if(TagVSDto.WILDTAG.equals(tag)) return AppVS.getInstance().getString(R.string.wildtag_lbl);
        else return tag;
    }

    public static String getCurrencyDescriptionMessage(Currency currency, Context context) {
        return currency.getAmount().toPlainString() + " " + currency.getCurrencyCode() +
                " " + context.getString(R.string.for_lbl ) + " '" +
                getTagVSMessage(currency.getTag()) + "'";
    }

    public static String getCertInfoMessage(X509Certificate certificate, Context context) {
        return context.getString(R.string.cert_info_formated_msg,
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotBefore()),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotAfter()));
    }

    public static String getCurrencyStateMessage(Currency currency, Context context) {
        switch(currency.getState()) {
            case EXPENDED: return context.getString(R.string.expended_lbl);
            case LAPSED: return context.getString(R.string.lapsed_lbl);
            default:return null;
        }
    }

    public static String getVotingStateMessage(EventVSDto.State state, Context context) {
        switch(state) {
            case ACTIVE: return context.getString(R.string.eventvs_election_active);
            case CANCELLED:
            case TERMINATED:
                return context.getString(R.string.eventvs_election_closed);
            case PENDING: return context.getString(R.string.eventvs_election_pending);
            default: return null;
        }
    }

    public static String getAnonymousSignedTransactionOKMsg(TransactionVSDto transactionRequest,
                 Context context) {
        String toUser = transactionRequest.getToUser() != null?
                transactionRequest.getToUser(): transactionRequest.getToUserIBAN().iterator().next();
        return context.getString(R.string.anonymous_signed_transaction_ok_msg,
                transactionRequest.getAmount().toString() + " " + transactionRequest.getCurrencyCode(),
                toUser);
    }

    public static String getCurrencyRequestMessage(TransactionVSDto transactionDto, Context context) {
        String tagMessage = getTagVSMessage(transactionDto.getTagVS().getName());
        return context.getString(R.string.currency_request_msg, transactionDto.getAmount().toPlainString(),
                transactionDto.getCurrencyCode(), tagMessage);
    }

    public static String getTransactionVSConfirmMessage(TransactionVSDto transactionDto, Context context) {
        return context.getString(R.string.transaction_request_confirm_msg,
                transactionDto.getDescription(context),
                transactionDto.getAmount().toString() + " " + transactionDto.getCurrencyCode(),
                transactionDto.getToUser());
    }


    public static String getVoteVSStateMsg(VoteVSDto.State voteState, Context context) {
        switch (voteState) {
            case OK: return context.getString(R.string.votevs_ok_msg);
            case CANCELLED: return context.getString(R.string.votevs_cancelled_msg);
            case ERROR: return context.getString(R.string.votevs_error_msg);
            default: return voteState.toString();
        }
    }

    public static String getUpdateCurrencyWithErrorMsg(Collection<Currency> currencyWithErrors, Context context) {
        Map<String,  Map<String, BigDecimal>> expendedMap = new HashMap<>();
        Map<String,  Map<String, BigDecimal>> lapsedMap = new HashMap<>();
        for(Currency currency : currencyWithErrors) {
            switch (currency.getState()) {
                case LAPSED:
                    if(lapsedMap.containsKey(currency.getCurrencyCode())) {
                        Map<String, BigDecimal> tagInfo = lapsedMap.get(currency.getCurrencyCode());
                        if(tagInfo == null) {
                            tagInfo = new HashMap<>();
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        } else {
                            BigDecimal tagAccumulated = tagInfo.get(currency.getTag()).add(
                                    currency.getAmount());
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        }
                        lapsedMap.put(currency.getCurrencyCode(), tagInfo);
                    } else {
                        Map<String, BigDecimal> tagInfo = new HashMap<>();
                        tagInfo.put(currency.getTag(), currency.getAmount());
                        lapsedMap.put(currency.getCurrencyCode(), tagInfo);
                    }
                    break;
                case EXPENDED:
                    if(expendedMap.containsKey(currency.getCurrencyCode())) {
                        Map<String, BigDecimal> tagInfo = expendedMap.get(currency.getCurrencyCode());
                        if(tagInfo == null) {
                            tagInfo = new HashMap<>();
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        } else {
                            BigDecimal tagAccumulated = tagInfo.get(currency.getTag()).add(
                                    currency.getAmount());
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        }
                        expendedMap.put(currency.getCurrencyCode(), tagInfo);
                    } else {
                        Map<String, BigDecimal> tagInfo = new HashMap<>();
                        tagInfo.put(currency.getTag(), currency.getAmount());
                        expendedMap.put(currency.getCurrencyCode(), tagInfo);
                    }
                    break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if(expendedMap.size() > 0) {
            for(String currency : expendedMap.keySet()) {
                Map<String, BigDecimal> tagInfo = expendedMap.get(currency);
                for(String tagVS: tagInfo.keySet()) {
                    sb.append(context.getString(R.string.currency_expended_msg, tagInfo.get(tagVS).
                            toString() + " " + currency, getTagVSMessage(tagVS)) + "<br/>");
                }

            }

        }
        if(lapsedMap.size() > 0) {
            for(String currency : lapsedMap.keySet()) {
                Map<String, BigDecimal> tagInfo = lapsedMap.get(currency);
                for(String tagVS: tagInfo.keySet()) {
                    sb.append(context.getString(R.string.currency_lapsed_msg, tagInfo.get(tagVS).
                            toString() + " " + currency, getTagVSMessage(tagVS)) + "<br/>");

                }
            }
        }
        String result = context.getString(R.string.updated_currency_with_error_msg) + ":<br/>" +
                sb.toString();
        return result;
    }
}
