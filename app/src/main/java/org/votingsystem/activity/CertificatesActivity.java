package org.votingsystem.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.votingsystem.android.R;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

public class CertificatesActivity extends AppCompatActivity {

    public static final String TAG = CertificatesActivity.class.getSimpleName();

    private View rootView;
    private GridView gridView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);

        setContentView(R.layout.simple_grid);
        gridView = (GridView) findViewById(R.id.gridview);

        List<KeyEntry> certificates = null;
        try {
            certificates = loadCertificates();
        } catch (Exception e) {
            e.printStackTrace();
        }
        CertsGridAdapter adapter = new CertsGridAdapter(this, certificates);

        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });

        UIUtils.setSupportActionBar(this, getString(R.string.certificates_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG + ".onListItemClick", "Clicked item - position:" + position + " -id: " + id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.certificates_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.cert_request:
                Intent settingsIntent = new Intent(this, CertificateRequestFormActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public class CertsGridAdapter extends BaseAdapter {

        private Context context;
        private final List<KeyEntry> certEntries;

        public CertsGridAdapter(Context context, List<KeyEntry> certEntries) {
            this.context = context;
            this.certEntries = certEntries;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View gridView = null;
            if (convertView == null) {

            } else {
                gridView = (View) convertView;
            }
            return gridView;
        }

        @Override
        public int getCount() {
            return certEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

    private List<KeyEntry> loadCertificates() throws CertificateException, NoSuchAlgorithmException,
            IOException, KeyStoreException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Enumeration<String> certAliasEnum = keyStore.aliases();
        List<KeyEntry> keyEntryList = new ArrayList<>();
        while(certAliasEnum.hasMoreElements()) {
            String alias = certAliasEnum.nextElement();
            if(alias.contains(Constants.VOTINGSYSTEM_CERTIFICATE)) {
                KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)keyStore
                        .getEntry(alias, null);
                X509Certificate x509Certificate = (X509Certificate) keyEntry.getCertificateChain()[0];
                keyEntryList.add(new KeyEntry(x509Certificate, alias));
            }
        }
        return keyEntryList;
    }

    private static class KeyEntry {

        private X509Certificate x509Certificate;
        private String alias;

        public KeyEntry(X509Certificate x509Certificate, String alias) {
            this.x509Certificate = x509Certificate;
            this.alias = alias;
        }

        public X509Certificate getX509Certificate() {
            return x509Certificate;
        }

        public String getAlias() {
            return alias;
        }

    }


}