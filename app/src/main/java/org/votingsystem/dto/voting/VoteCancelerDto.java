package org.votingsystem.dto.voting;

import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HashUtils;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteCancelerDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originRevocationHash;
    private String revocationHashBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private OperationType operation = OperationType.CANCEL_VOTE;
    private String UUID;


    public VoteCancelerDto() {
    }

    public void validate() throws ValidationException, NoSuchAlgorithmException {
        if (operation == null || OperationType.CANCEL_VOTE != operation)
            throw new ValidationException(
                    "ERROR - expected operation 'CANCEL_VOTE' - found: " + operation);
        if (originRevocationHash == null)
            throw new ValidationException("ERROR - missing param 'originRevocationHash'");
        if (revocationHashBase64 == null)
            throw new ValidationException("ERROR - missing param 'revocationHashBase64'");
        if (hashAccessRequestBase64 == null)
            throw new ValidationException("ERROR - missing param 'hashAccessRequestBase64'");
        if (originHashAccessRequest == null)
            throw new ValidationException("ERROR - missing param 'originHashAccessRequest'");
        if (originHashAccessRequest == null)
            throw new ValidationException("ERROR - missing param 'originHashAccessRequest'");
        if (!hashAccessRequestBase64.equals(HashUtils.getHashBase64(originHashAccessRequest.getBytes(),
                Constants.DATA_DIGEST_ALGORITHM)))
            throw new ValidationException("voteCancellationAccessRequestHashError");
        if (!revocationHashBase64.equals(HashUtils.getHashBase64(originRevocationHash.getBytes(),
                Constants.DATA_DIGEST_ALGORITHM)))
            throw new ValidationException("voteCancellationHashCertificateError");
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}
