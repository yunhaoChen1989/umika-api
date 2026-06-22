package ca.umika.api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI umikaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Umika Sushi API")
                        .description("Backend API for ordering, loyalty rewards, referrals, payments, and admin workflows.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Umika Sushi")
                                .url("https://umikasushi.ca"))
                        .license(new License()
                                .name("Proprietary")));
    }
}
