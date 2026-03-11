package org.delcom.watchlist.network.service

import okhttp3.MultipartBody
import org.delcom.watchlist.network.data.*
import retrofit2.http.*

interface WatchListApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun postRegister(@Body request: RequestAuthRegister): ResponseMessage<ResponseAuthRegister?>

    @POST("auth/login")
    suspend fun postLogin(@Body request: RequestAuthLogin): ResponseMessage<ResponseAuthLogin?>

    @POST("auth/logout")
    suspend fun postLogout(@Body request: RequestAuthLogout): ResponseMessage<String?>

    @POST("auth/refresh-token")
    suspend fun postRefreshToken(@Body request: RequestAuthRefreshToken): ResponseMessage<ResponseAuthLogin?>

    // ── Users ─────────────────────────────────────────────────────────────────

    @GET("users/me")
    suspend fun getUserMe(@Header("Authorization") authToken: String): ResponseMessage<ResponseUser?>

    @PUT("users/me")
    suspend fun putUserMe(
        @Header("Authorization") authToken: String,
        @Body request: RequestUserChange
    ): ResponseMessage<String?>

    @PUT("users/me/password")
    suspend fun putUserMePassword(
        @Header("Authorization") authToken: String,
        @Body request: RequestUserChangePassword
    ): ResponseMessage<String?>

    @Multipart
    @PUT("users/me/photo")
    suspend fun putUserMePhoto(
        @Header("Authorization") authToken: String,
        @Part file: MultipartBody.Part
    ): ResponseMessage<String?>

    @PUT("users/me/about")
    suspend fun putUserMeAbout(
        @Header("Authorization") authToken: String,
        @Body request: RequestUserAbout
    ): ResponseMessage<String?>

    // ── Movies (Todos) ────────────────────────────────────────────────────────

    @GET("todos/stats")
    suspend fun getMovieStats(@Header("Authorization") authToken: String): ResponseMessage<ResponseStats?>

    @GET("todos")
    suspend fun getMovies(
        @Header("Authorization") authToken: String,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 10,
        @Query("isDone") isDone: Boolean? = null,
        @Query("urgency") urgency: String? = null
    ): ResponseMessage<ResponseMoviesPaginated?>

    @POST("todos")
    suspend fun postMovie(
        @Header("Authorization") authToken: String,
        @Body request: RequestMovie
    ): ResponseMessage<ResponseMovieAdd?>

    @GET("todos/{movieId}")
    suspend fun getMovieById(
        @Header("Authorization") authToken: String,
        @Path("movieId") movieId: String
    ): ResponseMessage<ResponseMovie?>

    @PUT("todos/{movieId}")
    suspend fun putMovie(
        @Header("Authorization") authToken: String,
        @Path("movieId") movieId: String,
        @Body request: RequestMovie
    ): ResponseMessage<String?>

    @Multipart
    @PUT("todos/{movieId}/cover")
    suspend fun putMovieCover(
        @Header("Authorization") authToken: String,
        @Path("movieId") movieId: String,
        @Part file: MultipartBody.Part
    ): ResponseMessage<String?>

    @DELETE("todos/{movieId}")
    suspend fun deleteMovie(
        @Header("Authorization") authToken: String,
        @Path("movieId") movieId: String
    ): ResponseMessage<String?>
}
