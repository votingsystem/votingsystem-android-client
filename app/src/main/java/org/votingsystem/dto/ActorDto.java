package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorDto implements java.io.Serializable {
	
	public static final String TAG = "ActorDto";

    private static final long serialVersionUID = 1L;

    public enum Type {CONTROL_CENTER, ACCESS_CONTROL, CURRENCY, TIMESTAMP_SERVER;}

    public enum State { SUSPENDED, OK, PAUSED;}

    private Long id;
    private String serverURL;
    private String name;
    private String timeStampServerURL;
    private String urlBlog;
    private String certChainPEM;
    private String timeStampCertPEM;
    private Date dateCreated;
    private Date lastUpdated;
    private State state;
    private Type serverType;
    private String webSocketURL;
    private String certificateURL;
    private String certificatePEM;
    private X509Certificate certificate;
    private Collection<X509Certificate> certChain;
    private X509Certificate timeStampCert = null;
    private Set<TrustAnchor> trustAnchors;


    public String getWebSocketURL() {
        return webSocketURL;
    }

    public String getTimeStampCertPEM() {
        return timeStampCertPEM;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

    public String getMenuUserURL() {
        return getServerURL() + "/app/user?menu=user";
    }

    public String getMenuAdminURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }

    public void setCertChainPEM(String certChainPEM) {
        this.certChainPEM = certChainPEM;
    }

    public Type getServerType() {
        return serverType;
    }

    public void setServerType(Type serverType) {
        this.serverType = serverType;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public String getServerURL() {
            return serverURL;
    }

    public String getCertChainURL () {
        return serverURL + "/rest/certificateVS/certChain";
    }


    public void setUrlBlog(String urlBlog) {
            this.urlBlog = urlBlog;
    }

    public String getURLBlog() {
            return urlBlog;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public State getState() {
        return state;    }

    public void setState(State state) { this.state = state; }

    public String getCertificateURL() {
        return certificateURL;
    }

    public void setCertificateURL(String certificateURL) {
        this.certificateURL = certificateURL;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    public X509Certificate getCertificate() throws Exception {
        if(certificate == null & certChainPEM != null) {
            certificate = PEMUtils.fromPEMToX509CertCollection(
                    certChainPEM.getBytes()).iterator().next();
        }
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public Collection<X509Certificate> getCertChain() throws Exception {
        if(certChain == null & certChainPEM != null) {
            certChain = PEMUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
        }
		return certChain;
	}

	public void setCertChain(Collection<X509Certificate> certChain) {
		this.certChain = certChain;
	}
	
    public void setTimeStampCertPEM(String timeStampCertPEM) throws Exception {
        this.timeStampCertPEM = timeStampCertPEM;
    }

    public String getTimeStampServiceURL() {
        return getTimeStampServerURL() + "/timestamp";
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = StringUtils.checkURL(timeStampServerURL);
    }

    public X509Certificate getTimeStampCert() throws Exception {
        if(timeStampCert == null && timeStampCertPEM != null) {
            timeStampCert = PEMUtils.fromPEMToX509CertCollection(timeStampCertPEM.getBytes()).iterator().next();
        }
    	return timeStampCert;
    }

    public static String getServerInfoURL (String serverURL) {
        return StringUtils.checkURL(serverURL) + "/rest/serverInfo";
    }

    public String getCSRSignedWithIDCardServiceURL () {
        return serverURL + "/rest/user/csrSignedWithIDCard";
    }


    public Set<TrustAnchor> getTrustAnchors() throws Exception {
        if(trustAnchors != null) return trustAnchors;
        if(certChainPEM == null) return null;
        certChain = PEMUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
        trustAnchors = new HashSet<>();
        for (X509Certificate cert:certChain) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }
        return trustAnchors;
    }

}
