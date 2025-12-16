package com.example.aiNews.controller;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.service.GoogleQuery;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import com.example.aiNews.service.SearchEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final GoogleQuery googleQuery;
    private final SearchEngine searchEngine;

    public SearchController(GoogleQuery googleQuery, SearchEngine searchEngine) {
        this.googleQuery = googleQuery;
        this.searchEngine = searchEngine;
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam String keyword) {
        List<SearchItem> items = googleQuery.search(keyword);
        return searchEngine.rankPages(items, keyword);
    }
}