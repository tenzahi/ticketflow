package com.ticketflow.bank_service.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;

@EnableWs
@Configuration
public class WebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);

        return ServletRegistrationBean(
                servlet,
                "/ws/*"
        );
    }

    private ServletRegistrationBean<MessageDispatcherServlet> ServletRegistrationBean(MessageDispatcherServlet servlet,
            String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ServletRegistrationBean'");
    }

    @Bean(name = "bank")
    public DefaultWsdl11Definition defaultWsdl11Definition(
            XsdSchema bankSchema) {

        DefaultWsdl11Definition wsdl11Definition =
                new DefaultWsdl11Definition();

        wsdl11Definition.setPortTypeName("BankPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace(
                "http://ticketflow.com/bank"
        );
        wsdl11Definition.setSchema(bankSchema);

        return wsdl11Definition;
    }

    @Bean
    public XsdSchema bankSchema() {
        return new SimpleXsdSchema(
                new ClassPathResource("xsd/bank.xsd")
        );
    }
}