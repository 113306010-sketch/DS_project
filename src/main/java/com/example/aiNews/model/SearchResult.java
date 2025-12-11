package com.example.aiNews.model;

public class SearchResult {

    public String url;
    public String title;
    public int aiKeywordCount;
    public int userKeywordCount;
    public int score;

    public SearchResult(String url, String title, int aiKeywordCount, int userKeywordCount, int score) {
        this.url = url;
        this.title = title;
        this.aiKeywordCount = aiKeywordCount;
        this.userKeywordCount = userKeywordCount;
        this.score = score;
    }
}