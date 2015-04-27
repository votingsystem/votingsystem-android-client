package org.votingsystem.util;

import android.content.Context;

import org.votingsystem.android.R;

import java.util.Arrays;
import java.util.List;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum Payment {

    SIGNED_TRANSACTION, CURRENCY_BATCH, CASH_SEND;

    public static List<String> getPaymentMethods(Context context) {
        //preserve the same order
        List<String> result = Arrays.asList(
                context.getString(R.string.signed_transaction_lbl),
                context.getString(R.string.anonymous_signed_transaction_lbl),
                context.getString(R.string.currency_send_lbl));
        return result;
    }

    public static Payment getByPosition(int position) {
        if(position == 0) return SIGNED_TRANSACTION;
        if(position == 1) return CURRENCY_BATCH;
        if(position == 2) return CASH_SEND;
        return null;
    }

    public String getDescription(Context context) {
        switch(this) {
            case SIGNED_TRANSACTION: return context.getString(R.string.signed_transaction_lbl);
            case CURRENCY_BATCH: return context.getString(R.string.anonymous_signed_transaction_lbl);
            case CASH_SEND: return context.getString(R.string.currency_send_lbl);
        }
        return null;
    }
}
