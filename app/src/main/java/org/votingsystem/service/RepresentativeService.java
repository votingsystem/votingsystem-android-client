package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
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

    public RepresentativeService() {
        super(TAG);
    }

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String dtoStr = arguments.getString(ContextVS.DTO_KEY);
        LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - serviceCaller: " + serviceCaller);
        if(appVS.getAccessControl() == null) {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(
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
            case STATE:
                checkRepresentationState(serviceCaller);
                break;
            case REPRESENTATIVE_SELECTION:
                try {
                    processRepresentativeSelection(
                            JSON.readValue(dtoStr, RepresentativeDelegationDto.class), serviceCaller);
                } catch (IOException e) { e.printStackTrace(); }
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                try {
                    processAnonymousRepresentativeSelection(
                            JSON.readValue(dtoStr, RepresentativeDelegationDto.class), serviceCaller);
                } catch (IOException e) { e.printStackTrace(); }
                break;
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                processAnonymousRepresentativeSelectionCancelation(serviceCaller);
                break;
            default: LOGD(TAG + ".onHandleIntent", "unhandled operation: " + operation.toString());
        }
    }

    private void checkRepresentationState(String serviceCaller) {
        if(appVS.getUserVS() == null) {
            appVS.broadcastResponse(ResponseVS.ERROR(getString(R.string.error_lbl),
                    getString(R.string.cert_required_error_msg)).setServiceCaller(serviceCaller));
            return;
        }
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
            responseVS = ResponseVS.OK();
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.STATE);
        appVS.broadcastResponse(responseVS);
    }


    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                ResultListDto<UserVSDto> resultListDto = (ResultListDto<UserVSDto>)
                        responseVS.getMessage(new TypeReference<ResultListDto<UserVSDto>>() {});
                UserContentProvider.setNumTotalRepresentatives(resultListDto.getTotalCount());
                List<ContentValues> contentValuesList = new ArrayList<>();
                for(UserVSDto representative : resultListDto.getResultList()) {
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

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = appVS.getAccessControl().getRepresentativeURL(representativeId);
        String imageServiceURL = appVS.getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        ResponseVS responseVS = null;
        org.votingsystem.dto.UserVSDto representative = null;
        try {
            responseVS = HttpHelper.getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                representative = (org.votingsystem.dto.UserVSDto) responseVS.getMessage(org.votingsystem.dto.UserVSDto.class);
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

    private void processRepresentativeSelection(
            RepresentativeDelegationDto delegationDto, String serviceCaller) {
        ResponseVS responseVS = null;
        try {
            delegationDto.setUUID(UUID.randomUUID().toString());
            SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                    JSON.writeValueAsString(delegationDto),
                    getString(R.string.representative_delegation_lbl));
            responseVS = HttpHelper.sendData(smimeMessage.getBytes(),
                    ContentTypeVS.JSON_SIGNED,
                    appVS.getAccessControl().getRepresentativeDelegationServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                RepresentationStateDto stateDto = new RepresentationStateDto();
                stateDto.setState(RepresentationStateDto.State.WITH_PUBLIC_REPRESENTATION);
                stateDto.setLastCheckedDate(new Date());
                stateDto.setRepresentative(delegationDto.getRepresentative());
                responseVS = ResponseVS.OK().setCaption(getString(R.string.operation_ok_msg))
                        .setMessage(getString(R.string.representative_selected_msg));
                PrefUtils.putRepresentationState(stateDto, this);
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
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.REPRESENTATIVE_SELECTION);
            appVS.broadcastResponse(responseVS);
        }
    }

    private void processAnonymousRepresentativeSelectionCancelation(String serviceCaller) {
        LOGD(TAG + ".processAnonymousRepresentativeSelectionCancelation",
                "processAnonymousRepresentativeSelectionCancelation");
        ResponseVS responseVS = null;
        try {
            RepresentativeDelegationDto representativeDelegationDto = PrefUtils.getAnonymousDelegation(this);
            if(representativeDelegationDto == null) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                        getString(R.string.missing_anonymous_delegation_cancellation_data));
            } else {
                responseVS = processAnonymousRepresentativeSelectionCancelation(representativeDelegationDto);
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
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
            appVS.broadcastResponse(responseVS);
        }
    }

    private ResponseVS processAnonymousRepresentativeSelectionCancelation(
            RepresentativeDelegationDto delegation) throws Exception {
        LOGD(TAG + ".processAnonymousRepresentativeSelectionCancelation",
                "processAnonymousRepresentativeSelectionCancelation");
        RepresentativeDelegationDto anonymousCancelationRequest = delegation.getAnonymousCancelationRequest();
        RepresentativeDelegationDto anonymousRepresentationDocumentCancelationRequest =
                delegation.getAnonymousRepresentationDocumentCancelationRequest();
        SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                JSON.getMapper().writeValueAsString(anonymousCancelationRequest),
                getString(R.string.anonymous_delegation_cancellation_lbl));
        SMIMEMessage anonymousSmimeMessage = delegation.getCertificationRequest().getSMIME(
                delegation.getHashCertVSBase64(),
                AppVS.getInstance().getAccessControl().getName(),
                JSON.getMapper().writeValueAsString(anonymousRepresentationDocumentCancelationRequest),
                getString(R.string.anonymous_delegation_cancellation_lbl));
        MessageTimeStamper timeStamper = new MessageTimeStamper(anonymousSmimeMessage,
                AppVS.getInstance().getAccessControl().getTimeStampServiceURL());
        anonymousSmimeMessage = timeStamper.call();

        Map<String, Object> mapToSend = new HashMap<>();
        mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
        mapToSend.put(ContextVS.SMIME_ANONYMOUS_FILE_NAME, anonymousSmimeMessage.getBytes());
        ResponseVS responseVS =  HttpHelper.sendObjectMap(mapToSend,
                AppVS.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SMIMEMessage delegationReceipt = responseVS.getSMIME();
            Collection matches = delegationReceipt.checkSignerCert(
                    AppVS.getInstance().getAccessControl().getCertificate());
            if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
            responseVS.setSMIME(delegationReceipt);
        }
        return responseVS;
    }

    private void processAnonymousRepresentativeSelection(RepresentativeDelegationDto delegationDto,
                                                         String serviceCaller) {
        ResponseVS responseVS = null;
        try {
            String messageSubject = getString(R.string.representative_delegation_lbl);
            delegationDto.setServerURL(AppVS.getInstance().getAccessControl().getServerURL());
            RepresentativeDelegationDto anonymousCertRequest = delegationDto.getAnonymousCertRequest();
            RepresentativeDelegationDto anonymousDelegationRequest = delegationDto.getDelegation();
            SMIMEMessage smimeMessage = appVS.signMessage(AppVS.getInstance().getAccessControl().getName(),
                    JSON.writeValueAsString(anonymousCertRequest),
                    getString(R.string.anonimous_representative_request_lbl),
                    appVS.getTimeStampServiceURL());
            delegationDto.setAnonymousDelegationRequestBase64ContentDigest(smimeMessage.getContentDigestStr());
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, delegationDto.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend,
                    AppVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                delegationDto.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                //this is the delegation request signed with anonymous cert
                smimeMessage = delegationDto.getCertificationRequest().getSMIME(
                        delegationDto.getHashCertVSBase64(),
                        AppVS.getInstance().getAccessControl().getName(),
                        JSON.getMapper().writeValueAsString(anonymousDelegationRequest),
                        messageSubject);
                smimeMessage = new MessageTimeStamper(smimeMessage,
                        AppVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
                responseVS = HttpHelper.sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                        AppVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    delegationDto.setDelegationReceipt(responseVS.getSMIME(),
                            AppVS.getInstance().getAccessControl().getCertificate());
                    PrefUtils.putAnonymousDelegation(delegationDto, this);

                    SMIMEMessage delegationReceipt = new SMIMEMessage(responseVS.getMessageBytes());
                    Collection matches = delegationReceipt.checkSignerCert(
                            appVS.getAccessControl().getCertificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");

                    PrefUtils.putAnonymousDelegation(delegationDto, this);
                    responseVS.setCaption(getString(R.string.anonymous_delegation_caption))
                            .setNotificationMessage(getString(R.string.anonymous_delegation_msg,
                            delegationDto.getRepresentative().getName(),
                            delegationDto.getWeeksOperationActive()));
                } else processAnonymousRepresentativeSelectionCancelation(delegationDto);
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

}