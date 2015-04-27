package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.UserVSDto;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeDto extends UserVSDto {

    private Long id;
    private Long numRepresentations;
    private String nif;
    private String name;
    private String firstName;
    private String lastName;
    private String description;
    private String URL;
    private String representativeMessageURL;
    private String imageURL;
    private UserVSDto.Type type;

    public RepresentativeDto() {}

    public Long getId() {
        return id;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public String getNif() {
        return nif;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDescription() {
        return description;
    }

    public String getURL() {
        return URL;
    }

    public String getRepresentativeMessageURL() {
        return representativeMessageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public UserVSDto.Type getType() {
        return type;
    }
}
