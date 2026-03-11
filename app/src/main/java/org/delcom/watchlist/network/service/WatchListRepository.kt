package org.delcom.watchlist.network.service

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.delcom.watchlist.BuildConfig
import org.delcom.watchlist.network.data.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ── Repository Interface ──────────────────────────────────────────────────────

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
        urgency: String? = null
    ): ResponseMessage<ResponseMoviesPaginated?>
    suspend fun postMovie(authToken: String, request: RequestMovie): ResponseMessage<ResponseMovieAdd?>
    suspend fun getMovieById(authToken: String, movieId: String): ResponseMessage<ResponseMovie?>
    suspend fun putMovie(authToken: String, movieId: String, request: RequestMovie): ResponseMessage<String?>
    suspend fun putMovieCover(authToken: String, movieId: String, file: MultipartBody.Part): ResponseMessage<String?>
    suspend fun deleteMovie(authToken: String, movieId: String): ResponseMessage<String?>
}

// ── Repository Implementation ─────────────────────────────────────────────────

class WatchListRepository(private val apiService: WatchListApiService) : IWatchListRepository {

    private suspend fun <T> safe(call: suspend () -> ResponseMessage<T>): ResponseMessage<T> {
        return try {
            call()
        } catch (e: retrofit2.HttpException) {
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val msg = try {
                com.google.gson.Gson().fromJson(body, ResponseMessage::class.java)?.message
                    ?: "Server error (${e.code()})"
            } catch (_: Exception) { "Server error (${e.code()})" }
            ResponseMessage<T>(status = "error", message = msg, data = null)
        } catch (e: Exception) {
            ResponseMessage<T>(status = "error", message = e.message ?: "Unknown error", data = null)
        }
    }

    override suspend fun postRegister(request: RequestAuthRegister) = safe { apiService.postRegister(request) }
    override suspend fun postLogin(request: RequestAuthLogin) = safe { apiService.postLogin(request) }
    override suspend fun postLogout(request: RequestAuthLogout) = safe { apiService.postLogout(request) }
    override suspend fun postRefreshToken(request: RequestAuthRefreshToken) = safe { apiService.postRefreshToken(request) }

    override suspend fun getUserMe(authToken: String) = safe { apiService.getUserMe("Bearer $authToken") }
    override suspend fun putUserMe(authToken: String, request: RequestUserChange) = safe { apiService.putUserMe("Bearer $authToken", request) }
    override suspend fun putUserMePassword(authToken: String, request: RequestUserChangePassword) = safe { apiService.putUserMePassword("Bearer $authToken", request) }
    override suspend fun putUserMePhoto(authToken: String, file: MultipartBody.Part) = safe { apiService.putUserMePhoto("Bearer $authToken", file) }
    override suspend fun putUserMeAbout(authToken: String, request: RequestUserAbout) = safe { apiService.putUserMeAbout("Bearer $authToken", request) }

    override suspend fun getMovieStats(authToken: String) = safe { apiService.getMovieStats("Bearer $authToken") }
    override suspend fun getMovies(authToken: String, search: String?, page: Int, perPage: Int, isDone: Boolean?, urgency: String?) =
        safe { apiService.getMovies("Bearer $authToken", search, page, perPage, isDone, urgency) }
    override suspend fun postMovie(authToken: String, request: RequestMovie) = safe { apiService.postMovie("Bearer $authToken", request) }
    override suspend fun getMovieById(authToken: String, movieId: String) = safe { apiService.getMovieById("Bearer $authToken", movieId) }
    override suspend fun putMovie(authToken: String, movieId: String, request: RequestMovie) = safe { apiService.putMovie("Bearer $authToken", movieId, request) }
    override suspend fun putMovieCover(authToken: String, movieId: String, file: MultipartBody.Part) = safe { apiService.putMovieCover("Bearer $authToken", movieId, file) }
    override suspend fun deleteMovie(authToken: String, movieId: String) = safe { apiService.deleteMovie("Bearer $authToken", movieId) }
}

// ── App Container ─────────────────────────────────────────────────────────────

interface IWatchListAppContainer {
    val repository: IWatchListRepository
}

class WatchListAppContainer : IWatchListAppContainer {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(loggingInterceptor)
        connectTimeout(2, TimeUnit.MINUTES)
        readTimeout(2, TimeUnit.MINUTES)
        writeTimeout(2, TimeUnit.MINUTES)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val apiService: WatchListApiService by lazy {
        retrofit.create(WatchListApiService::class.java)
    }

    override val repository: IWatchListRepository by lazy {
        WatchListRepository(apiService)
    }
}
