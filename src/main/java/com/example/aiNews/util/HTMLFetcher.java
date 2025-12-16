package com.example.aiNews.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class HTMLFetcher {

    public static String fetch(String url) {
        // [防護 1] 絕對不要去碰執行檔或壓縮檔 (這是病毒最常見的來源)
        if (url.matches(".*\\.(exe|zip|rar|pdf|doc|docx|xls|xlsx|ppt|pptx|jpg|png|gif|mp4|mp3)$")) {
            System.out.println("⚠️ Skip binary file: " + url);
            return "";
        }

        try {
            // 設定寬鬆的 SSL (為了抓取憑證過期的老舊新聞網)
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000) // 設定 5 秒逾時 (避免卡在惡意網站)
                    .ignoreContentType(true) // 允許各種格式，但我們在上面過濾了危險副檔名
                    .sslSocketFactory(sc.getSocketFactory())
                    .maxBodySize(1024 * 1024 * 2) // [防護 2] 限制最大下載 2MB (新聞通常不會超過 2MB，避免記憶體攻擊)
                    .get();

            return doc.text();
            
        } catch (IOException e) {
            // 遇到連線錯誤是正常的 (對方擋爬蟲或網站掛了)，不用太緊張
            // System.out.println("Fetch error for url: " + url + " -> " + e.getMessage());
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}