FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    tesseract-ocr \
    tesseract-ocr-tur \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

COPY . .

RUN npm install --prefix faturaasistan || true

EXPOSE 3001

CMD ["npm", "start", "--prefix", "faturaasistan"]