package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.CurrencyAccountsPagerFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.MessagesGridFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.QRActionsFragment;
import org.votingsystem.fragment.ReceiptGridFragment;
import org.votingsystem.fragment.WalletFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.BuildConfig;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.debug.DebugActionRunnerFragment;

import java.lang.ref.WeakReference;
import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ActivityBase extends ActivityConnected
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = ActivityBase.class.getSimpleName();

    private WeakReference<Fragment> currentFragment;

    private AppVS appVS = null;
    private NavigationView navigationView;
    private Thread mDataBootstrapThread = null;
    private Menu menu;
    private MenuItem messagesMenuItem;
    private Integer menuType;

    private String broadCastId = ActivityBase.class.getSimpleName();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(ContextVS.WEBSOCKET_MSG_KEY);
            if(socketMsg != null) {
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
                if(ResponseVS.SC_ERROR == responseVS.getStatusCode()) {
                    MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                            getString(R.string.error_lbl), responseVS.getMessage(),
                            getSupportFragmentManager());
                } else {
                    if(TypeVS.MESSAGEVS == responseVS.getTypeVS()) {
                        if(messagesMenuItem != null)
                                messagesMenuItem.setTitle(MsgUtils.getMessagesDrawerItemMessage());

                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = UIUtils.setSupportActionBar(this);
        appVS = (AppVS) getApplicationContext();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        messagesMenuItem = navigationView.getMenu().findItem(R.id.messages);
        navigationView.setNavigationItemSelectedListener(this);

        int selectedFragmentMenuId = getIntent().getIntExtra(ContextVS.FRAGMENT_KEY, -1);
        if(selectedFragmentMenuId > 0) selectedContentFragment(selectedFragmentMenuId);
    }

    private void setConnectionStatusUI() {
        if(appVS.isWithSocketConnection() && menu != null) {
            if(menu.findItem(R.id.connect) != null)
                menu.findItem(R.id.connect).setIcon(R.drawable.fa_bolt_16_ffff00);
        } else {
            if(menu != null && menu.findItem(R.id.connect) != null)
                menu.findItem(R.id.connect).setIcon(R.drawable.fa_plug_16_ffffff);
        }
    }

    private void showConnectionStatusDialog() {
        if(appVS.isWithSocketConnection()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.msg_lbl), getString(R.string.disconnect_from_service_quesion_msg), this);
            builder.setPositiveButton(getString(R.string.disconnect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            closeWebSocketConnection();
                            dialog.dismiss();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
    }

    public void closeWebSocketConnection() {
        LOGD(TAG + ".toggleWebSocketServiceConnection", "closeWebSocketConnection");
        Intent startIntent = new Intent(AppVS.getInstance(), WebSocketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.WEB_SOCKET_CLOSE);
        startService(startIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(currentFragment != null && currentFragment.get() != null)
                currentFragment.get().onActivityResult(requestCode, resultCode, data);
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
        getMenuInflater().inflate(R.menu.activity_base, menu);
        if (!BuildConfig.DEBUG) {
            menu.findItem(R.id.menu_debug).setVisible(false);
        }
        if(menuType != null && menuType == R.menu.drawer_currency) {
            menu.findItem(R.id.connect).setVisible(true);
        } else if(menuType != null && menuType == R.menu.drawer_voting) {
            menu.findItem(R.id.connect).setVisible(false);
        }
        this.menu = menu;
        setConnectionStatusUI();
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
            case R.id.connect:
                if(appVS.isWithSocketConnection()) {
                    showConnectionStatusDialog();
                } else {
                    ConnectionUtils.initConnection(ActivityBase.this);
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

    public void setMenu(int menuType) {
        this.menuType = menuType;
        navigationView.inflateMenu(menuType);
    }

    private boolean selectedContentFragment(int menuId) {
        //this is to remove actionbar spinners
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setSubtitle(null);
        Intent intent = null;
        switch (menuId) {
            case R.id.polls_menu_item:
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
            case R.id.currency_menu_item:
                intent = new Intent(this, CurrencyMainActivity.class);
                startActivity(intent);
                finish();
            case R.id.currency_accounts:
                currentFragment = new WeakReference<Fragment>(new CurrencyAccountsPagerFragment());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        currentFragment.get(), CurrencyAccountsPagerFragment.TAG).commit();
                break;
            case R.id.wallet:
                getSupportActionBar().setTitle(getString(R.string.wallet_lbl));
                currentFragment = new WeakReference<Fragment>(new WalletFragment());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        currentFragment.get(), WalletFragment.TAG).commit();
                break;
            case R.id.contacts:
                intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.fa_qrcode:
                currentFragment = new WeakReference<Fragment>(new QRActionsFragment());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        currentFragment.get(), QRActionsFragment.TAG).commit();
                break;
            case R.id.receipts:
                getSupportActionBar().setTitle(getString(R.string.receipts_lbl));
                currentFragment = new WeakReference<Fragment>(new ReceiptGridFragment());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        currentFragment.get(), ReceiptGridFragment.TAG).commit();
                break;
            case R.id.messages:
                getSupportActionBar().setTitle(getString(R.string.messages_lbl));
                currentFragment = new WeakReference<Fragment>(new MessagesGridFragment());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        currentFragment.get(), MessagesGridFragment.TAG).commit();
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
        if(AppVS.getInstance().isRootedPhone()) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.non_rooted_phones_required_msg), positiveButton, null, this);
            return;
        }
        if(!PrefUtils.isDNIeEnabled()) {
            AppVS.getInstance().setToken(UUID.randomUUID().toString().toCharArray());
            startActivity(new Intent(getBaseContext(), UserDataFormActivity.class));
            return;
        }
        if(PrefUtils.getCryptoDeviceAccessMode() == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startActivity(new Intent(ActivityBase.this,
                                    CryptoDeviceAccessModeSelectorActivity.class));
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.access_mode_passw_required_msg), positiveButton, null, this);
            return;
        }
        if(messagesMenuItem != null)
            messagesMenuItem.setTitle(MsgUtils.getMessagesDrawerItemMessage());
        setConnectionStatusUI();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onStart() {
        super.onStart();
        /*if (!PrefUtils.isDataBootstrapDone()) {
            if(mDataBootstrapThread != null) return;
            LOGD(TAG, "performDataBootstrap - starting activity bootstrap background thread");
            mDataBootstrapThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LOGD(TAG, "Starting data bootstrap process.");
                    try {// Load data from bootstrap raw resource
                        String bootstrapJson = StringUtils.parseResource(getApplicationContext(), R.raw.bootstrap_data);
                        PrefUtils.markDataBootstrapDone();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    mDataBootstrapThread = null;
                }
            });
            mDataBootstrapThread.start();
        }*/
    }

}
