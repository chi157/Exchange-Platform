# exchange-web-app (clean starter)

最小可跑的 Spring Boot 3 專案，用來確認路由與封包映射是否正常。

## 需求
- Java 17
- Maven 3.9+

## 快速開始
```bash
mvn -q clean package
java -jar target/exchange-web-app-0.0.1-SNAPSHOT.jar
```

啟動後測試：
- `GET http://localhost:8080/api/health` → 回傳 `OK`
- `GET http://localhost:8080/api/hello` → 回傳歡迎文字

> 若 404/401 或無法映射，請先檢查封包路徑、Security 是否引入、或是否修改過 context-path。
