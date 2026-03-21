package org.delcom.watchlist

import android.app.Application
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Application class — entry point Hilt DI.
 *
 * Mengkonfigurasi Coil dengan SSL bypass agar bisa memuat gambar
 * dari server yang menggunakan self-signed certificate (development only).
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(buildUnsafeImageLoader())
    }

    /**
     * Membangun [ImageLoader] dengan OkHttp yang mengabaikan validasi SSL.
     * **Hanya untuk development** — ganti dengan konfigurasi SSL proper di production.
     */
    private fun buildUnsafeImageLoader(): ImageLoader {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }
        val unsafeClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(unsafeClient)
            .build()
    }
}