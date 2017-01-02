package org.votingsystem.dto.identity;

import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.util.OperationType;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IdentityTokenDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum State {ok,  error}

    private OperationType type;
    private State state;
    private UserDto user;
    private SystemEntityDto indentityServiceEntity;
    private String revocationHashBase64;
    private String UUID;
    private String base64Data;

    public IdentityTokenDto() {}

    public IdentityTokenDto(OperationType type, SystemEntityDto indentityServiceEntity, String UUID) {
        this.type = type;
        this.indentityServiceEntity = indentityServiceEntity;
        this.UUID = UUID;
    }

    public OperationType getType() {
        return type;
    }

    public IdentityTokenDto setType(OperationType type) {
        this.type = type;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public IdentityTokenDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public UserDto getUser() {
        return user;
    }

    public IdentityTokenDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public SystemEntityDto getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public IdentityTokenDto setIndentityServiceEntity(SystemEntityDto indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        return this;
    }

    public State getState() {
        return state;
    }

    public IdentityTokenDto setState(State state) {
        this.state = state;
        return this;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public IdentityTokenDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }
}
