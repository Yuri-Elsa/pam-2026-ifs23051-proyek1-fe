# WatchList — Android Movie Tracker

Aplikasi Android untuk mencatat daftar film yang ingin ditonton, sedang ditonton, atau sudah ditonton.
Dibangun dengan **Kotlin + Jetpack Compose + Hilt + Retrofit**.

---

## Fitur

- **Autentikasi** — Register & Login (token disimpan di SharedPreferences)
- **Watchlist** — Tambah, edit, hapus film dengan filter status
- **Status Tonton** — Tiga status berwarna:
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
│
├── App.kt                              # @HiltAndroidApp + konfigurasi Coil
├── MainActivity.kt                     # Entry point; memegang 3 ViewModel
│
├── helper/
│   └── Helpers.kt                      # ToolsHelper · ImageCompressHelper · RouteHelper
│
├── module/
│   └── AppModule.kt                    # Hilt @Module (Repository, AuthTokenPref)
│
├── network/
│   ├── data/
│   │   └── Models.kt                   # Data class + WatchStatus enum
│   └── service/
│       ├── WatchListApiService.kt      # Retrofit interface
│       └── WatchListRepository.kt      # Interface + Impl + WatchListAppContainer
│
├── prefs/
│   └── AuthTokenPref.kt                # SharedPreferences wrapper (token auth)
│
└── ui/
    ├── WatchListApp.kt                 # NavHost root — routing berdasarkan session
    │
    ├── components/
    │   ├── MovieItemUI.kt              # Card item film di list
    │   ├── NavComponents.kt            # TopBar · BottomNav · Snackbar
    │   └── WatchStatusBadge.kt         # Badge status + Selector (Add/Edit)
    │
    ├── screens/
    │   ├── auth/
    │   │   ├── LoginScreen.kt
    │   │   └── RegisterScreen.kt
    │   ├── home/
    │   │   ├── HomeScreen.kt           # Dashboard + statistik + infinite scroll
    │   │   └── ProfileScreen.kt        # Profil + edit + ganti foto
    │   └── movies/
    │       ├── MovieListScreen.kt      # Daftar + filter chips + infinite scroll
    │       ├── MovieAddScreen.kt
    │       ├── MovieDetailScreen.kt    # Detail + upload poster
    │       └── MovieEditScreen.kt
    │
    ├── theme/
    │   ├── Color.kt                    # CinemaRed · CinemaGold · WatchStatus colors
    │   ├── Theme.kt
    │   └── Type.kt
    │
    └── viewmodels/
        ├── UiState.kt                  # Generic sealed interface UiState<T>
        ├── AuthViewModel.kt            # Session · Register · Login · Logout
        ├── MovieViewModel.kt           # Stats · HomeList · WatchList · Detail · CRUD
        └── ProfileViewModel.kt         # Profile · Update · Password · About · Photo
```

---

## Arsitektur ViewModel

### Sebelum (masalah lama)

```
MovieViewModel
 └── UIStateMovie (12 field sekaligus)
      ├── profile, stats, movies, movie
      ├── movieAdd, movieChange, movieDelete, movieChangeCover
      ├── profileUpdate, profilePassword, profileAbout, profilePhoto

AuthViewModel
 └── UIStateAuth
      ├── AuthUIState (sealed interface sendiri)
      ├── AuthActionUIState (sealed interface sendiri)
      └── AuthLogoutUIState (sealed interface sendiri)
```

**Masalah:** Satu ViewModel menampung terlalu banyak tanggung jawab. Setiap state punya
sealed interface sendiri yang isinya identik (`Loading`, `Success`, `Error`, `Idle`).

---

### Sesudah (struktur baru)

```
UiState<T>  ← satu sealed interface generik, dipakai di mana saja
  ├── Idle
  ├── Loading
  ├── Success(data: T)
  └── Error(message: String)

AuthViewModel         → hanya session, register, login, logout
  └── AuthUiState
       ├── session: UiState<ResponseAuthLogin>
       ├── register: ActionState   (= UiState<String>)
       └── logout: ActionState

MovieViewModel        → hanya film (stats, list, detail, CRUD)
  ├── stats:     UiState<ResponseStatsData>   (StateFlow terpisah)
  ├── homeList:  MovieListUiState             (items + pagination + loading flags)
  ├── watchList: MovieListUiState
  ├── detail:    MovieDetailUiState           (movie + cover + edit + delete)
  └── addState:  ActionState

