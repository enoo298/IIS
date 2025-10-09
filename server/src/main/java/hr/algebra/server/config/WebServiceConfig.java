package hr.algebra.server.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;



@EnableWs
@Configuration
public class WebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext ctx) {
        var servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(ctx);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "search")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema searchSchema) {
        var def = new DefaultWsdl11Definition();
        def.setPortTypeName("SearchResultsPort");
        def.setLocationUri("/ws");
        def.setTargetNamespace("http://iis.com/search");
        def.setSchema(searchSchema);
        return def;
    }

    @Bean
    public XsdSchema searchSchema() {
        return new SimpleXsdSchema(new org.springframework.core.io.ClassPathResource("schema/search.xsd"));
    }
}