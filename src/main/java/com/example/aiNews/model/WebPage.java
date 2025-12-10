package com.example.aiNews.model;

import com.example.aiNews.util.HTMLFetcher;

import java.util.Arrays;
import java.util.List;

public class WebPage {

    public String url;
    public String content;
    public int aiKeywordCount;
    public int userKeywordCount;
    public int score;

    private static final List<String> AI_KEYWORDS = Arrays.asList(
            "ai",
            "artificial intelligence",
            "machine learning",
            "deep learning",
            "neural network",
            "llm",
            "gpt",
            "generative ai"
    );

    public WebPage(String url, String userKeyword) {
        this.url = url;
        this.content = HTMLFetcher.fetch(url);
        String text = content == null ? "" : content.toLowerCase();
        this.aiKeywordCount = countAiKeywords(text);
        this.userKeywordCount = countKeyword(text, userKeyword == null ? "" : userKeyword.toLowerCase());
        this.score = 0;
    }

    private int countAiKeywords(String text) {
        if (text == null || text.isEmpty()) return 0;
        int total = 0;
        for (String k : AI_KEYWORDS) {
            total += countKeyword(text, k.toLowerCase());
        }
        return total;
    }

    private int countKeyword(String text, String keyword) {
        if (keyword == null || keyword.isEmpty()) return 0;
        int count = 0;
        int index = text.indexOf(keyword);
        while (index != -1) {
            count++;
            index = text.indexOf(keyword, index + keyword.length());
        }
        return count;
    }
}
