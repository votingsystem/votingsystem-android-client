package org.votingsystem.util.debug.actions;

import android.content.Context;

import org.votingsystem.AppVS;
import org.votingsystem.util.debug.DebugAction;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ContextVS;

public class PrefsAction implements DebugAction {

    private static final String TAG = PrefsAction.class.getSimpleName();

    @Override public void run(final Context context, final Callback callback) {
        PrefUtils.putCsrRequest(1L, null,context);
        PrefUtils.putPin(1234, context);
        PrefUtils.putAppCertState(((AppVS)context.getApplicationContext()).
                getAccessControl().getServerURL(), ContextVS.State.WITHOUT_CSR, null, context);
    }

    @Override public String getLabel() {
        return "Change PrefsUtil";
    }

}
