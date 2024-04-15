package eu.kanade.tachiyomi.extension.es.ikigaimangas

<<<<<<< HEAD
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayloadSeriesDto(
    val data: List<SeriesDto>,
    @SerialName("current_page")val currentPage: Int = 0,
    @SerialName("last_page") val lastPage: Int = 0,
)

@Serializable
data class SeriesDto(
    val id: Long,
    val name: String,
    val slug: String,
    val cover: String? = null,
    val type: String? = null,
    val summary: String? = null,
    val status: SeriesStatusDto? = null,
    val genres: List<FilterDto>? = null,
)

@Serializable
data class PayloadSeriesDetailsDto(
=======
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat

@Serializable
class PayloadLatestDto(
    val data: List<LatestDto>,
    @SerialName("current_page") private val currentPage: Int = 0,
    @SerialName("last_page") private val lastPage: Int = 0,
) {
    fun hasNextPage() = currentPage < lastPage
}

@Serializable
class LatestDto(
    @SerialName("series_id") private val id: Long,
    @SerialName("series_name") private val name: String,
    @SerialName("series_slug") private val slug: String,
    private val thumbnail: String? = null,
    val type: String? = null,
) {
    fun toSManga() = SManga.create().apply {
        url = "/series/comic-$slug#$id"
        title = name
        thumbnail_url = thumbnail
    }
}

@Serializable
class PayloadSeriesDto(
    val data: List<SeriesDto>,
    @SerialName("current_page") private val currentPage: Int = 0,
    @SerialName("last_page") private val lastPage: Int = 0,
) {
    fun hasNextPage() = currentPage < lastPage
}

@Serializable
class SeriesDto(
    private val id: Long,
    private val name: String,
    private val slug: String,
    private val cover: String? = null,
    val type: String? = null,
    private val summary: String? = null,
    private val status: SeriesStatusDto? = null,
    private val genres: List<FilterDto>? = null,
) {
    fun toSManga() = SManga.create().apply {
        url = "/series/comic-$slug#$id"
        title = name
        thumbnail_url = cover
    }

    fun toSMangaDetails() = SManga.create().apply {
        title = name
        thumbnail_url = cover
        description = summary
        status = parseStatus(this@SeriesDto.status?.id)
        genre = genres?.joinToString { it.name.trim() }
    }

    private fun parseStatus(statusId: Long?) = when (statusId) {
        906397890812182531, 911437469204086787 -> SManga.ONGOING
        906409397258190851 -> SManga.ON_HIATUS
        906409532796731395, 911793517664960513 -> SManga.COMPLETED
        906426661911756802, 906428048651190273, 911793767845265410, 911793856861798402 -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }
}

@Serializable
class PayloadSeriesDetailsDto(
>>>>>>> remotes/keiyoushi/main
    val series: SeriesDto,
)

@Serializable
<<<<<<< HEAD
data class PayloadChaptersDto(
    var data: List<ChapterDto>,
)

@Serializable
data class ChapterDto(
    val id: Long,
    val name: String,
    @SerialName("published_at") val date: String,
)

@Serializable
data class PayloadPagesDto(
=======
class PayloadChaptersDto(
    val data: List<ChapterDto>,
    val meta: ChapterMetaDto,
)

@Serializable
class ChapterDto(
    private val id: Long,
    private val name: String,
    @SerialName("published_at") val date: String,
) {
    fun toSChapter(dateFormat: SimpleDateFormat) = SChapter.create().apply {
        url = "/capitulo/$id"
        name = "Capítulo ${this@ChapterDto.name}"
        date_upload = try {
            dateFormat.parse(date)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

@Serializable
class ChapterMetaDto(
    @SerialName("current_page") private val currentPage: Int,
    @SerialName("last_page") private val lastPage: Int,
) {
    fun hasNextPage() = currentPage < lastPage
}

@Serializable
class PayloadPagesDto(
>>>>>>> remotes/keiyoushi/main
    val chapter: PageDto,
)

@Serializable
<<<<<<< HEAD
data class PageDto(
=======
class PageDto(
>>>>>>> remotes/keiyoushi/main
    val pages: List<String>,
)

@Serializable
<<<<<<< HEAD
data class SeriesStatusDto(
    val id: Long,
    val name: String,
)

@Serializable
data class PayloadFiltersDto(
=======
class SeriesStatusDto(
    val id: Long,
)

@Serializable
class PayloadFiltersDto(
>>>>>>> remotes/keiyoushi/main
    val data: GenresStatusesDto,
)

@Serializable
<<<<<<< HEAD
data class GenresStatusesDto(
=======
class GenresStatusesDto(
>>>>>>> remotes/keiyoushi/main
    val genres: List<FilterDto>,
    val statuses: List<FilterDto>,
)

@Serializable
<<<<<<< HEAD
data class FilterDto(
=======
class FilterDto(
>>>>>>> remotes/keiyoushi/main
    val id: Long,
    val name: String,
)