ProfileViewModel      → hanya profil user
  └── ProfileUiState
       ├── profile:  UiState<ResponseUserData>
       ├── update:   ActionState
       ├── password: ActionState
       ├── about:    ActionState
       └── photo:    ActionState
```

---

## Konvensi Penting

### Penyimpanan Tahun Rilis

Backend tidak memiliki field terpisah untuk tahun. Tahun disimpan sebagai prefix di field `description`:

```
[2014] Sebuah perjalanan melintasi lubang cacing...
```

- `ResponseMovieData.releaseYear` → mengekstrak `"2014"`
- `ResponseMovieData.cleanDescription` → mengembalikan deskripsi tanpa prefix

### Mapping Status → Urgency

| Status UI        | `urgency` (API) | Warna  |
|------------------|-----------------|--------|
| Sedang Ditonton  | `low`           | Biru   |
| Belum Ditonton   | `medium`        | Ungu   |
| Sudah Ditonton   | `high`          | Hijau  |

### URL Gambar

**Cover film:**
```
{BASE_URL}{movie.cover}?t={updatedAt}
```
`movie.cover` adalah path relatif, misal `uploads/watchlists/abc.jpg`.

**Foto profil:**
```
{BASE_URL}images/users/{userId}?t={timestamp}
```
Parameter `?t=` digunakan sebagai cache-buster agar Coil selalu memuat ulang setelah update.

### Snackbar Format

```
"<type>|<pesan>"
```
Contoh: `"success|Film berhasil ditambahkan"`, `"error|Token tidak valid"`

Tipe yang didukung: `success` · `error` · `warning` · `info`

---

## Setup

### 1. Clone & buka di Android Studio

```bash
git clone <repo-url>
cd watchlist
```

Buka di **Android Studio Jellyfish** atau lebih baru.

### 2. Konfigurasi `local.properties`

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

```bash
./gradlew assembleDebug
```

---

## Dependensi Utama

| Library | Versi | Kegunaan |
|---------|-------|----------|
| Jetpack Compose BOM | 2024.08.00 | UI framework |
| Navigation Compose | 2.7.7 | Navigasi antar screen |
| Hilt | 2.51.1 | Dependency Injection |
| Retrofit + Gson | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP layer + logging |
| Coil Compose | 2.7.0 | Async image loading |
| ExifInterface | 1.3.7 | Fix rotasi JPEG |

---

## Endpoint API

Semua endpoint (kecuali auth) memerlukan header `Authorization: Bearer <token>`.

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
| GET | `/watchlists` | List film (pagination, filter urgency) |
| POST | `/watchlists` | Tambah film |
| GET | `/watchlists/{id}` | Detail film |
| PUT | `/watchlists/{id}` | Edit film |
| DELETE | `/watchlists/{id}` | Hapus film |
| PUT | `/watchlists/{id}/cover` | Upload poster |
| GET | `/watchlists/stats` | Statistik (total/done/pending) |

---

## Panduan Menambah Fitur Baru

### Menambah endpoint API baru

1. Tambah method di `WatchListApiService.kt`
2. Tambah method di interface `IWatchListRepository`
3. Implementasikan di `WatchListRepository` (gunakan `safe { }`)

### Menambah state baru di ViewModel

Gunakan `UiState<T>` yang sudah ada:

```kotlin
// Di ViewModel
private val _myState = MutableStateFlow<UiState<MyData>>(UiState.Idle)
val myState = _myState.asStateFlow()

fun loadMyData() {
    viewModelScope.launch {
        _myState.value = UiState.Loading
        val result = repository.getMyData()
        _myState.value = if (result.status == "success" && result.data != null)
            UiState.Success(result.data)
        else UiState.Error(result.message)
    }
}
```

```kotlin
// Di Screen (Composable)
val myState by viewModel.myState.collectAsState()

when (val s = myState) {
    is UiState.Loading      -> CircularProgressIndicator()
    is UiState.Success      -> MyContent(data = s.data)
    is UiState.Error        -> Text("Error: ${s.message}")
    is UiState.Idle         -> { /* tampilkan kosong atau tidak ada */ }
}
```

### Menambah screen baru

1. Buat file di `ui/screens/<kategori>/NamaScreen.kt`
2. Tambah konstanta rute di `RouteHelper`
3. Daftarkan `composable(...)` di `WatchListApp.kt`