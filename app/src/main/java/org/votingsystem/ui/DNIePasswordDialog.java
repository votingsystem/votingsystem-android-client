package org.votingsystem.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import org.votingsystem.android.R;

import es.gob.jmulticard.ui.passwordcallback.CancelledOperationException;
import es.gob.jmulticard.ui.passwordcallback.DialogUIHandler;

public class DNIePasswordDialog implements DialogUIHandler {

    private static final String TAG = DNIePasswordDialog.class.getSimpleName();

    private final Activity activity;

    private final boolean cachePIN;
    private char[] password;

    public DNIePasswordDialog(Context context, char[] password, boolean cachePIN) {
        activity = ((Activity) context);
        this.cachePIN = cachePIN;
        this.password = password;
    }

    @Override
    public int showConfirmDialog(String message) {
        return doShowConfirmDialog(message);
    }

    @SuppressLint("InflateParams")
    private char[] doShowPasswordDialog(final int retries) {
        if(password != null) return password;

        final AlertDialog.Builder dialog 	= new AlertDialog.Builder(activity);
        final LayoutInflater inflater 		= activity.getLayoutInflater();
        final StringBuilder passwordBuilder = new StringBuilder();
        final DNIePasswordDialog instance 	= this;
        dialog.setMessage(getTriesMessage(retries));

        synchronized (instance) {
            activity.runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    try {
                        final View passwordView = inflater.inflate(R.layout.dnie_password_dialog, null);
                        final EditText passwordText = (EditText) passwordView.findViewById(R.id.password_edit);
                        final CheckBox passwordShow = (CheckBox) passwordView.findViewById(R.id.checkBoxShow);
                        dialog.setPositiveButton(R.string.ok_lbl, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                synchronized (instance) {
                                    passwordBuilder.delete(0, passwordBuilder.length());
                                    passwordBuilder.append(passwordText.getText().toString());
                                    instance.notifyAll();
                                }
                            }
                        });
                        dialog.setNegativeButton(R.string.cancel_lbl, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                synchronized (instance) {
                                    passwordBuilder.delete(0, passwordBuilder.length());
                                    instance.notifyAll();
                                }
                            }
                        });
                        passwordShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                                if (isChecked) {
                                    passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                    passwordShow.setText(activity.getString(R.string.psswd_dialog_show));
                                } else {
                                    passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                    passwordShow.setText(activity.getString(R.string.psswd_dialog_hide));
                                }
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.setView(passwordView);
                        dialog.create().show();
                    } catch (Throwable ex) {
                        Log.e(TAG, "Exception in DNIe password dialog: " + ex.getMessage());
                    }
                }
            });
            try {
                instance.wait();
                password = passwordBuilder.toString().toCharArray();
                return password;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public char[] showPasswordDialog(final int retries) {
        char[] returning;
        if (retries < 0 && cachePIN && password != null && password.length > 0)
            returning = password.clone();
        else returning = doShowPasswordDialog(retries);

        if (cachePIN && returning != null && returning.length > 0)
            password = returning.clone();
        else return null;
        return returning;
    }

    public char[] getPassword() {
        return password;
    }

    public int doShowConfirmDialog(String message) {
        final AlertDialog.Builder dialog 	= new AlertDialog.Builder(activity);
        final DNIePasswordDialog instance 	= this;
        final StringBuilder resultBuilder 	= new StringBuilder();
        resultBuilder.append(activity.getString(R.string.confirm_sign_msg));

        synchronized (instance)
        {
            activity.runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    try {
                        dialog.setTitle(activity.getString(R.string.idcard_signing_caption));
                        dialog.setMessage(resultBuilder);
                        dialog.setPositiveButton(R.string.ok_lbl, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                synchronized (instance) {
                                    resultBuilder.delete(0, resultBuilder.length());
                                    resultBuilder.append("0");
                                    instance.notifyAll();
                                }
                            }
                        });
                        dialog.setNegativeButton(R.string.cancel_lbl, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                synchronized (instance) {
                                    resultBuilder.delete(0, resultBuilder.length());
                                    resultBuilder.append("1");
                                    instance.notifyAll();
                                }
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.create().show();
                    } catch (CancelledOperationException ex) {
                        Log.e("MyPasswordFragment", "Excepción en diálogo de confirmación" + ex.getMessage());
                    } catch (Error err) {
                        Log.e("MyPasswordFragment", "Error en diálogo de confirmación" + err.getMessage());
                    }
                }
            });
            try
            {
                instance.wait();
                return Integer.parseInt(resultBuilder.toString());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception ex) {
                throw new CancelledOperationException();
            }
        }
    }

    @Override
    public Object getAndroidContext() {
        return activity;
    }

    /**
     * Genera el mensaje de reintentos del diálogo de contraseña.
     *
     * @param retries El número de reintentos pendientes. Si es negativo, se considera que no se conocen los intentos.
     * @return El mensaje a mostrar.
     */
    private String getTriesMessage(final int retries) {
        String text;
        if (retries < 0) {
            text = activity.getString(R.string.enter_password_msg);
        } else if (retries == 1) {
            text = activity.getString(R.string.enter_password_last_try_msg);
        } else {
            text = activity.getString(R.string.enter_password_retry_msg, retries);
        }
        return text;
    }

}