package com.springboot.jenka_coffee.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Helper class để lấy messages theo ngôn ngữ hiện tại
 */
@Component
public class MessageHelper {

    private final MessageSource messageSource;

    public MessageHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Lấy message theo key và ngôn ngữ hiện tại
     */
    public String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Lấy message với parameters
     */
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Lấy message với default value nếu key không tồn tại
     */
    public String getMessage(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Lấy ngôn ngữ hiện tại
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Kiểm tra ngôn ngữ hiện tại có phải tiếng Việt không
     */
    public boolean isVietnamese() {
        return "vi".equals(getCurrentLocale().getLanguage());
    }

    /**
     * Kiểm tra ngôn ngữ hiện tại có phải tiếng Anh không
     */
    public boolean isEnglish() {
        return "en".equals(getCurrentLocale().getLanguage());
    }
}
