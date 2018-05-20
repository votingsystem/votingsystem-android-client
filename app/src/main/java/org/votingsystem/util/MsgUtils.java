package org.votingsystem.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.votingsystem.android.R;
import org.votingsystem.dto.voting.VoteDto;

import java.security.cert.X509Certificate;
import java.util.Formatter;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    public static String formatElectionSubtitle(Context context, String params) {
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
                PrefUtils.getDisplayTimeZone().getID()).toString();
    }

    public static String getHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }
        }
        return null;
    }

    public static String getCertInfoMessage(X509Certificate certificate, Context context) {
        return context.getString(R.string.cert_info_formated_msg,
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotBefore(), "HH:mm"),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotAfter(), "HH:mm"));
    }

    public static String getVoteStateMsg(VoteDto.State voteState, Context context) {
        switch (voteState) {
            case OK:
                return context.getString(R.string.vote_ok_lbl);
            case CANCELLED:
                return context.getString(R.string.vote_cancelled_msg);
            case ERROR:
                return context.getString(R.string.vote_error_msg);
            default:
                return voteState.toString();
        }
    }

}
