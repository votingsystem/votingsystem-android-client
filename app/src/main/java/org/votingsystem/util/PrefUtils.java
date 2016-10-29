package org.votingsystem.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.App;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.EncryptedBundleDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.crypto.CertificationRequest;
import org.votingsystem.util.crypto.Encryptor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.votingsystem.util.Constants.APPLICATION_ID_KEY;
import static org.votingsystem.util.Constants.PRIVATE_PREFS;
import static org.votingsystem.util.LogUtils.LOGD;


public class PrefUtils {

    private static final String TAG = PrefUtils.class.getSimpleName();

    public static final String CRYPTO_DEVICE_ACCESS_MODE_KEY = "CRYPTO_DEVICE_ACCESS_MODE_KEY";

    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

    private static RepresentationStateDto representation;
    private static RepresentativeDelegationDto representativeDelegationDto;
    private static BalancesDto userBalances;

    public static void init() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(Constants.BOOTSTRAP_DONE, false).commit();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    getRepresentationState();
                    getBalances();
                } catch (Exception ex) { ex.printStackTrace();}
            }
        }).start();
    }

    public static void markDataBootstrapDone() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(Constants.BOOTSTRAP_DONE, true).commit();
    }

    public static boolean isDataBootstrapDone() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.BOOTSTRAP_DONE, false);
    }

    public static String getDeviceId()  {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(Constants.APPLICATION_ID_KEY, null);
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
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(Constants.DNIE_KEY, enabled);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static boolean isDNIeEnabled() {
        SharedPreferences pref = App.getInstance().getSharedPreferences(Constants.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        return pref.getBoolean(Constants.DNIE_KEY, false);
    }

    public static void putDNIeCAN(String CAN) {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Constants.CAN_KEY, CAN.trim());
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getDNIeCAN() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getString(Constants.CAN_KEY, null);
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

    public static Date getCurrencyAccountsLastCheckDate() {
        SharedPreferences pref = App.getInstance().getSharedPreferences(Constants.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(Constants.USER_ACCOUNT_LAST_CHECKED_KEY, 0));
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public static BalancesDto getBalances() throws Exception {
        if(userBalances != null) return userBalances;
        SharedPreferences pref = App.getInstance().getSharedPreferences(
                Constants.PRIVATE_PREFS, Context.MODE_PRIVATE);
        String balancesStr = pref.getString(Constants.BALANCE_KEY, null);
        if(balancesStr == null) return new BalancesDto();
        try {
            userBalances = JSON.readValue(balancesStr, BalancesDto.class);
        } catch (Exception ex) { ex.printStackTrace();}
        return userBalances;
    }

    public static void putBalances(BalancesDto balancesDto) throws Exception {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(Constants.USER_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        try {
            editor.putString(Constants.BALANCE_KEY, JSON.writeValueAsString(balancesDto));
            editor.commit();
            userBalances = balancesDto;
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static void resetPasswordRetries() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(Constants.RETRIES_KEY, 0);
        editor.commit();
    }

    public static int incrementPasswordRetries() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        int numRetries = settings.getInt(Constants.RETRIES_KEY, 0) + 1;
        if(numRetries >= Constants.NUM_MAX_PASSW_RETRIES) {
            LOGD(TAG, "NUM. MAX RETRIES EXCEEDED (3). Resseting CryptoDeviceAccessMode");
            putCryptoDeviceAccessMode(null);
            resetPasswordRetries();
        } else {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.RETRIES_KEY, numRetries);
            editor.commit();
        }
        return numRetries;
    }

    public static void putProtectedPassword(CryptoDeviceAccessMode.Mode accessMode,
                                            char[] passw, char[] passwordToEncrypt) {
        try {
            EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                    new String(passw) , new String(passwordToEncrypt).getBytes()).toDto();
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Constants.PROTECTED_PASSWORD_KEY, JSON.writeValueAsString(ebDto));
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
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(Constants.PROTECTED_PASSWORD_KEY, null);
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
            SharedPreferences settings = App.getInstance().getSharedPreferences(
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
            editor.putString(Constants.WALLET_FILE_NAME, encryptedWallet);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}

    }

    public static Set<Currency> getWallet(char[] passw, char[] token) {
        Set<Currency> result = null;
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(Constants.WALLET_FILE_NAME, null);
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
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(Constants.CSR_KEY, null);
    }

    public static void putCsrRequest(CertificationRequest certificationRequest) {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
        try {
            editor.putString(Constants.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch(Exception ex) {ex.printStackTrace();}
        editor.commit();
    }

    public static void putAppUser(UserDto user) {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUser = ObjectUtils.serializeObject(user);
        try {
            editor.putString(Constants.USER_KEY, new String(serializedUser, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserDto getAppUser() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUser = settings.getString(Constants.USER_KEY, null);
        if(serializedUser != null) {
            UserDto user = (UserDto) ObjectUtils.deSerializeObject(serializedUser.getBytes());
            return user;
        }
        return null;
    }

    public static void putRepresentationState(RepresentationStateDto stateDto) {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
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
        SharedPreferences settings = App.getInstance().getSharedPreferences(
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
        SharedPreferences settings = App.getInstance().getSharedPreferences(
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
            editor.putString(Constants.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, serializedDelegation);
            editor.commit();
            putRepresentationState(representation);
            representativeDelegationDto = delegation;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static RepresentativeDelegationDto getAnonymousDelegation() {
        if(representativeDelegationDto != null) return representativeDelegationDto;
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedObject = settings.getString(
                Constants.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, null);
        if(serializedObject != null) {
            representativeDelegationDto = (RepresentativeDelegationDto) ObjectUtils.
                    deSerializeObject(serializedObject.getBytes());
        }
        return representativeDelegationDto;
    }

    public static void putCryptoDeviceAccessMode(CryptoDeviceAccessMode passwAccessMode) {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
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
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serialized = settings.getString(CRYPTO_DEVICE_ACCESS_MODE_KEY, null);
        CryptoDeviceAccessMode result = null;
        if(serialized != null) {
            result = (CryptoDeviceAccessMode) ObjectUtils.deSerializeObject(serialized.getBytes());
        }
        return result;
    }
}
