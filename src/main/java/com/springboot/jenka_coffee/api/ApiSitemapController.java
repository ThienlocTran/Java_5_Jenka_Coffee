package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.NewsRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dynamic Sitemap XML — được Googlebot crawl để index toàn bộ sản phẩm & tin tức
 * Endpoint: GET /sitemap.xml
 */
@RestController
public class ApiSitemapController {

    private static final String SITE_URL = "https://jenka-coffee-ui.vercel.app";
    private static final DateTimeFormatter W3C_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProductRepository productRepository;
    private final NewsRepository newsRepository;
    private final com.springboot.jenka_coffee.repository.CategoryRepository categoryRepository;

    public ApiSitemapController(ProductRepository productRepository,
                                NewsRepository newsRepository,
                                com.springboot.jenka_coffee.repository.CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.newsRepository = newsRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:image=\"https://www.google.com/schemas/sitemap-image/1.1\">\n");

        String today = LocalDateTime.now().format(W3C_DATE);

        // ── Trang tĩnh ──────────────────────────────────────────────
        addUrl(xml, SITE_URL + "/",              "1.0",  "daily",   today);
        addUrl(xml, SITE_URL + "/product/list",  "0.9",  "daily",   today);
        addUrl(xml, SITE_URL + "/news",          "0.8",  "weekly",  today);
        addUrl(xml, SITE_URL + "/contact",       "0.5",  "monthly", today);
        addUrl(xml, SITE_URL + "/booking",       "0.6",  "monthly", today);

        // ── Danh mục sản phẩm ───────────────────────────────────────────
        try {
            List<Category> categories = categoryRepository.findAll();
            for (com.springboot.jenka_coffee.entity.Category c : categories) {
                addUrl(xml, SITE_URL + "/category/" + c.getId(), "0.85", "weekly", today);
            }
        } catch (Exception e) {
            // không fail toàn bộ sitemap
        }

        // ── Sản phẩm — paginate để không OOM khi 500+ sản phẩm ────────
        // VULN-SEO-SABOTAGE FIX: Use slug-based URLs matching frontend router
        try {
            int productPage = 0;
            int productPageSize = 100;
            org.springframework.data.domain.Page<Product> productPageResult;
            do {
                productPageResult = productRepository.findAllWithCategory(
                        PageRequest.of(productPage, productPageSize));
                for (Product p : productPageResult.getContent()) {
                    // Use slug if available, fallback to ID for backward compatibility
                    String productPath = (p.getSlug() != null && !p.getSlug().isBlank())
                            ? "/san-pham/" + p.getSlug()
                            : "/product/detail/" + p.getId();
                    String url = SITE_URL + productPath;
                    xml.append("  <url>\n");
                    xml.append("    <loc>").append(url).append("</loc>\n");
                    xml.append("    <changefreq>weekly</changefreq>\n");
                    xml.append("    <priority>0.8</priority>\n");
                    if (p.getImage() != null && !p.getImage().isBlank()) {
                        String imgUrl = p.getImage().startsWith("http")
                                ? p.getImage()
                                : "https://res.cloudinary.com/dqtrmwuxa/image/upload/" + p.getImage();
                        xml.append("    <image:image>\n");
                        xml.append("      <image:loc>").append(escapeXml(imgUrl)).append("</image:loc>\n");
                        xml.append("      <image:title>").append(escapeXml(p.getName())).append("</image:title>\n");
                        xml.append("    </image:image>\n");
                    }
                    xml.append("  </url>\n");
                }
                productPage++;
            } while (productPageResult.hasNext());
        } catch (Exception e) {
            // không fail toàn bộ sitemap
        }

        // ── Tin tức ──────────────────────────────────────────────────
        try {
            List<News> newsList = newsRepository.findByAvailableTrueOrderByCreateDateDesc();
            for (News n : newsList) {
                String url = SITE_URL + "/news/detail/" + n.getId();
                String lastmod = n.getCreateDate() != null
                        ? n.getCreateDate().format(W3C_DATE)
                        : today;
                addUrl(xml, url, "0.7", "monthly", lastmod);
            }
        } catch (Exception e) {
            // Log nhưng không fail toàn bộ sitemap
        }

        xml.append("</urlset>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private void addUrl(StringBuilder xml, String loc, String priority, String changefreq, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
