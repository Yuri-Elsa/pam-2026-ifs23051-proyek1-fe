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

object ToolsHelper {
    /**
     * Bangun URL cover film.
     * Backend mengembalikan field `cover` berisi path relatif, contoh:
     *   "uploads/watchlists/948d07d9-....jpg"
     * Jadi URL lengkapnya = BASE_URL + cover
     *
     * Parameter [coverPath] = nilai `movie.cover` dari API (bisa null/blank → return null).
     * Parameter [t] = cache-buster timestamp.
     */
    fun getMovieImageUrl(coverPath: String?, t: String = "0"): String? {
        if (coverPath.isNullOrBlank()) return null
        return "${BuildConfig.BASE_URL}$coverPath?t=$t"
    }

    /**
     * URL foto profil user.
     * Pola path profil diasumsikan sama: BASE_URL + path relatif dari field photo user.
     * Jika backend belum mengembalikan field photo, pakai fallback userId.
     */
    fun getUserImageUrl(photoPath: String?, userId: String, t: String = "0"): String {
        return if (!photoPath.isNullOrBlank()) {
            "${BuildConfig.BASE_URL}$photoPath?t=$t"
        } else {
            "${BuildConfig.BASE_URL}images/users/$userId?t=$t"
        }
    }
}

object ImageCompressHelper {
    private const val MAX_WIDTH  = 1080
    private const val MAX_HEIGHT = 1080
    private const val QUALITY    = 80

    fun uriToCompressedMultipart(context: Context, uri: Uri, partName: String): MultipartBody.Part {
        val bytes = compressImage(context, uri)
        val body  = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, "image.jpg", body)
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

        opts.inSampleSize = calculateInSampleSize(opts, MAX_WIDTH, MAX_HEIGHT)
        opts.inJustDecodeBounds = false

        var bitmap = context.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, opts)!!
        }
        bitmap = fixRotation(context, uri, bitmap)
        bitmap = scaleBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        var n = 1
        val (h, w) = opts.outHeight to opts.outWidth
        if (h > reqH || w > reqW) {
            val hH = h / 2; val hW = w / 2
            while (hH / n >= reqH && hW / n >= reqW) n *= 2
        }
        return n
    }

    private fun fixRotation(context: Context, uri: Uri, bmp: Bitmap): Bitmap = try {
        val s = context.contentResolver.openInputStream(uri) ?: return bmp
        val exif = ExifInterface(s); s.close()
        val rot = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bmp
        }
        val m = Matrix().apply { postRotate(rot) }
        val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (r != bmp) bmp.recycle(); r
    } catch (e: Exception) { bmp }

    private fun scaleBitmap(bmp: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (bmp.width <= maxW && bmp.height <= maxH) return bmp
        val scale = minOf(maxW.toFloat() / bmp.width, maxH.toFloat() / bmp.height)
        val s = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        if (s != bmp) bmp.recycle(); return s
    }
}

object RouteHelper {
    const val LOGIN    = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME     = "home"
    const val MOVIES   = "movies"
    const val MOVIE_ADD    = "movies/add"
    const val MOVIE_DETAIL = "movies/{movieId}"
    const val MOVIE_EDIT   = "movies/{movieId}/edit"
    const val PROFILE  = "profile"

    fun movieDetail(id: String) = "movies/$id"
    fun movieEdit(id: String)   = "movies/$id/edit"
}