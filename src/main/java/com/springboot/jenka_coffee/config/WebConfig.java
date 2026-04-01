package com.springboot.jenka_coffee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only serve local uploads folder (avatars uploaded before Cloudinary migration)
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }

    /**
     * Set SameSite=None trên session cookie qua Tomcat processor.
     * Hoạt động trên cả HTTP (local) và HTTPS (Railway).
     * Browser hiện đại chấp nhận SameSite=None không có Secure trên localhost.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieSameSiteCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
            processor.setSameSiteCookies(SameSiteCookies.NONE.getValue());
            context.setCookieProcessor(processor);
        });
    }
}
