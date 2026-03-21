package org.delcom.watchlist.network.service

import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.delcom.watchlist.BuildConfig
import org.delcom.watchlist.network.data.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

// ═════════════════════════════════════════════════════════════════════════════
// Repository Interface
// ═════════════════════════════════════════════════════════════════════════════

interface IWatchListRepository {
    // Auth
    suspend fun postRegister(request: RequestAuthRegister): ResponseMessage<ResponseAuthRegister?>
    suspend fun postLogin(request: RequestAuthLogin): ResponseMessage<ResponseAuthLogin?>
    suspend fun postLogout(request: RequestAuthLogout): ResponseMessage<String?>
    suspend fun postRefreshToken(request: RequestAuthRefreshToken): ResponseMessage<ResponseAuthLogin?>

    // User
    suspend fun getUserMe(authToken: String): ResponseMessage<ResponseUser?>
    suspend fun putUserMe(authToken: String, request: RequestUserChange): ResponseMessage<String?>
    suspend fun putUserMePassword(authToken: String, request: RequestUserChangePassword): ResponseMessage<String?>
    suspend fun putUserMePhoto(authToken: String, file: MultipartBody.Part): ResponseMessage<String?>
    suspend fun putUserMeAbout(authToken: String, request: RequestUserAbout): ResponseMessage<String?>

    // Movies
    suspend fun getMovieStats(authToken: String): ResponseMessage<ResponseStats?>
    suspend fun getMovies(
        authToken: String,
        search: String? = null,
        page: Int = 1,
        perPage: Int = 10,
        isDone: Boolean? = null,
        urgency: String? = null,
    ): ResponseMessage<ResponseMoviesPaginated?>
    suspend fun postMovie(authToken: String, request: RequestMovie): ResponseMessage<ResponseMovieAdd?>
    suspend fun getMovieById(authToken: String, movieId: String): ResponseMessage<ResponseMovie?>
    suspend fun putMovie(authToken: String, movieId: String, request: RequestMovie): ResponseMessage<String?>
    suspend fun putMovieCover(authToken: String, movieId: String, file: MultipartBody.Part): ResponseMessage<String?>
    suspend fun deleteMovie(authToken: String, movieId: String): ResponseMessage<String?>
}

// ═════════════════════════════════════════════════════════════════════════════
// Repository Implementation
// ═════════════════════════════════════════════════════════════════════════════

class WatchListRepository(
    private val apiService: WatchListApiService,
) : IWatchListRepository {

    /**
     * Membungkus setiap API call dengan error handling terpusat.
     * HTTP error body di-parse ulang untuk mendapat pesan dari server.
     */
    private suspend fun <T> safe(call: suspend () -> ResponseMessage<T>): ResponseMessage<T> {
        return try {
            call()
        } catch (e: retrofit2.HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val msg = runCatching {
                Gson().fromJson(body, ResponseMessage::class.java)?.message
                    ?: "Server error (${e.code()})"
            }.getOrDefault("Server error (${e.code()})")
            ResponseMessage(status = "error", message = msg, data = null)
        } catch (e: Exception) {
            ResponseMessage(status = "error", message = e.message ?: "Unknown error", data = null)
        }
    }

    // Prefix "Bearer " ditambahkan di sini agar API Service tetap bersih
    private fun String.bearer() = "Bearer $this"

    // ── Auth ──────────────────────────────────────────────────────────────────

    override suspend fun postRegister(request: RequestAuthRegister) =
        safe { apiService.postRegister(request) }

    override suspend fun postLogin(request: RequestAuthLogin) =
        safe { apiService.postLogin(request) }

    override suspend fun postLogout(request: RequestAuthLogout) =
        safe { apiService.postLogout(request) }

    override suspend fun postRefreshToken(request: RequestAuthRefreshToken) =
        safe { apiService.postRefreshToken(request) }

    // ── User ──────────────────────────────────────────────────────────────────

    override suspend fun getUserMe(authToken: String) =
        safe { apiService.getUserMe(authToken.bearer()) }

    override suspend fun putUserMe(authToken: String, request: RequestUserChange) =
        safe { apiService.putUserMe(authToken.bearer(), request) }

    override suspend fun putUserMePassword(authToken: String, request: RequestUserChangePassword) =
        safe { apiService.putUserMePassword(authToken.bearer(), request) }

    override suspend fun putUserMePhoto(authToken: String, file: MultipartBody.Part) =
        safe { apiService.putUserMePhoto(authToken.bearer(), file) }

    override suspend fun putUserMeAbout(authToken: String, request: RequestUserAbout) =
        safe { apiService.putUserMeAbout(authToken.bearer(), request) }

    // ── Movies ────────────────────────────────────────────────────────────────

    override suspend fun getMovieStats(authToken: String) =
        safe { apiService.getMovieStats(authToken.bearer()) }

    override suspend fun getMovies(
        authToken: String, search: String?, page: Int, perPage: Int,
        isDone: Boolean?, urgency: String?,
    ) = safe { apiService.getMovies(authToken.bearer(), search, page, perPage, isDone, urgency) }

    override suspend fun postMovie(authToken: String, request: RequestMovie) =
        safe { apiService.postMovie(authToken.bearer(), request) }

    override suspend fun getMovieById(authToken: String, movieId: String) =
        safe { apiService.getMovieById(authToken.bearer(), movieId) }

    override suspend fun putMovie(authToken: String, movieId: String, request: RequestMovie) =
        safe { apiService.putMovie(authToken.bearer(), movieId, request) }

    override suspend fun putMovieCover(authToken: String, movieId: String, file: MultipartBody.Part) =
        safe { apiService.putMovieCover(authToken.bearer(), movieId, file) }

    override suspend fun deleteMovie(authToken: String, movieId: String) =
        safe { apiService.deleteMovie(authToken.bearer(), movieId) }
}

// ═════════════════════════════════════════════════════════════════════════════
// App Container
// ═════════════════════════════════════════════════════════════════════════════

interface IWatchListAppContainer {
    val repository: IWatchListRepository
}

class WatchListAppContainer : IWatchListAppContainer {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    /**
     * OkHttp dengan SSL bypass untuk server self-signed (development only).
     * Ganti dengan konfigurasi SSL yang proper untuk production.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val apiService: WatchListApiService by lazy {
        retrofit.create(WatchListApiService::class.java)
    }

    override val repository: IWatchListRepository by lazy {
        WatchListRepository(apiService)
    }
}