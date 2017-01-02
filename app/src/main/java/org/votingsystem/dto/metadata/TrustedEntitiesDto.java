package org.votingsystem.dto.metadata;


import org.votingsystem.http.SystemEntityType;

import java.io.Serializable;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TrustedEntitiesDto implements Serializable {

    public static final long serialVersionUID = 1L;


    private Set<EntityDto> entities;

    public TrustedEntitiesDto() { }


    public Set<EntityDto> getEntities() {
        return entities;
    }

    public void setEntities(Set<EntityDto> entities) {
        this.entities = entities;
    }


    public static class EntityDto {

        private String id;
        private SystemEntityType type;
        private String countryCode;
        private String description;

        public EntityDto() {  }


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SystemEntityType getType() {
            return type;
        }

        public void setType(SystemEntityType type) {
            this.type = type;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
