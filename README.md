# Takım Yoklama

Futbol okulu yoklama uygulaması. Kotlin Multiplatform + Compose Multiplatform ile
tek kod tabanından Android ve iOS.

Sadece koçlar giriş yapar. Koç kendi kadrosunu yönetir, her antrenman için yoklama
alır ve gelmeyen öğrencileri tek ekranda görür.

## Teknoloji

| Katman | Seçim |
|---|---|
| UI | Compose Multiplatform 1.9.3 (paylaşılan) |
| Dil | Kotlin 2.2.21 |
| Derleme | Gradle 8.14.3 / AGP 8.13.2 |
| Kimlik doğrulama | Firebase Authentication (e-posta/şifre) |
| Veritabanı | Cloud Firestore (europe-west3) |
| Firebase KMP köprüsü | GitLive firebase-kotlin-sdk 2.7.0 |
| DI | Koin 4.1.1 |
| Navigasyon | androidx.navigation (KMP) 2.9.2 |

## Kurulum

1. `local.properties` içine Android SDK yolu (`sdk.dir=...`) yazılı olmalı.
2. Firebase Console'dan indirilen `google-services.json` dosyasını `composeApp/`
   klasörüne koy. Bu dosya `.gitignore`'da — repoya girmez.
3. iOS için `GoogleService-Info.plist` dosyası `iosApp/` klasörüne konur.

## Derleme

```bash
./gradlew :composeApp:assembleDebug        # Android APK
```

## Firestore

- `firestore.rules` — güvenlik kuralları. Firebase Console > Firestore > Rules'a yapıştırılır.
- `firestore.indexes.json` — gereken composite index'ler.

Veri modeli ve faz planı için `docs/PLAN.md` dosyasına bak.

## Lisans

MIT — ayrıntılar için `LICENSE` dosyasına bak.
