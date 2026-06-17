# 1. Java ve Maven ortamı için base image
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 2. Sistem bağımlılıklarını (Node.js, Python, Tesseract) yükleyelim
RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    tesseract-ocr \
    tesseract-ocr-tur \
    && rm -rf /var/lib/apt/lists/*

# 3. Node.js 18+ Kurulumu
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

# 4. Proje dosyalarını içeri aktaralım
COPY . .

# 5. Bağımlılıkları yükleme
RUN npm install --prefix backend || true

# 6. Uygulamanın çalışacağı port
EXPOSE 3001

# 7. Uygulamayı başlatma komutu 
CMD ["npm", "start", "--prefix", "backend"]