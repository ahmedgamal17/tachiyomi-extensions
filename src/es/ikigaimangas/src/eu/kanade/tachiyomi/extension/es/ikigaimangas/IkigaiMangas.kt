package eu.kanade.tachiyomi.extension.es.ikigaimangas

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
<<<<<<< HEAD
=======
import kotlin.concurrent.thread
>>>>>>> remotes/keiyoushi/main

class IkigaiMangas : HttpSource() {

    override val baseUrl: String = "https://ikigaimangas.com"
    private val apiBaseUrl: String = "https://panel.ikigaimangas.com"
<<<<<<< HEAD
    private val pageViewerUrl: String = "https://ikigaitoon.com"
=======
>>>>>>> remotes/keiyoushi/main

    override val lang: String = "es"
    override val name: String = "Ikigai Mangas"

    override val supportsLatest: Boolean = true

<<<<<<< HEAD
    override val client = super.client.newBuilder()
=======
    override val client = network.cloudflareClient.newBuilder()
>>>>>>> remotes/keiyoushi/main
        .rateLimitHost(baseUrl.toHttpUrl(), 1, 2)
        .rateLimitHost(apiBaseUrl.toHttpUrl(), 2, 1)
        .build()

    override fun headersBuilder() = super.headersBuilder()
<<<<<<< HEAD
=======
        .add("Origin", baseUrl)
>>>>>>> remotes/keiyoushi/main
        .add("Referer", "$baseUrl/")

