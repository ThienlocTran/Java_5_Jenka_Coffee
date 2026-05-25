package com.springboot.jenka_coffee.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRobotsController {

    private final String siteUrl;

    public ApiRobotsController(@Value("${app.site-url:https://jenkacoffee.com}") String siteUrl) {
        this.siteUrl = (siteUrl == null || siteUrl.isBlank())
                ? "https://jenkacoffee.com"
                : siteUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = String.join("\n",
                "User-agent: *",
                "Allow: /",
                "Disallow: /admin",
                "Disallow: /auth",
                "Disallow: /cart",
                "Disallow: /checkout",
                "Disallow: /orders",
                "Disallow: /profile",
                "Sitemap: " + siteUrl + "/sitemap.xml",
                "");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}
