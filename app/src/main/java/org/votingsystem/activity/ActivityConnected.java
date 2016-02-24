package org.votingsystem.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import org.votingsystem.util.ConnectionUtils;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ActivityConnected extends AppCompatActivity {

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ConnectionUtils.onActivityResult(requestCode, resultCode, data, this);
    }
}
