# Baş Zincir - Teklif (Android Uygulaması)

Bu proje, saha içinde/dışında hızlıca müşteri teklifi hazırlayıp PDF olarak
telefona indirebileceğiniz basit bir Android uygulamasıdır. Veriler
(ürünler, müşteriler, geçmiş teklifler) Google **Firebase Firestore**
üzerinde tutulur; APK ise **GitHub Actions** ile otomatik derlenir.
Bilgisayarda kuruluma gerek yoktur.

## Uygulamanın yaptıkları

- **Teklif Hazırla**: Müşteri seç (arama kutulu), ürün ekle (arama kutulu,
  fiyat listesindeki 536 ürün + kendi eklediğiniz manuel ürünler), adet ve
  birim fiyatı elle değiştirebilme, iskonto yüzdesi girince tutar otomatik
  hesaplanır, %20 KDV otomatik eklenir, teklif geçerlilik tarihi otomatik
  hesaplanır (ertesi iş günü saat 17:00, Cuma günü hazırlanan tekliflerde
  Pazartesi saat 17:00). Her satırın altında **"Bu ürüne iskonto uygula"**
  onay kutusu bulunur — örneğin genel iskontoyu 8 ürüne uygulayıp, fiyatını
  zaten elle/pazarlıklı girdiğiniz 3 ürünü bu kutunun işaretini kaldırarak
  iskonto hesabının dışında tutabilirsiniz. İskonto tutarı sadece işaretli
  (iskontoya tabi) ürünlerin toplamı üzerinden hesaplanır; genel toplam ve
  KDV yine tüm ürünleri kapsar. **KDV oranı** için %20 (standart) veya %0
  (KDV'siz teklif) seçeneklerinden biri işaretlenir; varsayılan %20'dir.
- **PDF Oluştur**: Teklifi "**Müşteri Adı – Teklif.pdf**" adıyla telefonun
  İndirilenler klasörüne kaydeder.
- **Eski Teklifler**: Daha önce kaydedilen tüm teklifleri listeler, üzerine
  dokunarak açıp düzenleyebilir ve tekrar PDF alabilirsiniz.
- **Ürün / Müşteri Yönetimi**: Ana ekrandaki bu bağlantıdan, Firebase
  Console'a hiç girmeden uygulama içinden ürün/müşteri ekleyebilir,
  düzenleyebilir veya silebilirsiniz. Ayrıca teklif hazırlarken "Manuel
  Ekle" ile eklediğiniz bir ürün, o teklife eklenmesinin yanı sıra kalıcı
  ürün listesine de otomatik kaydedilir (kategorisi "Manuel Eklenen Ürün"
  olarak işaretlenir, dilerseniz Yönetim ekranından kategori/detaylarını
  sonradan düzenleyebilirsiniz).

## 1) Firebase projesi oluşturma (10 dakika, tek seferlik)

1. https://console.firebase.google.com adresine gidip Google hesabınızla
   giriş yapın.
2. **"Proje ekle"** ile yeni bir proje oluşturun (örn. "Bas Zincir Teklif").
   Google Analytics'i isterseniz kapatabilirsiniz, gerekli değil.
3. Proje açıldıktan sonra sol menüden **Build > Firestore Database**'e girin,
   **"Veritabanı oluştur"** deyin. Konum olarak Türkiye'ye yakın bir bölge
   (örn. `europe-west1` / `eur3`) seçin. Güvenlik kuralı olarak "Test modu"
   seçebilirsiniz, birazdan adım 6'daki güvenli kuralla değiştireceğiz.
4. **Giriş (Authentication) etkinleştirme:** Sol menüden
   **Build > Authentication**'a girin, **"Get started"**'a tıklayın.
   **"Sign-in method"** sekmesinden **E-posta/Şifre (Email/Password)**
   sağlayıcısını etkinleştirin.
5. **Yalnızca izinli kişilerin giriş yapabileceği hesapları oluşturma:**
   Aynı ekranda **"Users"** sekmesine geçip **"Add user"** ile aşağıdaki
   4 e-posta için birer hesap oluşturun (şifreyi siz belirlersiniz,
   ilgili kişiye ayrıca iletirsiniz):
   - pdrercumentessiz@gmail.com
   - baszincirosb@gmail.com
   - baszincir@gmail.com
   - pazarlama2@baszincir.com.tr

   Uygulamada kayıt ol (sign up) ekranı **yoktur** — sadece bu 4 hesaptan
   biriyle giriş yapılabilir, başka hiç kimse hesap oluşturamaz.
6. **Firestore güvenlik kuralları:** Firestore Database ekranında
   **"Rules"** (Kurallar) sekmesine girip mevcut kuralın tamamını silip
   yerine şunu yapıştırın ve **"Publish"**'e basın:

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null &&
           request.auth.token.email.lower() in [
             'pdrercumentessiz@gmail.com',
             'baszincirosb@gmail.com',
             'baszincir@gmail.com',
             'pazarlama2@baszincir.com.tr'
           ];
       }
     }
   }
   ```

   Bu kuralla, yukarıdaki 4 e-postadan biriyle giriş yapmış olmayan
   **hiç kimse** (API anahtarlarını ele geçirse bile) veriye okuma veya
   yazma erişimi elde edemez. Uygulama artık açılışta bir giriş ekranı
   gösterecek; sadece bu 4 hesaptan biriyle giriş yapabilirsiniz. Ana
   ekrandaki **"Çıkış Yap"** ile istediğiniz an oturumu kapatabilirsiniz.
7. Sol üstteki dişli simgesinden **"Proje ayarları"**na girin, aşağıda
   **"Uygulamalarınız"** bölümünden **Android simgesine** tıklayıp yeni
   bir Android uygulaması ekleyin:
   - **Android paket adı:** `com.baszincir.teklif`  (bu proje ile birebir
     aynı olmalı, değiştirmeyin)
   - Takma ad: "Baş Zincir Teklif" (opsiyonel)
   - SHA-1: boş bırakabilirsiniz (gerekli değil)
8. **`google-services.json`** dosyasını indirin.
9. Bu dosyayı, projenin **`app/`** klasörünün içine, yani
   `app/google-services.json` olacak şekilde koyun (bu depoya / GitHub
   reponuza bu dosyayı da yükleyeceksiniz — bkz. adım 2).

## 2) GitHub'a yükleme ve APK derleme

1. GitHub'da yeni bir **repo** oluşturun (public ya da private, ikisi de
   olur), örn. `bas-zincir-teklif`.
2. Bu klasördeki tüm dosyaları (bu README dahil) o reponun içine
   yükleyin — GitHub web arayüzünden "Add file > Upload files" ile sürükle
   bırak da yapabilirsiniz, ya da bilgisayarınızda `git` varsa:

   ```
   git init
   git add .
   git commit -m "İlk yükleme"
   git branch -M main
   git remote add origin https://github.com/KULLANICI_ADINIZ/bas-zincir-teklif.git
   git push -u origin main
   ```

3. Az önce indirdiğiniz **`google-services.json`** dosyasını da `app/`
   klasörünün içine ekleyip aynı şekilde GitHub'a yükleyin/commit edin.
4. GitHub reponuzda üstteki **"Actions"** sekmesine girin. `main` dalına her
   yükleme/push yaptığınızda **"APK Derle"** iş akışı otomatik başlar
   (ilk yüklemede de otomatik başlar). Dilerseniz "Run workflow" butonuyla
   elle de tetikleyebilirsiniz.
5. İşlem yeşil tik ile bittiğinde, o çalıştırmanın sayfasına girip en
   altta **"Artifacts"** bölümünden **`BasZincirTeklif-apk`** dosyasını
   indirin. İçinden **`app-debug.apk`** çıkacak — bu, telefonunuza
   kuracağınız dosyadır.

## 3) APK'yi telefona kurma

1. `app-debug.apk` dosyasını telefonunuza indirin (GitHub'daki artifact
   linkini telefonunuzdan da açabilirsiniz, ya da bilgisayardan WhatsApp/
   e-posta ile kendinize gönderin).
2. Dosyaya dokununca Android "bilinmeyen kaynaklardan yükleme" izni
   isteyebilir — izin verip kuruluma devam edin.
3. Uygulama telefonunuzun ana ekranına **"Baş Zincir - Teklif"** adıyla ve
   logonuzla eklenecektir.
4. Uygulamayı ilk açtığınızda internete ihtiyaç vardır: ürün ve müşteri
   listesi bir kereliğine Firebase'e otomatik yüklenir (birkaç saniye
   sürebilir). Bu işlem sadece **ilk açılışta ve bir kez** yapılır.

## 4) Fiyat / müşteri güncelleme (uygulamayı güncellemeden!)

Fiyatlar değiştiğinde veya yeni müşteri eklemek istediğinizde APK'yi tekrar
derletmenize gerek yoktur. Bunu **iki şekilde** yapabilirsiniz:

**A) Uygulama içinden (önerilen, en pratik):** Ana ekrandaki
"Ürün / Müşteri Yönetimi" bağlantısına girin; buradan ürün/müşteri
arayabilir, üzerine dokunup düzenleyebilir, "Sil" ile kaldırabilir ya da
sağ alttaki **+** butonuyla yeni ürün/müşteri ekleyebilirsiniz. Değişiklik
anında Firebase'e kaydolur.

**B) Firebase Console üzerinden:**
1. https://console.firebase.google.com üzerinden projenize girin.
2. **Firestore Database**'i açın.
3. **`urunler`** koleksiyonunda ilgili ürün belgesini bulup `fiyat` alanını
   güncelleyin (veya yeni ürün belgesi ekleyin — alanlar: `id`, `ad`,
   `kategori`, `birim`, `fiyat`, `ekBilgi`).
4. **`musteriler`** koleksiyonuna aynı şekilde yeni müşteri ekleyebilir ya
   da mevcut kaydı (`ad`, `il` alanları) düzenleyebilirsiniz.
5. Uygulamayı kapatıp yeniden açtığınızda (ya da ürün/müşteri seçim
   ekranına her girdiğinizde) güncel veriler Firestore'dan çekilir.

## Önemli notlar

- **Fiyat listesi tekrar güncellendi.** Uygulamayı bu sürümle güncellediğinizde
  Firebase'deki mevcut ürün fiyatları otomatik olarak silinip yeni listeyle
  değiştirilir; elle bir şey yapmanıza gerek yok, sadece yeni APK'yı kurup
  uygulamayı bir kez internet bağlantısıyla açmanız yeterli.
- Teklif hazırlarken **kilo/metre ile satılan** bir ürün eklediğinizde miktar
  kutusu artık boş gelir (siz elle girersiniz); **adet ile satılan** ürünlerde
  pratik olması için "1" ile başlar.
- Teklif ekranının sol üstünde artık bir **geri oku** var; ayrıca "Kaydet"
  veya "PDF Oluştur" işlemi başarıyla tamamlandığında uygulama otomatik
  olarak bir önceki ekrana döner (bilgisayarda BlueStack gibi bir emülatörde
  fiziksel geri tuşu olmadan da rahatça kullanabilirsiniz).

- **Fiyat listesi güncellendi (v2).** Uygulamayı bu yeni sürümle güncellediğinizde,
  Firebase'deki mevcut "urunler" koleksiyonundaki eski/yanlış fiyatlar otomatik
  olarak silinip yeni fiyat listesiyle değiştirilir — elle bir şey yapmanıza
  gerek yok, sadece yeni APK'yı kurup uygulamayı bir kez internet bağlantısıyla
  açmanız yeterli (ilk açılışta birkaç saniye sürebilir). Elle eklediğiniz özel
  ürünler bu işlemden etkilenmez, olduğu gibi kalır.
- **Birimler:** Makaralı Zincir, Torba Zincirler (DIN 5685/763), Bükülü Zincir
  ve Düğümlü Zincir **kilo**; G-80 Zincir, DIN 766 Kalibre Zincir ve Florasan
  Askı Zinciri **metre**; diğer tüm ürünler **adet** ile satılır. Teklif
  hazırlarken bir ürün seçtiğinizde miktar kutusunun etiketi otomatik olarak
  "Kilogram", "Metre" veya "Adet" olarak değişir.
- **Ürünler kategorilere ayrılmış ve küçükten büyüğe sıralı olarak** görünür
  (hem teklif hazırlarken ürün seçme ekranında, hem Ürün Yönetimi'nde).
  Arama kutusuna bir şey yazdığınızda kategoriler kalkar, sadece eşleşen
  ürünler listelenir.
- **Giriş zorunludur.** Uygulama açılışta e-posta/şifre sorar; sadece
  Firebase Authentication'da tanımladığınız 4 hesap giriş yapabilir. Kayıt
  ol ekranı yoktur, şifresini unutan kişi için Firebase Console >
  Authentication > Users üzerinden şifre sıfırlayabilirsiniz.
- Bu derleme **debug APK**'dır (imzasız test paketi). Kişisel/işletme içi
  kullanım için tamamen çalışır durumdadır. İleride Play Store'a koymak
  isterseniz "release" imzalama adımı ayrıca eklenmelidir.
- **Besi Zinciri** kategorisindeki ~360 kalem, fiyat listesi PDF'indeki iç
  içe geçmiş iki sütunlu tablodan otomatik metin ayrıştırma ile çıkarılmıştır
  (fiyatlar PDF ile birebir eşleştirilerek kontrol edildi). Diğer tüm
  kategoriler elle satır satır kontrol edilerek girilmiştir. Herhangi bir
  hata görürseniz Ürün Yönetimi ekranından tek tek kolayca düzeltebilirsiniz.
- **Bağlantı Zinciri** ve **2,5 Ton Çeki Zinciri** kategorileri, talebiniz
  üzerine listeye hiç eklenmedi.
- PDF'in en üstünde şirket antetiniz (adres, vergi no, telefon, e-posta
  bilgileri) tam genişlikte görünür.
- PDF oluşturulduktan sonra çıkan pencerede **"Paylaş"** ile WhatsApp, Mail
  veya telefonunuzdaki başka bir uygulama üzerinden doğrudan gönderebilirsiniz.
- Ürün/müşteri arama ve Eski Teklifler'deki arama artık **Türkçe karaktere
  duyarlı, büyük/küçük harf farketmeksizin** çalışır (ör. "OSTİM ZİNCİR"
  kaydını küçük harflerle "ostim zincir" yazarak da bulabilirsiniz).
- Uygulamada ürün/müşteri listesi ayrı bir sekme olarak gösterilmez; sadece
  teklif hazırlarken arama kutusuyla seçilir (istediğiniz gibi).
- PDF, telefonun standart **İndirilenler** klasörüne kaydedilir; dosya
  yöneticinizden veya "İndirilenler" uygulamasından ulaşabilirsiniz.
- İnternet olmadan da (Firestore'un kendi önbelleği sayesinde) daha önce
  bir kez yüklenmiş ürün/müşteri listesiyle teklif hazırlayabilirsiniz;
  ancak yeni eklenen/değiştirilen ürün-müşteri verileri için internet
  gerekir.

## Proje yapısı (özet)

```
app/
  src/main/
    java/com/baszincir/teklif/
      data/        -> Product, Customer, Offer modelleri + Firestore erişimi
      ui/           -> Ekranlar (MainActivity, OfferActivity, ...)
      util/         -> Tarih hesaplama ve PDF üretimi
    res/            -> Ekran tasarımları (layout), renkler, ikonlar
    assets/
      products.json   -> İlk kurulumda Firestore'a yüklenecek 536 ürün
      customers.json  -> İlk kurulumda Firestore'a yüklenecek 1148 müşteri
.github/workflows/build.yml -> GitHub Actions APK derleme betiği
```

Herhangi bir adımda takılırsanız veya Besi Zinciri fiyatlarının tamamını
tekrar kontrol etmemi isterseniz, bana yazmanız yeterli.
