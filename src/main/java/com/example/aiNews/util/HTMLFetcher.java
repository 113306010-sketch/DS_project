package com.example.aiNews.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HTMLFetcher {

    public static String fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();
            return doc.text();
        } catch (Exception e) {
            System.out.println("Fetch error for url: " + url + " -> " + e.getMessage());
            return "";
        }
    }
}
