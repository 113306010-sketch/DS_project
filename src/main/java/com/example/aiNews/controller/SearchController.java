package com.example.aiNews.controller;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.service.GoogleQuery;
import com.example.aiNews.service.SearchEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final GoogleQuery googleQuery;
    private final SearchEngine searchEngine = new SearchEngine();

    public SearchController(GoogleQuery googleQuery) {
        this.googleQuery = googleQuery;
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam String keyword) {
        List<String> urls = googleQuery.search(keyword);
        return searchEngine.rankPages(urls, keyword);
    }
}