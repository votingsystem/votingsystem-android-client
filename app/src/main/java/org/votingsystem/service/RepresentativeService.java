package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONObject;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.AnonymousSMIMESender;
import org.votingsystem.callable.SignedMapSender;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDto;
import org.votingsystem.model.AnonymousDelegation;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = RepresentativeService.class.getSimpleName();

    private AppVS appVS;

    public RepresentativeService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - serviceCaller: " + serviceCaller);
        if(appVS.getAccessControl() == null) {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR, appVS.getString(
                    R.string.server_connection_error_msg, appVS.getAccessControlURL()));
            appVS.broadcastResponse(responseVS.setServiceCaller(
                    serviceCaller).setTypeVS(operation));
        }
        switch(operation) {
            case ITEMS_REQUEST:
                requestRepresentatives(arguments.getString(ContextVS.URL_KEY), serviceCaller);
                break;
            case ITEM_REQUEST:
                requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY), serviceCaller);
                break;
            case NIF_REQUEST:
                String nif = arguments.getString(ContextVS.NIF_KEY);
                requestRepresentativeByNif(nif, serviceCaller);
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                anonymousDelegation(intent.getExtras(), serviceCaller);
                break;
            case REPRESENTATIVE_SELECTION:
                publicDelegation(intent.getExtras(), serviceCaller);
                break;
            case STATE:
                checkRepresentationState(serviceCaller);
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                cancelAnonymousDelegation(intent.getExtras(), serviceCaller);
                break;
            default: LOGD(TAG + ".onHandleIntent", "unhandled operation: " + operation.toString());
        }
    }

    private void checkRepresentationState(String serviceCaller) {
        ResponseVS responseVS = updateRepresentationState();
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.STATE);
        appVS.broadcastResponse(responseVS);
    }

    private ResponseVS updateRepresentationState() {
        String serviceURL = appVS.getAccessControl().getRepresentationStateServiceURL(
                appVS.getUserVS().getNIF());
        ResponseVS responseVS = null;
        try {
            RepresentationStateDto stateDto = HttpHelper.getData(
                    RepresentationStateDto.class, serviceURL, MediaTypeVS.JSON);
            switch (stateDto.getState()) {
                case REPRESENTATIVE:
                case WITH_PUBLIC_REPRESENTATION:
                    ResponseVS representativeImageReponse = HttpHelper.getData(
                            appVS.getAccessControl().getRepresentativeImageURL(
                                    stateDto.getRepresentative().getId()), null);
                    if (ResponseVS.SC_OK == representativeImageReponse.getStatusCode()) {
                        stateDto.getRepresentative().setImageBytes(
                                representativeImageReponse.getMessageBytes());
                    }
                    break;
                case WITH_ANONYMOUS_REPRESENTATION:
                    break;
                case WITHOUT_REPRESENTATION:
                    break;
            }
            PrefUtils.putRepresentationState(stateDto, this);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                ResultListDto<RepresentativeDto> resultListDto = (ResultListDto<RepresentativeDto>)
                        responseVS.getMessage(new TypeReference<ResultListDto<RepresentativeDto>>() {});
                UserContentProvider.setNumTotalRepresentatives(resultListDto.getTotalCount());
                List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                for(RepresentativeDto representative : resultListDto.getResultList()) {
                    contentValuesList.add(UserContentProvider.getContentValues(representative));
                }
                if(!contentValuesList.isEmpty()) {
                    int numRowsCreated = getContentResolver().bulkInsert(
                            UserContentProvider.CONTENT_URI,contentValuesList.toArray(
                            new ContentValues[contentValuesList.size()]));
                    LOGD(TAG + ".requestRepresentatives", "inserted: " + numRowsCreated + " rows");
                } else { //To notify ContentProvider Listeners
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                }
                responseVS = new ResponseVS(ResponseVS.SC_OK);
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, this);
            }
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEMS_REQUEST);
        appVS.broadcastResponse(responseVS);
    }


    private void requestRepresentativeByNif(String nif, String serviceCaller) {
        String serviceURL = appVS.getAccessControl().getRepresentativeURLByNif(nif);
        ResponseVS responseVS = HttpHelper.getData(serviceURL, null);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            responseVS.setCaption(getString(R.string.operation_error_msg));
        } else {
            try {
                JSONObject jsonResponse = new JSONObject(responseVS.getMessage());
                Long representativeId = jsonResponse.getLong("representativeId");
                requestRepresentative(representativeId, serviceCaller);
            } catch(Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, this);
            }
        }
        responseVS.setTypeVS(TypeVS.ITEM_REQUEST).setServiceCaller(serviceCaller);
        appVS.broadcastResponse(responseVS);
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = appVS.getAccessControl().getRepresentativeURL(representativeId);
        String imageServiceURL = appVS.getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        ResponseVS responseVS = null;
        UserVSDto representative = null;
        try {
            responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                representative = (UserVSDto) responseVS.getMessage(UserVSDto.class);
                representative.setImageBytes(representativeImageBytes);
                Uri representativeURI = UserContentProvider.getUserVSURI(
                        representative.getId());
                getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
                responseVS.setUri(representativeURI);
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEM_REQUEST);
        Intent intent = new Intent(serviceCaller);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        intent.putExtra(ContextVS.USER_KEY, representative);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void publicDelegation(Bundle arguments, String serviceCaller) {
        ResponseVS responseVS = null;
        UserVSDto representative = (UserVSDto) arguments.getSerializable(ContextVS.USER_KEY);
        String serviceURL = appVS.getAccessControl().getRepresentativeDelegationServiceURL();
        String messageSubject = getString(R.string.representative_delegation_lbl);
        try {
            Map signatureDataMap = new HashMap();
            signatureDataMap.put("operation", TypeVS.REPRESENTATIVE_SELECTION.toString());
            signatureDataMap.put("UUID", UUID.randomUUID().toString());
            signatureDataMap.put("accessControlURL", appVS.getAccessControl().getServerURL());
            signatureDataMap.put("representativeNif", representative.getNIF());
            signatureDataMap.put("representativeName", representative.getName());
            JSONObject signatureContent = new JSONObject(signatureDataMap);
            SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                    signatureContent.toString(), messageSubject);
            responseVS = HttpHelper.sendData(smimeMessage.getBytes(),
                    ContentTypeVS.JSON_SIGNED, serviceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.error_lbl));
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    MessageDto messageDto = (MessageDto) responseVS.getMessage(MessageDto.class);
                    responseVS.setNotificationMessage(messageDto.getMessage());
                    responseVS.setData(messageDto.getURL());
                } else responseVS.setNotificationMessage(responseVS.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.REPRESENTATIVE_SELECTION);
            appVS.broadcastResponse(responseVS);
        }
    }

    private void cancelAnonymousDelegation(Bundle arguments, String serviceCaller) {
        LOGD(TAG + ".cancelAnonymousDelegation", "cancelAnonymousDelegation");
        ResponseVS responseVS = null;
        try {
            AnonymousDelegation anonymousDelegation = PrefUtils.getAnonymousDelegation(this);
            if(anonymousDelegation == null) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                        getString(R.string.missing_anonymous_delegation_cancellation_data));
            } else {
                responseVS = cancelAnonymousDelegation(anonymousDelegation);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    PrefUtils.putAnonymousDelegation(null, this);
                    responseVS.setCaption(getString(R.string.cancel_anonymouys_representation_lbl)).
                            setNotificationMessage(getString(R.string.cancel_anonymous_representation_ok_msg));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED);
            appVS.broadcastResponse(responseVS);
        }
    }

    private ResponseVS cancelAnonymousDelegation(
            AnonymousDelegation anonymousDelegation) throws Exception {
        LOGD(TAG + ".cancelAnonymousDelegation", "cancelAnonymousDelegation");
        JSONObject requestJSON = anonymousDelegation.getCancellationRequest();
        SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                requestJSON.toString(), getString(R.string.anonymous_delegation_cancellation_lbl));
        ResponseVS responseVS = HttpHelper.sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                appVS.getAccessControl().getCancelAnonymousDelegationServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessage delegationReceipt = new SMIMEMessage(new ByteArrayInputStream(
                    responseVS.getMessageBytes()));
            Collection matches = delegationReceipt.checkSignerCert(
                    appVS.getAccessControl().getCertificate());
            if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
            responseVS.setSMIME(delegationReceipt);
        }
        return responseVS;
    }

    private void anonymousDelegation(Bundle arguments, String serviceCaller) {
        Integer weeksOperationActive = Integer.valueOf(arguments.getString(ContextVS.TIME_KEY));
        Date dateFrom = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        Integer weeksDelegation = Integer.valueOf(weeksOperationActive);
        Date dateTo = DateUtils.addDays(dateFrom, weeksDelegation * 7).getTime();
        UserVSDto representative = (UserVSDto) arguments.getSerializable(ContextVS.USER_KEY);
        ResponseVS responseVS = null;
        try {
            String messageSubject = getString(R.string.representative_delegation_lbl);
            AnonymousDelegation anonymousDelegation = new AnonymousDelegation(weeksOperationActive,
                    dateFrom, dateTo, appVS.getAccessControl().getServerURL());
            String fromUser = appVS.getUserVS().getNIF();
            String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                    MediaTypeVS.JSON_SIGNED;
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.TEXT.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, anonymousDelegation.
                    getCertificationRequest().getCsrPEM());
            //request signed with user certificate (data signed without representative data)
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    appVS.getAccessControl().getName(),
                    anonymousDelegation.getRequest().toString(), mapToSend, messageSubject, null,
                    appVS.getAccessControl().getAnonymousDelegationRequestServiceURL(),
                    representativeDataFileName, (AppVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                responseVS.setData(anonymousDelegation.getCertificationRequest());
                String fromAnonymousUser = anonymousDelegation.getHashCertVS();
                String toUser = appVS.getAccessControl().getName();
                //delegation signed with anonymous certificate (with delegation data)
                AnonymousSMIMESender anonymousSender = new AnonymousSMIMESender(fromAnonymousUser,
                        toUser, anonymousDelegation.getDelegation(representative.getNIF(),
                        representative.getName()).toString(), messageSubject,
                        appVS.getAccessControl().getAnonymousDelegationServiceURL(), null,
                        anonymousDelegation.getCertificationRequest(),
                        (AppVS)getApplicationContext());
                responseVS = anonymousSender.call();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    SMIMEMessage delegationReceipt = new SMIMEMessage(new ByteArrayInputStream(
                            responseVS.getMessageBytes()));
                    Collection matches = delegationReceipt.checkSignerCert(
                            appVS.getAccessControl().getCertificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                    anonymousDelegation.setRepresentative(representative);
                    PrefUtils.putAnonymousDelegation(anonymousDelegation, this);
                    responseVS.setCaption(getString(R.string.anonymous_delegation_caption)).setNotificationMessage(
                            getString(R.string.anonymous_delegation_msg,
                                    representative.getName(), weeksOperationActive));
                } else cancelAnonymousDelegation(anonymousDelegation);
            } else {
                responseVS.setCaption(getString(R.string.error_lbl));
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    MessageDto messageDto = (MessageDto) responseVS.getMessage(MessageDto.class);
                    responseVS.setNotificationMessage(messageDto.getMessage());
                    responseVS.setData(messageDto.getURL());
                } else responseVS.setNotificationMessage(responseVS.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller).setTypeVS(
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
            appVS.broadcastResponse(responseVS);
        }
    }


    private byte[] reduceImageFileSize(Uri imageUri) {
        byte[] imageBytes = null;
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            int compressFactor = 80;
            //Gallery images form phones like Nexus4 can be greater than 3 MB
            //InputStream inputStream = getContentResolver().openInputStream(imageUri);
            //representativeImageBytes = FileUtils.getBytesFromInputStream(inputStream);
            //Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            do {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                //0 meaning compress for small size, 100 meaning compress for max quality.
                // Some formats, like PNG which is lossless, will ignore the quality setting
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressFactor, out);
                imageBytes = out.toByteArray();
                compressFactor = compressFactor - 10;
                LOGD(TAG + ".reduceImageFileSize", "compressFactor: " + compressFactor +
                        " - imageBytes: " + imageBytes.length);
            } while(imageBytes.length > ContextVS.MAX_REPRESENTATIVE_IMAGE_FILE_SIZE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return imageBytes;
    }

}