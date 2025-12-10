package com.example.aiNews.model;

public class SearchResult {

    public String url;
    public int aiKeywordCount;
    public int userKeywordCount;
    public int score;

    public SearchResult(String url, int aiKeywordCount, int userKeywordCount, int score) {
        this.url = url;
        this.aiKeywordCount = aiKeywordCount;
        this.userKeywordCount = userKeywordCount;
        this.score = score;
    }
}
