package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.QRUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class  QRFragment extends Fragment {

    public static final String TAG = QRFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(ContextVS.WEBSOCKET_MSG_KEY);
            if(socketMsg != null) {
                switch (socketMsg.getOperation()) {
                    case TRANSACTION_RESPONSE:
                        if(ResponseVS.SC_OK == socketMsg.getStatusCode()) {
                            DialogButton dialogButton = new DialogButton(getString(R.string.accept_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            getActivity().onBackPressed();
                                        }
                                    });
                            UIUtils.showMessageDialog(getString(R.string.payment_ok_caption),
                                    intent.getStringExtra(ContextVS.MESSAGE_KEY), dialogButton, null,
                                    getActivity());
                        }
                        break;
                }
            }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_fragment, container, false);
        Intent intent = getActivity().getIntent();
        TransactionDto dto = (TransactionDto) intent.getSerializableExtra(
                ContextVS.TRANSACTION_KEY);
        QRMessageDto qrDto = new QRMessageDto(AppVS.getInstance().getConnectedDevice(),
                TypeVS.TRANSACTION_INFO);
        qrDto.setData(dto);
        Bitmap bitmap = null;
        try {
            bitmap = QRUtils.encodeAsBitmap(JSON.writeValueAsString(qrDto), getActivity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ImageView view = (ImageView) rootView.findViewById(R.id.image_view);
        view.setImageBitmap(bitmap);
        setHasOptionsMenu(true);
        AppVS.getInstance().putQRMessage(qrDto);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_code_lbl));
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                getString(R.string.qr_transaction_request_msg, dto.getAmount() + " " + dto.getCurrencyCode(),
                MsgUtils.getTagVSMessage(dto.getTagName())));
        if(!AppVS.getInstance().isWithSocketConnection()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.qr_code_lbl), getString(R.string.qr_connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().finish();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
        return rootView;
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

}