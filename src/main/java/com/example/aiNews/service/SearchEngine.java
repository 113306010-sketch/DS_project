package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchEngine {

    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com",
            "wired.com",
            "reuters.com",
            "bloomberg.com",
            "techcrunch.com",
            "cnet.com",
            "engadget.com",
            "digitaltrends.com",
            "bbc.com"
    );

    public List<SearchResult> rankPages(List<String> urls, String userKeyword) {
        List<WebPage> pages = new ArrayList<>();

        for (String url : urls) {
            WebPage page = new WebPage(url, userKeyword);
            int score = page.aiKeywordCount * 10 + page.userKeywordCount * 5;
            if (isNewsSite(url)) {
                score += 30;
            }
            page.score = score;
            pages.add(page);
        }

        pages.sort((a, b) -> Integer.compare(b.score, a.score));

        List<SearchResult> results = new ArrayList<>();
        for (WebPage p : pages) {
            if (p.userKeywordCount == 0) {
                continue;
            }
            results.add(new SearchResult(p.url, p.aiKeywordCount, p.userKeywordCount, p.score));
        }
        return results;
    }

    private boolean isNewsSite(String url) {
        for (String domain : NEWS_DOMAINS) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }
}