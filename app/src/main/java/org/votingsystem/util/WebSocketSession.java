package org.votingsystem.util;

import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.crypto.AESParams;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private AESParams aesParams;
    private T data;
    private DeviceVSDto deviceVS;
    private String broadCastId;
    private String UUID;

    public WebSocketSession(SocketMessageDto socketMsg) {
        this.aesParams = socketMsg.getAesEncryptParams();
        this.typeVS = socketMsg.getOperation();
    }

    public WebSocketSession(AESParams aesParams, DeviceVSDto deviceVS) {
        this.aesParams = aesParams;
        this.deviceVS = deviceVS;
    }

    public AESParams getAESParams() {
        return aesParams;
    }

    public void setAESParams(AESParams aesParams) {
        this.aesParams = aesParams;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public DeviceVSDto getDeviceVS() {
        return deviceVS;
    }

    public void setDeviceVS(DeviceVSDto deviceVS) {
        this.deviceVS = deviceVS;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public String getUUID() {
        return UUID;
    }

    public WebSocketSession setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getBroadCastId() {
        return broadCastId;
    }

    public WebSocketSession setBroadCastId(String broadCastId) {
        this.broadCastId = broadCastId;
        return this;
    }
}
