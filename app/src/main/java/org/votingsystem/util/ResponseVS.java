package org.votingsystem.util;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.android.R;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.signature.smime.SMIMEMessage;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ResponseVS<T> implements Parcelable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = ResponseVS.class.getSimpleName();

    public static final int SC_OK                       = 200;
    public static final int SC_OK_WITHOUT_BODY          = 204;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_REQUEST_TIMEOUT          = 408;
    public static final int SC_ERROR_REQUEST            = 400;
    public static final int SC_NOT_FOUND                = 404;
    public static final int SC_ERROR_REQUEST_REPEATED   = 409;
    public static final int SC_EXCEPTION                = 490;
    public static final int SC_NULL_REQUEST             = 472;
    public static final int SC_CURRENCY_EXPENDED           = 473;
    public static final int SC_ERROR                    = 500;
    public static final int SC_CONNECTION_TIMEOUT       = 522;
    public static final int SC_ERROR_TIMESTAMP          = 570;
    public static final int SC_PROCESSING               = 700;
    public static final int SC_TERMINATED               = 710;
    public static final int SC_WS_CONNECTION_INIT_OK    = 800;
    public static final int SC_WS_MESSAGE_SEND_OK       = 801;
    public static final int SC_WS_MESSAGE_ENCRYPTED     = 810;
    public static final int SC_WS_CONNECTION_INIT_ERROR = 840;
    public static final int SC_WS_CONNECTION_NOT_FOUND  = 841;
    public static final int SC_CANCELED                = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;


    private int statusCode;
    private OperationVS operation;
    private String caption;
    private String notificationMessage;
    private String message;
    private String serviceCaller;
    private String url;
    private T data;
    private TypeVS typeVS;
    private SMIMEMessage smimeMessage;
    private byte[] smimeMessageBytes;
    private ContentTypeVS contentType = ContentTypeVS.TEXT;
    private byte[] messageBytes;
    private Uri uri;



    public ResponseVS() {  }

    public ResponseVS(Parcel source) {
        // Must read values in the same order as they were placed in
        statusCode = source.readInt();
        serviceCaller = source.readString();
        caption = source.readString();
        notificationMessage = source.readString();
        message = source.readString();
        String operationStr = source.readString();
        if(operationStr != null) {
            try {
                operation = JSON.readValue(operationStr, OperationVS.class);
            } catch (Exception ex) { ex.printStackTrace();}
        }
        typeVS = (TypeVS) source.readSerializable();
        contentType = (ContentTypeVS) source.readSerializable();
        messageBytes = new byte[source.readInt()];
        source.readByteArray(messageBytes);
        smimeMessageBytes = new byte[source.readInt()];
        source.readByteArray(smimeMessageBytes);
        uri = source.readParcelable(Uri.class.getClassLoader());
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
                      TypeVS typeVS) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
              TypeVS typeVS, SMIMEMessage smimeMessage) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.smimeMessage = smimeMessage;
    }

    public ResponseVS(int statusCode, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.typeVS = typeVS;
    }


    public ResponseVS(int statusCode, String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, SMIMEMessage smimeMessage) {
        this.statusCode = statusCode;
        this.smimeMessage = smimeMessage;
    }

    public ResponseVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public ResponseVS(int statusCode, String message, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.message = message;
        this.contentType = contentType;
    }

    public ResponseVS(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, byte[] messageBytes, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        this.contentType = contentType;
    }

    public ResponseVS(int statusCode) {
        this.statusCode = statusCode;
    }
    public ResponseVS(TypeVS typeVS, T data) {
        this.typeVS = typeVS;
        this.data = data;
    }

    public static ResponseVS getResponse(Integer statusCode, String serviceCaller, String caption,
             String notificationMessage, TypeVS typeVS) {
        ResponseVS result = new ResponseVS(statusCode);
        result.setCaption(caption).setNotificationMessage(notificationMessage);
        result.setServiceCaller(serviceCaller);
        result.setTypeVS(typeVS);
        return result;
    }

    public static ResponseVS EXCEPTION(String caption, String message) {
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        responseVS.setCaption(caption);
        responseVS.setNotificationMessage(message);
        return responseVS;
    }

    public static ResponseVS EXCEPTION(Exception ex, Context context) {
        String message = ex.getMessage();
        if(message == null || message.isEmpty()) message = context.getString(R.string.exception_lbl);
        return EXCEPTION(context.getString(R.string.exception_lbl), message);
    }

    public static ResponseVS ERROR(String caption, String message) {
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        responseVS.setCaption(caption);
        responseVS.setNotificationMessage(message);
        return responseVS;
    }

    public String getMessage() {
        if(message == null && messageBytes != null) {
            try {
                message = new String(messageBytes, "UTF-8");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return message;
    }

    public <S> S getMessage(Class<S> type) throws Exception {
        return JSON.readValue(getMessage(), type);
    }

    public <T> T getMessage(TypeReference<T> type) throws Exception {
        return JSON.readValue(getMessage(), type);
    }

    public ResponseVS setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ResponseVS setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public ResponseVS setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}

    public SMIMEMessage getSMIME() {
        if(smimeMessage == null && (smimeMessageBytes != null || messageBytes != null)) {
            try {
                byte[] smimeBytes = (smimeMessageBytes != null)? smimeMessageBytes:messageBytes;
                smimeMessage = new SMIMEMessage(smimeBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return smimeMessage;
    }

	public void setSMIME(SMIMEMessage smimeMessage) {
		this.smimeMessage = smimeMessage;
	}

    public ContentTypeVS getContentType() {
        return contentType;
    }

    public void setContentType(ContentTypeVS contentType) {
        this.contentType = contentType;
    }

    public String getCaption() {
        if(caption == null && operation != null) return operation.getSignedMessageSubject();
        else return caption;
    }

    public ResponseVS setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getServiceCaller() {
        return serviceCaller;
    }

    public ResponseVS setServiceCaller(String serviceCaller) {
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

    public static final Creator<ResponseVS> CREATOR =
            new Creator<ResponseVS>() {

        @Override public ResponseVS createFromParcel(Parcel source) {
            return new ResponseVS(source);
        }

        @Override public ResponseVS[] newArray(int size) {
            return new ResponseVS[size];
        }

    };

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(statusCode);
        parcel.writeString(serviceCaller);
        parcel.writeString(caption);
        parcel.writeString(notificationMessage);
        parcel.writeString(message);
        if(operation != null) {
            try {
                parcel.writeString(JSON.writeValueAsString(operation));
            } catch (Exception ex) { ex.printStackTrace();}
        } else parcel.writeString(null);
        parcel.writeSerializable(typeVS);
        parcel.writeSerializable(contentType);
        if(messageBytes != null) {
            parcel.writeInt(messageBytes.length);
            parcel.writeByteArray(messageBytes);
        } else {
            parcel.writeInt(0);
            parcel.writeByteArray(new byte[0]);
        }
        if(smimeMessage != null) {
            try {
                byte[] smimeMessageBytes = smimeMessage.getBytes();
                parcel.writeInt(smimeMessageBytes.length);
                parcel.writeByteArray(smimeMessageBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        } else {
            parcel.writeInt(0);
            parcel.writeByteArray(new byte[0]);
        }
        parcel.writeParcelable(uri, flags);
    }

    public String getNotificationMessage() {
        if(notificationMessage == null) notificationMessage = getMessage();
        return notificationMessage;
    }

    public ResponseVS setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
        return this;
    }

    public OperationVS getOperation() {
        return operation;
    }

    public void setOperation(OperationVS operation) {
        this.operation = operation;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + " - statusCode: " + statusCode +
                " - typeVS:" + typeVS;
    }

}