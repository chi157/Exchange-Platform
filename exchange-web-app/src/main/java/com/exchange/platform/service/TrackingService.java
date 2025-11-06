package com.exchange.platform.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TrackingService {

    private static final String API_URL = "https://eservice.7-11.com.tw/E-Tracking";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    // 儲存每個 session 的資源（cookies, VIEWSTATE 等）
    private final Map<String, Map<String, Object>> sessionResources = new HashMap<>();
    
    /**
     * 生成驗證碼並返回 session ID
     */
    public String generateCaptcha(HttpSession httpSession) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        
        // 步驟一：取得查詢頁面
        URL url = new URL(API_URL + "/search.aspx");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        
        // 取得所有 cookies（可能有多個 Set-Cookie headers）
        Map<String, String> cookies = new HashMap<>();
        List<String> setCookieHeaders = conn.getHeaderFields().get("Set-Cookie");
        if (setCookieHeaders != null) {
            for (String cookieHeader : setCookieHeaders) {
                String[] cookieParts = cookieHeader.split(";");
                if (cookieParts.length > 0) {
                    String[] keyValue = cookieParts[0].split("=", 2);
                    if (keyValue.length == 2) {
                        cookies.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
        }
        
        // 解析 HTML 取得 VIEWSTATE
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        
        Document doc = Jsoup.parse(response.toString());
        Element viewStateElement = doc.getElementById("__VIEWSTATE");
        Element viewStateGenElement = doc.getElementById("__VIEWSTATEGENERATOR");
        
        String viewState = viewStateElement != null ? viewStateElement.attr("value") : "";
        String viewStateGen = viewStateGenElement != null ? viewStateGenElement.attr("value") : "";
        
        // 建立 cookie header
        StringBuilder cookieBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
            cookieBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        String cookieStr = cookieBuilder.toString();
        
        // 步驟二：下載驗證碼圖片
        long ts = System.currentTimeMillis();
        URL imageUrl = new URL(API_URL + "/ValidateImage.aspx?ts=" + ts);
        HttpURLConnection imageConn = (HttpURLConnection) imageUrl.openConnection();
        imageConn.setRequestMethod("GET");
        imageConn.setRequestProperty("Cookie", cookieStr);
        imageConn.setRequestProperty("User-Agent", USER_AGENT);
        imageConn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
        
        // 儲存圖片
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "tracking-captchas");
        Files.createDirectories(tempDir);
        Path imagePath = tempDir.resolve(sessionId + ".jpg");
        
        try (InputStream imageStream = imageConn.getInputStream();
             FileOutputStream fos = new FileOutputStream(imagePath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = imageStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        log.info("Captcha image saved: {}", imagePath);
        
        // 儲存資源供後續查詢使用
        Map<String, Object> resource = new HashMap<>();
        resource.put("cookies", cookieStr);
        resource.put("viewState", viewState);
        resource.put("viewStateGen", viewStateGen);
        resource.put("imagePath", imagePath.toString());
        
        sessionResources.put(sessionId, resource);
        
        return sessionId;
    }
    
    /**
     * 取得驗證碼圖片
     */
    public Resource getCaptchaImage(String sessionId) {
        Map<String, Object> resource = sessionResources.get(sessionId);
        if (resource == null) return null;
        
        String imagePath = (String) resource.get("imagePath");
        if (imagePath == null) return null;
        
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) return null;
        
        return new FileSystemResource(imageFile);
    }
    
    /**
     * 使用驗證碼查詢物流狀態
     */
    public Map<String, Object> queryTracking(String sessionId, String code, String trackingNumber) throws Exception {
        Map<String, Object> resource = sessionResources.get(sessionId);
        if (resource == null) {
            throw new IllegalArgumentException("無效的 session ID");
        }
        
        String cookies = (String) resource.get("cookies");
        String viewState = (String) resource.get("viewState");
        String viewStateGen = (String) resource.get("viewStateGen");
        
        // 建立 POST 請求
        URL url = new URL(API_URL + "/search.aspx");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Cookie", cookies);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        // 準備表單數據
        Map<String, String> formData = new HashMap<>();
        formData.put("__LASTFOCUS", "");
        formData.put("__EVENTTARGET", "");
        formData.put("__EVENTARGUMENT", "");
        formData.put("__VIEWSTATE", viewState);
        formData.put("__VIEWSTATEGENERATOR", viewStateGen);
        formData.put("txtProductNum", trackingNumber);
        formData.put("tbChkCode", code);
        formData.put("aaa", "");
        formData.put("txtIMGName", "");
        formData.put("txtPage", "1");
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (postData.length() > 0) postData.append("&");
            postData.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
            postData.append("=");
            postData.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        
        // 發送請求
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.toString().getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
        
        // 讀取響應
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        
        // 解析結果
        Document doc = Jsoup.parse(response.toString());
        Element txtPageElement = doc.getElementById("txtPage");
        
        if (txtPageElement != null && "2".equals(txtPageElement.attr("value"))) {
            // 查詢成功，解析物流資訊
            Map<String, Object> result = new HashMap<>();
            
            Element infoDiv = doc.selectFirst("div.info");
            if (infoDiv != null) {
                Elements infoChildren = infoDiv.children();
                if (infoChildren.size() > 0) {
                    Element pickupInfo = infoChildren.get(0);
                    
                    // 取貨門市
                    Element storeName = pickupInfo.getElementById("store_name");
                    if (storeName != null) result.put("storeName", storeName.text());
                    
                    // 取貨門市地址
                    Element storeAddress = pickupInfo.getElementById("store_address");
                    if (storeAddress != null) result.put("storeAddress", storeAddress.text());
                    
                    // 取貨截止日
                    Element deadline = pickupInfo.getElementById("deadline");
                    if (deadline != null) result.put("pickupDeadline", deadline.text());
                }
                
                if (infoChildren.size() > 1) {
                    // 付款資訊
                    Element paymentInfo = infoChildren.get(1);
                    Element serviceType = paymentInfo.getElementById("servicetype");
                    if (serviceType != null) result.put("paymentType", serviceType.text());
                }
            }
            
            // 貨態資訊
            Element shippingDiv = doc.selectFirst("div.shipping");
            if (shippingDiv != null) {
                Elements statusElements = shippingDiv.select("li");
                List<String> statusList = new ArrayList<>();
                Pattern datePattern = Pattern.compile("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}");
                
                for (Element statusElement : statusElements) {
                    String statusText = statusElement.text();
                    Matcher matcher = datePattern.matcher(statusText);
                    if (matcher.find()) {
                        String dateTime = matcher.group();
                        String description = statusText.replace(dateTime, "").trim();
                        statusList.add(dateTime + " " + description);
                    }
                }
                
                Collections.reverse(statusList);
                result.put("statusList", statusList);
            }
            
            // 清理該 session 的資源
            cleanupSession(sessionId);
            
            return result;
        } else {
            // 查詢失敗
            cleanupSession(sessionId);
            throw new IllegalArgumentException("查詢失敗，請確認驗證碼或交貨便單號是否正確");
        }
    }
    
    /**
     * 清理 session 資源
     */
    private void cleanupSession(String sessionId) {
        Map<String, Object> resource = sessionResources.remove(sessionId);
        if (resource != null) {
            String imagePath = (String) resource.get("imagePath");
            if (imagePath != null) {
                try {
                    Files.deleteIfExists(Paths.get(imagePath));
                } catch (IOException e) {
                    log.warn("Failed to delete captcha image: {}", imagePath, e);
                }
            }
        }
    }
}
