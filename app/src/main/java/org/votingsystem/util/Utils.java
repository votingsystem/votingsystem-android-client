package org.votingsystem.util;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;

import com.google.zxing.integration.android.IntentIntegrator;

import org.votingsystem.App;
import org.votingsystem.activity.ActivityBase;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.PatternLockActivity;
import org.votingsystem.activity.PinActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.MessagesGridFragment;
import org.votingsystem.service.PaymentService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static void launchQRScanner(Activity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.addExtra("SCAN_WIDTH", 500);
        integrator.addExtra("SCAN_HEIGHT", 500);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", activity.getString(R.string.set_focus_on_qrcode));
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES, activity);
    }

    public static void launchQRScanner(android.support.v4.app.Fragment fragment) {
        IntentIntegrator integrator = new IntentIntegrator(fragment);
        integrator.addExtra("SCAN_WIDTH", 500);
        integrator.addExtra("SCAN_HEIGHT", 500);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", fragment.getString(R.string.set_focus_on_qrcode));
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    protected void sendEmail(Context context, List<String> recipients,
                             String toUser, String content) {

        //String[] recipients = {recipient.getText().toString()};
        Intent email = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        // prompts email clients only
        email.setType("message/rfc822");

        email.putExtra(Intent.EXTRA_EMAIL, recipients.toArray());
        email.putExtra(Intent.EXTRA_SUBJECT, toUser);
        email.putExtra(Intent.EXTRA_TEXT, content);

        try {
            // the user can choose the email client
            context.startActivity(Intent.createChooser(email, "Choose an email client from..."));
        } catch (android.content.ActivityNotFoundException ex) {
            //Toast.makeText(context, "No email client installed.", Toast.LENGTH_LONG).show();
        }
    }

    public static <T> Set<T> asSet(T... args ) {
        Set<T> result = new HashSet<>();
        for(T arg : args) {
            result.add(arg);
        }
        return result;
    }

    public static Uri createTempFile(byte[] fileBytes, Context context) throws IOException {
        Uri result = null;
        File tempFile = File.createTempFile("cms", ".p7s", context.getExternalCacheDir());
        try {
            tempFile.createNewFile();
            FileOutputStream fo = new FileOutputStream(tempFile);
            fo.write(fileBytes);
            fo.close();
            result = Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ResponseDto getBroadcastResponse(OperationType operation, String serviceCaller,
                                                   ResponseDto responseDto, Context context) {
        if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
            if(responseDto.getCaption() == null) responseDto.setCaption(context.getString(R.string.ok_lbl));

        } else {
            if(responseDto.getCaption() == null) responseDto.setCaption(context.getString(R.string.error_lbl));
        }
        responseDto.setOperationType(operation).setServiceCaller(serviceCaller);
        return responseDto;
    }

    public static void printKeyStoreInfo() throws CertificateException, NoSuchAlgorithmException,
            IOException, KeyStoreException, UnrecoverableEntryException {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.KeyStore.PrivateKeyEntry keyEntry = (java.security.KeyStore.PrivateKeyEntry)
                keyStore.getEntry("USER_CERT_ALIAS", null);
        Enumeration aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            LOGD(TAG, "Subject DN: " + cert.getSubjectX500Principal().toString());
            LOGD(TAG, "Issuer DN: " + cert.getIssuerDN().getName());
        }
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null)return arguments;
        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }
        return arguments;
    }

    public static void launchCurrencyStatusCheck(String broadCastId, String revocationHash) {
        Intent startIntent = new Intent(App.getInstance(), PaymentService.class);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.CURRENCY_CHECK);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.HASH_CERTVS_KEY, revocationHash);
        App.getInstance().startService(startIntent);
    }

    public static void showAccountsUpdatedNotification(Context context){
        final NotificationManager mgr = (NotificationManager)context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(context, ActivityBase.class);
        clickIntent.putExtra(Constants.FRAGMENT_KEY, R.id.currency_accounts);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                Constants.ACCOUNTS_UPDATED_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(context.getString(R.string.currency_accounts_updated))
                .setContentText(DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime(), "HH:mm"))
                .setSound(soundUri).setSmallIcon(R.drawable.fa_money_32);
        mgr.notify(Constants.ACCOUNTS_UPDATED_NOTIFICATION_ID, builder.build());
    }

    public static void showNewMessageNotification(){
        final NotificationManager mgr = (NotificationManager) App.getInstance().getSystemService(
                Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(App.getInstance(), FragmentContainerActivity.class);
        clickIntent.putExtra(Constants.FRAGMENT_KEY, MessagesGridFragment.class.getName());
        PendingIntent pendingIntent = PendingIntent.getActivity(App.getInstance(),
                Constants.NEW_MESSAGE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getInstance())
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(App.getInstance().getString(R.string.msg_lbl))
                .setContentText(DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime(), "HH:mm"))
                .setSound(soundUri).setSmallIcon(R.drawable.fa_comment_32);
        mgr.notify(Constants.NEW_MESSAGE_NOTIFICATION_ID, builder.build());
    }

    public static void getProtectionPassword(Integer requestCode, String msg,
                     Integer activityMode, AppCompatActivity activity) {
        Intent intent = null;
        CryptoDeviceAccessMode passwordAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        switch (passwordAccessMode.getMode()) {
            case PATTER_LOCK:
                intent = new Intent(activity, PatternLockActivity.class);
                break;
            case PIN:
                intent = new Intent(activity, PinActivity.class);
                break;
        }
        intent.putExtra(Constants.MESSAGE_KEY, msg);
        intent.putExtra(Constants.MODE_KEY, activityMode);
        activity.startActivityForResult(intent, requestCode);
    }


    public static void getProtectionPasswordForSocketOperation(String msg, String msgUUID,
                   String operationCode, Context context) {
        Intent intent = null;
        CryptoDeviceAccessMode passwordAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        switch (passwordAccessMode.getMode()) {
            case PATTER_LOCK:
                intent = new Intent(context, PatternLockActivity.class);
                break;
            case PIN:
                intent = new Intent(context, PinActivity.class);
                break;
        }
        intent.putExtra(Constants.MESSAGE_KEY, msg);
        intent.putExtra(Constants.OPERATION_KEY, OperationType.WEB_SOCKET_REQUEST);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.OPERATION_CODE_KEY, operationCode);
        intent.putExtra(Constants.UUID_KEY, msgUUID);
        context.startActivity(intent);
    }

    //http://stackoverflow.com/questions/1995439/get-android-phone-model-programmatically
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model.toLowerCase();
        } else {
            return (manufacturer + " " + model);
        }
    }

    //http://www.pocketmagic.net/android-unique-device-id/
    //requires adding a permission in AndroidManifest.xml -> android.permission.READ_PHONE_STATE
    public static String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) App.getInstance().getSystemService(
                Context.TELEPHONY_SERVICE);
        // phone = telephonyManager.getLine1Number(); -> operator dependent
        //IMSI
        //phone = telephonyManager.getSubscriberId();
        //the IMEI for GSM and the MEID or ESN for CDMA phones. Null if device ID is not available,
        //only for Android devices with Phone use.
        String deviceId = telephonyManager.getDeviceId();
        if(deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = android.os.Build.SERIAL;
            if(deviceId == null || deviceId.trim().isEmpty()) {
                deviceId = UUID.randomUUID().toString();
            }
        }
        return deviceId;
    }

    private byte[] reduceImageFileSize(Uri imageUri) {
        byte[] imageBytes = null;
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    App.getInstance().getContentResolver().openFileDescriptor(imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            int compressFactor = 80;
            //Gallery images form phones like Nexus4 can be greater than 3 MB
            //InputStream inputStream = getContentResolver().openInputStream(imageUri);
            //representativeImageBytes = FileUtils.getBytesFromInputStream(inputStream);
            //Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            do {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                //0 meaning compress for small size, 100 meaning compress for max quality.
                // Some formats, like PNG which is lossless, will ignore the quality setting
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressFactor, out);
                imageBytes = out.toByteArray();
                compressFactor = compressFactor - 10;
                LOGD(TAG + ".reduceImageFileSize", "compressFactor: " + compressFactor +
                        " - imageBytes: " + imageBytes.length);
            } while(imageBytes.length > Constants.MAX_REPRESENTATIVE_IMAGE_FILE_SIZE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return imageBytes;
    }
}
