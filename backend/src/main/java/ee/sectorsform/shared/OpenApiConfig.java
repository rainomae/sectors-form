package ee.sectorsform.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI sectorsFormOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Sectors form API")
                .version("v1")
                .description("Sector reference data and form submissions. Errors are RFC 9457 problem details."));
    }
}
