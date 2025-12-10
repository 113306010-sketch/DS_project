package com.example.aiNews.model;

import java.util.ArrayList;
import java.util.List;

public class WebNode {
    public WebPage page;
    public List<WebNode> children = new ArrayList<>();
    public int totalScore;

    public WebNode(WebPage page) {
        this.page = page;
    }
}
