# WatchList — Android Movie Tracker

Aplikasi Android untuk mencatat daftar film yang ingin ditonton, sedang ditonton, atau sudah ditonton.
Dibangun dengan **Kotlin + Jetpack Compose + Hilt + Retrofit**.

---

## Fitur

- **Autentikasi** — Register & Login (token disimpan di SharedPreferences)
- **Watchlist** — Tambah, edit, hapus film dengan filter status
- **Status Tonton** — Tiga status warna:
  - 🔵 Sedang Ditonton (`urgency=low`)
  - 🟣 Belum Ditonton (`urgency=medium`)
  - 🟢 Sudah Ditonton (`urgency=high`)
- **Poster film** — Upload & tampilkan cover (dikompresi otomatis)
- **Pagination** — Infinite scroll 10 item/halaman dengan deduplication
- **Profil** — Edit nama, username, password, bio, foto profil

---

## Struktur Proyek

```
app/src/main/java/org/delcom/watchlist/
├── App.kt                          # @HiltAndroidApp
├── MainActivity.kt                 # Entry point
├── helper/
│   └── Helpers.kt                  # ToolsHelper, ImageCompressHelper, RouteHelper
├── module/
│   └── AppModule.kt                # Hilt @Module
├── network/
│   ├── data/Models.kt              # Data classes + WatchStatus enum
│   └── service/
│       ├── WatchListApiService.kt  # Retrofit interface
│       └── WatchListRepository.kt  # Repository + OkHttp setup
├── prefs/
│   └── AuthTokenPref.kt            # SharedPreferences wrapper
└── ui/
    ├── WatchListApp.kt             # NavHost root composable
    ├── components/
    │   ├── MovieItemUI.kt          # List item card
    │   ├── NavComponents.kt        # TopBar, BottomNav, Snackbar
    │   └── WatchStatusBadge.kt     # Badge + Selector
    ├── screens/
    │   ├── auth/
    │   │   ├── LoginScreen.kt
    │   │   └── RegisterScreen.kt
    │   ├── home/
    │   │   ├── HomeScreen.kt       # Dashboard + statistik
    │   │   └── ProfileScreen.kt
    │   └── movies/
    │       ├── MovieListScreen.kt  # Daftar + filter chips
    │       ├── MovieAddScreen.kt
    │       ├── MovieDetailScreen.kt
    │       └── MovieEditScreen.kt
    ├── theme/
    │   ├── Color.kt                # CinemaRed, CinemaGold, dll
    │   ├── Theme.kt
    │   └── Type.kt
    └── viewmodels/
        ├── AuthViewModel.kt
        └── MovieViewModel.kt
```

---

## Setup

### 1. Clone & buka di Android Studio

```bash
git clone <repo-url>
cd watchlist
```

Buka folder di **Android Studio Jellyfish** atau lebih baru.

### 2. Konfigurasi `local.properties`

Salin file contoh dan isi nilai yang sesuai:

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
BASE_URL=https://your.api.host/
```

> `BASE_URL` **harus diakhiri dengan `/`** (trailing slash).

### 3. Build & Run

Sync Gradle lalu jalankan di emulator atau device fisik (API 24+).

```bash
./gradlew assembleDebug
```

---

## Konvensi Penting

### Penyimpanan Tahun Rilis

Backend tidak memiliki field terpisah untuk tahun. Tahun disimpan sebagai prefix di field `description`:

```
[2014] Sebuah perjalanan melintasi lubang cacing...
```

Helper `ResponseMovieData.releaseYear` mengekstrak `"2014"`,  
dan `cleanDescription` mengembalikan deskripsi tanpa prefix tersebut.

### Mapping Status → Urgency

| Status UI        | `urgency` (API) |
|------------------|-----------------|
| Sedang Ditonton  | `low`           |
| Belum Ditonton   | `medium`        |
| Sudah Ditonton   | `high`          |

### URL Cover

```
{BASE_URL}images/todos/{movieId}?t={updatedAt}
```

Parameter `t` digunakan sebagai cache-buster agar Coil selalu memuat gambar terbaru setelah update.

---

## Dependensi Utama

| Library | Versi | Kegunaan |
|---|---|---|
| Jetpack Compose BOM | 2024.08.00 | UI framework |
| Navigation Compose | 2.7.7 | Navigasi antar screen |
| Hilt | 2.51.1 | Dependency Injection |
| Retrofit + Gson | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP layer + logging |
| Coil Compose | 2.7.0 | Image loading async |
| ExifInterface | 1.3.7 | Fix rotasi JPEG |

---

## Endpoint API

Semua endpoint memerlukan header `Authorization: Bearer <token>`.

| Method | Path | Deskripsi |
|--------|------|-----------|
| POST | `/auth/register` | Daftar akun |
| POST | `/auth/login` | Login, returns `authToken` + `refreshToken` |
| DELETE | `/auth/logout` | Logout |
| GET | `/users/me` | Profil user |
| PUT | `/users/me` | Update nama & username |
| PUT | `/users/me/password` | Ganti password |
| PUT | `/users/me/about` | Update bio |
| PUT | `/users/me/photo` | Upload foto profil |
| GET | `/todos` | List film (pagination, filter urgency) |
| POST | `/todos` | Tambah film |
| GET | `/todos/{id}` | Detail film |
| PUT | `/todos/{id}` | Edit film |
| DELETE | `/todos/{id}` | Hapus film |
| PUT | `/todos/{id}/cover` | Upload poster |
| GET | `/todos/stats` | Statistik (total/done/pending) |
