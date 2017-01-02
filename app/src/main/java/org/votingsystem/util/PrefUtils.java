package org.votingsystem.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.votingsystem.App;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.EncryptedBundleDto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.votingsystem.util.Constants.APPLICATION_ID_KEY;
import static org.votingsystem.util.Constants.PRIVATE_PREFS;
import static org.votingsystem.util.LogUtils.LOGD;

public class PrefUtils {

    private static final String TAG = PrefUtils.class.getSimpleName();
    private static final String LIST_SEPARATOR = "####";

    public static final String CRYPTO_DEVICE_ACCESS_MODE_KEY = "CRYPTO_DEVICE_ACCESS_MODE_KEY";

    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

    public static String getDeviceId() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(Constants.APPLICATION_ID_KEY, null);
        if (applicationId == null) {
            applicationId = UUID.randomUUID().toString();
            saveStringToPrefs(APPLICATION_ID_KEY, applicationId);
            LOGD(TAG, ".getDeviceId - new applicationId: " + applicationId);
        }
        return applicationId;
    }

    public static void putDNIeEnabled(boolean enabled) {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(Constants.DNIE_KEY, enabled);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isDNIeEnabled() {
        SharedPreferences pref = App.getInstance().getSharedPreferences(Constants.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        return pref.getBoolean(Constants.DNIE_KEY, false);
    }

    public static void putDNIeCAN(String CAN) {
        saveStringToPrefs(Constants.CAN_KEY, CAN);
    }

    public static String getDNIeCAN() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getString(Constants.CAN_KEY, null);
    }

    public static List<String> getVotingServiceProviders() {
        List<String> result = new ArrayList<>();
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String votingServiceProvidersStr = sp.getString(Constants.VOTING_SERVICE_ENTITY_ID, null);
        if(votingServiceProvidersStr != null) {
            String[] votingServiceProviders = votingServiceProvidersStr.split(LIST_SEPARATOR);
            for(String votingServiceProvider : votingServiceProviders)
                result.add(votingServiceProvider);
        }
        return result;
    }

    public static List<String> addVotingServiceProvider(String entityId) {
        int maxNumberOfServiceProviders = 5;
        List<String> votingServiceProviders = getVotingServiceProviders();
        boolean alreadyStored = false;
        for(String votingServiceProvider : votingServiceProviders) {
            if(votingServiceProvider.equals(entityId.trim()))
                alreadyStored = true;
        }
        if(alreadyStored) {
            votingServiceProviders.remove(entityId.trim());
        }
        votingServiceProviders.add(0, entityId.trim());
        if(votingServiceProviders.size() > maxNumberOfServiceProviders) {
            votingServiceProviders = votingServiceProviders.subList(0, maxNumberOfServiceProviders - 1);
        }
        String votingServiceProvidersStr = null;
        for(String votingServiceProvider : votingServiceProviders) {
            if(votingServiceProvidersStr == null)
                votingServiceProvidersStr = votingServiceProvider;
            else votingServiceProvidersStr = votingServiceProvidersStr + LIST_SEPARATOR + votingServiceProvider;
        }
        saveStringToPrefs(Constants.VOTING_SERVICE_ENTITY_ID, votingServiceProvidersStr);
        return votingServiceProviders;
    }

    public static Calendar getLastPendingOperationCheckedTime() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        Calendar lastCheckedTime = Calendar.getInstance();
        lastCheckedTime.setTimeInMillis(sp.getLong(Constants.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0L));
        return lastCheckedTime;
    }

    public static void registerPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void resetPasswordRetries() {
        saveIntToPrefs(Constants.RETRIES_KEY, 0);
    }

    public static int incrementPasswordRetries() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        int numRetries = settings.getInt(Constants.RETRIES_KEY, 0) + 1;
        if (numRetries >= Constants.NUM_MAX_PASSW_RETRIES) {
            LOGD(TAG, "NUM. MAX RETRIES EXCEEDED (3). Resseting CryptoDeviceAccessMode");
            putCryptoDeviceAccessMode(null);
            saveStringToPrefs(Constants.PROTECTED_PASSWORD_KEY, null);
            resetPasswordRetries();
        } else {
            saveIntToPrefs(Constants.RETRIES_KEY, numRetries);
        }
        return numRetries;
    }

    public static void putProtectedPassword(CryptoDeviceAccessMode.Mode accessMode,
                                            char[] passw, char[] passwordToEncrypt) {
        try {
            EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                    new String(passw), new String(passwordToEncrypt).getBytes()).toDto();
            saveStringToPrefs(Constants.PROTECTED_PASSWORD_KEY, new String(ObjectUtils.serializeObject(ebDto)));
            putCryptoDeviceAccessMode(new CryptoDeviceAccessMode(accessMode, passw));
        } catch (Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
    }

    private static void saveStringToPrefs(String key, String value) {
        try {
            if(value != null)
                value = value.trim();
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(key.trim(), value);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void saveIntToPrefs(String key, Integer value) {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(key.trim(), value);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static char[] getProtectedPassword(char[] passw) {
        char[] password = null;
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(Constants.PROTECTED_PASSWORD_KEY, null);
            if (dtoStr != null) {
                EncryptedBundleDto ebDto = (EncryptedBundleDto) ObjectUtils.deSerializeObject(dtoStr.getBytes());
                byte[] resultBytes = Encryptor.pbeAES_Decrypt(new String(passw),
                        ebDto.getEncryptedBundle());
                password = new String(resultBytes, "UTF-8").toCharArray();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
        return password;
    }

    public static String getCsrRequest() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(Constants.CSR_KEY, null);
    }

    public static void putCsrRequest(CertificationRequest certificationRequest) {
        try {
            byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
            saveStringToPrefs(Constants.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void putCryptoDeviceAccessMode(CryptoDeviceAccessMode passwAccessMode) {
        String passwAccessModeStr = null;
        if(passwAccessMode != null) {
            byte[] serialized = ObjectUtils.serializeObject(passwAccessMode);
            try {
                passwAccessModeStr = new String(serialized, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        saveStringToPrefs(CRYPTO_DEVICE_ACCESS_MODE_KEY, passwAccessModeStr);
    }

    public static CryptoDeviceAccessMode getCryptoDeviceAccessMode() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serialized = settings.getString(CRYPTO_DEVICE_ACCESS_MODE_KEY, null);
        CryptoDeviceAccessMode result = null;
        if (serialized != null) {
            result = (CryptoDeviceAccessMode) ObjectUtils.deSerializeObject(serialized.getBytes());
        }
        return result;
    }

}