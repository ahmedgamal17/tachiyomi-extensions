package eu.kanade.tachiyomi.extension.es.olympusscanlation

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat

@Serializable
class PayloadHomeDto(
    val data: HomeDto,
)

@Serializable
class HomeDto(
    @SerialName("popular_comics") val popularComics: String,
)

@Serializable
class PayloadSeriesDto(val data: PayloadSeriesDataDto)

@Serializable
class PayloadSeriesDataDto(
    val series: SeriesDto,
)

@Serializable
class SeriesDto(
    val current_page: Int,
    val data: List<MangaDto>,
    val last_page: Int,
)

@Serializable
class PayloadMangaDto(val data: List<MangaDto>)

@Serializable
class MangaDto(
    private val name: String,
    private val slug: String,
    private val cover: String? = null,
    val type: String? = null,
    private val summary: String? = null,
    private val status: MangaStatusDto? = null,
    private val genres: List<FilterDto>? = null,
) {
    fun toSManga() = SManga.create().apply {
        title = name
        url = "/series/comic-$slug"
        thumbnail_url = cover
    }

    fun toSMangaDetails() = toSManga().apply {
        description = summary
        status = parseStatus()
        genre = genres?.joinToString { it.name.trim() }
    }

    private fun parseStatus(): Int {
        val status = this.status ?: return SManga.UNKNOWN
        return when (status.id) {
            1 -> SManga.ONGOING
            3 -> SManga.ON_HIATUS
            4 -> SManga.COMPLETED
            5 -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
    }
}

@Serializable
class NewChaptersDto(
    val data: List<LatestMangaDto>,
    val current_page: Int,
    val last_page: Int,
)

@Serializable
class LatestMangaDto(
    private val name: String,
    private val slug: String,
    private val cover: String? = null,
    val type: String? = null,
) {
    fun toSManga() = SManga.create().apply {
        title = name
        url = "/series/comic-$slug"
        thumbnail_url = cover
    }
}

@Serializable
class MangaDetailDto(
    var data: MangaDto,
)

@Serializable
class PayloadChapterDto(var data: List<ChapterDto>, val meta: MetaDto)

@Serializable
class ChapterDto(
    private val id: Int,
    private val name: String,
    @SerialName("published_at") private val date: String,
) {
    fun toSChapter(mangaSlug: String, dateFormat: SimpleDateFormat) = SChapter.create().apply {
        name = "Capitulo ${this@ChapterDto.name}"
        url = "/capitulo/$id/comic-$mangaSlug"
        date_upload = try {
            dateFormat.parse(date)!!.time
        } catch (e: ParseException) {
            0L
        }
    }
}

@Serializable
class MetaDto(val total: Int)

@Serializable
class PayloadPagesDto(val chapter: PageDto)

@Serializable
class PageDto(val pages: List<String>)

@Serializable
class MangaStatusDto(
    val id: Int,
)

@Serializable
class GenresStatusesDto(
    val genres: List<FilterDto>,
    val statuses: List<FilterDto>,
)

@Serializable
class FilterDto(
    val id: Int,
    val name: String,
)
