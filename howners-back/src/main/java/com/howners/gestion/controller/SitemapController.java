package com.howners.gestion.controller;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur public qui génère le sitemap.xml pour les moteurs de recherche.
 * Inclut les pages statiques (accueil, annonces) et les annonces publiées.
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ListingRepository listingRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        List<Listing> publishedListings =
                listingRepository.findByStatusOrderByPublishedAtDesc(ListingStatus.PUBLISHED);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Page d'accueil
        appendUrl(xml, frontendUrl + "/", null, "daily", "1.0");

        // Page de recherche d'annonces
        appendUrl(xml, frontendUrl + "/listings", null, "daily", "0.9");

        // Pages d'annonces publiées
        for (Listing listing : publishedListings) {
            String loc = frontendUrl + "/listings/" + listing.getId();
            LocalDateTime lastmod = listing.getUpdatedAt() != null
                    ? listing.getUpdatedAt()
                    : listing.getPublishedAt();
            appendUrl(xml, loc, lastmod, "weekly", "0.7");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String loc, LocalDateTime lastmod,
                           String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>")
               .append(lastmod.format(DateTimeFormatter.ISO_DATE))
               .append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
