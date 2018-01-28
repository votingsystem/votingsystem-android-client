package org.votingsystem.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.ui.DNIePasswordDialog;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.xades.SignatureAlgorithm;
import org.votingsystem.xades.SignatureBuilder;
import org.votingsystem.xades.XAdESUtils;
import org.votingsystem.xml.XMLUtils;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.ui.passwordcallback.CancelledOperationException;
import es.gob.jmulticard.ui.passwordcallback.DNIeDialogManager;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ID_CardNFCReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    public static final String TAG = ID_CardNFCReaderActivity.class.getSimpleName();

    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN          = "CertFirmaDigital";

    public static final int MODE_REQUEST_PASSWORD = 0;

    public static final int RC_PROTECTED_PASSWORD = 0;

    private char[] idCardPassword = null;

    private byte[] contentToSign;
    private String dnieCAN;
    private String timeStampServiceURL;
    private NfcAdapter myNfcAdapter;
    private Tag tagFromIntent;
    private int activityRequestMode;

    private Handler myHandler = new Handler();

    final Runnable newRead = new Runnable() {
        public void run() {
            ((TextView) findViewById(R.id.msg)).setText(R.string.process_msg_reading);
            ((ImageView) findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_peq);
        }
    };

    final Runnable askForRead = new Runnable() {
        public void run() {
            ((ImageView) findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_grey_peq);
            ((TextView) findViewById(R.id.msg)).setText(R.string.op_dgtinit);
        }
    };

    private void showMessageDialog(String caption, String message) {
        ProgressDialogFragment.hide(getSupportFragmentManager());
        MessageDialogFragment.showDialog(caption, message, getSupportFragmentManager());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.idcard_nfc_reader_activity);
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (myNfcAdapter == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ID_CardNFCReaderActivity.this.finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.nfc_required_msg), positiveButton, null, this);
        } else {
            dnieCAN = PrefUtils.getDNIeCAN();

            if (dnieCAN == null) {
                startActivity(new Intent(getBaseContext(), UserDataFormActivity.class));
                return;
            }

            myNfcAdapter.setNdefPushMessage(null, this);
            myNfcAdapter.setNdefPushMessageCallback(null, this);
            timeStampServiceURL = getIntent().getExtras().getString(Constants.TIMESTAMP_SERVER_KEY);
            contentToSign = getIntent().getExtras().getByteArray(Constants.MESSAGE_CONTENT_KEY);
            String qrMessage = getIntent().getExtras().getString(Constants.QR_CODE_KEY);
            if (qrMessage != null) {
                ((TextView) findViewById(R.id.qrCodeMsg)).setText(qrMessage.toUpperCase());
                findViewById(R.id.qrCodeMsg).setVisibility(View.VISIBLE);
            } else findViewById(R.id.qrCodeMsg).setVisibility(View.GONE);
            String message = getIntent().getExtras().getString(Constants.MESSAGE_KEY);
            if (message != null) {
                ((TextView) findViewById(R.id.topMsg)).setText(message);
            } else findViewById(R.id.topMsg).setVisibility(View.GONE);
        }
        activityRequestMode = getIntent().getExtras().getInt(Constants.MODE_KEY, -1);
        if(MODE_REQUEST_PASSWORD != activityRequestMode) {
            CryptoDeviceAccessMode cryptoDeviceAccessMode = PrefUtils.getCryptoDeviceAccessMode();
            if(cryptoDeviceAccessMode != null && cryptoDeviceAccessMode.getMode() !=
                    CryptoDeviceAccessMode.Mode.DNIE_PASSW) {
                Intent intent = null;
                switch (cryptoDeviceAccessMode.getMode()) {
                    case PATTER_LOCK:
                        intent = new Intent(this, PatternLockActivity.class);
                        intent.putExtra(Constants.MODE_KEY, PatternLockActivity.MODE_VALIDATE_INPUT);
                        break;
                    case PIN:
                        intent = new Intent(this, PinActivity.class);
                        intent.putExtra(Constants.MODE_KEY, PinActivity.MODE_VALIDATE_INPUT);
                        break;
                }
                startActivityForResult(intent, RC_PROTECTED_PASSWORD);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myNfcAdapter != null)
            enableNFCReaderMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        LOGD(TAG, "disableNFCReaderMode");
        if (myNfcAdapter != null)
            disableNFCReaderMode();
    }

    private void enableNFCReaderMode() {
        LOGD(TAG, "enableNFCReaderMode");
        Bundle options = new Bundle();
        //30 secs to check NFC reader
        //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
        myNfcAdapter.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B, options);
    }

    private void disableNFCReaderMode() {
        LOGD(TAG, "disableNFCReaderMode");
        myNfcAdapter.disableReaderMode(this);
    }

    Runnable cardOperation = new Runnable() {
        @Override
        public void run() {
            ResponseDto responseDto = null;
            DnieProvider dnieProvider = new DnieProvider();
            LOGD(TAG, "is dnieProvider loaded: " + (Security.getProvider(dnieProvider.getName()) != null));
            try {
                dnieProvider.setProviderTag(tagFromIntent);
                dnieProvider.setProviderCan(dnieCAN);
                Security.insertProviderAt(dnieProvider, 1);
                //Deactivate fastmode
                //System.setProperty("es.gob.jmulticard.fastmode", "false");
                DNIePasswordDialog passwordDialog = new DNIePasswordDialog(
                        ID_CardNFCReaderActivity.this, idCardPassword, true);
                DNIeDialogManager.setDialogUIHandler(passwordDialog);

                KeyStore ksUserDNIe = KeyStore.getInstance("MRTD");
                ksUserDNIe.load(null, null);
                //force load real certs
                //ksUserDNIe.getKey(CERT_SIGN, null);
                //X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);
                //LOGD(TAG, "userCert: " + userCert.toString());

                PrivateKey privateKey = (PrivateKey) ksUserDNIe.getKey(CERT_SIGN, null);
                X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);

                LOGD(TAG, "userCert: " + userCert.toString());
                if(activityRequestMode == MODE_REQUEST_PASSWORD) {
                    finishOk(ResponseDto.OK().setMessage(new String(passwordDialog.getPassword())));
                    return;
                }

                Certificate[] chain = ksUserDNIe.getCertificateChain(CERT_SIGN);
                List<X509Certificate> certChain = new ArrayList<>();
                for (Certificate certificate : chain) {
                    certChain.add((X509Certificate) certificate);
                 }
                String contentToSign = XMLUtils.prepareRequestToSign(ID_CardNFCReaderActivity.this.contentToSign);
                
                byte[] signatureBytes = new SignatureBuilder(contentToSign.getBytes(),
                        XAdESUtils.XML_MIME_TYPE,
                        SignatureAlgorithm.RSA_SHA_256.getName(),
                        privateKey, userCert, certChain, timeStampServiceURL).build();
                responseDto = new ResponseDto(ResponseDto.SC_OK, null, signatureBytes);

                myHandler.post(askForRead);
                ProgressDialogFragment.hide(getSupportFragmentManager());
                if (responseDto != null)
                    finishOk(responseDto);
            } catch (CancelledOperationException ex) {
                showMessageDialog(getString(R.string.error_lbl), ex.getMessage());
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                myHandler.post(askForRead);
                showMessageDialog(getString(R.string.error_lbl),
                        getString(R.string.dnie_connection_null_error_msg));
            } catch (Exception ex) {
                ex.printStackTrace();
                String exMsg = "";
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("pace. can"))
                    exMsg = " - " + ex.getMessage();
                myHandler.post(askForRead);
                showMessageDialog(getString(R.string.error_lbl), getString(R.string.dnie_connection_error_msg) + exMsg);
            } finally {
                //this seems a bad idea
                //LOGD(TAG, "removing provider: " + dnieProvider.getName());
                //Security.removeProvider(dnieProvider.getName());
            }
        }
    };

    private void finishOk(ResponseDto response) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.RESPONSE_KEY, response);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        LOGD(TAG, "onTagDiscovered");
        tagFromIntent = tag;
        NfcA exNfcA = NfcA.get(tagFromIntent);
        NfcB exNfcB = NfcB.get(tagFromIntent);
        IsoDep exIsoDep = IsoDep.get(tagFromIntent);
        if ((exNfcA != null) || (exNfcB != null) || (exIsoDep != null)) {
            myHandler.post(newRead);
            MessageDialogFragment.hide(getSupportFragmentManager());
            ProgressDialogFragment.showDialog(getString(R.string.dnie_sign_progress_caption),
                    getString(R.string.dnie_sign_connecting_msg),
                    getSupportFragmentManager());
            new Thread(cardOperation).start();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if (Activity.RESULT_OK == resultCode) {
            switch (requestCode) {
                case RC_PROTECTED_PASSWORD:
                    ResponseDto response = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    idCardPassword = PrefUtils.getProtectedPassword(response.getMessage().toCharArray());
                    break;
            }

        } else {
            LOGD(TAG, "onActivityResult - finish");
            finish();
        }
    }

}