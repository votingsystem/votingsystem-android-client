package org.votingsystem.http;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum SystemEntityType {

    VOTING_SERVICE_PROVIDER("voting-service-provider"),
    VOTING_ID_PROVIDER("voting-idprovider"),
    TIMESTAMP_SERVER("timestamp-server"),
    CURRENCY_SERVER("currency-server"),
    ANONYMIZER("anonymizer");

    private String name;

    SystemEntityType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SystemEntityType getByName(String name) {
        for (SystemEntityType systemEntityType : SystemEntityType.values()) {
            if (systemEntityType.getName().equals(name))
                return systemEntityType;
        }
        throw new RuntimeException("There's not SystemEntityType with name: " + name);
    }

}
