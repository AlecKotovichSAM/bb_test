package eu.bb.app.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bbOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("BB Backend Server");

        Contact contact = new Contact();
        contact.setName("BB Team");
        contact.setEmail("support@bb.app");

        License license = new License()
                .name("Proprietary")
                .url("https://bb.app/license");

        Info info = new Info()
                .title("BB Backend API")
                .version("1.0.0")
                .description("REST API for managing children's birthday events, guests, gifts, and chat")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
