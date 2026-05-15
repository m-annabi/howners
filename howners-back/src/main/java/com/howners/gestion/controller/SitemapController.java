package com.howners.gestion.controller;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * sitemap.xml conforme à <a href="https://www.sitemaps.org/protocol.html">sitemap.org</a>.
 *
 * Liste les URLs publiquement indexables :
 *  - Landing /
 *  - Browse /listings
 *  - Auth: /auth/login, /auth/register
 *  - Une URL par annonce PUBLISHED (lastmod = updatedAt)
 *
 * Le robots.txt référence /sitemap.xml — sans cet endpoint, Googlebot reçoit un 404.
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ListingRepository listingRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        String today = LocalDate.now().toString();
        appendUrl(xml, frontendUrl, today, "weekly", "1.0");
        appendUrl(xml, frontendUrl + "/listings", today, "daily", "0.9");
        appendUrl(xml, frontendUrl + "/auth/register", today, "monthly", "0.5");
        appendUrl(xml, frontendUrl + "/auth/login", today, "monthly", "0.3");

        // Each published listing as its own URL
        List<Listing> published = listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED);
        for (Listing l : published) {
            LocalDateTime updated = l.getUpdatedAt() != null ? l.getUpdatedAt() : l.getPublishedAt();
            String lastmod = updated != null
                    ? updated.toLocalDate().toString()
                    : today;
            appendUrl(xml, frontendUrl + "/listings/" + l.getId(), lastmod, "weekly", "0.7");
        }

        xml.append("</urlset>\n");
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=3600")
                .body(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n")
           .append("    <loc>").append(loc).append("</loc>\n")
           .append("    <lastmod>").append(lastmod).append("</lastmod>\n")
           .append("    <changefreq>").append(changefreq).append("</changefreq>\n")
           .append("    <priority>").append(priority).append("</priority>\n")
           .append("  </url>\n");
    }
}
