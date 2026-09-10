# Futbol Takımı Yoklama Uygulaması — Plan

## Context

Sıfırdan, hem Android hem iOS'ta çalışan bir futbol okulu yoklama uygulaması kurulacak. Uygulamaya **sadece koçlar** giriş yapar; öğrencilerin kendi hesabı yoktur. Koç kendi kadrosunu oluşturur (öğrenci ekler/siler/düzenler), her antrenman için tek tek yoklama alır ve **gelmeyen öğrencileri tek ekranda net biçimde görür**.

Bu proje mevcut işlerden tamamen bağımsızdır; kendi klasöründe, kendi git reposunda geliştirilir.

**Verilen kararlar:**

| Konu | Karar |
|---|---|
| Teknoloji | Kotlin Multiplatform + Compose Multiplatform (tek kod tabanı, paylaşılan UI) |
| Veri deposu | Firebase — Authentication (e-posta/şifre) + Cloud Firestore |
| Kullanıcı modeli | Birden fazla koç; her koç yalnızca kendi öğrencilerini görür |
| Konum | `~/Documents/AndroidProjects/FutbolYoklama` |
| iOS hedefi | Şimdilik yalnızca simülatörde doğrulama |
| Ek özellikler | Devamsızlık raporu, öğrenci profil detayları, Mazeretli/Geç geldi durumu, dışa aktarma |

---

## ⚠️ Başlamadan önce: ortam durumu

Makinede kontrol ettiğim gerçek durum:

| Bileşen | Durum |
|---|---|
| Android Studio | ✅ Kurulu |
| Android SDK (platform 34, 36) | ✅ Kurulu |
| JDK 17 (JetBrains Runtime) | ✅ Kurulu — KMP için yeterli |
| Homebrew | ✅ 4.1.1 |
| Disk alanı | ✅ 661 GB boş |
| **Xcode** | ❌ **KURULU DEĞİL** — sadece Command Line Tools var (`/Library/Developer/CommandLineTools`) |
| iOS simülatörü | ❌ Yok (Xcode olmadan gelmiyor) |
| CocoaPods | ❌ Yok |

**Xcode'un kurulu olduğunu belirtmiştin ama makinede yok.** Bu bir engel değil, sadece sıralamayı belirliyor: **Android tarafının tamamı Xcode olmadan yazılıp çalıştırılabilir.** iOS derlemesi en sona (Faz 7) bırakıldı; o faza gelene kadar Xcode'u kurmuş olman yeterli.

Xcode kurulumu senin yapman gereken adımlar (şifre istediği için ben çalıştıramam):

```bash
# 1. Mac App Store'dan "Xcode" indir (~17 GB, kurulum dahil ~40 GB yer ister)
# 2. Bir kez aç, lisansı kabul et, sonra:
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
sudo xcodebuild -runFirstLaunch
xcodebuild -downloadPlatform iOS
brew install cocoapods
```

> Not: Bu uygulama emülatörde ve simülatörde sorunsuz çalışır; fiziksel cihaz gerektiren başka projelerle karıştırılmamalı.

---

## Verilerin nerede tutulacağı

### Neden Firebase?

- **Auth hazır gelir** — koç giriş/kayıt/şifre sıfırlama için tek satır sunucu kodu yazmıyoruz.
- **Veri bulutta** — telefon değişse, uygulama silinse, tablet+telefon aynı anda kullanılsa veri kaybolmaz.
- **Çevrimdışı çalışır** — Firestore mobil SDK'sında offline persistence varsayılan olarak açıktır. Sahada internet yokken yoklama alınır, cihaz internete bağlanınca kendiliğinden senkronlanır. Ek kod gerektirmez.
- **Ücretsiz** — Spark planı günde 50.000 okuma / 20.000 yazma verir. 30 kişilik kadroda günlük yoklama ≈ 35 yazma. Bu ölçekte ücretli plana hiç geçilmez.

Bölge olarak **`europe-west3` (Frankfurt)** seçilecek — Türkiye'ye en yakın Firestore bölgesi, en düşük gecikme. **Bölge sonradan değiştirilemez**, bu yüzden proje kurulumunda dikkat edilecek.

### Firestore veri modeli

Tüm belgelerde `ownerUid` alanı var; güvenlik kuralları buna dayanıyor. Böylece koçlar birbirlerinin verisini asla göremez.

