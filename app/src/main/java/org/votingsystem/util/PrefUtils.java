package org.votingsystem.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.signature.smime.EncryptedBundle;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.votingsystem.util.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.util.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;
import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.makeLogTag;

public class PrefUtils {

    private static final String TAG = makeLogTag(PrefUtils.class.getSimpleName());

    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

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
            applicationId = Utils.getDeviceId();
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
            editor.putString(ContextVS.CAN_KEY, CAN.trim());
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
        UserVSDto userVS = getAppUser();
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

    public static void resetLockRetries() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(ContextVS.RETRIES_KEY, 0);
        editor.commit();
    }

    public static int incrementNumPatternLockRetries() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        int numRetries = settings.getInt(ContextVS.RETRIES_KEY, 0) + 1;
        if(numRetries == ContextVS.NUM_MAX_LOCK_PATERN_RETRIES) {
            LOGD(TAG, "NUM. MAX RETRIES EXCEEDED (3). Resseting password lock");
            putLockPatter(null);
        }
        editor.putInt(ContextVS.RETRIES_KEY, numRetries);
        editor.commit();
        return numRetries;
    }

    public static void putLockPatterProtectedPassword(String lockPattern, char[] passwordToEncrypt) {
        try {
            EncryptedBundle eb = Encryptor.pbeAES_Encrypt(lockPattern, new String(passwordToEncrypt).getBytes());
            EncryptedBundleDto ebDto = new EncryptedBundleDto(eb);
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.LOCK_PATTERN_PASSWORD_KEY, JSON.writeValueAsString(ebDto));
            editor.commit();
            putLockPatter(lockPattern);
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static char[] getLockPatterProtectedPassword(String lockPattern) {
        char[] password = null;
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(ContextVS.LOCK_PATTERN_PASSWORD_KEY, null);
            if(dtoStr != null) {
                EncryptedBundleDto ebDto = JSON.readValue(dtoStr, EncryptedBundleDto.class);
                byte[] resultBytes = Encryptor.pbeAES_Decrypt(lockPattern, ebDto.getEncryptedBundle());
                password = new String(resultBytes, "UTF-8").toCharArray();
            }
        } catch(Exception ex) {ex.printStackTrace();}
        return password;
    }

    public static void putLockPatter(String lockPattern) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if(lockPattern != null) lockPattern =  StringUtils.getHashBase64(lockPattern,
                    ContextVS.VOTING_DATA_DIGEST);
            editor.putString(ContextVS.LOCK_PATTERN_KEY, lockPattern);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static void checkLockPatternHash(String patternPassword, Context context)
            throws ExceptionVS {
        try {
            String expectedHash =  PrefUtils.getLockPatternHash();
            String patternPasswordHash = StringUtils.getHashBase64(patternPassword,
                    ContextVS.VOTING_DATA_DIGEST);
            if(!expectedHash.equals(patternPasswordHash)) {
                int numRetries = PrefUtils.incrementNumPatternLockRetries();
                throw new ExceptionVS(context.getString(R.string.pin_error_msg) + ", " +
                        context.getString(R.string.enter_password_retry_msg,
                                (ContextVS.NUM_MAX_LOCK_PATERN_RETRIES - numRetries)));
            }
            PrefUtils.resetLockRetries();
        } catch (Exception ex) {
            if(ex instanceof ExceptionVS) throw (ExceptionVS)ex;
            else throw new ExceptionVS(ex.getMessage());
        }
    }

    public static String getLockPatternHash() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.LOCK_PATTERN_KEY, null);
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

    public static void putAppUser(UserVSDto userVS) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUserVS = ObjectUtils.serializeObject(userVS);
        try {
            editor.putString(ContextVS.USER_KEY, new String(serializedUserVS, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserVSDto getAppUser() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUserVS = settings.getString(ContextVS.USER_KEY, null);
        if(serializedUserVS != null) {
            UserVSDto userVS = (UserVSDto) ObjectUtils.deSerializeObject(serializedUserVS.getBytes());
            return userVS;
        }
        return null;
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
