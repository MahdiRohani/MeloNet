# MeloNet

پلتفرم موسیقی اندروید با بک‌اند اختصاصی: پخش آنلاین، کتابخانه شخصی، چت هم‌زمان، کارائوکه، و تبدیل صدای خواننده (Voice Cover) با هوش مصنوعی.

<p align="center">
  <img src="docs/media/player.png" alt="MeloNet player" width="280" />
  &nbsp;&nbsp;
  <img src="docs/media/following.png" alt="MeloNet following" width="280" />
</p>

<p align="center">
  <b>پلیر کامل</b> با ویژوالایزر و کنترل‌های پیشرفته &nbsp;·&nbsp; <b>Following</b> برای هنرمندان و افراد
</p>

---

## دمو

ویدیوی کوتاه از جریان استفاده در اپ:

<video src="docs/media/demo.mp4" controls width="360" poster="docs/media/player.png">
  مرورگر شما تگ ویدیو را پشتیبانی نمی‌کند —
  <a href="docs/media/demo.mp4">دانلود / تماشای demo.mp4</a>
</video>

اگر پیش‌نمایش ویدیو در GitHub نشان داده نشد، فایل را از مسیر زیر باز کنید:

[`docs/media/demo.mp4`](docs/media/demo.mp4)

---

## قابلیت‌ها

### پخش و تجربه شنیدن
- **پلیر تمام‌صفحه** با کاور آلبوم، گرادیان پالت‌محور، و **ویژوالایزر صوتی**
- کنترل‌های کامل: پخش/توقف، قبلی/بعدی، shuffle، repeat، seek
- **مینی‌پلیر** در پایین اپ + انتقال نرم به پلیر کامل (shared element)
- سرعت پخش (مثلاً `1x`)، **خواب‌تایمر**، و **اکولایزر**
- دانلود برای شنیدن آفلاین (Premium)
- همگام‌سازی با MediaSession / نوتیفیکیشن سیستم

### کاتالوگ و کشف
- خانه با پیشنهادها و ترندها
- جستجو در آهنگ‌ها و هنرمندان
- صفحه جزئیات هنرمند و آهنگ
- کاتالوگ دسته‌بندی‌شده

### کتابخانه شخصی
- لایک آهنگ‌ها، اخیراً پخش‌شده‌ها
- پلی‌لیست‌های کاربر (ساخت، جزئیات، افزودن آهنگ)
- **موسیقی لوکال** از حافظه گوشی
- مدیریت دانلودها

### اجتماعی
- **Following** — افراد و هنرمندان، با تب جدا و جستجو داخل لیست
- پروفایل کاربر / ویرایش پروفایل
- اشتراک‌گذاری آهنگ (داخل چت یا خارج از اپ)

### چت هم‌زمان
- لیست مکالمات و چت یک‌به‌یک
- ارسال پیام و اشتراک آهنگ داخل چت
- اتصال **WebSocket** برای پیام لحظه‌ای و وضعیت آنلاین/آفلاین

### کارائوکه
- انتخاب آهنگ، ضبط با میکروفون
- مدیریت takeها و پخش ضبط‌ها

### Voice Cover (AI)
- انتخاب آهنگ و مدل صدای هنرمند
- صف پردازش در بک‌اند (Demucs → RVC → mix)
- پخش کاور آماده داخل اپ

### حساب کاربری و تنظیمات
- ثبت‌نام / ورود با JWT
- تنظیمات اپ، پشتیبانی RTL و رشته‌های فارسی/انگلیسی
- حالت Premium برای دانلود آفلاین

---

## معماری مخزن

```text
MeloNet/
├── melonet-android/     # اپلیکیشن Android (Kotlin + Jetpack Compose)
├── melonet-backend/     # API (Go) + Voice Worker (Python)
└── docs/media/          # اسکرین‌شات و دمو برای README
```

| بخش | نقش |
|-----|-----|
| **melonet-android** | کلاینت موبایل، UI، پخش، کش محلی |
| **melonet-backend** | REST API، WebSocket چت، استریم، استوریج |
| **voice-worker** | جاب‌های Voice Cover روی Redis |

