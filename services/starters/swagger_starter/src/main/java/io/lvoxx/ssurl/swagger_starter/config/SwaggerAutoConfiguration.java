package io.lvoxx.ssurl.swagger_starter.config;

import io.lvoxx.ssurl.common.util.Constants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Simple URL Shortener API")
                .description("High-performance URL shortener — create, manage, and track short links")
                .version("1.0.0")
                .license(new License().name("MIT").url("https://opensource.org/license/mit/")))
            .addSecurityItem(new SecurityRequirement().addList(Constants.Beans.BEARER_AUTH))
            .components(new Components()
                .addSecuritySchemes(Constants.Beans.BEARER_AUTH, new SecurityScheme()
                    .name(Constants.Beans.BEARER_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat(Constants.Jwt.TOKEN_TYPE)));
    }
}
