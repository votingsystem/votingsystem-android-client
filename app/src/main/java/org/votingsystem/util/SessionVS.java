package org.votingsystem.util;



import org.votingsystem.dto.UserVSDto;

import javax.websocket.Session;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVS {

    private Session session;
    private UserVSDto userVS;

    public SessionVS(Session session, UserVSDto userVS) {
        this.setSession(session);
        this.setUserVS(userVS);
    }

    public SessionVS(Session session) {
        this.setSession(session);
    }

    public SessionVS(UserVSDto userVS) {
        this.setUserVS(userVS);
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }
}