```
coaches/{uid}
  name, email, teamId, createdAt

teams/{teamId}
  ownerUid, name, season, createdAt

players/{playerId}
  ownerUid, teamId
  firstName, lastName
  birthDate        (yyyy-MM-dd, opsiyonel)
  jerseyNumber     (opsiyonel)
  parentPhone      (opsiyonel)
  photoUrl         (opsiyonel — Faz 6)
  isActive         (true/false — "arşivle" için)
  stats { totalSessions, present, absent, excused, late }   ← denormalize sayaçlar
  createdAt

sessions/{sessionId}                              ← bir antrenman
  ownerUid, teamId
  date (yyyy-MM-dd), note
  presentCount, absentCount, excusedCount, lateCount, playerCount
  createdAt, updatedAt

attendance/{sessionId}_{playerId}                 ← deterministik ID
  ownerUid, teamId, sessionId, playerId
  playerName       (denormalize — rapor tek sorguda çıksın diye)
  date (yyyy-MM-dd)
  status           PRESENT | ABSENT | EXCUSED | LATE
  note             (opsiyonel — "hasta", "sınavı var")
```

**Tasarım gerekçeleri:**

- **`attendance` alt koleksiyon değil, üst düzey koleksiyon.** Böylece "son 3 ayın devamsızlıkları" tek bir sorgu (`where teamId == X and date >= ...`). Alt koleksiyon olsaydı her antrenman için ayrı okuma gerekirdi.
- **Belge ID'si `{sessionId}_{playerId}`.** Aynı öğrenciye aynı antrenmanda iki kayıt yazmak fiziksel olarak imkânsız hale gelir.
- **`players.stats` denormalize sayaçlar.** "En çok devamsızlık yapanlar" listesi tek okumayla, hiç sorgu çalıştırmadan gelir. Yoklama kaydedilirken `WriteBatch` içinde `FieldValue.increment()` ile atomik güncellenir.
- **`playerName` attendance'a kopyalanır.** Rapor ekranı 30 ayrı `players` okuması yapmadan isimleri gösterir.
- **Öğrenci silme iki seviyeli:** "Kadrodan çıkar" → `isActive=false` (geçmiş yoklamalar korunur, raporda görünmeye devam eder). "Kalıcı sil" → öğrenci + tüm attendance kayıtları silinir, geri alınamaz, ekstra onay diyaloğu ister.

### Güvenlik kuralları

`firestore.rules` dosyası olarak repoda tutulacak ve Firebase Console'a yapıştırılacak:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn()      { return request.auth != null; }
    function owns(res)       { return signedIn() && request.auth.uid == res.data.ownerUid; }
    function creatingOwn()   { return signedIn() && request.auth.uid == request.resource.data.ownerUid; }

    match /coaches/{uid} {
      allow read, write: if signedIn() && request.auth.uid == uid;
    }

    // teams, players, sessions, attendance için aynı kalıp:
    match /players/{docId} {
      allow read, delete: if owns(resource);
      allow create:       if creatingOwn();
      allow update:       if owns(resource) && creatingOwn();
    }
    // ... teams / sessions / attendance için birebir aynısı tekrarlanır
  }
}
```

Gereken composite index'ler (`firestore.indexes.json` olarak repoda):

- `players`: `teamId` ASC, `isActive` ASC, `lastName` ASC
- `sessions`: `teamId` ASC, `date` DESC
- `attendance`: `teamId` ASC, `date` ASC
- `attendance`: `playerId` ASC, `date` DESC

---

## Uygulama mimarisi

```
FutbolYoklama/
├── composeApp/
│   ├── src/commonMain/kotlin/com/senademirci/futbolyoklama/
│   │   ├── App.kt                        # NavHost, tema
│   │   ├── data/
│   │   │   ├── model/                    # Player, Team, TrainingSession,
│   │   │   │                             #   AttendanceRecord, AttendanceStatus (enum)
│   │   │   └── repository/               # AuthRepository, PlayerRepository,
│   │   │                                 #   AttendanceRepository, ReportRepository
│   │   ├── di/AppModule.kt               # Koin
│   │   └── ui/
│   │       ├── theme/                    # Renkler, tipografi (saha yeşili/beyaz)
│   │       ├── auth/                     # LoginScreen, SignUpScreen + ViewModel
│   │       ├── roster/                   # RosterScreen, PlayerEditScreen,
│   │       │                             #   PlayerDetailScreen + ViewModel
│   │       ├── attendance/               # TakeAttendanceScreen,
│   │       │                             #   SessionHistoryScreen + ViewModel
│   │       ├── report/                   # AbsenceReportScreen + ViewModel
│   │       ├── settings/                 # SettingsScreen
│   │       └── components/               # Ortak bileşenler (StatusChip, PlayerRow…)
│   ├── src/androidMain/                  # MainActivity, Application, google-services.json
│   └── src/iosMain/                      # MainViewController, Firebase init
├── iosApp/                               # Xcode projesi, SwiftUI sarmalayıcı,
│                                         #   GoogleService-Info.plist, Podfile
├── firestore.rules
├── firestore.indexes.json
└── gradle/libs.versions.toml
```

### Kütüphaneler

| Amaç | Kütüphane |
|---|---|
| UI (paylaşılan) | Compose Multiplatform 1.8.x / Kotlin 2.1.x |
| Firebase (KMP) | **GitLive** `dev.gitlive:firebase-auth`, `firebase-firestore` — altta gerçek Android ve iOS Firebase SDK'larını sarmalar |
| Navigasyon | `androidx.navigation:navigation-compose` (multiplatform sürümü) |
| DI | Koin (`koin-compose-viewmodel`) |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` (artık KMP) |
| Tarih/saat | `kotlinx-datetime` |
| Dosya seçici (Faz 6) | FileKit |

