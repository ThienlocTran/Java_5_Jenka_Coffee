package com.springboot.jenka_coffee.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}")
    private String[] allowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,https://*.vercel.app}")
    private String[] allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(clean(allowedOrigins))
                .allowedOriginPatterns(clean(allowedOriginPatterns))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }

    private String[] clean(String[] values) {
        if (values == null) {
            return new String[0];
        }
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    /**
     * Ngăn Googlebot index API endpoints và sitemap trả về đúng Content-Type
     */
    @Bean
    public OncePerRequestFilter robotsHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest req,
                                            @NonNull  HttpServletResponse res,
                                            @NonNull  FilterChain chain) throws ServletException, IOException {
                String path = req.getRequestURI();
                // Block Google khỏi index API responses
                if (path.startsWith("/api/") && !path.equals("/api/sitemap.xml")) {
                    res.setHeader("X-Robots-Tag", "noindex, nofollow");
                }
                chain.doFilter(req, res);
            }
        };
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieSameSiteCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
            // VULN-053 FIX: SameSite=Lax thay vì None — giảm CSRF risk
            // SameSite=None + CSRF disabled = vulnerable nếu CORS misconfigured
            // SameSite=Lax: cookie gửi khi navigate đến site, không gửi cross-site POST
            // Trade-off: cross-origin cookie không tự gửi → frontend dùng Authorization header fallback
            processor.setSameSiteCookies(SameSiteCookies.LAX.getValue());
            context.setCookieProcessor(processor);
        });
    }
}
