# Hướng dẫn Triển khai và Build Sản phẩm (Deploy Rule)
## 1. Quy trình Build Frontend
- Di chuyển vào thư mục frontend:
  ```bash
  cd e:/PMH/code/frontend
  npm run build
  ```
- Kết quả build sẽ nằm tại thư mục `dist/frontend`. Cấu hình máy chủ web (như Nginx) để phục vụ Single Page Application với cơ chế chuyển hướng fallback về `index.html`:
  ```nginx
  location / {
      try_files $uri $uri/ /index.html;
  }
  ```

## 2. Quy trình Build Backend Jar
- Di chuyển vào thư mục backend:
  ```bash
  cd e:/PMH/code/backend
  mvn clean package -DskipTests
  ```
- Chạy file JAR đã build trên môi trường máy chủ:
  ```bash
  java -jar target/paymenthub-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
  ```

## 3. Cấu hình Connection Pool & Bộ nhớ JVM
- **JVM Memory tuning**: Định cấu hình RAM sử dụng tối đa cho Java trên môi trường máy chủ Docker container để tránh tràn bộ nhớ gây sập container:
  `-XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=50.0`
- **Database Pool (HikariCP)**: Thiết lập kích thước kết nối phù hợp, tránh đặt quá lớn gây cạn kiệt tài nguyên Oracle Session:
  ```yaml
  spring:
    datasource:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        idle-timeout: 300000
        connection-timeout: 20000
  ```
