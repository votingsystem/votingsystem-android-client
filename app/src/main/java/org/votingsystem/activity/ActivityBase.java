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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.fragment.CurrencyAccountsPagerFragment;
import org.votingsystem.fragment.EventVSGridFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.MessagesGridFragment;
import org.votingsystem.fragment.QRActionsFragment;
import org.votingsystem.fragment.ReceiptGridFragment;
import org.votingsystem.fragment.RepresentationStateFragment;
import org.votingsystem.fragment.WalletFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ActivityResult;
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
    public static final String MENU_KEY = "MENU_KEY";

    private WeakReference<Fragment> currentFragment;

    private AppVS appVS = null;
    private NavigationView navigationView;
    private Menu menu;
    private MenuItem messagesMenuItem;
    private Integer menuType;
    private ActivityResult activityResult;

    private String broadCastId = ActivityBase.class.getSimpleName();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(responseVS != null) {
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
        if(savedInstanceState == null) selectedContentFragment(R.id.currency_accounts);
        else {
            setMenu(savedInstanceState.getInt(MENU_KEY));
        }
    }

    public void changeConnectionStatus() {
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
        if(currentFragment != null && currentFragment.get() != null) {
            currentFragment.get().onActivityResult(requestCode, resultCode, data);
        } else this.activityResult = new ActivityResult(requestCode, resultCode, data);
    }

    public ActivityResult getActivityResult() {
        ActivityResult result = this.activityResult;
        this.activityResult = null;
        return result;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(!AppVS.getInstance().isHistoryEmpty()) {
                selectedContentFragment(AppVS.getInstance().getHistoryItem());
            } else super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!BuildConfig.DEBUG) {
            MenuItem debugMenuItem = menu.findItem(R.id.menu_debug);
            if(debugMenuItem != null) debugMenuItem.setVisible(false);
        }
        if(menuType != null &&
                (menuType == R.menu.drawer_currency || menuType == R.menu.drawer_voting)) {
            MenuItem connectMenuItem = menu.findItem(R.id.connect);
            if(connectMenuItem != null) connectMenuItem.setVisible(false);
        }
        this.menu = menu;
        changeConnectionStatus();
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
        AppVS.getInstance().addHistoryItem(item.getItemId());
        return selectedContentFragment(item.getItemId());
    }

    public void setMenu(int menuType) {
        this.menuType = menuType;
        navigationView.getMenu().clear();
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
                replaceFragment(new EventVSGridFragment(), EventVSGridFragment.TAG);
                setMenu(R.menu.drawer_voting);
                break;
            case R.id.representatives:
                replaceFragment(new RepresentationStateFragment(), RepresentationStateFragment.TAG);
                break;
            case R.id.currency_accounts:
            case R.id.currency_menu_item:
                replaceFragment(new CurrencyAccountsPagerFragment(), CurrencyAccountsPagerFragment.TAG);
                setMenu(R.menu.drawer_currency);
                break;
            case R.id.wallet:
                replaceFragment(new WalletFragment(), WalletFragment.TAG);
                break;
            case R.id.contacts:
                intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                break;
            case R.id.fa_qrcode:
                replaceFragment(new QRActionsFragment(), QRActionsFragment.TAG);
                break;
            case R.id.receipts:
                replaceFragment(new ReceiptGridFragment(), ReceiptGridFragment.TAG);
                break;
            case R.id.messages:
                replaceFragment(new MessagesGridFragment(), MessagesGridFragment.TAG);
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

    private void replaceFragment(Fragment newFragment, String tag) {
        if(currentFragment != null)
            getSupportFragmentManager().beginTransaction().remove(currentFragment.get()).commit();
        currentFragment = new WeakReference<>(newFragment);
        FragmentTransaction transaction =  getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, currentFragment.get(), tag).commit();
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
        changeConnectionStatus();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public boolean isConnectionRequired() { return false;}

    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(MENU_KEY, menuType);
    }

}
