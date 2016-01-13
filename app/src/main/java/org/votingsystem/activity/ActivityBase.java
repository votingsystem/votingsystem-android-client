package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.MessageContentProvider;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.fragment.CurrencyAccountsPagerFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.MessagesGridFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.QRActionsFragment;
import org.votingsystem.fragment.ReceiptGridFragment;
import org.votingsystem.fragment.WalletFragment;
import org.votingsystem.util.BuildConfig;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.debug.DebugActionRunnerFragment;

import java.io.IOException;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ActivityBase extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = ActivityBase.class.getSimpleName();


    private AppVS appVS = null;
    private NavigationView navigationView;
    private Thread mDataBootstrapThread = null;

    private TextView connectionStatusText;
    private TextView headerTextView;
    private ImageView connectionStatusView;

    private String broadCastId = ActivityBase.class.getSimpleName();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            SocketMessageDto socketMsg = null;
            String socketMsgStr = intent.getStringExtra(ContextVS.WEBSOCKET_MSG_KEY);
            if(socketMsgStr != null) {
                try {
                    socketMsg = JSON.readValue(socketMsgStr, SocketMessageDto.class);
                } catch (Exception ex) { ex.printStackTrace();}
            }
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(responseVS.getTypeVS()) {
                    case WEB_SOCKET_INIT:
                        ProgressDialogFragment.showDialog(getString(R.string.connecting_caption),
                                getString(R.string.connecting_to_service_msg),
                                getSupportFragmentManager());
                        new Thread(new Runnable() {@Override public void run() {
                            Utils.toggleWebSocketServiceConnection(); }}).start();
                        break;
                }
            } else if(socketMsg != null) {
                LOGD(TAG + ".broadcastReceiver", "WebSocketMessage typeVS: " + socketMsg.getOperation());
                ProgressDialogFragment.hide(getSupportFragmentManager());
                setConnectionStatusUI();
                if(ResponseVS.SC_ERROR == socketMsg.getStatusCode() ||
                        ResponseVS.SC_WS_CONNECTION_INIT_ERROR == socketMsg.getStatusCode()) {
                    MessageDialogFragment.showDialog(socketMsg.getStatusCode(),
                            socketMsg.getCaption(), socketMsg.getMessage(),
                            getSupportFragmentManager());
                }
            } else if(responseVS != null) {
                if(ResponseVS.SC_ERROR == responseVS.getStatusCode())
                    MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                            getString(R.string.error_lbl), responseVS.getMessage(),
                            getSupportFragmentManager());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        appVS = (AppVS) getApplicationContext();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = LayoutInflater.from(this).inflate(R.layout.activity_base_header, null);
        connectionStatusText = (TextView)header.findViewById(R.id.connection_status_text);
        headerTextView = (TextView)header.findViewById(R.id.headerTextView);
        connectionStatusView = (ImageView)header.findViewById(R.id.connection_status_img);
        LinearLayout userBox = (LinearLayout)header.findViewById(R.id.user_box);
        userBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGD(TAG, "userBox clicked");
                if(!appVS.isWithSocketConnection()) {
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId, getString(
                            R.string.init_authenticated_session_pin_msg), false, TypeVS.WEB_SOCKET_INIT);
                } else {showConnectionStatusDialog();}
            }
        });
        navigationView.addHeaderView(header);

        int selectedFragmentMenuId = getIntent().getIntExtra(ContextVS.FRAGMENT_KEY, -1);
        if(selectedFragmentMenuId > 0) selectedContentFragment(selectedFragmentMenuId);
    }

    private void setConnectionStatusUI() {
        if(appVS.isWithSocketConnection()) {
            connectionStatusText.setText(getString(R.string.connected_lbl));
            connectionStatusView.setVisibility(View.VISIBLE);
            headerTextView.setText(PrefUtils.getSessionUserVS().getEmail());
            headerTextView.setVisibility(View.VISIBLE);
        } else {
            connectionStatusText.setText(getString(R.string.disconnected_lbl));
            connectionStatusView.setVisibility(View.GONE);
            headerTextView.setVisibility(View.GONE);
        }
    }

    private void showConnectionStatusDialog() {
        if(appVS.isWithSocketConnection()) {
            UserVSDto sessionUserVS = PrefUtils.getSessionUserVS();
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.connected_with_lbl), sessionUserVS.getEmail(), this);
            builder.setPositiveButton(getString(R.string.disconnect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Utils.toggleWebSocketServiceConnection();
                            dialog.dismiss();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
    }

    private void performDataBootstrap() {
        final Context appContext = getApplicationContext();
        LOGD(TAG, "performDataBootstrap - starting activity bootstrap background thread");
        mDataBootstrapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGD(TAG, "Starting data bootstrap process.");
                try {// Load data from bootstrap raw resource
                    String bootstrapJson = StringUtils.parseResource(appContext, R.raw.bootstrap_data);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                mDataBootstrapThread = null;
            }
        });
        mDataBootstrapThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent = null;
        switch (id) {
            case R.id.menu_debug:
                if (BuildConfig.DEBUG) {
                    intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                    intent.putExtra(ContextVS.FRAGMENT_KEY, DebugActionRunnerFragment.class.getName());
                    startActivity(intent);
                }
                return true;
            case R.id.close_app:
                appVS.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return selectedContentFragment(item.getItemId());
    }

    private boolean selectedContentFragment(int menuId) {
        //this is to remove actionbar spinners
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setSubtitle(null);
        Intent intent = null;
        switch (menuId) {
            case R.id.polls:
                intent = new Intent(this, EventVSMainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.representatives:
                intent = new Intent(this, RepresentativesMainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.currency_accounts:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new CurrencyAccountsPagerFragment(), CurrencyAccountsPagerFragment.TAG).commit();
                break;
            case R.id.wallet:
                getSupportActionBar().setTitle(getString(R.string.wallet_lbl));
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new WalletFragment(), WalletFragment.TAG).commit();
                break;
            case R.id.contacts:
                intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.fa_qrcode:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new QRActionsFragment(), QRActionsFragment.TAG).commit();
                break;
            case R.id.receipts:
                getSupportActionBar().setTitle(getString(R.string.receipts_lbl));
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new ReceiptGridFragment(), ReceiptGridFragment.TAG).commit();
                break;
            case R.id.messages:
                getSupportActionBar().setTitle(getString(R.string.messages_lbl));
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new MessagesGridFragment(), MessagesGridFragment.TAG).commit();
                break;
            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override protected void onResume() {
        super.onResume();
        navigationView.getMenu().findItem(R.id.messages).setTitle(MsgUtils.getMessagesDrawerItemMessage(this));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
        setConnectionStatusUI();
    }

    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onStart() {
        super.onStart();
        if (!PrefUtils.isDataBootstrapDone() && mDataBootstrapThread == null) {
            performDataBootstrap();
        }
    }

}
