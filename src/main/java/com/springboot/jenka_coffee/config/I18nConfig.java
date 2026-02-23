package com.springboot.jenka_coffee.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

/**
 * Cấu hình đa ngôn ngữ (I18n) cho ứng dụng
 * Hỗ trợ: Tiếng Việt (vi) và Tiếng Anh (en)
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * Cấu hình MessageSource để load các file messages
     * - messages_vi.properties (Tiếng Việt)
     * - messages_en.properties (Tiếng Anh)
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Cache 1 hour
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(new Locale("vi")); // Mặc định tiếng Việt
        return messageSource;
    }

    /**
     * Cấu hình LocaleResolver để lưu ngôn ngữ vào Cookie
     * Cookie name: "lang", tồn tại 365 ngày
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
        resolver.setDefaultLocale(new Locale("vi")); // Mặc định tiếng Việt
        resolver.setCookieMaxAge(java.time.Duration.ofDays(365)); // 1 năm
        return resolver;
    }

    /**
     * Interceptor để thay đổi ngôn ngữ qua parameter ?lang=vi hoặc ?lang=en
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // ?lang=vi hoặc ?lang=en
        return interceptor;
    }

    /**
     * Tích hợp MessageSource với Validator để validation messages hỗ trợ đa ngôn ngữ
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    /**
     * Đăng ký LocaleChangeInterceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
