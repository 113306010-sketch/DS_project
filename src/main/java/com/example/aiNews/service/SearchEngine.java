package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.service.GoogleQuery.SearchItem;

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

    public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
        List<WebPageWithTitle> pages = new ArrayList<>();

        for (SearchItem item : items) {
            WebPage page = new WebPage(item.url, userKeyword);
            int score = page.aiKeywordCount * 10 + page.userKeywordCount * 5;
            if (isNewsSite(item.url)) {
                score += 30;
            }
            page.score = score;
            pages.add(new WebPageWithTitle(page, item.title));
        }

        pages.sort((a, b) -> Integer.compare(b.page.score, a.page.score));

        List<SearchResult> results = new ArrayList<>();
        for (WebPageWithTitle p : pages) {
            if ( p.page.userKeywordCount == 0) {
                continue;
            }
            results.add(new SearchResult(
                p.page.url, 
                p.title, 
                p.page.aiKeywordCount, 
                p.page.userKeywordCount, 
                p.page.score
            ));
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

    private static class WebPageWithTitle {
        WebPage page;
        String title;
        
        WebPageWithTitle(WebPage page, String title) {
            this.page = page;
            this.title = title;
        }
    }
}