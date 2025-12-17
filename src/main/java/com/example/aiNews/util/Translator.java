package com.example.aiNews.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Translator {

    public static String translate(String langFrom, String langTo, String text) {
        try {
            // 使用 Google Translate 的免費公開接口 (GTX)
            String urlStr = "https://translate.googleapis.com/translate_a/single?" +
                    "client=gtx&" +
                    "sl=" + langFrom + "&" +
                    "tl=" + langTo + "&" +
                    "dt=t&" +
                    "q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 解析 JSON 回傳: [[["TranslatedText","Original",...]]]
            String json = response.toString();
            int start = json.indexOf("\"");
            int end = json.indexOf("\"", start + 1);
            
            if (start != -1 && end != -1) {
                return json.substring(start + 1, end);
            }
            
            return text; // 失敗回傳原文

        } catch (Exception e) {
            System.out.println("Translation Error: " + e.getMessage());
            return text;
        }
    }
}