    private val json: Json by injectLazy()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun popularMangaRequest(page: Int): Request {
<<<<<<< HEAD
        val apiUrl = "$apiBaseUrl/api/swf/series?page=$page&column=view_count&direction=desc"
        return GET(apiUrl, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val apiUrl = "$apiBaseUrl/api/swf/series?page=$page&column=last_chapter_date&direction=desc"
        return GET(apiUrl, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)
=======
        val apiUrl = "$apiBaseUrl/api/swf/series/ranking-list?type=total_ranking&series_type=comic"
        return GET(apiUrl, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<PayloadSeriesDto>(response.body.string())
        val mangaList = result.data.map { it.toSManga() }
        return MangasPage(mangaList, false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val apiUrl = "$apiBaseUrl/api/swf/new-chapters?page=$page"
        return GET(apiUrl, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<PayloadLatestDto>(response.body.string())
        val mangaList = result.data.filter { it.type == "comic" }.map { it.toSManga() }
        return MangasPage(mangaList, result.hasNextPage())
    }
>>>>>>> remotes/keiyoushi/main

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val sortByFilter = filters.firstInstanceOrNull<SortByFilter>()

        val apiUrl = "$apiBaseUrl/api/swf/series".toHttpUrl().newBuilder()

        if (query.isNotEmpty()) apiUrl.addQueryParameter("search", query)

        apiUrl.addQueryParameter("page", page.toString())
<<<<<<< HEAD
=======
        apiUrl.addQueryParameter("type", "comic")
>>>>>>> remotes/keiyoushi/main

        val genres = filters.firstInstanceOrNull<GenreFilter>()?.state.orEmpty()
            .filter(Genre::state)
            .map(Genre::id)
            .joinToString(",")

        val statuses = filters.firstInstanceOrNull<StatusFilter>()?.state.orEmpty()
            .filter(Status::state)
            .map(Status::id)
            .joinToString(",")

        if (genres.isNotEmpty()) apiUrl.addQueryParameter("genres", genres)
        if (statuses.isNotEmpty()) apiUrl.addQueryParameter("status", statuses)

        apiUrl.addQueryParameter("column", sortByFilter?.selected ?: "name")
        apiUrl.addQueryParameter("direction", if (sortByFilter?.state?.ascending == true) "asc" else "desc")
<<<<<<< HEAD
        apiUrl.addQueryParameter("type", "comic")
=======
>>>>>>> remotes/keiyoushi/main

        return GET(apiUrl.build(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
<<<<<<< HEAD
        runCatching { fetchFilters() }
        val result = json.decodeFromString<PayloadSeriesDto>(response.body.string())
        val mangaList = result.data.filter { it.type == "comic" }.map {
            SManga.create().apply {
                url = "/series/comic-${it.slug}#${it.id}"
                title = it.name
                thumbnail_url = it.cover
            }
        }
        val hasNextPage = result.currentPage < result.lastPage
        return MangasPage(mangaList, hasNextPage)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val slug = response.request.url
            .toString()
            .substringAfter("/series/comic-")
            .substringBefore("#")
        val apiUrl = "$apiBaseUrl/api/swf/series/$slug".toHttpUrl()
        val newResponse = client.newCall(GET(url = apiUrl, headers = headers)).execute()
        val result = json.decodeFromString<PayloadSeriesDetailsDto>(newResponse.body.string())
        return SManga.create().apply {
            title = result.series.name
            thumbnail_url = result.series.cover
            description = result.series.summary
            status = parseStatus(result.series.status?.id)
            genre = result.series.genres?.joinToString { it.name.trim() }
        }
    }

    override fun getChapterUrl(chapter: SChapter): String = pageViewerUrl + chapter.url

    override fun chapterListRequest(manga: SManga): Request {
        val id = manga.url.substringAfterLast("#")
        return GET("$apiBaseUrl/api/swf/series/$id/chapter-list")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.decodeFromString<PayloadChaptersDto>(response.body.string())
        return result.data.map {
            SChapter.create().apply {
                url = "/capitulo/${it.id}"
                name = "Capítulo ${it.name}"
                date_upload = runCatching { dateFormat.parse(it.date)?.time }
                    .getOrNull() ?: 0L
            }
        }.reversed()
=======
        val result = json.decodeFromString<PayloadSeriesDto>(response.body.string())
        val mangaList = result.data.filter { it.type == "comic" }.map { it.toSManga() }
        return MangasPage(mangaList, result.hasNextPage())
    }

    override fun getMangaUrl(manga: SManga) = baseUrl + manga.url.substringBefore("#")

    override fun mangaDetailsRequest(manga: SManga): Request {
        val slug = manga.url
            .substringAfter("/series/comic-")
            .substringBefore("#")

        return GET("$apiBaseUrl/api/swf/series/$slug", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = json.decodeFromString<PayloadSeriesDetailsDto>(response.body.string())
        return result.series.toSMangaDetails()
    }

    override fun getChapterUrl(chapter: SChapter) = baseUrl + chapter.url.substringBefore("#")

    override fun chapterListRequest(manga: SManga): Request {
        val slug = manga.url.substringAfter("/series/comic-").substringBefore("#")
        return GET("$apiBaseUrl/api/swf/series/$slug/chapters?page=1", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val slug = response.request.url.toString()
            .substringAfter("/series/")
            .substringBefore("/chapters")
        var result = json.decodeFromString<PayloadChaptersDto>(response.body.string())
        val mangas = mutableListOf<SChapter>()
        mangas.addAll(result.data.map { it.toSChapter(dateFormat) })
        var page = 2
        while (result.meta.hasNextPage()) {
            val newResponse = client.newCall(GET("$apiBaseUrl/api/swf/series/$slug/chapters?page=$page", headers)).execute()
            result = json.decodeFromString<PayloadChaptersDto>(newResponse.body.string())
            mangas.addAll(result.data.map { it.toSChapter(dateFormat) })
            page++
        }
        return mangas
>>>>>>> remotes/keiyoushi/main
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val id = chapter.url.substringAfter("/capitulo/")
<<<<<<< HEAD
        return GET("$apiBaseUrl/api/swf/chapters/$id")
=======
        return GET("$apiBaseUrl/api/swf/chapters/$id", headers)
>>>>>>> remotes/keiyoushi/main
    }

    override fun pageListParse(response: Response): List<Page> {
        return json.decodeFromString<PayloadPagesDto>(response.body.string()).chapter.pages.mapIndexed { i, img ->
            Page(i, "", img)
        }
    }

<<<<<<< HEAD
    override fun imageUrlParse(response: Response): String = throw Exception("Not used")

    private fun parseStatus(statusId: Long?) = when (statusId) {
        906397890812182531, 911437469204086787 -> SManga.ONGOING
        906409397258190851 -> SManga.ON_HIATUS
        906409532796731395, 911793517664960513 -> SManga.COMPLETED
        906426661911756802, 906428048651190273, 911793767845265410, 911793856861798402 -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    data class SortProperty(val name: String, val value: String) {
        override fun toString(): String = name
    }

    private fun getSortProperties(): List<SortProperty> = listOf(
        SortProperty("Nombre", "name"),
        SortProperty("Creado en", "created_at"),
        SortProperty("Actualización más reciente", "last_chapter_date"),
        SortProperty("Número de favoritos", "bookmark_count"),
        SortProperty("Número de valoración", "rating_count"),
        SortProperty("Número de vistas", "view_count"),
    )

    override fun getFilterList(): FilterList {
=======
    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getFilterList(): FilterList {
        fetchFilters()

>>>>>>> remotes/keiyoushi/main
        val filters = mutableListOf<Filter<*>>(
            SortByFilter("Ordenar por", getSortProperties()),
        )

<<<<<<< HEAD
        filters += if (genresList.isNotEmpty() || statusesList.isNotEmpty()) {
=======
        filters += if (filtersState == FiltersState.FETCHED) {
>>>>>>> remotes/keiyoushi/main
            listOf(
                StatusFilter("Estados", getStatusFilters()),
                GenreFilter("Géneros", getGenreFilters()),
            )
        } else {
            listOf(
                Filter.Header("Presione 'Restablecer' para intentar cargar los filtros"),
            )
        }

        return FilterList(filters)
    }

<<<<<<< HEAD
=======
    private fun getSortProperties(): List<SortProperty> = listOf(
        SortProperty("Nombre", "name"),
        SortProperty("Creado en", "created_at"),
        SortProperty("Actualización más reciente", "last_chapter_date"),
        SortProperty("Número de favoritos", "bookmark_count"),
        SortProperty("Número de valoración", "rating_count"),
        SortProperty("Número de vistas", "view_count"),
    )

>>>>>>> remotes/keiyoushi/main
    private fun getGenreFilters(): List<Genre> = genresList.map { Genre(it.first, it.second) }
    private fun getStatusFilters(): List<Status> = statusesList.map { Status(it.first, it.second) }

    private var genresList: List<Pair<String, Long>> = emptyList()
    private var statusesList: List<Pair<String, Long>> = emptyList()
    private var fetchFiltersAttempts = 0
<<<<<<< HEAD
    private var fetchFiltersFailed = false

    private fun fetchFilters() {
        if (fetchFiltersAttempts <= 3 && ((genresList.isEmpty() && statusesList.isEmpty()) || fetchFiltersFailed)) {
            val filters = runCatching {
                val response = client.newCall(GET("$apiBaseUrl/api/swf/filter-options", headers)).execute()
                json.decodeFromString<PayloadFiltersDto>(response.body.string())
            }

            fetchFiltersFailed = filters.isFailure
            genresList = filters.getOrNull()?.data?.genres?.map { it.name.trim() to it.id } ?: emptyList()
            statusesList = filters.getOrNull()?.data?.statuses?.map { it.name.trim() to it.id } ?: emptyList()
=======
    private var filtersState = FiltersState.NOT_FETCHED

    private fun fetchFilters() {
        if (filtersState != FiltersState.NOT_FETCHED || fetchFiltersAttempts >= 3) return
        filtersState = FiltersState.FETCHING
        fetchFiltersAttempts++
        thread {
            try {
                val response = client.newCall(GET("$apiBaseUrl/api/swf/filter-options", headers)).execute()
                val filters = json.decodeFromString<PayloadFiltersDto>(response.body.string())

                genresList = filters.data.genres.map { it.name.trim() to it.id }
                statusesList = filters.data.statuses.map { it.name.trim() to it.id }

                filtersState = FiltersState.FETCHED
            } catch (e: Throwable) {
                filtersState = FiltersState.NOT_FETCHED
            }
>>>>>>> remotes/keiyoushi/main
        }
    }

    private inline fun <reified R> List<*>.firstInstanceOrNull(): R? =
        filterIsInstance<R>().firstOrNull()
<<<<<<< HEAD
=======

    private enum class FiltersState { NOT_FETCHED, FETCHING, FETCHED }
>>>>>>> remotes/keiyoushi/main
}
