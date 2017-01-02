package org.votingsystem.xml;

import org.kxml2.kdom.Element;
import org.votingsystem.dto.AddressDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.identity.IdentityRequestDto;
import org.votingsystem.dto.metadata.ContactPersonDto;
import org.votingsystem.dto.metadata.CountryDto;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.dto.metadata.LocationDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.OrganizationDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.OperationType;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class XmlReader {

    public static final String TAG = XmlReader.class.getSimpleName();

    public static ElectionDto readElection(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        return getElection(mainElement);
    }

    public static QRResponseDto readQRResponse(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        QRResponseDto qrResponse = new QRResponseDto();
        mainElement.getAttributeValue(null, "QRResponse");
        qrResponse.setOperationType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        qrResponse.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return qrResponse;
    }

    public static ResponseDto readResponse(byte[] xmlBytes) throws IOException, XmlPullParserException {
        ResponseDto responseDto = new ResponseDto();
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        if(mainElement.getAttributeValue(null, "StatusCode") != null) {
            responseDto.setStatusCode(Integer.valueOf(mainElement.getAttributeValue(null, "StatusCode")));
        }
        responseDto.setMessage(XMLUtils.getTextChild(mainElement, "Message"));
        responseDto.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return responseDto;
    }

    public static MetadataDto readMetadata(byte[] xmlBytes) throws IOException, XmlPullParserException {
        MetadataDto metadata = new MetadataDto();
        SystemEntityDto systemEntityDto = new SystemEntityDto();
        metadata.setEntity(systemEntityDto);
        Element metadataElement = XMLUtils.parse(xmlBytes).getRootElement();
        metadata.setLanguage(metadataElement.getAttributeValue(null, "Language"));
        metadata.setTimeZone(metadataElement.getAttributeValue(null, "TimeZone"));
        metadata.setValidUntil(metadataElement.getAttributeValue(null, "ValidUntil"));

        Element entityElement = metadataElement.getElement(null, "Entity");
        systemEntityDto.setId(entityElement.getAttributeValue(null, "Id"));
        if (entityElement.getAttributeValue(null, "Type") != null)
            systemEntityDto.setEntityType(SystemEntityType.getByName(entityElement.getAttributeValue(null, "Type")));

        Element organizationElement = entityElement.getElement(null, "Organization");
        if (organizationElement != null) {
            OrganizationDto organization = new OrganizationDto();
            organization.setOrganizationName(XMLUtils.getTextChild(organizationElement, "OrganizationName"));
            organization.setOrganizationUnit(XMLUtils.getTextChild(organizationElement, "OrganizationUnit"));
            organization.setOrganizationURL(XMLUtils.getTextChild(organizationElement, "OrganizationURL"));
            systemEntityDto.setOrganization(organization);
        }


        Element locationElement = entityElement.getElement(null, "Location");
        if (locationElement != null) {
            LocationDto location = new LocationDto();
            location.setCity(XMLUtils.getTextChild(locationElement, "City"));

            Element countryElement = locationElement.getElement(null, "Country");
            if (countryElement != null) {
                CountryDto country = new CountryDto();
                country.setCode(countryElement.getAttributeValue(null, "Code"));
                country.setDisplayName(countryElement.getAttributeValue(null, "DisplayName"));
                country.setLanguage(countryElement.getAttributeValue(null, "Language"));
                location.setCountry(country);
            }

            Element addressElement = locationElement.getElement(null, "Address");
            if (addressElement != null) {
                AddressDto address = new AddressDto();
                address.setPostalCode(addressElement.getAttributeValue(null, "PostalCode"));
                if (addressElement.getChild(0) != null) {
                    String addressStr = new String(org.bouncycastle2.util.encoders.Base64.decode(
                            ((String) addressElement.getChild(0)).getBytes()));
                    address.setAddress(addressStr);
                }
                location.setAddress(address);
            }
            systemEntityDto.setLocation(location);
        }

        Element contactPersonElement = entityElement.getElement(null, "ContactPerson");
        if (contactPersonElement != null) {
            ContactPersonDto contactPerson = new ContactPersonDto();
            if (contactPersonElement.getAttributeValue(null, "ContactType") != null)
                contactPerson.setContactType(ContactPersonDto.Type.valueOf(
                        contactPersonElement.getAttributeValue(null, "ContactType").toUpperCase()));
            contactPerson.setCompany(XMLUtils.getTextChild(contactPersonElement, "Company"));
            contactPerson.setGivenName(XMLUtils.getTextChild(contactPersonElement, "GivenName"));
            contactPerson.setSurName(XMLUtils.getTextChild(contactPersonElement, "SurName"));
            contactPerson.setEmailAddress(XMLUtils.getTextChild(contactPersonElement, "Phone"));
            contactPerson.setTelephoneNumber(XMLUtils.getTextChild(contactPersonElement, "Email"));
            systemEntityDto.setContactPerson(contactPerson);
        }

        Element keysElement = metadataElement.getElement(null, "Keys");
        if (keysElement != null) {
            Set<KeyDto> keyDescriptorSet = new HashSet<>();
            for (int i = 0; i < keysElement.getChildCount(); i++) {
                Element keyElement = keysElement.getElement(i);
                KeyDto keyDto = new KeyDto();
                if (keyElement.getAttributeValue(null, "Type") != null)
                    keyDto.setType(KeyDto.Type.valueOf(keyElement.getAttributeValue(null, "Type").toUpperCase()));
                if (keyElement.getAttributeValue(null, "Use") != null)
                    keyDto.setUse(KeyDto.Use.valueOf(keyElement.getAttributeValue(null, "Use").toUpperCase()));
                keyDto.setX509CertificateBase64((String) keyElement.getChild(0));
                keyDescriptorSet.add(keyDto);
            }
            metadata.setKeyDescriptorSet(keyDescriptorSet);
        }
        Element trustedEntitiesElement = metadataElement.getElement(null, "TrustedEntities");
        if (trustedEntitiesElement != null) {
            TrustedEntitiesDto trustedEntities = new TrustedEntitiesDto();
            Set<TrustedEntitiesDto.EntityDto> entities = new HashSet<>();
            trustedEntities.setEntities(entities);
            for (int i = 0; i < trustedEntitiesElement.getChildCount(); i++) {
                Element trustedEntityElement = trustedEntitiesElement.getElement(i);
                TrustedEntitiesDto.EntityDto entity = new TrustedEntitiesDto.EntityDto();
                entity.setCountryCode(trustedEntityElement.getAttributeValue(null, "CountryCode"));
                entity.setId(trustedEntityElement.getAttributeValue(null, "Id"));
                if (trustedEntityElement.getAttributeValue(null, "Type") != null)
                    entity.setType(SystemEntityType.getByName(trustedEntityElement.getAttributeValue(null, "Type")));
                entities.add(entity);
            }
            metadata.setTrustedEntities(trustedEntities);
        }
        return metadata;
    }

    public static IdentityRequestDto readIdentityRequest(byte[] xmlBytes) throws IOException,
            XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        IdentityRequestDto request = new IdentityRequestDto();
        if(mainElement.getAttributeValue(null, "Date") != null) {
            request.setDate(DateUtils.getXmlDate(mainElement.getAttributeValue(null, "Date")));
        }
        request.setType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        Element identityServiceElement = mainElement.getElement(null, "IndentityServiceEntity");
        if(identityServiceElement != null) {
            SystemEntityDto identityService = new SystemEntityDto(identityServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(identityServiceElement.getAttributeValue(null, "Type")));
            request.setIndentityServiceEntity(identityService.getId());
        }
        Element callbackServiceElement = mainElement.getElement(null, "CallbackServiceEntity");
        if(callbackServiceElement != null) {
            SystemEntityDto callbackService = new SystemEntityDto(callbackServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(callbackServiceElement.getAttributeValue(null, "Type")));
            request.setCallbackServiceEntityId(callbackService.getId());
        }
        request.setRevocationHashBase64(XMLUtils.getTextChild(mainElement, "RevocationHashBase64"));
        request.setUUID(XMLUtils.getTextChild(mainElement, "UUID"));
        return request;
    }

    public static VoteDto readVote(byte[] xmlBytes) throws IOException, XmlPullParserException {
        VoteDto vote = new VoteDto();
        Element voteElement = XMLUtils.parse(xmlBytes).getRootElement();
        vote.setOperation(OperationType.valueOf(XMLUtils.getTextChild(voteElement, "Operation")));
        vote.setRevocationHashBase64(XMLUtils.getTextChild(voteElement, "RevocationHashBase64"));
        vote.setIndentityServiceEntity(XMLUtils.getTextChild(voteElement, "IndentityServiceEntity"));
        vote.setVotingServiceEntity(XMLUtils.getTextChild(voteElement, "VotingServiceEntity"));
        Element optionElement = XMLUtils.getElement(voteElement, "OptionSelected");
        if(optionElement != null) {
            ElectionOptionDto electionOptionDto = new ElectionOptionDto()
                    .setContent(XMLUtils.getTextChild(optionElement, "Content"));
            String numVotesStr = XMLUtils.getTextChild(optionElement, "NumVotes");
            if(numVotesStr != null) {
                electionOptionDto.setNumVotes(Long.valueOf(numVotesStr));
            }
            vote.setOptionSelected(electionOptionDto);
        }
        vote.setElectionUUID(XMLUtils.getTextChild(voteElement, "ElectionUUID"));
        return vote;
    }

    public static ElectionDto getElection(Element electionElement) throws IOException, XmlPullParserException {
        ElectionDto election = new ElectionDto();
        String electionId = XMLUtils.getTextChild(electionElement, "Id");
        if(electionId != null)
            election.setId(Long.valueOf(XMLUtils.getTextChild(electionElement, "Id")));

        Element optionsElement = electionElement.getElement(null, "Options");
        if(optionsElement != null) {
            Set<ElectionOptionDto> electionOptions = new HashSet<>();
            for(int i = 0; i < optionsElement.getChildCount(); i++) {
                Element optionElement = optionsElement.getElement(i);
                electionOptions.add(new ElectionOptionDto(XMLUtils.getTextChild(optionElement, "Content"), null));
            }
            election.setElectionOptions(electionOptions);
        }
        String electionState = XMLUtils.getTextChild(electionElement, "State");
        if(electionState != null) {
            election.setState(ElectionDto.State.valueOf(electionState));
        }
        election.setDateBegin(DateUtils.getXmlDate(XMLUtils.getTextChild(electionElement, "DateBegin")));
        election.setDateFinish(DateUtils.getXmlDate(XMLUtils.getTextChild(electionElement, "DateFinish")));
        election.setEntityId(XMLUtils.getTextChild(electionElement, "EntityId"));
        election.setSubject(XMLUtils.getTextChild(electionElement, "Subject"));
        election.setContent(XMLUtils.getHTMLContent(electionElement, "Content"));
        election.setPublisher(XMLUtils.getTextChild(electionElement, "Publisher"));
        election.setUUID(XMLUtils.getTextChild(electionElement, "UUID"));
        return election;
    }

    public static ResultListDto<ElectionDto> readElections(byte[] xmlBytes) {
        ResultListDto<ElectionDto> result = new ResultListDto();
        try {
            Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
            if(mainElement.getAttributeValue(null, "Type") != null)
                result.setType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
            if(mainElement.getAttributeValue(null, "StatusCode") != null)
                result.setStatusCode(Integer.valueOf(mainElement.getAttributeValue(null, "StatusCode")));
            if(mainElement.getAttributeValue(null, "Offset") != null)
                result.setOffset(Integer.valueOf(mainElement.getAttributeValue(null, "Offset")));
            if(mainElement.getAttributeValue(null, "Max") != null)
                result.setMax(Integer.valueOf(mainElement.getAttributeValue(null, "Max")));
            if(mainElement.getAttributeValue(null, "TotalCount") != null)
                result.setTotalCount(Long.valueOf(mainElement.getAttributeValue(null, "TotalCount")));
            if(mainElement.getElement(null, "ItemList") != null) {
                Set<ElectionDto> elections = new HashSet<>();
                Element itemListElement = mainElement.getElement(null, "ItemList");
                for(int i = 0; i < itemListElement.getChildCount(); i++) {
                    Element itemElement = itemListElement.getElement(i);
                    elections.add(getElection(itemElement));
                }
                result.setResultList(elections);
            }
            result.setMessage(XMLUtils.getTextChild(mainElement, "Message"));
            result.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }
}
