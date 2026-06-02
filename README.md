# AI Fatura Asistanı — Lokal Demo

PDF veya görsel (JPG/PNG) fatura yükleyip **tamamen bilgisayarınızda** metin çıkaran ve fatura alanlarını JSON olarak üreten demo uygulaması.

- Bulut API veya API anahtarı **gerekmez**
- Veriler sunucuya gönderilmez (lokal OCR + lokal LLM)
- Bu sürüm **ürün değil, müşteri/sunum demosudur** (veritabanı ve kullanıcı girişi yok)

---

## İçindekiler

1. [Nasıl çalışır?](#nasıl-çalışır)
2. [Gereksinimler](#gereksinimler)
3. [Projeyi indirdikten sonra — ilk kurulum](#projeyi-indirdikten-sonra--ilk-kurulum)
4. [Her çalıştırmada — 4 terminal](#her-çalıştırmada--4-terminal)
5. [Uygulamayı kullanma](#uygulamayı-kullanma)
6. [Ollama model seçimi](#ollama-model-seçimi)
7. [Yapılandırma](#yapılandırma)
8. [Proje yapısı](#proje-yapısı)
9. [API özeti](#api-özeti)
10. [Sorun giderme](#sorun-giderme)

---

## Nasıl çalışır?

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Tarayıcı   │────▶│  Frontend :5173  │────▶│  Backend :8080  │
│  (React)    │     │  (Vite)          │     │  (Spring Boot)  │
└─────────────┘     └──────────────────┘     └────────┬────────┘
                                                      │
                        ┌─────────────────────────────┼─────────────────────────────┐
                        ▼                             ▼                             │
               ┌─────────────────┐           ┌─────────────────┐                    │
               │ OCR Servisi     │           │ Ollama :11434   │                    │
               │ :8866           │           │ (yerel LLM)     │                    │
               │ PDF/görsel→metin│           │ metin→JSON      │                    │
               └─────────────────┘           └─────────────────┘                    │
```

| Adım | Ne olur? |
|------|----------|
| 1 | Kullanıcı faturayı (PDF/JPG/PNG) yükler |
| 2 | Backend dosyayı OCR servisine gönderir → ham metin alınır |
| 3 | Backend metni Ollama’ya gönderir → yapılandırılmış fatura JSON’u alınır |
| 4 | Sonuç ekranda listelenir; isteğe bağlı Excel indirilir |

**OCR motoru:** Ortamınıza göre otomatik seçilir (PaddleOCR → EasyOCR → Tesseract). macOS + Python 3.14’te genelde **Tesseract** kullanılır.

---

## Gereksinimler

Aşağıdakilerin kurulu olması gerekir:

| Yazılım | Minimum sürüm | Kontrol komutu |
|---------|---------------|---------------|
| **Java** | 21 | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Node.js** | 18+ | `node -v` |
| **npm** | 9+ | `npm -v` |
| **Python** | 3.10+ | `python3 --version` |
| **Ollama** | güncel | `ollama --version` |
| **Tesseract** (macOS, önerilir) | 5+ | `tesseract --version` |

**Donanım (önerilen):**

- RAM: en az **8 GB** (hafif LLM için); `qwen3.6` gibi büyük modeller **20 GB+** ister — demo için kullanmayın
- Disk: Ollama modelleri için birkaç GB boş alan

**macOS için ek kurulum:**

```bash
# OCR (Python 3.14'te neredeyse zorunlu)
brew install tesseract tesseract-lang

# Maven yoksa
brew install maven

# İsteğe bağlı: gerçek PaddleOCR için
brew install python@3.12
```

**Ollama:** [https://ollama.com](https://ollama.com) adresinden indirip kurun. Kurulumdan sonra uygulama genelde arka planda `11434` portunda çalışır.

---

## Projeyi indirdikten sonra — ilk kurulum

Projeyi klonladıysanız veya ZIP indirdiyseniz, proje kök dizinine gidin:

```bash
cd /yol/002_FaturaAsistani
```

Script’lere çalıştırma izni verin (bir kez):

```bash
chmod +x scripts/*.sh
```

### Adım 1 — Ollama ve LLM modeli

1. Ollama’nın çalıştığından emin olun (macOS’ta menü çubuğundaki Ollama ikonu veya):

   ```bash
   ollama serve
   ```

2. Yüklü modelleri listeleyin:

   ```bash
   ollama list
   ```

3. **Demo için hafif model indirin** (önerilen, ~2 GB):

   ```bash
   ./scripts/pull-ollama-light.sh
   ```

   Bu komut `qwen2.5:3b` modelini indirir.

4. Backend’in bu modeli kullanması için (aynı terminal oturumunda veya `~/.zshrc` içine ekleyebilirsiniz):

   ```bash
   export OLLAMA_MODEL=qwen2.5:3b
   ```

> **Önemli:** `qwen3.6` gibi çok büyük modeller ~24 GB RAM kullanır ve demo’yu dakikalarca kilitleyebilir. Kullanmayın.

`ollama list` çıktısındaki **tam model adını** kullanın (örnek: `qwen2.5:3b`, `qwen2.5-coder:7b`).

### Adım 2 — OCR servisi (Python)

İlk çalıştırmada sanal ortam ve paketler otomatik kurulur (birkaç dakika sürebilir).

```bash
./scripts/start-paddleocr.sh
```

Başarılıysa terminalde şuna benzer bir satır görürsünüz:

```text
OCR servisi: http://localhost:8866
```

**Kontrol** (yeni bir terminalde):

```bash
curl http://localhost:8866/health
```

Örnek cevap:

```json
{"status":"ok","engine":"tesseract","note":"..."}
```

`engine` değeri `tesseract`, `easyocr` veya `paddleocr` olabilir — hepsi demo için uygundur.

**Python 3.14 sorunu:** PaddlePaddle henüz 3.14 desteklemez; script otomatik Tesseract’a geçer. Tesseract yoksa:

```bash
brew install tesseract tesseract-lang
rm -rf paddleocr-service/.venv
./scripts/start-paddleocr.sh
```

Bu terminal **açık kalmalı** (servis çalışır durumda).

### Adım 3 — Backend (Java)

**Yeni bir terminal** açın:

```bash
cd backend
mvn spring-boot:run
```

İlk seferde Maven bağımlılıkları indirilebilir (birkaç dakika).

**Kontrol:**

```bash
curl http://localhost:8080/api/demo/health
```

Beklenen alanlar:

- `paddleocr.up`: `true`
- `ollama.up`: `true`
- `ollamaModelReady`: `true`
- `ollamaModel`: kullandığınız model adı

Bu terminal de **açık kalmalı**.

### Adım 4 — Frontend (React)

**Üçüncü bir terminal** açın:

```bash
cd frontend
npm install
npm run dev
```

Tarayıcıda açın: **http://localhost:5173**

Üst çubukta **OCR ✓** ve **LLM ✓** yeşil görünmeli. Kırmızıysa ilgili servis ayakta değildir — [Sorun giderme](#sorun-giderme) bölümüne bakın.

---

## Her çalıştırmada — 4 terminal

Projeyi her açışınızda **dört bileşen** çalışır olmalıdır:

| # | Bileşen | Komut | Port |
|---|---------|-------|------|
| 1 | Ollama | Uygulama açık / `ollama serve` | 11434 |
| 2 | OCR servisi | `./scripts/start-paddleocr.sh` | 8866 |
| 3 | Backend | `cd backend && mvn spring-boot:run` | 8080 |
| 4 | Frontend | `cd frontend && npm run dev` | 5173 |

**Hızlı sağlık kontrolü** (beşinci terminal, isteğe bağlı):

```bash
curl -s http://localhost:8866/health && echo ""
curl -s http://localhost:8080/api/demo/health | python3 -m json.tool
```

---

## Uygulamayı kullanma

1. Tarayıcıda `http://localhost:5173` adresini açın.
2. **Fatura yükle** alanına tıklayın veya PDF/JPG/PNG dosyasını sürükleyip bırakın.
3. İşlem sırasında “OCR + … işleniyor” mesajı görünür (ilk yüklemede **1–3 dakika** sürebilir).
4. Sol listeden faturayı seçin; sağda **fatura bilgileri**, **kalemler** ve **ham metin** görünür.
5. **Excel İndir** ile o oturumda yüklenen tüm faturaları `.xlsx` olarak indirin.

**Desteklenen dosyalar:** `.pdf`, `.jpg`, `.jpeg`, `.png` (maks. 10 MB)

**Not:** Faturalar bellekte tutulur; backend yeniden başlatılınca liste sıfırlanır.

---

## Ollama model seçimi

| Model | Yaklaşık RAM | Demo için |
|-------|----------------|-----------|
| `qwen2.5:3b` | ~2–3 GB | **Önerilen** — hızlı |
| `qwen2.5-coder:7b` | ~5–6 GB | İyi — varsayılan (yüklüyse) |
| `llama3.2:3b` | ~2–3 GB | İyi alternatif |
| `qwen3.6` / büyük modeller | 20 GB+ | **Kullanmayın** — çok yavaş |

**Model değiştirme:**

```bash
# 1) Modeli indirin
ollama pull qwen2.5:3b

# 2) Ortam değişkeni (backend başlamadan önce)
export OLLAMA_MODEL=qwen2.5:3b

# 3) Backend'i yeniden başlatın
cd backend && mvn spring-boot:run
```

Kalıcı yapmak için `~/.zshrc` dosyanıza ekleyin:

```bash
export OLLAMA_MODEL=qwen2.5:3b
```

---

## Yapılandırma

Ana ayar dosyası: `backend/src/main/resources/application.yml`

```yaml
app:
  paddleocr:
    base-url: http://localhost:8866    # OCR servisi
    read-timeout-ms: 120000

  ollama:
    base-url: http://localhost:11434    # Ollama
    model: ${OLLAMA_MODEL:qwen2.5-coder:7b}   # ortam değişkeni ile override
    max-text-chars: 5000                # LLM'e giden metin üst sınırı (hız)
    max-tokens: 1024                    # JSON cevap uzunluk sınırı
    read-timeout-ms: 300000             # 5 dakika
```

| Ortam değişkeni | Açıklama |
|-----------------|----------|
| `OLLAMA_MODEL` | Kullanılacak Ollama model adı (`ollama list` ile aynı olmalı) |

OCR veya Ollama farklı makinede çalışıyorsa `base-url` değerlerini o makinenin IP’si ile güncelleyin.

---

## Proje yapısı

```
002_FaturaAsistani/
├── backend/                 # Spring Boot 3, Java 21 — REST API
├── frontend/                # React + Vite — web arayüzü
├── paddleocr-service/       # FastAPI — OCR HTTP servisi
├── scripts/
│   ├── start-paddleocr.sh   # OCR servisini başlatır
│   ├── start-backend.sh     # Backend kısayolu
│   ├── start-frontend.sh    # Frontend kısayolu
│   └── pull-ollama-light.sh # Hafif LLM modeli indirir
├── proje_prompt.md          # Orijinal proje tanımı
└── README.md                # Bu dosya
```

---

## API özeti

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| `POST` | `/api/invoices/upload` | Fatura yükle (`multipart`, alan adı: `file`) |
| `GET` | `/api/invoices` | Yüklenen faturalar listesi |
| `GET` | `/api/invoices/{id}` | Tek fatura detayı |
| `DELETE` | `/api/invoices/{id}` | Faturayı sil |
| `GET` | `/api/invoices/export/excel` | Excel indir |
| `GET` | `/api/demo/health` | OCR + Ollama + model durumu |

**Upload yanıt örneği:**

```json
{
  "id": "uuid",
  "fileName": "fatura.pdf",
  "rawText": "OCR ile çıkan metin...",
  "invoice": {
    "invoiceNumber": "...",
    "supplierName": "...",
    "totalAmount": 1200.00,
    "currency": "TRY",
    "lineItems": []
  },
  "uploadedAt": "2026-06-03T..."
}
```

---

## Sorun giderme

### Frontend: `ECONNREFUSED` / `http proxy error: /api/invoices`

**Sebep:** Backend (port 8080) çalışmıyor.

**Çözüm:**

```bash
cd backend
mvn spring-boot:run
```

---

### Ekran “İşleniyor”da uzun süre kalıyor

**Sebep:** Çok büyük Ollama modeli (ör. `qwen3.6`) veya ilk model yükleme.

**Çözüm:**

```bash
export OLLAMA_MODEL=qwen2.5:3b
ollama pull qwen2.5:3b
# Backend'i yeniden başlatın
```

Activity Monitor’da RAM kullanımını kontrol edin.

---

### HTTP 502 — `model not found`

**Sebep:** `application.yml` veya `OLLAMA_MODEL`’deki ad, `ollama list` ile uyuşmuyor.

**Çözüm:**

```bash
ollama list
export OLLAMA_MODEL=<listedeki-tam-ad>
# Backend yeniden başlat
```

---

### `PaddleOCR servisine ulaşılamadı`

**Sebep:** OCR servisi (8866) kapalı.

**Çözüm:**

```bash
./scripts/start-paddleocr.sh
curl http://localhost:8866/health
```

---

### `No module named 'paddle'`

**Sebep:** Python 3.14 + PaddlePaddle uyumsuzluğu.

**Çözüm:** Tesseract kurun ve venv’i yenileyin:

```bash
brew install tesseract tesseract-lang
rm -rf paddleocr-service/.venv
./scripts/start-paddleocr.sh
```

---

### OCR metni boş veya çok kötü

- Daha net fotoğraf veya PDF deneyin
- `brew install tesseract-lang` (Türkçe dil paketi)
- PDF taranmış görüntüyse çözünürlük düşük olabilir

---

### Üst çubukta LLM ✗ (kırmızı)

```bash
curl http://localhost:8080/api/demo/health
```

- `ollama.up: false` → `ollama serve` veya Ollama uygulamasını açın
- `ollamaModelReady: false` → `export OLLAMA_MODEL=...` ile doğru modeli seçin

---

### Maven / Java bulunamadı

```bash
brew install openjdk@21 maven
```

`JAVA_HOME` ayarı gerekebilir (Homebrew çıktısındaki talimatları izleyin).

---

## Sık kullanılan komutlar (özet)

```bash
# İlk kurulum
chmod +x scripts/*.sh
brew install tesseract tesseract-lang
./scripts/pull-ollama-light.sh
export OLLAMA_MODEL=qwen2.5:3b

# Her çalıştırma (3 ayrı terminal + Ollama uygulaması)
./scripts/start-paddleocr.sh
cd backend && mvn spring-boot:run
cd frontend && npm run dev

# Kontrol
curl http://localhost:8866/health
curl http://localhost:8080/api/demo/health
open http://localhost:5173
```

---

## Lisans ve kapsam

Bu repo **lokal demo** amaçlıdır: kalıcı veritabanı, kimlik doğrulama, Docker dağıtımı ve üretim güvenliği bu sürümde yoktur. Gerçek ürün için OCR kalitesi, veri saklama ve hata yönetimi ayrıca planlanmalıdır.

Sorularınız için `proje_prompt.md` dosyasındaki orijinal gereksinimlere de bakabilirsiniz.
