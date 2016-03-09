package org.votingsystem.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.Encryptor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.votingsystem.util.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.util.ContextVS.PRIVATE_PREFS;
import static org.votingsystem.util.LogUtils.LOGD;


public class PrefUtils {

    private static final String TAG = PrefUtils.class.getSimpleName();

    public static final String CRYPTO_DEVICE_ACCESS_MODE_KEY = "CRYPTO_DEVICE_ACCESS_MODE_KEY";

    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

    private static RepresentationStateDto representation;
    private static RepresentativeDelegationDto representativeDelegationDto;

    public static void init() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
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
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(ContextVS.BOOTSTRAP_DONE, true).commit();
    }

    public static void markAccessControlLoaded(String accessControlURL) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL).commit();
    }

    public static boolean isDataBootstrapDone() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(ContextVS.BOOTSTRAP_DONE, false);
    }

    public static String getDeviceId()  {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(ContextVS.APPLICATION_ID_KEY, null);
        if(applicationId == null) {
            applicationId = Utils.getDeviceId();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(APPLICATION_ID_KEY, applicationId);
            editor.commit();
            LOGD(TAG, ".getDeviceId - new applicationId: " + applicationId);
        }
        return applicationId;
    }

    public static void putDNIeEnabled(boolean enabled) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(ContextVS.DNIE_KEY, enabled);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static boolean isDNIeEnabled() {
        SharedPreferences pref = AppVS.getInstance().getSharedPreferences(ContextVS.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        return pref.getBoolean(ContextVS.DNIE_KEY, false);
    }

    public static void putDNIeCAN(String CAN) {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.CAN_KEY, CAN.trim());
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getDNIeCAN() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getString(ContextVS.CAN_KEY, null);
    }

    public static Calendar getLastPendingOperationCheckedTime() {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        Calendar lastCheckedTime = Calendar.getInstance();
        lastCheckedTime.setTimeInMillis(sp.getLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0L));
        return lastCheckedTime;
    }

    public static void registerPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static Date getCurrencyAccountsLastCheckDate() {
        SharedPreferences pref = AppVS.getInstance().getSharedPreferences(ContextVS.PRIVATE_PREFS,
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
                ContextVS.PRIVATE_PREFS, Context.MODE_PRIVATE);
        String balancesStr = pref.getString(editorKey, null);
        try {
            return JSON.readValue(balancesStr, BalancesDto.class);
        } catch (Exception ex) { ex.printStackTrace();}
        return null;
    }

    public static void putBalances(BalancesDto balancesDto, TimePeriod timePeriod) throws Exception {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getPath(timePeriod.getDateFrom());
        try {
            editor.putString(editorKey, JSON.writeValueAsString(balancesDto));
            editor.commit();
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static void resetPasswordRetries() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(ContextVS.RETRIES_KEY, 0);
        editor.commit();
    }

    public static int incrementPasswordRetries() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        int numRetries = settings.getInt(ContextVS.RETRIES_KEY, 0) + 1;
        if(numRetries >= ContextVS.NUM_MAX_PASSW_RETRIES) {
            LOGD(TAG, "NUM. MAX RETRIES EXCEEDED (3). Resseting CryptoDeviceAccessMode");
            putCryptoDeviceAccessMode(null);
            resetPasswordRetries();
        } else {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ContextVS.RETRIES_KEY, numRetries);
            editor.commit();
        }
        return numRetries;
    }

    public static void putProtectedPassword(CryptoDeviceAccessMode.Mode accessMode,
                                            char[] passw, char[] passwordToEncrypt) {
        try {
            EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                    new String(passw) , new String(passwordToEncrypt).getBytes()).toDto();
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ContextVS.PROTECTED_PASSWORD_KEY, JSON.writeValueAsString(ebDto));
            editor.commit();
            putCryptoDeviceAccessMode(new CryptoDeviceAccessMode(accessMode, passw));
        } catch(Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
    }

    public static char[] getProtectedPassword(char[] passw) {
        char[] password = null;
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(ContextVS.PROTECTED_PASSWORD_KEY, null);
            if(dtoStr != null) {
                EncryptedBundleDto ebDto = JSON.readValue(dtoStr, EncryptedBundleDto.class);
                byte[] resultBytes = Encryptor.pbeAES_Decrypt(new String(passw),
                        ebDto.getEncryptedBundle());
                password = new String(resultBytes, "UTF-8").toCharArray();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
        return password;
    }

    public static void putWallet(Collection<Currency> currencyCollection, char[] passw,
                                 char[] token) throws Exception {
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String encryptedWallet = null;
            if(currencyCollection != null) {
                Set<CurrencyDto> currencyDtoSet = CurrencyDto.serializeCollection(currencyCollection);
                byte[] walletBytes = JSON.writeValueAsBytes(currencyDtoSet);
                EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                        new String(passw) + new String(token), walletBytes).toDto();
                encryptedWallet = JSON.writeValueAsString(ebDto);
            }
            editor.putString(ContextVS.WALLET_FILE_NAME, encryptedWallet);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}

    }

    public static Set<Currency> getWallet(char[] passw, char[] token) {
        Set<Currency> result = null;
        try {
            SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(ContextVS.WALLET_FILE_NAME, null);
            if(dtoStr != null) {
                EncryptedBundleDto ebDto = JSON.readValue(dtoStr, EncryptedBundleDto.class);
                byte[] walletBytes = Encryptor.pbeAES_Decrypt(
                        new String(passw) + new String(token), ebDto.getEncryptedBundle());
                Set<CurrencyDto> currencyDtoSet = JSON.readValue(
                        walletBytes, new TypeReference<Set<CurrencyDto>>(){});
                result = CurrencyDto.deSerializeCollection(currencyDtoSet);
            }
        } catch(Exception ex) {ex.printStackTrace();}
        if(result == null) return new HashSet<>();
        return result;
    }

    public static String getCsrRequest() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.CSR_KEY, null);
    }

    public static void putCsrRequest(CertificationRequestVS certificationRequest) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
        try {
            editor.putString(ContextVS.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch(Exception ex) {ex.printStackTrace();}
        editor.commit();
    }

    public static void putAppUser(UserVSDto userVS) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUserVS = ObjectUtils.serializeObject(userVS);
        try {
            editor.putString(ContextVS.USER_KEY, new String(serializedUserVS, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserVSDto getAppUser() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUserVS = settings.getString(ContextVS.USER_KEY, null);
        if(serializedUserVS != null) {
            UserVSDto userVS = (UserVSDto) ObjectUtils.deSerializeObject(serializedUserVS.getBytes());
            return userVS;
        }
        return null;
    }

    public static void putRepresentationState(RepresentationStateDto stateDto) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
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
                PRIVATE_PREFS, Context.MODE_PRIVATE);
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
                PRIVATE_PREFS, Context.MODE_PRIVATE);
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
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedObject = settings.getString(
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, null);
        if(serializedObject != null) {
            representativeDelegationDto = (RepresentativeDelegationDto) ObjectUtils.
                    deSerializeObject(serializedObject.getBytes());
        }
        return representativeDelegationDto;
    }

    public static void putCryptoDeviceAccessMode(CryptoDeviceAccessMode passwAccessMode) {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if(passwAccessMode == null) {
            editor.putString(CRYPTO_DEVICE_ACCESS_MODE_KEY, null);
        } else {
            byte[] serialized = ObjectUtils.serializeObject(passwAccessMode);
            try {
                editor.putString(CRYPTO_DEVICE_ACCESS_MODE_KEY, new String(serialized, "UTF-8"));
            } catch(Exception ex) {ex.printStackTrace();}
        }
        editor.commit();
    }

    public static CryptoDeviceAccessMode getCryptoDeviceAccessMode() {
        SharedPreferences settings = AppVS.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serialized = settings.getString(CRYPTO_DEVICE_ACCESS_MODE_KEY, null);
        CryptoDeviceAccessMode result = null;
        if(serialized != null) {
            result = (CryptoDeviceAccessMode) ObjectUtils.deSerializeObject(serialized.getBytes());
        }
        return result;
    }
}
