# 1. Aşama: Maven ile Java projesini derleme
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Sistem bağımlılıklarını (Python ve Tesseract) yükleyelim
RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    tesseract-ocr \
    tesseract-ocr-tur \
    && rm -rf /var/lib/apt/lists/*

# Proje dosyalarını kopyala
COPY . .

# Maven ile backend projesini build edip JAR dosyasını üretelim
RUN mvn -f backend/pom.xml clean package -DskipTests

# 2. Aşama: Çalışma ortamı
FROM eclipse-temurin:21-jre
WORKDIR /app

# Çalışma ortamına da Tesseract ve Python bağımlılıklarını ekleyelim
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    tesseract-ocr \
    tesseract-ocr-tur \
    && rm -rf /var/lib/apt/lists/*

# İlk aşamada üretilen JAR dosyasını buraya alalım
# Not: pom.xml içindeki artifactId ve versiyona göre oluşan jar ismini kopyalıyoruz.
# Eğer jar ismi tam uyuşmazsa target klasörünün altındaki isme göre revize edilebilir.
COPY --from=build /app/backend/target/*.jar app.jar
COPY --from=build /app/paddleocr-service ./paddleocr-service
COPY --from=build /app/scripts ./scripts

# Uygulamanın çalışacağı port (Belge amaçlı)
EXPOSE 3001

# Spring Boot'a 3001 portunda çalışmasını emrediyoruz
ENV SERVER_PORT=3001

# Spring Boot uygulamasını başlatma komutu
CMD ["java", "-jar", "app.jar"]