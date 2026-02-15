package org.example.realtimenotify.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI apiInfo() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Realtime Notify API")
                .version("v1")
                .description("API for Realtime Notifications & Presence Service")
                .contact(new Contact().name("Asim Alam").email("asimalam8@gmail.com")));
  }
}
