package org.votingsystem.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.debug.DebugActionRunnerFragment;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.QRActionsFragment;
import org.votingsystem.fragment.ReceiptGridFragment;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.Constants;
import org.votingsystem.util.UIUtils;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String MENU_KEY = "MENU_KEY";

    private WeakReference<Fragment> currentFragment;

    private App app = null;
    private Menu menu;
    private Integer menuType;
    private ActivityResult activityResult;

    public static final String BROADCAST_ID = MainActivity.class.getSimpleName();
    public static final String CHILD_FRAGMENT = "CHILD_FRAGMENT";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            if (responseDto != null) {
                if (ResponseDto.SC_ERROR == responseDto.getStatusCode()) {
                    MessageDialogFragment.showDialog(responseDto.getStatusCode(),
                            getString(R.string.error_lbl), responseDto.getMessage(),
                            getSupportFragmentManager());
                } else if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                    if(responseDto.getServiceCaller() == CHILD_FRAGMENT) {
                        selectedContentFragment(Integer.valueOf(responseDto.getMessage()));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplicationContext();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Uri data = this.getIntent().getData();
        if (data != null && data.isHierarchical()) {
            if (data.getQueryParameter("vote_election") != null) {
                String param1 = data.getQueryParameter("vote_election");
                byte[] decodedMsg = Base64.decode(param1.getBytes(), Base64.NO_WRAP);
                LOGD(TAG + ".onCreate", " === vote_election: " + new String(decodedMsg));
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (currentFragment != null && currentFragment.get() != null) {
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
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!Constants.IS_DEBUG_SESSION) {
            MenuItem debugMenuItem = menu.findItem(R.id.menu_debug);
            if (debugMenuItem != null) debugMenuItem.setVisible(false);
        }
        this.menu = menu;
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
            case R.id.show_elections:
                break;
            case R.id.menu_debug:
                if (Constants.IS_DEBUG_SESSION) {
                    intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                    intent.putExtra(Constants.FRAGMENT_KEY, DebugActionRunnerFragment.class.getName());
                    startActivity(intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        selectedContentFragment(item.getItemId());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean selectedContentFragment(int menuId) {
        //this is to remove actionbar spinners
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setSubtitle(null);
        switch (menuId) {
            case R.id.qr_codes:
                replaceFragment(new QRActionsFragment(), QRActionsFragment.TAG);
                break;
            case R.id.certificates:
                Intent certsIntent = new Intent(this, CertificatesActivity.class);
                startActivity(certsIntent);
                break;
            case R.id.receipts:
                replaceFragment(new ReceiptGridFragment(), ReceiptGridFragment.TAG);
                break;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment newFragment, String tag) {
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction().remove(currentFragment.get()).commit();
        currentFragment = new WeakReference<>(newFragment);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, currentFragment.get(), tag).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app.isRootedPhone()) {
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
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(BROADCAST_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(MENU_KEY, menuType);
    }

}
