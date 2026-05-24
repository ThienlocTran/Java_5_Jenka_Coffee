package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.NewsRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.util.SlugUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
public class ApiSitemapController {

    private static final DateTimeFormatter W3C_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> LANDING_PATHS = Arrays.asList(
            "/may-pha-ca-phe",
            "/may-xay-ca-phe",
            "/may-pha-ca-phe-cho-quan",
            "/setup-quan-ca-phe"
    );

    private final ProductRepository productRepository;
    private final NewsRepository newsRepository;
    private final CategoryRepository categoryRepository;
    private final String siteUrl;

    public ApiSitemapController(ProductRepository productRepository,
                                NewsRepository newsRepository,
                                CategoryRepository categoryRepository,
                                @Value("${app.site-url:https://jenkacoffee.com}") String siteUrl) {
        this.productRepository = productRepository;
        this.newsRepository = newsRepository;
        this.categoryRepository = categoryRepository;
        this.siteUrl = normalizeSiteUrl(siteUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String today = LocalDateTime.now().format(W3C_DATE);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        addUrl(xml, siteUrl + "/", "1.0", "daily", today);
        addUrl(xml, siteUrl + "/product/list", "0.7", "daily", today);
        addUrl(xml, siteUrl + "/tin-tuc", "0.6", "weekly", today);
        appendLandingUrls(xml, today);

        appendCategoryUrls(xml, today);
        appendProductUrls(xml, today);
        appendNewsUrls(xml, today);

        xml.append("</urlset>");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private void appendLandingUrls(StringBuilder xml, String today) {
        for (String path : LANDING_PATHS) {
            addUrl(xml, siteUrl + path, "0.8", "weekly", today);
        }
    }

    private void appendCategoryUrls(StringBuilder xml, String today) {
        try {
            List<Category> categories = categoryRepository.findAll();
            for (Category category : categories) {
                String slug = resolveCategorySlug(category);
                if (slug.isBlank()) {
                    continue;
                }
                addUrl(xml, siteUrl + "/category/" + slug, "0.8", "weekly", today);
            }
        } catch (Exception ignored) {
        }
    }

    private void appendProductUrls(StringBuilder xml, String today) {
        try {
            int page = 0;
            Page<Product> productPage;
            do {
                productPage = productRepository.findByAvailableTrueWithCategory(PageRequest.of(page, 100));
                for (Product product : productPage.getContent()) {
                    if (product.getSlug() == null || product.getSlug().isBlank()) {
                        continue;
                    }
                    String lastmod = product.getCreateDate() != null
                            ? product.getCreateDate().format(W3C_DATE)
                            : today;
                    addUrl(xml, siteUrl + "/san-pham/" + product.getSlug(), "0.7", "weekly", lastmod);
                }
                page++;
            } while (productPage.hasNext());
        } catch (Exception ignored) {
        }
    }

    private void appendNewsUrls(StringBuilder xml, String today) {
        try {
            int page = 0;
            Page<News> newsPage;
            do {
                newsPage = newsRepository.findByAvailableTrueOrderByCreateDateDesc(PageRequest.of(page, 100));
                for (News news : newsPage.getContent()) {
                    String path = (news.getSlug() != null && !news.getSlug().isBlank())
                            ? "/tin-tuc/" + news.getSlug()
                            : "/news/detail/" + news.getId();
                    String lastmod = news.getCreateDate() != null
                            ? news.getCreateDate().format(W3C_DATE)
                            : today;
                    addUrl(xml, siteUrl + path, "0.6", "monthly", lastmod);
                }
                page++;
            } while (newsPage.hasNext());
        } catch (Exception ignored) {
        }
    }

    private String resolveCategorySlug(Category category) {
        if (category == null) return "";
        if (category.getSlug() != null && !category.getSlug().isBlank()) {
            return category.getSlug().trim();
        }
        if (category.getName() != null && !category.getName().isBlank()) {
            return SlugUtils.toSlug(category.getName());
        }
        return category.getId() != null ? category.getId().trim().toLowerCase() : "";
    }

    private void addUrl(StringBuilder xml, String loc, String priority, String changefreq, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String normalizeSiteUrl(String rawSiteUrl) {
        if (rawSiteUrl == null || rawSiteUrl.isBlank()) {
            return "https://jenkacoffee.com";
        }
        return rawSiteUrl.replaceAll("/+$", "");
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
