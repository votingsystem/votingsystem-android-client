package org.votingsystem.util;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.MediaType;

import java.nio.charset.Charset;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Constants {

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;


    public static final String VOTING_SYSTEM_BASE_OID             = "0.0.0.0.0.0.0.0.0.";
    public static final String VOTE_OID                           = VOTING_SYSTEM_BASE_OID + 0;
    public static final String REPRESENTATIVE_VOTE_OID            = VOTING_SYSTEM_BASE_OID + 1;
    public static final String ANON_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID + 2;
    public static final String CURRENCY_OID                       = VOTING_SYSTEM_BASE_OID + 3;
    public static final String DEVICE_OID                         = VOTING_SYSTEM_BASE_OID + 4;
    public static final String ANON_CERT_OID                      = VOTING_SYSTEM_BASE_OID + 5;



    public static final boolean IS_DEBUG_SESSION = Boolean.TRUE;
    public static final boolean ALLOW_ROOTED_PHONES = Boolean.TRUE;

    public static final int KEY_SIZE = 2048;
    public static final int SYMETRIC_ENCRYPTION_KEY_LENGTH = 256;
    public static final int SYMETRIC_ENCRYPTION_ITERATION_COUNT = 100;

    public static final String DATA_DIGEST_ALGORITHM = "SHA256";
    public static final String SIG_NAME = "RSA";
    public static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String USER_CERT_ALIAS = "USER_CERT_ALIAS";

    public static final String PRIVATE_PREFS = "VOTING_SYSTEM_PRIVATE_PREFS";

    public static final String SIGNED_FILE_NAME = "signedFile";

    public static final String CSR_FILE_NAME                      = "csr" + ":" + ContentType.TEXT.getName();
    public static final String ANON_CERTIFICATE_REQUEST_FILE_NAME = "anonCertRequest" + ":" + MediaType.XML;

    public static final String CURRENCY_REQUEST_FILE_NAME         = "currencyRequest" + ":" + MediaType.XML;
    public static final String ANDROID_PROVIDER                   = "AndroidOpenSSL";
    public static final String WEB_SOCKET_BROADCAST_ID            = "WEB_SOCKET_BROADCAST_ID";


    public static final String VOTINGSYSTEM_CERTIFICATE = "VOTINGSYSTEM_CERTIFICATE";


    //Intent keys
    public static final String BOOTSTRAP_DONE = "BOOTSTRAP_DONE";
    public static final String DNIE_KEY = "DNIE_KEY";
    public static final String FRAGMENT_KEY = "FRAGMENT_KEY";
    public static final String RESPONSE_KEY = "RESPONSE_KEY";
    public static final String DTO_KEY = "DTO_KEY";

    public static final String PROTECTED_PASSWORD_KEY = "PROTECTED_PASSWORD_KEY";
    public static final String CAN_KEY = "CAN_KEY";
    public static final String DOUBLE_BACK_KEY = "DOUBLE_BACK_KEY";
    public static final String URL_KEY = "URL";
    public static final String SESSION_KEY = "SESSION_KEY";
    public static final String TAG_KEY = "TAG";
    public static final String JS_COMMAND_KEY = "JS_COMMAND_KEY";
    public static final String FORM_DATA_KEY = "FORM_DATA";
    public static final String ID_SERVICE_ENTITY_ID = "ID_SERVICE_ENTITY_ID";
    public static final String VOTING_SERVICE_ENTITY_ID = "VOTING_SERVICE_ENTITY_ID";
    public static final String URI_KEY = "URI_DATA";
    public static final String CALLER_KEY = "CALLER_KEY";
    public static final String MESSAGE_KEY = "MESSAGE_KEY";
    public static final String MESSAGE_CONTENT_KEY = "MESSAGE_CONTENT_KEY";
    public static final String PASSWORD_CONFIRM_KEY = "PASSWORD_CONFIRM_KEY";
    public static final String OPERATION_TYPE = "OPERATION_TYPE";
    public static final String MODE_KEY = "MODE_KEY";
    public static final String MESSAGE_SUBJECT_KEY = "MESSAGE_SUBJECT_KEY";
    public static final String RESPONSE_STATUS_KEY = "RESPONSE_STATUS";
    public static final String OFFSET_KEY = "OFFSET";
    public static final String NUM_ITEMS_KEY = "NUM_ITEMS_KEY";
    public static final String CAPTION_KEY = "CAPTION";
    public static final String IMAGE_KEY = "IMAGE_KEY";
    public static final String OPERATION_KEY = "OPERATION_KEY";
    public static final String OPERATION_CODE_KEY = "OPERATION_CODE_KEY";
    public static final String QR_CODE_KEY = "QR_CODE_KEY";
    public static final String UUID_KEY = "UUID_KEY";
    public static final String LIST_STATE_KEY = "LIST_STATE";
    public static final String ITEM_ID_KEY = "ITEM_ID";
    public static final String CURSOR_POSITION_KEY = "CURSOR_POSITION";
    public static final String ELECTION_STATE_KEY = "EVENT_STATE";
    public static final String ELECTION_KEY = "ELECTION_KEY";
    public static final String VOTE_KEY = "VOTE_KEY";
    public static final String RECEIPT_KEY = "RECEIPT_KEY";
    public static final String STATE_KEY = "STATE";
    public static final String CSR_KEY = "csrKey";
    public static final String APPLICATION_ID_KEY = "APPLICATION_ID_KEY";
    public static final String REFRESH_KEY = "REFRESH_KEY";
    public static final String CMS_MSG_KEY = "CMS_MSG_KEY";
    public static final String XML_SIGNED_MSG_KEY = "XML_SIGNED_MSG_KEY";
    public static final String MAX_VALUE_KEY = "MAX_VALUE_KEY";
    public static final String DEFAULT_VALUE_KEY = "DEFAULT_VALUE_KEY";
    public static final String RETRIES_KEY = "RETRIES_KEY";
    public static final String TIMESTAMP_SERVER_KEY = "TIMESTAMP_SERVER_KEY";
    public static final String PENDING_OPERATIONS_LAST_CHECKED_KEY =
            "PENDING_OPERATIONS_LAST_CHECKED_KEY";

    public static final int ELECTIONS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    public static final int SELECTED_OPTION_MAX_LENGTH = 60;

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final Integer NUM_MAX_PASSW_RETRIES = 3;

    //Notifications IDs
    public static int VOTE_SERVICE_NOTIFICATION_ID = 1000000000;

}