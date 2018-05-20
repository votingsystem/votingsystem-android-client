package org.votingsystem.crypto;

import android.content.Context;

import org.votingsystem.android.R;
import org.votingsystem.dto.UserDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XmlSignature;

import java.io.Serializable;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptContainer implements Serializable {

    public static final String TAG = ReceiptContainer.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}

    private Long localId = -1L;
    private byte[] receipt;
    private OperationType operationType;
    private String subject;
    private String url;
    private String systemEntityId;

    public ReceiptContainer() { }

    public ReceiptContainer(OperationType operationType, String url) {
        this.operationType = operationType;
        this.url = url;
    }

    public String getTypeDescription(Context context) {
        switch (getOperationType()) {
            case SEND_VOTE:
                return context.getString(R.string.receipt_vote_subtitle);
            case ANON_VOTE_CERT_REQUEST:
                return context.getString(R.string.access_request_lbl);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + getOperationType().toString();
        }
    }

    public String getCardSubject(Context context) {
        switch (getOperationType()) {
            case ANON_VOTE_CERT_REQUEST:
                return context.getString(R.string.access_request_lbl);
            case SEND_VOTE:
                return context.getString(R.string.receipt_vote_subtitle);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + subject;
        }
    }

    public int getLogoId() {
        switch (getOperationType()) {
            case SEND_VOTE:
                return R.drawable.ic_insert_chart_24px;
            default:
                return R.drawable.ic_label_outline_24px;
        }
    }

    public String getURL() {
        return url;
    }

    public byte[] getReceipt() {
        return receipt;
    }

    public void setReceipt(byte[] receipt) {
        this.receipt = receipt;
    }

    public String getSubject() {
        return subject;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public UserDto getSigner() {
        return null;
    }

    public Set<UserDto> getSigners() {
        return null;
    }

    public Set<XmlSignature> getSignatures() {
        return null;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public String getSystemEntityId() {
        return systemEntityId;
    }

    public void setSystemEntityId(String systemEntityId) {
        this.systemEntityId = systemEntityId;
    }
}