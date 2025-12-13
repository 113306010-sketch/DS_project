package com.example.aiNews.model;

import java.util.ArrayList;
import java.util.List;

public class WebNode {
    public WebNode parent;
    public List<WebNode> children;
    public WebPage webPage;
    public double nodeScore; // 這個節點的總分 (包含子節點的回饋)

    public WebNode(WebPage webPage) {
        this.webPage = webPage;
        this.children = new ArrayList<>();
    }

    public void addChild(WebNode child) {
        this.children.add(child);
        child.parent = this;
    }

    // 深度優先搜尋 (DFS) 計算分數
    // 父節點分數 = 本身網頁分數 + (所有子網頁分數總和 * 0.6)
    public double computeNodeScore() {
        double childrenScore = 0;
        for (WebNode child : children) {
            childrenScore += child.computeNodeScore() * 0.6; // 子網頁權重衰減
        }
        this.nodeScore = webPage.score + childrenScore;
        return this.nodeScore;
    }
    
    public int getDepth(){
		int retVal = 1;
		WebNode currNode = this;
		while(currNode.parent!=null){
			retVal ++;
			currNode = currNode.parent;
		}
		return retVal;
	}
}

