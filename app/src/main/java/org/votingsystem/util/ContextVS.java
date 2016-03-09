package org.votingsystem.util;

import java.nio.charset.Charset;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContextVS {

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final int VOTE_TAG                                = 0;
    public static final int REPRESENTATIVE_VOTE_TAG                 = 1;
    public static final int ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG = 2;
    public static final int CURRENCY_TAG                            = 3;
    public static final int DEVICEVS_TAG                            = 4;
    public static final int ANONYMOUS_CERT_TAG                      = 5;

    public static final String VOTING_SYSTEM_BASE_OID = "0.0.0.0.0.0.0.0.0.";
    public static final String REPRESENTATIVE_VOTE_OID = VOTING_SYSTEM_BASE_OID + REPRESENTATIVE_VOTE_TAG;
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID +
            ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG;
    public static final String VOTE_OID = VOTING_SYSTEM_BASE_OID + VOTE_TAG;
    public static final String CURRENCY_OID = VOTING_SYSTEM_BASE_OID + CURRENCY_TAG;
    public static final String DEVICEVS_OID = VOTING_SYSTEM_BASE_OID + DEVICEVS_TAG;
    public static final String ANONYMOUS_CERT_OID = VOTING_SYSTEM_BASE_OID + ANONYMOUS_CERT_TAG;

    public static final String VOTING_SYSTEM_PRIVATE_PREFS = "VotingSystemSharedPrivatePreferences";

    public static final String DNIe_SIGN_MECHANISM             = "SHA1withRSA";
    public static final String WALLET_FILE_NAME                = "wallet.wvs";
    public static final String SIGNED_FILE_NAME                = "signedFile";
    public static final String CSR_FILE_NAME                   = "csr" + ":" + ContentTypeVS.TEXT.getName();
    public static final String CMS_FILE_NAME                   = "cms" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String CMS_ANONYMOUS_FILE_NAME         = "cmsAnonymous" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String ACCESS_REQUEST_FILE_NAME        = "accessRequest" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String CURRENCY_REQUEST_DATA_FILE_NAME = "currencyRequestData" + ":" + MediaTypeVS.JSON_SIGNED;
    public static final String DEFAULT_SIGNED_FILE_NAME        = "cmsMessage.p7m";
    public static final String PROVIDER                        = "BC";
    public static final String ANDROID_PROVIDER                = "AndroidOpenSSL";
    public static final String WEB_SOCKET_BROADCAST_ID         = "WEB_SOCKET_BROADCAST_ID";


    //Intent keys
    public static final String BOOTSTRAP_DONE = "BOOTSTRAP_DONE";
    public static final String DNIE_KEY = "DNIE_KEY";
    public static final String FRAGMENT_KEY = "FRAGMENT_KEY";
    public static final String RESPONSEVS_KEY = "RESPONSEVS_KEY";
    public static final String DTO_KEY = "DTO_KEY";
    public static final String WEBSOCKET_MSG_KEY = "WEBSOCKET_MSG_KEY";

    public static final String PIN_KEY = "PIN";
    public static final String PASSWORD_KEY = "PASSWORD_KEY";
    public static final String PROTECTED_PASSWORD_KEY = "PROTECTED_PASSWORD_KEY";
    public static final String CAN_KEY = "CAN_KEY";
    public static final String DOUBLE_BACK_KEY = "DOUBLE_BACK_KEY";
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY = "ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY";
    public static final String PERIOD_KEY = "PERIOD";
    public static final String URL_KEY = "URL";
    public static final String SESSION_KEY = "SESSION_KEY";
    public static final String TAG_KEY = "TAG";
    public static final String JS_COMMAND_KEY = "JS_COMMAND_KEY";
    public static final String FORM_DATA_KEY = "FORM_DATA";
    public static final String HASH_CERTVS_KEY = "HASH_CERTVS_KEY";
    public static final String USER_KEY = "USER_KEY";
    public static final String URI_KEY = "URI_DATA";
    public static final String ACCESS_CONTROL_URL_KEY = "ACCESS_CONTROL_URL";
    public static final String CURRENCY_SERVER_URL = "CURRENCY_SERVER_URL";
    public static final String CALLER_KEY = "CALLER_KEY";
    public static final String MESSAGE_KEY = "MESSAGE_KEY";
    public static final String MESSAGE_CONTENT_KEY = "MESSAGE_CONTENT_KEY";
    public static final String PASSWORD_CONFIRM_KEY = "PASSWORD_CONFIRM_KEY";
    public static final String TYPEVS_KEY = "TYPEVS_KEY";
    public static final String MODE_KEY = "MODE_KEY";
    public static final String MESSAGE_SUBJECT_KEY = "MESSAGE_SUBJECT_KEY";
    public static final String RESPONSE_STATUS_KEY = "RESPONSE_STATUS";
    public static final String OFFSET_KEY = "OFFSET";
    public static final String CAPTION_KEY = "CAPTION";
    public static final String IMAGE_KEY = "IMAGE_KEY";
    public static final String OPERATIONVS_KEY = "OPERATIONVS_KEY";
    public static final String LIST_STATE_KEY = "LIST_STATE";
    public static final String ITEM_ID_KEY = "ITEM_ID";
    public static final String USERVS_ACCOUNT_LAST_CHECKED_KEY = "USERVS_ACCOUNT_LAST_CHECKED_KEY";
    public static final String CURSOR_POSITION_KEY = "CURSOR_POSITION";
    public static final String EVENT_STATE_KEY = "EVENT_STATE";
    public static final String EVENTVS_KEY  = "EVENTVS";
    public static final String CURRENCY_KEY  = "CURRENCY_KEY";
    public static final String TRANSACTION_KEY  = "TRANSACTION_KEY";
    public static final String VOTE_KEY  = "VOTE_KEY";
    public static final String RECEIPT_KEY  = "RECEIPT_KEY";
    public static final String STATE_KEY                   = "STATE";
    public static final String CSR_KEY                     = "csrKey";
    public static final String APPLICATION_ID_KEY          = "APPLICATION_ID_KEY";
    public static final String REFRESH_KEY                 = "REFRESH_KEY";
    public static final String CMS_MSG_KEY               = "CMS_MSG_KEY";
    public static final String MAX_VALUE_KEY               = "MAX_VALUE_KEY";
    public static final String DEFAULT_VALUE_KEY           = "DEFAULT_VALUE_KEY";
    public static final String RETRIES_KEY                 = "RETRIES_KEY";
    public static final String PENDING_OPERATIONS_LAST_CHECKED_KEY =
            "PENDING_OPERATIONS_LAST_CHECKED_KEY";


    public static final Integer NUM_MAX_PASSW_RETRIES = 3;
    public static final Integer REPRESENTATIVE_PAGE_SIZE = 20;
    //Num. max of weeks for anonymous delegations. 52 -> one year
    public static final int MAX_WEEKS_ANONYMOUS_DELEGATION = 52;

    //Notifications IDs
    public static int NEW_MESSAGE_NOTIFICATION_ID            = 1000000000;
    public static final int RSS_SERVICE_NOTIFICATION_ID      = 1000000001;
    public static final int VOTE_SERVICE_NOTIFICATION_ID     = 1000000002;
    public static final int ACCOUNTS_UPDATED_NOTIFICATION_ID = 1000000003;

    public static final int NUM_MIN_OPTIONS = 2;

    public static final int KEY_SIZE = 1024;
    public static final int SYMETRIC_ENCRYPTION_KEY_LENGTH = 256;
    public static final int SYMETRIC_ENCRYPTION_ITERATION_COUNT = 100;

    public static final int MAX_REPRESENTATIVE_IMAGE_WIDTH = 500;//max width in pixels
    public static final int MAX_REPRESENTATIVE_IMAGE_FILE_SIZE = 1024 * 1024;

    public static final int EVENTS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    public static final int SELECTED_OPTION_MAX_LENGTH       = 60;
    //TODO por el bug en froyo de -> JcaDigestCalculatorProviderBuilder
    public static final String VOTING_DATA_DIGEST = "SHA256";
    public static final String SIG_NAME = "RSA";
    /** Random Number Generator algorithm. */
    public static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    //public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA256WithRSA";
    public static final String USER_CERT_ALIAS = "USER_CERT_ALIAS";

    public static final Charset UTF_8 = Charset.forName("UTF-8");

}