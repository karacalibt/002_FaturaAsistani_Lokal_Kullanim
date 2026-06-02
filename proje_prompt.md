Sen uzman bir yazılımcı ve yazılım miamrısın, yapay zeka ürün geliştiren kıdemli bir geliştiricisin.
Bu projeye senden 3 kısım geliştirme isteyeceğim;
1- backend, gerekli promptlar aşağıda
2- frontEnd, back end ile uyumlu gerekli işlemleri yapabileceğimiz basit ama profesyonel görünen modern bir tasarım. fatura yükleme, yüklenen faturaları listeli şekilde görme, bunları excel olarak indirebilme gibi
özellikleri olmalı.
3- yapay zeka asistan,, yüklenen fatuları okuyup backend e json olarak veri dönebilecek bir yapı lazım. bunu OCR kullanarak gerekirse yapabilirsin. eğer işe yaramaz ise lokal bir llm kullanmayı deneyebiliriz. bunun için gerekli aksiyonu daha sonra alabiliriz.
şimdi aşağıda verilen tariflere riayet ederek geliştirmeleri yap lütfen.

Java 21 ve Spring Boot 3 ile "AI Fatura Asistanı" adında basit bir backend projesi oluştur.

Amaç:
Kullanıcı PDF veya resim formatında fatura yüklesin.
Backend dosyayı alsın.
Dosyadan metin çıkarsın.
Metni AI'a gönderip fatura bilgilerini JSON formatında çıkarsın.
Sonucu kullanıcıya dönsün.

İlk MVP için basit ve anlaşılır bir yapı kur.

Gereksinimler:

1. Maven projesi oluştur.
2. Spring Boot 3 kullan.
3. Java 21 kullan.
4. REST endpoint oluştur:

POST /api/invoices/upload

Bu endpoint multipart file alsın.

5. Desteklenen dosya türleri:
- PDF
- JPG
- PNG

6. Eğer dosya PDF ise metni Apache PDFBox ile çıkar.
7. Eğer dosya JPG veya PNG ise ilk aşamada OCRService sınıfına yönlendir.
8. OCRService için şimdilik sahte bir implementasyon yaz.
   Daha sonra Tesseract veya OpenAI Vision bağlanacak şekilde tasarla.

9. InvoiceAiService sınıfı oluştur.
Bu sınıf çıkarılan fatura metnini OpenAI API'ye gönderip yapılandırılmış JSON cevap alsın.

10. InvoiceExtractionResult adında bir DTO oluştur.
Alanlar:

- invoiceNumber
- invoiceDate
- supplierName
- supplierTaxNumber
- taxOffice
- currency
- subtotalAmount
- vatAmount
- totalAmount
- lineItems

11. InvoiceLineItem DTO oluştur.
Alanlar:

- description
- quantity
- unitPrice
- vatRate
- lineTotal

12. OpenAI API key application.yml dosyasından alınsın.

13. Controller şu cevabı dönsün:

{
  "fileName": "...",
  "rawText": "...",
  "invoice": {
     ...
  }
}

14. Kodlar başlangıç seviyesine uygun, temiz ve açıklamalı olsun.
15. İlk aşamada veritabanı, security, kullanıcı girişi, Docker ekleme.


Yapay zeka asistanı için;
Sen bir fatura okuma ve veri çıkarma asistanısın.

Aşağıdaki fatura metninden önemli bilgileri çıkar.

Kurallar:

- Sadece faturada yazan bilgileri kullan.

- Bilmediğin veya bulamadığın alanlara null yaz.

- Tutarları sayı olarak döndür.

- Para birimini ayrı alan olarak döndür.

- KDV oranlarını varsa satır bazında çıkar.

- Cevabı sadece geçerli JSON olarak ver.

- JSON dışında açıklama yazma.

Çıkarılacak alanlar:

{

  "invoiceNumber": null,

  "invoiceDate": null,

  "supplierName": null,

  "supplierTaxNumber": null,

  "taxOffice": null,

  "customerName": null,

  "customerTaxNumber": null,

  "currency": "TRY",

  "subtotalAmount": null,

  "vatAmount": null,

  "totalAmount": null,

  "lineItems": [

    {

      "description": null,

      "quantity": null,

      "unitPrice": null,

      "vatRate": null,

      "lineTotal": null

    }

  ],

  "confidence": 0.0,

  "warnings": []

}

Fatura metni:

{{INVOICE_TEXT}}