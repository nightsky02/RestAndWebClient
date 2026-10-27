package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;

public class ClientPropertiesValidator {
    public static boolean validateProperties(ExternalServiceProperties properties){
        if(properties.authToken() == null || properties.authToken().trim().isEmpty())
            return false;

        return properties.baseUrl() != null && !properties.baseUrl().trim().isEmpty();
    }
}
