package org.votingsystem.ui;

import android.content.DialogInterface;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogButton {

    private String label;
    private DialogInterface.OnClickListener clickListener;

    public DialogButton(String label, DialogInterface.OnClickListener clickListener) {
        this.label = label;
        this.clickListener = clickListener;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DialogInterface.OnClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(DialogInterface.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

}