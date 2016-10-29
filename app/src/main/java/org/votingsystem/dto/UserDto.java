package org.votingsystem.dto;

import android.net.Uri;
import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle2.asn1.x500.RDN;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x500.style.BCStyle;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.jce.X509Principal;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Country;
import org.votingsystem.util.crypto.CertUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = UserDto.class.getSimpleName();

    public enum Type {USER, GROUP, SYSTEM, REPRESENTATIVE, BANK, CONTACT}

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELLED}


    private Long id;
    private State state;
    private Type type;
    private String name;
    private String reason;
    private String description;
    private Set<DeviceDto> connectedDevices;
    private DeviceDto device;
    private Set<CertificateDto> certCollection = new HashSet<>();
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String metaInf;
    private String country;
    private String IBAN;
    private String NIF;
    private String URL;
    private String cn;
    private String deviceId;
    private UserDto representative;//this is for groups
    private String representativeMessageURL;
    private String imageURL;
    private Address address;
    @JsonIgnore private X509Certificate x509Certificate;
    @JsonIgnore private byte[] imageBytes;
    private transient Uri contactURI;
    private Long numRepresentations;


    @JsonIgnore private TimeStampToken timeStampToken;
    @JsonIgnore private SignerInformation signerInformation;

    public UserDto() {}

    public UserDto(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    @JsonIgnore
    public static UserDto getUser(X500Name subject) {
        UserDto result = new UserDto();
        for(RDN rdn : subject.getRDNs()) {
            AttributeTypeAndValue attributeTypeAndValue = rdn.getFirst();
            if(BCStyle.SERIALNUMBER.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setNIF(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.SURNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setLastName(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.GIVENNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setFirstName(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.CN.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCn(attributeTypeAndValue.getValue().toString());
            } else if(BCStyle.C.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCountry(attributeTypeAndValue.getValue().toString());
            } else LOGD(TAG, "oid: " + attributeTypeAndValue.getType().getId() +
                    " - value: " + attributeTypeAndValue.getValue().toString());
        }
        return result;
    }

    @JsonIgnore
    public static UserDto getUser(X509Certificate x509Cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(x509Cert).getSubject();
        UserDto user = getUser(x500name);
        user.setX509Certificate(x509Cert);
        try {
            CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                    x509Cert, Constants.DEVICE_OID);
            if(certExtensionDto != null) {
                user.setEmail(certExtensionDto.getEmail());
                user.setPhone(certExtensionDto.getMobilePhone());
                user.setDeviceId(certExtensionDto.getDeviceId());
            }
        } catch(Exception ex) {ex.printStackTrace();}
        return user;
    }

    public static UserDto getUser(Principal principal) {
        return getUser(new X500Name(principal.getName()));
    }

    public static UserDto getUser(X509Principal principal) {
        UserDto user = new UserDto();
        user.setNIF((String) principal.getValues(new DERObjectIdentifier("2.5.4.5")).get(0));
        user.setLastName((String) principal.getValues(new DERObjectIdentifier("2.5.4.4")).get(0));
        user.setFirstName((String) principal.getValues(new DERObjectIdentifier("2.5.4.42")).get(0));
        user.setCountry((String) principal.getValues(new DERObjectIdentifier("2.5.4.6")).get(0));
        return user;
    }

    @JsonIgnore
    public Set<DeviceDto> getDevices() throws Exception {
        return connectedDevices;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public SignerInformation getSignerInformation() {
        return signerInformation;
    }

    public void setSignerInformation(SignerInformation signerInformation) {
        this.signerInformation = signerInformation;
    }

    public String getFullName() {
        return firstName + " "  + lastName;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    @JsonIgnore
    public String getSignedContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return Base64.encodeToString(signerInformation.getContentDigest(), Base64.NO_WRAP);
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public String getNIF() {
        return NIF;
    }

    public UserDto setNIF(String NIF) {
        this.NIF = NIF;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public UserDto setType(Type type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public boolean checkUserFromCSR(X509Certificate x509CertificateToCheck)
            throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(x509CertificateToCheck).getSubject();
        UserDto userToCheck = getUser(x500name);
        if(!NIF.equals(userToCheck.getNIF())) return false;
        if(!firstName.equals(userToCheck.getFirstName())) return false;
        if(!lastName.equals(userToCheck.getLastName())) return false;
        return true;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DeviceDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceDto> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<CertificateDto> getCertCollection() {
        return certCollection;
    }

    public void setCertCollection(Collection<CertificateDto> certCollection) {
        if(certCollection == null) this.certCollection = null;
        else this.certCollection = new HashSet<>(certCollection);
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public UserDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserDto representative) {
        this.representative = representative;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
        if(address != null) {
            try {
                address.setCountry(Country.valueOf(country));
            }catch (Exception ex) {
                LOGD(TAG, ex.getMessage());
            }
        }
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Uri getContactURI() {
        return contactURI;
    }

    public void setContactURI(Uri contactURI) {
        this.contactURI = contactURI;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public void setNumRepresentations(Long numRepresentations) {
        this.numRepresentations = numRepresentations;
    }

    public String getRepresentativeMessageURL() {
        return representativeMessageURL;
    }

    public void setRepresentativeMessageURL(String representativeMessageURL) {
        this.representativeMessageURL = representativeMessageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if(contactURI != null) s.writeObject(contactURI.toString());
        else s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            String contactURIStr = (String) s.readObject();
            if(contactURIStr != null) contactURI = Uri.parse(contactURIStr);
        } catch(Exception ex) { LOGD(TAG, "readObject EXCEPTION");}
    }
}
