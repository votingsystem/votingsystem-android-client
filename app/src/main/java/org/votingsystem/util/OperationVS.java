package org.votingsystem.util;

import java.io.Serializable;
import java.util.Date;


public class OperationVS<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {PENDING, ERROR, FINISHED}

    private Long localId;
    private TypeVS typeVS;
    private State state;
    private int statusCode;
    private String message;
    private T data;
    private Date dateCreated;
    private Date lastUpdated;

    public OperationVS() {}

    public OperationVS(TypeVS typeVS, T data, State state) {
        this.typeVS = typeVS;
        this.data = data;
        this.state = state;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public State getState() {
        return state;
    }

    public OperationVS setState(State state) {
        this.state = state;
        return this;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public OperationVS setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OperationVS setMessage(String message) {
        this.message = message;
        return this;
    }

}
