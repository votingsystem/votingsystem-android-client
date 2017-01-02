package org.votingsystem.dto;

import android.util.Base64;

import org.votingsystem.util.Constants;
import org.votingsystem.util.HashUtils;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRMessageDto<T> implements Serializable {

    public static final String TAG = QRMessageDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public static final int VOTE = 0;

    public static final String DEVICE_ID_KEY      = "did";
    public static final String ITEM_ID_KEY        = "iid";
    public static final String OPERATION_KEY      = "op";
    public static final String PUBLIC_KEY_KEY     = "pk";
    public static final String MSG_KEY            = "msg";
    public static final String SYSTEM_ENTITY_KEY  = "eid";
    public static final String URL_KEY            = "url";
    public static final String UUID_KEY           = "uid";


    private T data;
    private String originRevocationHash;
    private DeviceDto device;
    private AESParamsDto aesParams;


    private Long deviceId;
    private String itemId;
    private Date dateCreated;
    private String revocationHash;
    private String publicKeyBase64;
    private OperationType operationType;
    private String msg;
    private String url;
    private String systemEntityId;
    private String UUID;

    public QRMessageDto() {
    }

    public QRMessageDto(DeviceDto deviceDto, org.votingsystem.util.OperationType operationType) {
        this.operationType = operationType;
        this.deviceId = deviceDto.getId();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0, 3);
    }

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceId(Long.valueOf(msg.split(DEVICE_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(ITEM_ID_KEY + "="))
            qrMessageDto.setItemId(msg.split(ITEM_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(OPERATION_KEY + "=")) {
            qrMessageDto.setOperation(OperationType.valueOf(
                    (msg.split(OPERATION_KEY + "=")[1].split(";")[0])));
        }
        if (msg.contains(SYSTEM_ENTITY_KEY + "="))
            qrMessageDto.setSystemEntityId(msg.split(SYSTEM_ENTITY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(UUID_KEY + "="))
            qrMessageDto.setUUID(msg.split(UUID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(PUBLIC_KEY_KEY + "="))
            qrMessageDto.setPublicKeyBase64(msg.split(PUBLIC_KEY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(MSG_KEY + "="))
            qrMessageDto.setMsg(msg.split(MSG_KEY + "=")[1].split(";")[0]);
        if (msg.contains(URL_KEY + "="))
            qrMessageDto.setUrl(msg.split(URL_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    public PublicKey getRSAPublicKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        //fix qr codes replacements of '+' with spaces
        publicKeyBase64 = publicKeyBase64.replace(" ", "+");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.NO_WRAP));
        return factory.generatePublic(pubKeySpec);
    }

    public DeviceDto getDevice() throws Exception {
        if (device != null) return device;
        DeviceDto dto = new DeviceDto(deviceId);
        if (publicKeyBase64 != null) dto.setPublicKey(getRSAPublicKey());
        return dto;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public static String toQRCode(org.votingsystem.util.OperationType operation, String deviceId) {
        StringBuilder result = new StringBuilder();
        if (deviceId != null)
            result.append(DEVICE_ID_KEY + "=" + deviceId + ";");
        if (operation != null)
            result.append(OPERATION_KEY + "=" + operation + ";");
        return result.toString();
    }

    public QRMessageDto createRequest() throws NoSuchAlgorithmException {
        this.originRevocationHash = java.util.UUID.randomUUID().toString();
        this.revocationHash = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public QRMessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public org.votingsystem.util.OperationType getOperation() {
        return operationType;
    }

    public QRMessageDto setOperation(org.votingsystem.util.OperationType operation) {
        this.operationType = operation;
        return this;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String key) {
        this.publicKeyBase64 = key;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSystemEntityId() {
        return systemEntityId;
    }

    public void setSystemEntityId(String systemEntityId) {
        this.systemEntityId = systemEntityId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}