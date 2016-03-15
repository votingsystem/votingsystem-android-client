package org.votingsystem.util;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.SocketMessageDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private T data;
    private DeviceDto device;
    private String broadCastId;
    private String UUID;

    public WebSocketSession(SocketMessageDto socketMsg) {
        this.typeVS = socketMsg.getOperation();
    }

    public WebSocketSession(DeviceDto device) {
        this.device = device;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
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
