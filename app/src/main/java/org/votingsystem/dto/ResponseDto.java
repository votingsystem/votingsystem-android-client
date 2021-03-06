package org.votingsystem.dto;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.votingsystem.android.R;
import org.votingsystem.http.ContentType;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ResponseDto<T> implements Parcelable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = ResponseDto.class.getSimpleName();

    public static final int SC_OK = 200;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_ERROR_REQUEST = 400;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_REQUEST_REPEATED = 409;
    public static final int SC_EXCEPTION = 490;
    public static final int SC_ERROR = 500;
    public static final int SC_CONNECTION_TIMEOUT = 522;
    public static final int SC_ERROR_TIMESTAMP = 570;
    public static final int SC_PROCESSING = 700;
    public static final int SC_TERMINATED = 710;
    public static final int SC_WS_CONNECTION_INIT_OK = 800;
    public static final int SC_WS_MESSAGE_SEND_OK = 801;
    public static final int SC_WS_MESSAGE_ENCRYPTED = 810;
    public static final int SC_WS_CONNECTION_INIT_ERROR = 840;
    public static final int SC_WS_CONNECTION_NOT_FOUND = 841;
    public static final int SC_WS_CONNECTION_CLOSED = 842;
    public static final int SC_CANCELED = 0;
    public static final int SC_INITIALIZED = 1;
    public static final int SC_PAUSED = 10;

    private int statusCode;
    private OperationDto operation;
    private String caption;
    private String notificationMessage;
    private String message;
    private String serviceCaller;
    private T data;
    private OperationType operationType;
    private ContentType contentType = ContentType.TEXT;
    private byte[]  messageBytes;
    private Uri uri;
    private String base64Data;

    public ResponseDto() {
    }

    public ResponseDto(Parcel source) {
        // Must read values in the same order as they were placed in
        statusCode = source.readInt();
        serviceCaller = source.readString();
        caption = source.readString();
        notificationMessage = source.readString();
        message = source.readString();
        String operationStr = source.readString();
        if (operationStr != null) {
            operation = (OperationDto) ObjectUtils.deSerializeObject(operationStr.getBytes());
        }
        operationType = (OperationType) source.readSerializable();
        contentType = (ContentType) source.readSerializable();
        messageBytes = new byte[source.readInt()];
        source.readByteArray(messageBytes);
        uri = source.readParcelable(Uri.class.getClassLoader());
    }

    public ResponseDto(int statusCode, String serviceCaller, String caption, String message,
                       OperationType operationType) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.operationType = operationType;
    }

    public ResponseDto(int statusCode, OperationType operationType) {
        this.statusCode = statusCode;
        this.operationType = operationType;
    }


    public ResponseDto(int statusCode, String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
    }

    public ResponseDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public ResponseDto(int statusCode, String message, ContentType contentType) {
        this.statusCode = statusCode;
        this.message = message;
        this.contentType = contentType;
    }

    public ResponseDto(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }

    public ResponseDto(int statusCode, byte[] messageBytes, ContentType contentType) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        this.contentType = contentType;
    }

    public ResponseDto(int statusCode) {
        this.statusCode = statusCode;
    }

    public ResponseDto(OperationType operationType, T data) {
        this.operationType = operationType;
        this.data = data;
    }

    public static ResponseDto getResponse(Integer statusCode, String serviceCaller, String caption,
                                          String notificationMessage, OperationType operationType) {
        ResponseDto result = new ResponseDto(statusCode);
        result.setCaption(caption).setNotificationMessage(notificationMessage);
        result.setServiceCaller(serviceCaller);
        result.setOperationType(operationType);
        return result;
    }

    public static ResponseDto EXCEPTION(String caption, String message) {
        ResponseDto responseDto = new ResponseDto(ResponseDto.SC_ERROR);
        responseDto.setCaption(caption);
        responseDto.setNotificationMessage(message);
        return responseDto;
    }

    public static ResponseDto EXCEPTION(Exception ex, Context context) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty())
            message = context.getString(R.string.exception_lbl);
        return EXCEPTION(context.getString(R.string.exception_lbl), message);
    }

    public static ResponseDto ERROR(String caption, String message) {
        ResponseDto responseDto = new ResponseDto(ResponseDto.SC_ERROR);
        responseDto.setCaption(caption);
        responseDto.setNotificationMessage(message);
        return responseDto;
    }

    public static ResponseDto OK() {
        ResponseDto responseDto = new ResponseDto(ResponseDto.SC_OK);
        return responseDto;
    }

    public String getMessage() {
        if (message == null && messageBytes != null) {
            try {
                message = new String(messageBytes, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return message;
    }

    public ResponseDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ResponseDto setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public ResponseDto setOperationType(OperationType operationType) {
        this.operationType = operationType;
        return this;
    }

    public T getData() {
        return data;
    }

    public ResponseDto setData(T data) {
        this.data = data;
        return this;
    }

    public byte[] getMessageBytes() {
        try {
            if (messageBytes == null && message != null)
                messageBytes = message.getBytes("UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return messageBytes;
    }

    public ResponseDto setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
        return this;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getCaption() {
        if (caption == null && operation != null) return operation.getSubject();
        else return caption;
    }

    public ResponseDto setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getServiceCaller() {
        return serviceCaller;
    }

    public ResponseDto setServiceCaller(String serviceCaller) {
        this.serviceCaller = serviceCaller;
        return this;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getLogStr() {
        return "statusCode: " + getStatusCode() + " - serviceCaller: " + getServiceCaller() +
                " - caption: " + getCaption() + " - message:" + getNotificationMessage();

    }

    public static final Creator<ResponseDto> CREATOR =
            new Creator<ResponseDto>() {

                @Override
                public ResponseDto createFromParcel(Parcel source) {
                    return new ResponseDto(source);
                }

                @Override
                public ResponseDto[] newArray(int size) {
                    return new ResponseDto[size];
                }

            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(statusCode);
        parcel.writeString(serviceCaller);
        parcel.writeString(caption);
        parcel.writeString(notificationMessage);
        parcel.writeString(message);
        if (operation != null) {
            parcel.writeString(new String(ObjectUtils.serializeObject(operation)));
        } else parcel.writeString(null);
        parcel.writeSerializable(operationType);
        parcel.writeSerializable(contentType);
        if (messageBytes != null) {
            parcel.writeInt(messageBytes.length);
            parcel.writeByteArray(messageBytes);
        } else {
            parcel.writeInt(0);
            parcel.writeByteArray(new byte[0]);
        }
        parcel.writeParcelable(uri, flags);
    }

    public String getNotificationMessage() {
        if (notificationMessage == null) notificationMessage = getMessage();
        return notificationMessage;
    }

    public ResponseDto setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
        return this;
    }

    public OperationDto getOperation() {
        return operation;
    }

    public void setOperation(OperationDto operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " - statusCode: " + statusCode +
                " - operationType:" + operationType;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }
}