package org.votingsystem.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.AddressVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.votingsystem.util.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.util.ContextVS.NIF_KEY;
import static org.votingsystem.util.ContextVS.STATE_KEY;
import static org.votingsystem.util.ContextVS.State;
import static org.votingsystem.util.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;
import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.makeLogTag;

public class PrefUtils {

    private static final String TAG = makeLogTag(PrefUtils.class.getSimpleName());

    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

    private static Integer numMessagesNotReaded;
    private static RepresentationStateDto representation;
    private static RepresentativeDelegationDto representativeDelegationDto;

    public static void init() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        //initialize listened keys
        sp.edit().putBoolean(ContextVS.BOOTSTRAP_DONE, false).commit();
        sp.edit().putString(ContextVS.ACCESS_CONTROL_URL_KEY, null).commit();
        sp.edit().remove(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY).commit();
        new Thread(new Runnable() {
            @Override public void run() { getRepresentationState(); }
        }).start();
    }

    public static void markDataBootstrapDone() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(ContextVS.BOOTSTRAP_DONE, true).commit();
    }

    public static void markAccessControlLoaded(String accessControlURL) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL).commit();
    }

    public static boolean isDataBootstrapDone() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(ContextVS.BOOTSTRAP_DONE, false);
    }

    public static String getApplicationId()  {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(ContextVS.APPLICATION_ID_KEY, null);
        if(applicationId == null) {
            TelephonyManager telephonyManager = (TelephonyManager) AppVS.getInstance().getSystemService(
                    Context.TELEPHONY_SERVICE);
            // phone = telephonyManager.getLine1Number(); -> operator dependent
            //IMSI
            //phone = telephonyManager.getSubscriberId();
            //the IMEI for GSM and the MEID or ESN for CDMA phones. Null if device ID is not available.
            applicationId = telephonyManager.getDeviceId();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(APPLICATION_ID_KEY, applicationId);
            editor.commit();
            LOGD(TAG, ".getApplicationId - new applicationId: " + applicationId);
        }
        return applicationId;
    }

    public static void putDNIeEnabled(boolean enabled) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(ContextVS.DNIE_KEY, enabled);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static boolean isDNIeEnabled() {
        SharedPreferences pref = AppVS.getInstance().getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        return pref.getBoolean(ContextVS.DNIE_KEY, true);
    }

    public static void putDNIeCAN(String CAN) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.CAN_KEY, CAN);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getDNIeCAN() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getString(ContextVS.CAN_KEY, null);
    }

    public static void putApplicationId(String applicationId) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.APPLICATION_ID_KEY, applicationId);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static Calendar getLastPendingOperationCheckedTime() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        Calendar lastCheckedTime = Calendar.getInstance();
        lastCheckedTime.setTimeInMillis(sp.getLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0L));
        return lastCheckedTime;
    }

    public static void markPendingOperationCheckedNow() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis()).commit();
    }

    public static void registerPreferenceChangeListener(
        SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static Date getCurrencyAccountsLastCheckDate() {
        SharedPreferences pref = AppVS.getInstance().getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY, 0));
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public static BalancesDto getBalances() throws Exception {
        Calendar currentMonday = DateUtils.getMonday(Calendar.getInstance());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getPath(currentMonday.getTime());
        SharedPreferences pref = AppVS.getInstance().getSharedPreferences(
                ContextVS.VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String balancesStr = pref.getString(editorKey, null);
        try {
            return JSON.readValue(balancesStr, BalancesDto.class);
        } catch (Exception ex) { ex.printStackTrace();}
        return null;
    }

    public static void putBalances(BalancesDto balancesDto, TimePeriod timePeriod) throws Exception {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        UserVSDto userVS = getSessionUserVS();
        putSessionUserVS(userVS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getPath(timePeriod.getDateFrom());
        try {
            editor.putString(editorKey, JSON.writeValueAsString(balancesDto));
            editor.commit();
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static void putPin(char[] pin) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.PIN_KEY,
                    StringUtils.getHashBase64(new String(pin), ContextVS.VOTING_DATA_DIGEST));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getPinHash() throws NoSuchAlgorithmException, ExceptionVS {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.PIN_KEY, null);
    }

    public static void putWallet(byte[] encryptedWalletBytesBase64) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if(encryptedWalletBytesBase64 == null) editor.putString(ContextVS.WALLET_FILE_NAME, null);
            else editor.putString(ContextVS.WALLET_FILE_NAME, new String(encryptedWalletBytesBase64));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getWallet() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.WALLET_FILE_NAME, null);
    }

    public static String getCsrRequest() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.CSR_KEY, null);
    }

    public static org.votingsystem.util.ContextVS.State getAppCertState(String accessControlURL) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControlURL, State.WITHOUT_CSR.toString());
        return State.valueOf(stateStr);
    }

    public static void putAppCertState(String accessControlURL, State state, String nif) {
        LOGD(TAG + ".putAppCertState", STATE_KEY + "_" + accessControlURL +
                " - state: " + state.toString());
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControlURL , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        editor.commit();
    }

    public static void putCsrRequest(Long requestId, CertificationRequestVS certificationRequest) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
        editor.putLong(ContextVS.CSR_REQUEST_ID_KEY, requestId);
        try {
            editor.putString(ContextVS.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch(Exception ex) {ex.printStackTrace();}
        editor.commit();
    }

    public static void putSessionUserVS(UserVSDto userVS) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUserVS = ObjectUtils.serializeObject(userVS);
        try {
            editor.putString(ContextVS.USER_KEY, new String(serializedUserVS, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserVSDto getSessionUserVS() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUserVS = settings.getString(ContextVS.USER_KEY, null);
        if(serializedUserVS != null) {
            UserVSDto userVS = (UserVSDto) ObjectUtils.deSerializeObject(serializedUserVS.getBytes());
            return userVS;
        }
        return null;
    }

    public static void putAddressVS(AddressVS addressVS) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        try {
            editor.putString(ContextVS.ADDRESS_KEY, JSON.writeValueAsString(addressVS));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static Integer getNumMessagesNotReaded() {
        if(numMessagesNotReaded != null) return numMessagesNotReaded;
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        numMessagesNotReaded = settings.getInt(ContextVS.NUM_MESSAGES_KEY, 0);
        if(numMessagesNotReaded < 0) numMessagesNotReaded = 0;
        return numMessagesNotReaded;
    }

    public static void addNumMessagesNotReaded(Integer amount) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        try {
            numMessagesNotReaded = numMessagesNotReaded + amount;
            editor.putInt(ContextVS.NUM_MESSAGES_KEY, numMessagesNotReaded);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }


    public static AddressVS getAddressVS() throws IOException {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String dtoStr = settings.getString(ContextVS.ADDRESS_KEY, null);
        if(dtoStr == null) return null;
        return JSON.readValue(dtoStr, AddressVS.class);
    }

    public static void putRepresentationState(RepresentationStateDto stateDto) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        try {
            editor.putString(RepresentationStateDto.class.getSimpleName(),
                    JSON.writeValueAsString(stateDto));
            editor.commit();
            representation = stateDto;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static RepresentationStateDto getRepresentationState() {
        if(representation != null) return representation;
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String stateJSON = settings.getString(
                RepresentationStateDto.class.getSimpleName(), null);
        if(stateJSON != null) {
            try {
                representation = JSON.readValue(stateJSON, RepresentationStateDto.class);
            } catch (IOException e) { e.printStackTrace(); }
        }
        return representation;
    }

    public static void putAnonymousDelegation(RepresentativeDelegationDto delegation) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        RepresentationStateDto representation = null;
        String serializedDelegation = null;
        try {
            if(delegation != null) {
                serializedDelegation = new String(ObjectUtils.serializeObject(delegation), "UTF-8");
                representation = new RepresentationStateDto(Calendar.getInstance().getTime(),
                        RepresentationStateDto.State.WITH_ANONYMOUS_REPRESENTATION, delegation.getRepresentative(),
                        delegation.getDateTo());
            } else {
                representation = new RepresentationStateDto(Calendar.getInstance().getTime(),
                        RepresentationStateDto.State.WITHOUT_REPRESENTATION, null, null);
            }
            editor.putString(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, serializedDelegation);
            editor.commit();
            putRepresentationState(representation);
            representativeDelegationDto = delegation;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static RepresentativeDelegationDto getAnonymousDelegation() {
        if(representativeDelegationDto != null) return representativeDelegationDto;
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedObject = settings.getString(
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, null);
        if(serializedObject != null) {
            representativeDelegationDto = (RepresentativeDelegationDto) ObjectUtils.
                    deSerializeObject(serializedObject.getBytes());
        }
        return representativeDelegationDto;
    }

    public static void changePin(char[] newPin, String oldPin)
            throws ExceptionVS, NoSuchAlgorithmException {
        String storedPinHash = PrefUtils.getPinHash();
        String pinHash = StringUtils.getHashBase64(oldPin, ContextVS.VOTING_DATA_DIGEST);
        if(!storedPinHash.equals(pinHash)) {
            throw new ExceptionVS(AppVS.getInstance().getString(R.string.pin_error_msg));
        }
        PrefUtils.putPin(newPin);
    }
}
