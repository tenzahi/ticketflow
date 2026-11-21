package com.ticketflow.bank_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;

import java.util.Properties;

@Configuration
public class SoapFaultConfig {

    @Bean
    public SoapFaultMappingExceptionResolver exceptionResolver() {

        SoapFaultMappingExceptionResolver resolver =
                new SoapFaultMappingExceptionResolver();

        SoapFaultDefinition defaultFault =
                new SoapFaultDefinition();

        defaultFault.setFaultCode(
                SoapFaultDefinition.SERVER
        );

        resolver.setDefaultFault(defaultFault);

        Properties exceptionMappings = new Properties();

        exceptionMappings.setProperty(
                IllegalArgumentException.class.getName(),
                "CLIENT,Requete invalide"
        );

        resolver.setExceptionMappings(exceptionMappings);

        resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return resolver;
    }
}
