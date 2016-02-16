package org.votingsystem.util;

import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.android.R;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptWrapper implements Serializable {

    public static final String TAG = ReceiptWrapper.class.getSimpleName();


    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}

    private Long localId = -1L;
    private transient SMIMEMessage receipt;
    private TypeVS typeVS;
    private String subject;
    private String url;

    public ReceiptWrapper() {}

    public ReceiptWrapper(TypeVS typeVS, String url) {
        this.typeVS = typeVS;
        this.url = url;
    }

    public ReceiptWrapper(TransactionVSDto transactionVS) {
        this.typeVS = transactionVS.getOperation();
        this.url = transactionVS.getMessageSMIMEURL();
    }

    public String getTypeDescription(Context context) {
        switch(getTypeVS()) {
            case SEND_VOTE:
                return context.getString(R.string.receipt_vote_subtitle);
            case ANONYMOUS_SELECTION_CERT_REQUEST:
                return context.getString(R.string.anonimous_representative_request_lbl);
            case CURRENCY_REQUEST:
                return context.getString(R.string.currency_request_subtitle);
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                return context.getString(R.string.delegation_lbl);
            case ACCESS_REQUEST:
                return context.getString(R.string.access_request_lbl);
            case FROM_GROUP_TO_ALL_MEMBERS:
                return context.getString(R.string.from_group_to_all_member_lbl);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + getTypeVS().toString();
        }
    }

    public String getCardSubject(Context context) {
        switch(getTypeVS()) {
            case ACCESS_REQUEST:
                return  context.getString(R.string.access_request_lbl);
            case SEND_VOTE:
                return  context.getString(R.string.receipt_vote_subtitle);
            case ANONYMOUS_SELECTION_CERT_REQUEST:
                return context.getString(R.string.anonimous_representative_request_lbl);
            case CURRENCY_REQUEST:
                return context.getString(R.string.currency_request_subtitle);
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                return context.getString(R.string.anonymous_representative_selection_lbl);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + subject;
        }
    }

    public int getLogoId() {
        switch(getTypeVS()) {
            case SEND_VOTE:
                return R.drawable.poll_32;
            case ANONYMOUS_SELECTION_CERT_REQUEST:
                return R.drawable.system_users_32;
            default:
                return R.drawable.receipt_32;
        }
    }

    public String getURL() {
        return url;
    }

    public void setReceiptBytes(byte[] receiptBytes) throws Exception {
        if(receiptBytes != null) {
            receipt = new SMIMEMessage(receiptBytes);
            subject = receipt.getSubject();
            Map dataMap = receipt.getSignedContent(new TypeReference<Map<String, Object>>() { });
            if(dataMap.containsKey("operation"))
                this.typeVS = TypeVS.valueOf((String) dataMap.get("operation"));
        }
    }

    public String getMessageId() {
        String result = null;
        if(receipt != null) {
            try {
                String[] headers = receipt.getHeader("Message-ID");
                if(headers != null && headers.length >0) return headers[0];
            } catch(Exception ex) { ex.printStackTrace(); }
        }
        return result;
    }

    public SMIMEMessage getReceipt() throws Exception {
        return receipt;
    }

    public boolean hashReceipt() {
        return (receipt != null);
    }

    public String getSubject() {
        return subject;
    }

    public TypeVS getTypeVS() {
        if(typeVS == null && receipt != null) {
            try {
                Map signedContent = JSON.readValue(receipt.getSignedContent(),
                        new TypeReference<Map<String, Object>>() {
                        });
                if (signedContent.containsKey("operation")) {
                    typeVS = TypeVS.valueOf((String) signedContent.get("operation"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Date getDateFrom() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotBefore();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Date getDateTo() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotAfter();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(receipt != null) s.writeObject(receipt.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] receiptBytes = (byte[]) s.readObject();
        if(receiptBytes != null) receipt = new SMIMEMessage(receiptBytes);
    }

}