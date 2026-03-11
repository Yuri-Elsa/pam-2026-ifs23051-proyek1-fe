package org.delcom.watchlist.network.data

import kotlinx.serialization.Serializable

// ── Generic Response ──────────────────────────────────────────────────────────

@Serializable
data class ResponseMessage<T>(
    val status: String,
    val message: String,
    val data: T? = null
)

// ── Auth ──────────────────────────────────────────────────────────────────────

@Serializable
data class RequestAuthRegister(
    val name: String,
    val username: String,
    val password: String
)

@Serializable
data class RequestAuthLogin(
    val username: String,
    val password: String
)

@Serializable
data class RequestAuthLogout(
    val authToken: String
)

@Serializable
data class RequestAuthRefreshToken(
    val authToken: String,
    val refreshToken: String
)

@Serializable
data class ResponseAuthRegister(
    val userId: String
)

@Serializable
data class ResponseAuthLogin(
    val authToken: String,
    val refreshToken: String
)

// ── User ──────────────────────────────────────────────────────────────────────

@Serializable
data class ResponseUser(
    val user: ResponseUserData
)

@Serializable
data class ResponseUserData(
    val id: String,
    val name: String,
    val username: String,
    val about: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class RequestUserChange(
    val name: String,
    val username: String
)

@Serializable
data class RequestUserChangePassword(
    val newPassword: String,
    val password: String
)

@Serializable
data class RequestUserAbout(
    val about: String
)

// ── Movies (Todos) ────────────────────────────────────────────────────────────

/**
 * Watch status mapped to urgency field in backend:
 *   "watching"  → urgency = "low"    (sedang ditonton)
 *   "planned"   → urgency = "medium" (belum ditonton / rencana)
 *   "completed" → urgency = "high"   (sudah ditonton)
 */

@Serializable
data class RequestMovie(
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val urgency: String = "medium"   // "low"=watching | "medium"=planned | "high"=completed
)

@Serializable
data class ResponseMovies(
    val todos: List<ResponseMovieData>
)

@Serializable
data class ResponseMoviesPaginated(
    val todos: List<ResponseMovieData>,
    val pagination: ResponsePagination? = null
)

@Serializable
data class ResponsePagination(
    val currentPage: Int,
    val perPage: Int,
    val total: Long,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean
)

@Serializable
data class ResponseMovie(
    val todo: ResponseMovieData
)

@Serializable
data class ResponseMovieData(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val isDone: Boolean = false,
    val urgency: String? = null,          // "low"|"medium"|"high"
    val cover: String? = null,
    val createdAt: String = "",
    var updatedAt: String = ""
) {
    /** Human-readable watch status */
    val watchStatus: WatchStatus get() = when (urgency?.lowercase()) {
        "low"  -> WatchStatus.WATCHING
        "high" -> WatchStatus.COMPLETED
        else   -> WatchStatus.PLANNED
    }

    /** Extracts release year from description prefix like "[2014] ..." */
    val releaseYear: String? get() {
        return if (description.startsWith("[") && description.contains("]")) {
            val year = description.substringAfter("[").substringBefore("]")
            if (year.length == 4 && year.all { it.isDigit() }) year else null
        } else null
    }

    /** Returns description without the [YEAR] prefix */
    val cleanDescription: String get() {
        return if (releaseYear != null) {
            description.substringAfter("]").trim()
        } else description
    }
}

enum class WatchStatus(
    val label: String,
    val apiValue: String,
    val colorHex: Long,
    val bgColorHex: Long,
    val dotColorHex: Long
) {
    WATCHING(
        label     = "Sedang Ditonton",
        apiValue  = "low",
        colorHex  = 0xFF1565C0,
        bgColorHex = 0xFFE3F2FD,
        dotColorHex = 0xFF1976D2
    ),
    PLANNED(
        label     = "Belum Ditonton",
        apiValue  = "medium",
        colorHex  = 0xFF6A1B9A,
        bgColorHex = 0xFFF3E5F5,
        dotColorHex = 0xFF7B1FA2
    ),
    COMPLETED(
        label     = "Sudah Ditonton",
        apiValue  = "high",
        colorHex  = 0xFF1B5E20,
        bgColorHex = 0xFFE8F5E9,
        dotColorHex = 0xFF2E7D32
    );

    companion object {
        fun fromApiValue(value: String?) = when (value?.lowercase()) {
            "low"  -> WATCHING
            "high" -> COMPLETED
            else   -> PLANNED
        }
    }
}

@Serializable
data class ResponseMovieAdd(
    val todoId: String
)

@Serializable
data class ResponseStats(
    val stats: ResponseStatsData
)

@Serializable
data class ResponseStatsData(
    val total: Long = 0,
    val done: Long = 0,
    val pending: Long = 0
)
