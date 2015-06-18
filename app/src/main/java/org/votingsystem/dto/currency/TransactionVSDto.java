package org.votingsystem.dto.currency;

import android.content.Context;
import android.net.Uri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type { CURRENCY_REQUEST, CURRENCY_SEND, CURRENCY_CHANGE, FROM_BANKVS, FROM_USERVS,
        FROM_GROUP_TO_MEMBER_GROUP, FROM_GROUP_TO_MEMBER, FROM_GROUP_TO_ALL_MEMBERS,
        CURRENCY_PERIOD_INIT, TRANSACTIONVS_INFO;}

    private TypeVS operation;
    private Long id;
    private Long localId;
    private Long userId;
    private UserVSDto fromUserVS;
    private UserVSDto toUserVS;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currencyCode;
    private String fromUser;
    private String toUser;
    private String fromUserIBAN;
    private String receipt;
    private String bankIBAN;
    private String messageSMIME;
    private String cancelationMessageSMIME;
    private String messageSMIMEURL;
    private String messageSMIMEParentURL;
    private String UUID;
    private BigDecimal amount;
    private Boolean timeLimited = Boolean.FALSE;
    private Integer numReceptors;
    private Type type;
    private Set<String> tags;
    private Set<String> toUserIBAN = new HashSet<>();
    private Long numChildTransactions;

    private String infoURL;
    private List<Type> paymentOptions;
    private TransactionVSDetailsDto details;
    private UserVSDto.Type userToType;

    @JsonIgnore private TagVSDto tagVS;
    @JsonIgnore private SMIMEMessage smimeMessage;
    @JsonIgnore private SMIMEMessage cancelationSmimeMessage;
    @JsonIgnore private List<UserVSDto> toUserVSList;
    @JsonIgnore private SocketMessageDto socketMessageDto;
    @JsonIgnore private UserVSDto signer;
    @JsonIgnore private UserVSDto receptor;
    @JsonIgnore private QRMessageDto qrMessageDto;


    public TransactionVSDto() {}


    public static TransactionVSDto PAYMENT_REQUEST(String toUser, UserVSDto.Type userToType,
            BigDecimal amount, String currencyCode, String toUserIBAN, String subject, String tag) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setOperation(TypeVS.TRANSACTIONVS_INFO);
        dto.setUserToType(userToType);
        dto.setToUser(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(new HashSet<>(Arrays.asList(toUserIBAN)));
        dto.setTags(new HashSet<>(Arrays.asList(tag)));
        dto.setDateCreated(new Date());
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionVSDto CURRENCY_REQUEST(BigDecimal amount, String currencyCode,
            TagVSDto tagVS, boolean timeLimited) {
        TransactionVSDto dto = new TransactionVSDto();
        dto.setOperation(TypeVS.CURRENCY_REQUEST);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setTagVS(tagVS);
        dto.setTimeLimited(timeLimited);
        return dto;
    }

    public void validate() throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        type = Type.valueOf(operation.toString());
        if(amount == null) throw new ValidationExceptionVS("missing param 'amount'");
        if(getCurrencyCode() == null) throw new ValidationExceptionVS("missing param 'currencyCode'");
        if(subject == null) throw new ValidationExceptionVS("missing param 'subject'");
        if(timeLimited) validTo = DateUtils.getCurrentWeekPeriod().getDateTo();
        if (tags.size() != 1) { //for now transactions can only have one tag associated
            throw new ValidationExceptionVS("invalid number of tags:" + tags.size());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInfoURL() {
        return infoURL;
    }

    @JsonIgnore
    public String getPaymentConfirmURL() {
        return infoURL + "/" + "payment";
    }

    public SocketMessageDto getSocketMessageDto() {
        return socketMessageDto;
    }

    public void setSocketMessageDto(SocketMessageDto socketMessageDto) {
        this.socketMessageDto = socketMessageDto;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public QRMessageDto getQrMessageDto() {
        return qrMessageDto;
    }

    public void setQrMessageDto(QRMessageDto qrMessageDto) {
        this.infoURL = qrMessageDto.getUrl();
        this.qrMessageDto = qrMessageDto;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public SMIMEMessage getSmimeMessage() throws Exception {
        if(smimeMessage == null && messageSMIME != null) {
            byte[] smimeMessageBytes = Base64.decode(messageSMIME.getBytes());
            smimeMessage = new SMIMEMessage(smimeMessageBytes);
        }
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) throws IOException, MessagingException {
        this.smimeMessage = smimeMessage;
        this.messageSMIME = new String(Base64.encode(smimeMessage.getBytes()));
    }

    public SMIMEMessage getCancelationSmimeMessage() throws Exception {
        if(cancelationSmimeMessage == null && cancelationSmimeMessage != null) {
            byte[] smimeMessageBytes = Base64.decode(cancelationSmimeMessage.getBytes());
            cancelationSmimeMessage = new SMIMEMessage(smimeMessageBytes);
        }
        return cancelationSmimeMessage;
    }

    public void setCancelationSmimeMessage(SMIMEMessage cancelationSmimeMessage)
            throws IOException, MessagingException {
        this.cancelationSmimeMessage = cancelationSmimeMessage;
        this.cancelationMessageSMIME = new String(Base64.encode(cancelationSmimeMessage.getBytes()));
    }

    public String getCancelationMessageSMIME() {
        return cancelationMessageSMIME;
    }

    public void setCancelationMessageSMIME(String cancelationMessageSMIME) {
        this.cancelationMessageSMIME = cancelationMessageSMIME;
    }

    @JsonIgnore public String getTagName() {
        if(tagVS != null) return tagVS.getName();
        else if (tags != null && !tags.isEmpty()) return tags.iterator().next();
        return null;
    }

    public UserVSDto getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVSDto fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVSDto getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVSDto toUserVS) {
        this.toUserVS = toUserVS;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Type getType() {
        if(type == null && operation != null) type = Type.valueOf(operation.toString());
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Long getNumChildTransactions() {
        return numChildTransactions;
    }

    public void setNumChildTransactions(Long numChildTransactions) {
        this.numChildTransactions = numChildTransactions;
    }

    public String getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(String messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getFromUserIBAN() {
        return fromUserIBAN;
    }

    public void setFromUserIBAN(String fromUserIBAN) {
        this.fromUserIBAN = fromUserIBAN;
    }

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public List<UserVSDto> getToUserVSList() {
        return toUserVSList;
    }

    public void setToUserVSList(List<UserVSDto> toUserVSList) {
        this.toUserVSList = toUserVSList;
        this.numReceptors = toUserVSList.size();
    }

    public TagVSDto getTagVS() {
        if(tagVS != null) return tagVS;
        else if(tags != null && !tags.isEmpty()) tagVS = new TagVSDto(tags.iterator().next());
        return tagVS;
    }

    public void setTagVS(TagVSDto tagVS) {
        this.tagVS = tagVS;
    }

    public Set<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(Set<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public UserVSDto getSigner() {
        return signer;
    }

    public void setSigner(UserVSDto signer) {
        this.signer = signer;
    }

    public UserVSDto getReceptor() {
        return receptor;
    }

    public void setReceptor(UserVSDto receptor) {
        this.receptor = receptor;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getBankIBAN() {
        return bankIBAN;
    }

    public void setBankIBAN(String bankIBAN) {
        this.bankIBAN = bankIBAN;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getMessageSMIMEParentURL() {
        return messageSMIMEParentURL;
    }

    public void setMessageSMIMEParentURL(String messageSMIMEParentURL) {
        this.messageSMIMEParentURL = messageSMIMEParentURL;
    }

    public UserVSDto.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(UserVSDto.Type userToType) {
        this.userToType = userToType;
    }

    public List<Type> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<Type> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public static List<String> getPaymentMethods(Context context) {
        //preserve the same order
        List<String> result = Arrays.asList(
                context.getString(R.string.signed_transaction_lbl),
                context.getString(R.string.currency_send_lbl),
                context.getString(R.string.currency_change_lbl));
        return result;
    }

    public static List<String> getPaymentMethods(List<Type> paymentOptions) {
        //preserve the same order
        List<String> result = new ArrayList<>();
        if(paymentOptions.contains(Type.FROM_USERVS)) result.add(AppVS.getInstance()
                .getString(R.string.signed_transaction_lbl));
        if(paymentOptions.contains(Type.CURRENCY_SEND)) result.add(AppVS.getInstance()
                .getString(R.string.currency_send_lbl));
        if(paymentOptions.contains(Type.CURRENCY_CHANGE)) result.add(AppVS.getInstance()
                .getString(R.string.currency_change_lbl));
        return result;
    }

    public static Type getByDescription(String description) throws ValidationExceptionVS {
        if(AppVS.getInstance().getString(R.string.signed_transaction_lbl).equals(description)) return Type.FROM_USERVS;
        if(AppVS.getInstance().getString(R.string.currency_send_lbl).equals(description)) return Type.CURRENCY_SEND;
        if(AppVS.getInstance().getString(R.string.currency_change_lbl).equals(description)) return Type.CURRENCY_CHANGE;
        throw new ValidationExceptionVS("type not found for description: " + description);
    }

    public static Type getByPosition(int position) {
        if(position == 0) return Type.FROM_USERVS;
        if(position == 1) return Type.CURRENCY_SEND;
        if(position == 2) return Type.CURRENCY_CHANGE;
        return null;
    }

    public String getDescription(Context context) {
        switch(type) {
            case FROM_USERVS: return context.getString(R.string.signed_transaction_lbl);
            case CURRENCY_SEND: return context.getString(R.string.currency_send_lbl);
            case CURRENCY_CHANGE: return context.getString(R.string.currency_change_lbl);
        }
        return null;
    }

    public String validateReceipt(SMIMEMessage smimeMessage, boolean isIncome) throws Exception {
        TypeVS typeVS = TypeVS.valueOf(smimeMessage.getHeader("TypeVS")[0]);
        switch(typeVS) {
            case FROM_USERVS:
                return validateFromUserVSReceipt(smimeMessage, isIncome);
            case CURRENCY_SEND:
                return validateCurrencySendReceipt(smimeMessage, isIncome);
            case CURRENCY_CHANGE:
                return validateCurrencyChangeReceipt(smimeMessage, isIncome);
            default: throw new ValidationExceptionVS("unknown receipt type: " + typeVS);
        }
    }

    private String validateCurrencyChangeReceipt(SMIMEMessage smimeMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = smimeMessage.getSignedContent(CurrencyBatchDto.class);
        if(TypeVS.CURRENCY_CHANGE != receiptDto.getOperation()) throw new ValidationExceptionVS("ERROR - expected type: " +
                TypeVS.CURRENCY_CHANGE + " - found: " + receiptDto.getOperation());
        if(type == TransactionVSDto.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(Type.CURRENCY_CHANGE)) throw new ValidationExceptionVS(
                    "unexpected type: " + receiptDto.getOperation());
        }
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome?AppVS.getInstance().getString(R.string.income_lbl):
                AppVS.getInstance().getString(R.string.expense_lbl);
        String result = AppVS.getInstance().getString(R.string.currency_change_receipt_ok_msg,
                action, receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagVSMessage(receiptDto.getTag()));
        if(receiptDto.timeLimited()) {
            result = result + " - " + AppVS.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    private String validateCurrencySendReceipt(SMIMEMessage smimeMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = smimeMessage.getSignedContent(CurrencyBatchDto.class);
        if(TypeVS.CURRENCY_SEND != receiptDto.getOperation()) throw new ValidationExceptionVS("ERROR - expected type: " +
                TypeVS.CURRENCY_SEND + " - found: " + receiptDto.getOperation());
        if(type == TransactionVSDto.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(TransactionVSDto.Type.CURRENCY_SEND)) throw new ValidationExceptionVS(
                    "unexpected type: " + receiptDto.getOperation());
        }
        Set<String> receptorsSet = new HashSet<>(Arrays.asList(receiptDto.getToUserIBAN()));
        if(!toUserIBAN.equals(receptorsSet)) throw new ValidationExceptionVS(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome?AppVS.getInstance().getString(R.string.income_lbl):
                AppVS.getInstance().getString(R.string.expense_lbl);
        String result = AppVS.getInstance().getString(R.string.currency_send_receipt_ok_msg,
                action, receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagVSMessage(receiptDto.getTag()));
        if(receiptDto.timeLimited()) {
            result = result + " - " + AppVS.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    private String validateFromUserVSReceipt(SMIMEMessage smimeMessage, boolean isIncome) throws Exception {
        TransactionVSDto receiptDto = JSON.getMapper().readValue(smimeMessage.getSignedContent(), TransactionVSDto.class);
        if(type == TransactionVSDto.Type.TRANSACTIONVS_INFO) {
            if(!paymentOptions.contains(receiptDto.getType())) throw new ValidationExceptionVS("unexpected type " +
                    receiptDto.getType());
        } else if(type != receiptDto.getType()) throw new ValidationExceptionVS("expected type " + type + " found " +
                receiptDto.getType());
        if(userToType != receiptDto.getUserToType()) throw new ValidationExceptionVS("expected userToType " + userToType +
                " found " + receiptDto.getUserToType());
        if(!new HashSet<>(toUserIBAN).equals(new HashSet<>(receiptDto.getToUserIBAN())) ||
                toUserIBAN.size() != receiptDto.getToUserIBAN().size()) throw new ValidationExceptionVS(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(!subject.equals(receiptDto.getSubject())) throw new ValidationExceptionVS("expected subject " + subject +
                " found " + receiptDto.getSubject());
        if(!toUser.equals(receiptDto.getToUser())) throw new ValidationExceptionVS(
                "expected toUser " + toUser + " found " + receiptDto.getToUser());
        if(amount.compareTo(receiptDto.getAmount()) != 0) throw new ValidationExceptionVS(
                "expected amount " + amount + " amount " + receiptDto.getAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationExceptionVS(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getUUID())) throw new ValidationExceptionVS(
                "expected UUID " + UUID + " found " + receiptDto.getUUID());
        if(details != null && !details.equals(receiptDto.getDetails())) throw new ValidationExceptionVS(
                "expected details " + details + " found " + receiptDto.getDetails());
        String action = isIncome?AppVS.getInstance().getString(R.string.income_lbl):
                AppVS.getInstance().getString(R.string.expense_lbl);
        String result = AppVS.getInstance().getString(R.string.from_uservs_receipt_ok_msg,
                action, receiptDto.getAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagVSMessage(receiptDto.getTagName()));
        if(receiptDto.isTimeLimited()) {
            result = result + " - " + AppVS.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    public TransactionVSDetailsDto getDetails() {
        return details;
    }

    public void setDetails(TransactionVSDetailsDto details) {
        this.details = details;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public int getIconId(Context context) {
        switch(type) {
            case CURRENCY_REQUEST:
                return R.drawable.edit_undo_24;
            case CURRENCY_SEND:
                return R.drawable.fa_money_24;
            case FROM_GROUP_TO_ALL_MEMBERS:
                return R.drawable.system_users_16;
            default:
                return R.drawable.pending;
        }
    }

    public static String getDescription(Context context, Type type) {
        switch(type) {
            case FROM_GROUP_TO_ALL_MEMBERS:
            case FROM_GROUP_TO_MEMBER:
            case FROM_GROUP_TO_MEMBER_GROUP:
                return context.getString(R.string.account_input);
            case CURRENCY_REQUEST:
                return context.getString(R.string.account_output);
            case CURRENCY_SEND:
                return context.getString(R.string.currency_send);
            default:
                return type.toString();
        }
    }

    public String getFormatted(Context context) throws Exception {
        String result = null;
        switch(operation) {
            case FROM_GROUP_TO_ALL_MEMBERS:
                result = timeLimited ? context.getString(R.string.time_limited_lbl) + "</br>":"";
                result = result + context.getString(R.string.from_group_to_all_member_smime_content,
                        fromUser, subject, amount, currencyCode, getTagVS().getName());
                break;
            default:
                result = JSON.writeValueAsString(this);
        }
        return result;
    }

    public static TransactionVSDto fromUri(Uri uriData) {
        TransactionVSDto transactionVS = new TransactionVSDto();
        transactionVS.setAmount(new BigDecimal(uriData.getQueryParameter("amount")));
        TagVSDto tagVS = null;
        if(uriData.getQueryParameter("tagVS") != null) tagVS = new TagVSDto(uriData.getQueryParameter("tagVS"));
        else tagVS = new TagVSDto(TagVSDto.WILDTAG);
        transactionVS.setTagVS(tagVS);
        transactionVS.setCurrencyCode(uriData.getQueryParameter("currencyCode"));
        transactionVS.setSubject(uriData.getQueryParameter("subject"));
        UserVSDto toUserVS = new UserVSDto();
        toUserVS.setName(uriData.getQueryParameter("toUser"));
        toUserVS.setIBAN(uriData.getQueryParameter("toUserIBAN"));
        transactionVS.setToUserVS(toUserVS);
        return transactionVS;
    }

    public static TransactionVSDto fromOperationVS(OperationVS operationVS) throws Exception {
        TransactionVSDto transactionDto = operationVS.getSignedContent(TransactionVSDto.class);
        if(transactionDto.getTagVS() == null) {
            transactionDto.setTagVS(new TagVSDto(TagVSDto.WILDTAG));
        }
        UserVSDto toUserVS = new UserVSDto();
        toUserVS.setName(transactionDto.getFromUser());
        if(operationVS.getTypeVS() == TypeVS.FROM_USERVS) {
            if(transactionDto.getToUserIBAN().size() != 1) throw new ExceptionVS("FROM_USERVS must have " +
                    "'one' receptor and it has '" + transactionDto.getToUserIBAN().size() + "'");
            toUserVS.setIBAN(transactionDto.getToUserIBAN().iterator().next());
        }
        transactionDto.setToUserVS(toUserVS);
        UserVSDto fromUserVS = new UserVSDto();
        fromUserVS.setName(transactionDto.getFromUser());
        fromUserVS.setIBAN(transactionDto.getFromUserIBAN());
        transactionDto.setFromUserVS(fromUserVS);
        return transactionDto;
    }
}
