package com.springboot.jenka_coffee.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtils {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");
    
    /**
     * Chuyển đổi chuỗi tiếng Việt có dấu thành slug không dấu
     * Ví dụ: "Máy Xay Cà Phê" -> "may-xay-ca-phe"
     */
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Chuyển sang lowercase
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");
        
        return slug.toLowerCase();
    }

}
