package org.delcom.watchlist.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.delcom.watchlist.BuildConfig
import java.io.ByteArrayOutputStream

// ═════════════════════════════════════════════════════════════════════════════
// ToolsHelper — URL builder untuk aset gambar
// ═════════════════════════════════════════════════════════════════════════════

object ToolsHelper {

    /**
     * Membangun URL lengkap cover film dari path relatif yang dikembalikan API.
     *
     * @param coverPath  nilai `movie.cover` dari API, misal `"uploads/watchlists/abc.jpg"`.
     *                   Jika null/blank, mengembalikan null (tidak ada cover).
     * @param t          cache-buster timestamp; diubah setiap kali gambar diperbarui.
     */
    fun getMovieImageUrl(coverPath: String?, t: String = "0"): String? {
        if (coverPath.isNullOrBlank()) return null
        return "${BuildConfig.BASE_URL}$coverPath?t=$t"
    }

    /**
     * Membangun URL foto profil user.
     *
     * @param photoPath  path relatif foto dari API (opsional).
     * @param userId     ID user; digunakan sebagai fallback bila [photoPath] kosong.
     * @param t          cache-buster timestamp.
     */
    fun getUserImageUrl(photoPath: String?, userId: String, t: String = "0"): String {
        return if (!photoPath.isNullOrBlank()) {
            "${BuildConfig.BASE_URL}$photoPath?t=$t"
        } else {
            "${BuildConfig.BASE_URL}images/users/$userId?t=$t"
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ImageCompressHelper — kompresi & konversi gambar untuk upload
// ═════════════════════════════════════════════════════════════════════════════

object ImageCompressHelper {

    private const val MAX_WIDTH  = 1080
    private const val MAX_HEIGHT = 1080
    private const val QUALITY    = 80

    /**
     * Mengkompresi gambar dari [uri], memperbaiki rotasi EXIF, lalu membungkusnya
     * sebagai [MultipartBody.Part] siap kirim ke API.
     *
     * @param context   context untuk membuka ContentResolver.
     * @param uri       URI gambar dari galeri atau kamera.
     * @param partName  nama field multipart (misal `"file"`).
     */
    fun uriToCompressedMultipart(
        context: Context,
        uri: Uri,
        partName: String,
    ): MultipartBody.Part {
        val bytes = compressImage(context, uri)
        val body  = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, "image.jpg", body)
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        // Baca dimensi dulu tanpa decode penuh (hemat memori)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }

        opts.inSampleSize   = calculateInSampleSize(opts, MAX_WIDTH, MAX_HEIGHT)
        opts.inJustDecodeBounds = false

        var bitmap = context.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, opts)!!
        }
        bitmap = fixRotation(context, uri, bitmap)
        bitmap = scaleBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT)

        return ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            bitmap.recycle()
        }.toByteArray()
    }

    private fun calculateInSampleSize(
        opts: BitmapFactory.Options,
        reqW: Int,
        reqH: Int,
    ): Int {
        var n = 1
        val h = opts.outHeight
        val w = opts.outWidth
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / n >= reqH && halfW / n >= reqW) n *= 2
        }
        return n
    }

    private fun fixRotation(context: Context, uri: Uri, bmp: Bitmap): Bitmap = runCatching {
        val stream = context.contentResolver.openInputStream(uri) ?: return bmp
        val exif = ExifInterface(stream)
        stream.close()
        val rotation = when (
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bmp
        }
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        if (rotated != bmp) bmp.recycle()
        rotated
    }.getOrDefault(bmp)

    private fun scaleBitmap(bmp: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (bmp.width <= maxW && bmp.height <= maxH) return bmp
        val scale  = minOf(maxW.toFloat() / bmp.width, maxH.toFloat() / bmp.height)
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).toInt(),
            (bmp.height * scale).toInt(),
            true,
        )
        if (scaled != bmp) bmp.recycle()
        return scaled
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// RouteHelper — konstanta dan builder untuk rute navigasi
// ═════════════════════════════════════════════════════════════════════════════

object RouteHelper {
    // Rute statis
    const val LOGIN    = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME     = "home"
    const val MOVIES   = "movies"
    const val MOVIE_ADD    = "movies/add"
    const val MOVIE_DETAIL = "movies/{movieId}"
    const val MOVIE_EDIT   = "movies/{movieId}/edit"
    const val PROFILE  = "profile"

    // Rute dinamis
    fun movieDetail(id: String) = "movies/$id"
    fun movieEdit(id: String)   = "movies/$id/edit"
}