Katman akışı: `Screen (Compose)` → `ViewModel (StateFlow)` → `Repository` → `GitLive Firestore` → `Firebase`. Repository katmanı Firestore'u `Flow` olarak dinler, böylece ekranlar canlı güncellenir (bir cihazda alınan yoklama diğerinde anında görünür).

---

## Ekranlar

1. **Giriş** — e-posta + şifre, "Şifremi unuttum" (Firebase'in kendi sıfırlama e-postası).
2. **Kayıt** — koç adı, e-posta, şifre, takım adı. Kayıt anında `coaches/{uid}` ve `teams/{teamId}` birlikte oluşturulur.
3. **Kadro (ana ekran)** — öğrenci listesi (soyada göre sıralı), arama kutusu, sağ altta ➕. Satıra uzun basınca "Düzenle / Kadrodan çıkar / Kalıcı sil". Üstte büyük bir **"Yoklama Al"** butonu.
4. **Öğrenci Ekle/Düzenle** — ad, soyad, doğum tarihi, forma no, veli telefonu. (Fotoğraf Faz 6'da eklenir.)
5. **Öğrenci Profili** — bilgiler + devamsızlık özeti (`stats`'tan anında) + son 10 antrenmandaki durumu renkli rozetlerle.
6. **Yoklama Al** — tarih seçici (varsayılan bugün), kadro listesi. Her satırda 4'lü segment: **Var / Yok / İzinli / Geç**. Varsayılan hepsi "Var" gelir, koç yalnızca gelmeyenlere dokunur — en hızlı akış. Üstte canlı sayaç ("24 var · 4 yok · 2 izinli"). Kaydet → tek `WriteBatch` (session + N attendance + N stats increment).
7. **Antrenman Geçmişi** — tarihe göre ters sıralı liste, her satırda "12 Eylül · 24/30 katıldı". Dokununca geçmiş yoklama düzenlenebilir.
8. **Devamsızlık Raporu (Gelmeyenler)** — asıl istenen ekran. Tarih aralığı filtresi (Son 1 ay / 3 ay / Sezon / Özel). Devamsızlık yüzdesine göre **azalan** sıralı liste; %30 üzeri kırmızı, %15–30 turuncu ile vurgulanır. Ayrı bir sekmede "Bugün gelmeyenler" — veli telefonuna tek dokunuşla arama.
9. **Ayarlar** — takım adı, sezon, dışa aktarma, çıkış yap.

---

## Uygulama fazları

| Faz | İçerik | Sonunda ne çalışır |
|---|---|---|
| **0** | Firebase projesi + Xcode kurulumu (senin yapacağın adımlar) | — |
| **1** | KMP + Compose MP iskeleti, Gradle yapılandırması, tema, navigasyon | Android emülatörde boş ama açılan uygulama |
| **2** | Firebase bağlantısı, `AuthRepository`, Giriş/Kayıt/Çıkış ekranları, güvenlik kuralları | Koç kayıt olup giriş yapabilir |
| **3** | `Player` modeli, `PlayerRepository`, Kadro + Ekle/Düzenle/Sil ekranları | Öğrenci ekleme, düzenleme, silme çalışır |
| **4** | `TrainingSession` + `AttendanceRecord`, Yoklama Al, Antrenman Geçmişi, batch yazma | **Ana işlev tamam:** yoklama alınıyor ve saklanıyor |
| **5** | `ReportRepository`, Devamsızlık Raporu ekranı, filtreler, "bugün gelmeyenler" | Gelmeyenler net görülüyor |
| **6** | CSV dışa aktarma + paylaşım sayfası, öğrenci fotoğrafı (Firebase Storage + FileKit) | Rapor paylaşılabiliyor |
| **7** | iOS derlemesi, CocoaPods, `GoogleService-Info.plist`, simülatörde doğrulama | Uygulama iPhone simülatöründe çalışıyor |

**Dışa aktarma hakkında not:** CSV (Excel'de doğrudan açılır) paylaşılan Kotlin kodunda üretilebilir, ucuzdur — Faz 6'ya dahil. **PDF ise KMP'de paylaşılan bir çözüm sunmuyor**; Android'de `PdfDocument`, iOS'ta `UIGraphicsPDFRenderer` ile ayrı ayrı `expect/actual` yazmak gerekir. Bu tek başına Faz 6'nın süresini yaklaşık iki katına çıkarır. Önerim: **v1'de CSV ile çıkalım**, gerçekten PDF gerektiğini gördükten sonra ayrı bir faz olarak ekleyelim. Sen "olsun" dersen plana dahil ederim.

---

## Faz 0 — Senin yapman gereken adımlar

Bunları ben yapamam (Google/Apple hesabına giriş gerektiriyor):

**Firebase:**
1. [console.firebase.google.com](https://console.firebase.google.com) → **Proje ekle** → ad: `FutbolYoklama`. Google Analytics'e gerek yok, kapatabilirsin.
2. **Authentication** → Başla → **E-posta/Şifre** yöntemini etkinleştir.
3. **Firestore Database** → Veritabanı oluştur → **Production mode** → konum **`europe-west3` (Frankfurt)**. ⚠️ Konum sonradan değiştirilemez.
4. Android uygulaması ekle → paket adı: `com.senademirci.futbolyoklama` → **`google-services.json`** indir, bana ilet (veya `composeApp/` klasörüne koy).
5. iOS uygulaması ekle → bundle ID: `com.senademirci.futbolyoklama` → **`GoogleService-Info.plist`** indir (Faz 7'de lazım).

**Xcode:** Yukarıdaki "Ortam durumu" bölümündeki komutlar. Faz 7'ye kadar acelesi yok.

---

## Doğrulama

**Faz 1–6 (Android, Xcode gerekmez):**

```bash
cd ~/Documents/AndroidProjects/FutbolYoklama && ./gradlew :composeApp:assembleDebug
```

Sonra emülatörü açıp uygulamayı kurarım ve şu senaryoyu uçtan uca kendim yürütüp ekran görüntüleriyle gösteririm:

1. Kayıt ol → giriş yap
2. 5 öğrenci ekle, birinin forma numarasını değiştir, birini sil
3. Yoklama al: 3'ü var, 1'i yok, 1'i izinli → kaydet
4. Uygulamayı kapat-aç → verinin geldiğini gör
5. Ertesi güne ait ikinci bir yoklama al
6. Rapor ekranında devamsızlık yüzdelerinin doğru hesaplandığını doğrula
7. **Çevrimdışı testi:** uçak modunu aç, yoklama al, kapat → senkronlandığını gör
8. **İzolasyon testi:** ikinci bir koç hesabı aç → birinci koçun öğrencilerinin görünmediğini doğrula

**Faz 7 (iOS):** Simülatörü açıp aynı 1–6 senaryosunu iOS'ta tekrarlar, ekran görüntüleriyle gösteririm.

Ayrıca her fazda `firestore.rules` için Firebase Console'un **Rules Playground**'unda "başka koçun belgesini okuma" denemesinin reddedildiğini doğrularım.

---

## Riskler ve bilinmesi gerekenler

- **Xcode kurulu değil.** Faz 1–6 etkilenmez; Faz 7 Xcode kurulana kadar başlayamaz.
- **GitLive Firebase SDK** resmî Google ürünü değil, topluluk kütüphanesidir. KMP'de Firebase için fiilî standart ve aktif bakımlı; ama Firebase'in en yeni özellikleri buraya gecikmeli gelir. Bizim ihtiyacımız (auth + firestore CRUD) çekirdek özellikler olduğu için risk düşük.
- **iOS tarafında CocoaPods gerekir** — GitLive iOS'ta Firebase'i Pod olarak çeker. `brew install cocoapods` yeterli.
- **Kayıt ekranı herkese açık.** "Sadece koç girebilecek" isteğini, öğrencilere hesap vermeyerek karşılıyoruz — ama teknik olarak uygulamayı indiren herkes kendine hesap açabilir (kendi boş takımını görür, kimsenin verisine erişemez). Gerçekten kapalı olsun istersen kayıt ekranını kaldırıp koç hesaplarını Firebase Console'dan elle açmak gerekir; söyle, öyle yapalım.
- **Yedekleme.** Spark (ücretsiz) planda otomatik Firestore yedeği yoktur. Faz 6'daki CSV dışa aktarma pratik bir yedek görevi de görür.
