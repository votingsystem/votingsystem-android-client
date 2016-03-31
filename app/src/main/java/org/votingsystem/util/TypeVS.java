package org.votingsystem.util;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum TypeVS {

    CURRENCY_SYSTEM,
    VOTING_SYSTEM,

    ACCESS_REQUEST,
    ITEM_REQUEST,
    ITEMS_REQUEST,
    VOTING_EVENT,

    EDIT_REPRESENTATIVE,
    REPRESENTATIVE_REVOKE,
    NEW_REPRESENTATIVE,
    REPRESENTATIVE,
    ANONYMOUS_REPRESENTATIVE_SELECTION,
    ANONYMOUS_SELECTION_CERT_REQUEST,
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION,

    PIN,
    EVENT_CANCELLATION,
    BACKUP_REQUEST,
    SEND_VOTE,
    CANCEL_VOTE,
    PUBLISH_EVENT,

    BROWSER_URL,
    CURRENCY,
    CURRENCY_CANCEL,
    CURRENCY_CHECK,
    FROM_USER,
    CURRENCY_GROUP_NEW,
    CURRENCY_PERIOD_INIT,
    CURRENCY_REQUEST,
    CURRENCY_CHANGE,
    CURRENCY_WALLET_CHANGE,
    CURRENCY_SEND,
    CURRENCY_ACCOUNTS_INFO,
    DEVICE_SELECT,
    TRANSACTION_INFO,
    TRANSACTION_RESPONSE,
    CURRENCY_BATCH,

    FROM_BANK,
    FROM_GROUP_TO_MEMBER_GROUP,
    FROM_GROUP_TO_MEMBER,
    FROM_GROUP_TO_ALL_MEMBERS,
    STATE,

    MESSAGE_INFO,
    MESSAGE_INFO_RESPONSE,
    USER_INFO,

    DELIVERY_WITHOUT_PAYMENT,
    DELIVERY_WITH_PAYMENT,
    REQUEST_FORM,

    MESSAGEVS,
    MSG_TO_DEVICE,
    MESSAGEVS_FROM_VS,

    OPERATION_PROCESS,
    OPERATION_RESULT,

    REPRESENTATIVE_STATE,

    LISTEN_TRANSACTIONS,
    INIT_SESSION,
    INIT_SIGNED_SESSION,
    INIT_REMOTE_SIGNED_SESSION,
    WEB_SOCKET_INIT,
    WEB_SOCKET_CLOSE,
    WEB_SOCKET_BAN_SESSION,
    WEB_SOCKET_REQUEST,
    RECEIPT;

}
