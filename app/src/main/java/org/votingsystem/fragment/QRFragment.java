package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.QRUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRFragment extends Fragment {

    public static final String TAG = QRFragment.class.getSimpleName();


    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_fragment, container, false);
        Intent intent = getActivity().getIntent();
        TransactionVSDto dto = (TransactionVSDto) intent.getSerializableExtra(
                ContextVS.TRANSACTION_KEY);
        QRMessageDto qrDto = new QRMessageDto(AppVS.getInstance().getConnectedDevice(),
                TypeVS.TRANSACTIONVS_INFO);
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
                getString(R.string.qr_transactionvs_request_msg, dto.getAmount() + " " + dto.getCurrencyCode(),
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

}