---

## تکنولوژی‌ها

### Android (`melonet-android`)

| حوزه | تکنولوژی |
|------|-----------|
| زبان | **Kotlin 2.2** |
| UI | **Jetpack Compose** + **Material 3** |
| معماری | MVI-style (Contract / ViewModel / Effect)، Navigation Compose |
| DI | **Koin** |
| شبکه | **Retrofit**، **OkHttp**، Gson |
| هم‌زمانی | **Kotlin Coroutines** + Flow |
| پخش | **Media3 (ExoPlayer)** + MediaSession |
| دیتابیس محلی | **Room** |
| لیست‌های بزرگ | **Paging 3** |
| ترجیحات | **DataStore** |
| تصویر | **Coil** + **Palette** (گرادیان پلیر) |
| پس‌زمینه | **WorkManager** (دانلود) |
| سریال‌سازی مسیرها | **Kotlinx Serialization** |
| تست | JUnit، MockK، Turbine، Espresso |

### Backend API (`melonet-backend`)

| حوزه | تکنولوژی |
|------|-----------|
| زبان | **Go 1.22+** |
| HTTP | **Gin** |
| دیتابیس | **PostgreSQL 16** + golang-migrate |
| کش / صف / presence | **Redis** |
| استوریج فایل | **MinIO** (S3-compatible) |
| احراز هویت | **JWT** (golang-jwt) |
| چت | **Gorilla WebSocket** |
| درایور DB | **pgx** |
| کاتالوگ صوتی | پروکسی استریم (از جمله مسیرهای Audius) |
| ارکستراسیون | **Docker Compose** |

### Voice Cover Worker

| حوزه | تکنولوژی |
|------|-----------|
| زبان | **Python** |
| صف | Redis list (`voice_cover:jobs`) |
| جداسازی ترک | **Demucs** |
| تبدیل صدا | **RVC** (`rvc-python`) |
| آپلود نتیجه | MinIO |
| وضعیت جاب | PostgreSQL |

### زیرساخت و ابزار

- Android Gradle Plugin 8.13، KSP  
- Flavors: `dev` / `staging` / `prod`  
- Makefile برای `docker-up`، seed، sync آدرس API روی LAN/USB  
- LRCLIB برای اشعار همگام‌شده در کلاینت  

---

## شروع سریع

### ۱) بک‌اند

```bash
cd melonet-backend
make docker-up      # Postgres + Redis + MinIO + API (+ voice-worker)
make docker-seed    # اختیاری: داده نمونه
```

API روی `http://localhost:8080`.

جزئیات بیشتر: [`melonet-backend/README.md`](melonet-backend/README.md)

### ۲) اندروید

```bash
cd melonet-android
# در local.properties آدرس API را تنظیم کنید، مثلاً:
# melonet.devApiBaseUrl=http://127.0.0.1:8080/
```

در Android Studio واریانت **`devDebug`** (یا `devRelease` با `adb reverse`) را بسازید و روی دستگاه/امولاتور نصب کنید.

برای گوشی فیزیکی با USB:

```bash
adb reverse tcp:8080 tcp:8080
```

> نسخهٔ `dev` به بک‌اند لوکال وابسته است؛ برای تست واقعی API باید روی لپتاپ بالا باشد.

---

## اسکرین‌شات‌ها

| پلیر | Following |
|------|-----------|
| ![Player](docs/media/player.png) | ![Following](docs/media/following.png) |

- **Player:** کاور، ویژوالایزر، لایک/شیر، seek، shuffle/repeat، دانلود، خواب‌تایمر، سرعت، اکولایزر  
- **Following:** جستجو، تب People / Artists، گرید هنرمندان دنبال‌شده  

---

## وضعیت پروژه

این مخزن شامل کلاینت اندروید و بک‌اند کامل برای توسعه و دموی لوکال است. سرورهای `staging` / `prod` در صورت استقرار جداگانه از طریق flavorهای اپ قابل انتخاب‌اند.

---

## لایسنس

در صورت نیاز، فایل لایسنس را به ریشهٔ مخزن اضافه کنید.
