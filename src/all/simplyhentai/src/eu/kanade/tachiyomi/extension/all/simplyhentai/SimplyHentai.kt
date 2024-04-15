package eu.kanade.tachiyomi.extension.all.simplyhentai

import android.app.Application
import android.net.Uri
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
<<<<<<< HEAD
import eu.kanade.tachiyomi.network.asObservableSuccess
=======
>>>>>>> remotes/keiyoushi/main
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
<<<<<<< HEAD
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

open class SimplyHentai(override val lang: String) : ConfigurableSource, HttpSource() {
=======
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

open class SimplyHentai(
    override val lang: String,
    private val langName: String,
) : ConfigurableSource, HttpSource() {

>>>>>>> remotes/keiyoushi/main
    override val name = "Simply Hentai"

    override val baseUrl = "https://www.simply-hentai.com"

    override val supportsLatest = true

    override val client = network.cloudflareClient

    override val versionId = 2

    private val apiUrl = "https://api.simply-hentai.com/v3"

<<<<<<< HEAD
    private val langName by lazy {
        Locale.forLanguageTag(lang).displayName
    }

    private val json by lazy { Injekt.get<Json>() }
=======
    private val json: Json by injectLazy()
>>>>>>> remotes/keiyoushi/main

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)!!
    }

    override fun popularMangaRequest(page: Int) =
<<<<<<< HEAD
        Uri.parse("$apiUrl/albums").buildUpon().run {
            appendQueryParameter("si", "0")
            appendQueryParameter("locale", lang)
            appendQueryParameter("language", langName)
            appendQueryParameter("sort", "spotlight")
=======
        Uri.parse("$apiUrl/tag/$langName").buildUpon().run {
            appendQueryParameter("type", "language")
>>>>>>> remotes/keiyoushi/main
            appendQueryParameter("page", page.toString())
            GET(build().toString(), headers)
        }

    override fun popularMangaParse(response: Response) =
<<<<<<< HEAD
        response.decode<SHList<SHObject>>().run {
            MangasPage(
                data.map {
                    SManga.create().apply {
                        url = it.path
                        title = it.title
                        thumbnail_url = it.preview.sizes.thumb
                    }
                },
=======
        response.decode<SHList<SHDataAlbum>>().run {
            MangasPage(
                data.albums.map(SHObject::toSManga),
>>>>>>> remotes/keiyoushi/main
                pagination.next != null,
            )
        }

    override fun latestUpdatesRequest(page: Int) =
<<<<<<< HEAD
        Uri.parse("$apiUrl/albums").buildUpon().run {
            appendQueryParameter("si", "0")
            appendQueryParameter("locale", lang)
            appendQueryParameter("language", langName)
            appendQueryParameter("sort", "newest")
            appendQueryParameter("page", page.toString())
=======
        Uri.parse("$apiUrl/tag/$langName").buildUpon().run {
            appendQueryParameter("type", "language")
            appendQueryParameter("page", page.toString())
            appendQueryParameter("sort", "newest")
>>>>>>> remotes/keiyoushi/main
            GET(build().toString(), headers)
        }

    override fun latestUpdatesParse(response: Response) =
        popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        Uri.parse("$apiUrl/search/complex").buildUpon().run {
<<<<<<< HEAD
            appendQueryParameter("si", "0")
            appendQueryParameter("locale", lang)
            appendQueryParameter("query", query)
            appendQueryParameter("page", page.toString())
            appendQueryParameter("blacklist", blacklist)
            appendQueryParameter("filter[languages][0]", langName)
=======
            appendQueryParameter("query", query)
            appendQueryParameter("page", page.toString())
            appendQueryParameter("blacklist", blacklist)
            appendQueryParameter("filter[language][0]", langName.replaceFirstChar(Char::uppercase))
>>>>>>> remotes/keiyoushi/main
            filters.forEach { filter ->
                when (filter) {
                    is SortFilter -> {
                        appendQueryParameter("sort", filter.orders[filter.state])
                    }
<<<<<<< HEAD
                    is SeriesFilter -> filter.value?.let {
=======
                    is SeriesFilter -> filter.value?.also {
>>>>>>> remotes/keiyoushi/main
                        appendQueryParameter("filter[series_title][0]", it)
                    }
                    is TagsFilter -> filter.value?.forEachIndexed { idx, tag ->
                        appendQueryParameter("filter[tags][$idx]", tag.trim())
                    }
                    is ArtistsFilter -> filter.value?.forEachIndexed { idx, tag ->
                        appendQueryParameter("filter[artists][$idx]", tag.trim())
                    }
                    is TranslatorsFilter -> filter.value?.forEachIndexed { idx, tag ->
                        appendQueryParameter("filter[translators][$idx]", tag.trim())
                    }
                    is CharactersFilter -> filter.value?.forEachIndexed { idx, tag ->
                        appendQueryParameter("filter[characters][$idx]", tag.trim())
                    }
                    else -> {}
                }
            }
            GET(build().toString(), headers)
        }

    override fun searchMangaParse(response: Response) =
<<<<<<< HEAD
        response.decode<SHList<SHWrapper>>().run {
            MangasPage(
                data.map {
                    SManga.create().apply {
                        url = it.`object`.path
                        title = it.`object`.title
                        thumbnail_url = it.`object`.preview.sizes.thumb
                    }
                },
=======
        response.decode<SHList<List<SHWrapper>>>().run {
            MangasPage(
                data.map { it.`object`.toSManga() },
>>>>>>> remotes/keiyoushi/main
                pagination.next != null,
            )
        }

<<<<<<< HEAD
    override fun mangaDetailsRequest(manga: SManga) =
        GET(baseUrl + manga.url, headers)

    override fun fetchMangaDetails(manga: SManga) =
        client.newCall(chapterListRequest(manga))
            .asObservableSuccess().map(::mangaDetailsParse)!!
=======
    override fun mangaDetailsRequest(manga: SManga) = chapterListRequest(manga)
>>>>>>> remotes/keiyoushi/main

    override fun mangaDetailsParse(response: Response) =
        SManga.create().apply {
            val album = response.decode<SHAlbum>().data
            url = album.path
            title = album.title
            description = buildString {
                if (!album.description.isNullOrEmpty()) {
<<<<<<< HEAD
                    append("${album.description}\n\n")
                }
                append("Series: ${album.series.title}\n")
=======
                    append(album.description, "\n\n")
                }
                append("Series: ", album.series.title, "\n")
>>>>>>> remotes/keiyoushi/main
                album.characters.joinTo(this, prefix = "Characters: ") { it.title }
            }
            thumbnail_url = album.preview.sizes.thumb
            genre = album.tags.joinToString { it.title }
            artist = album.artists.joinToString { it.title }
            author = artist
            initialized = true
        }

    override fun chapterListRequest(manga: SManga) =
<<<<<<< HEAD
        Uri.parse("$apiUrl/album").buildUpon().run {
            appendEncodedPath(manga.url.split('/')[2])
            appendQueryParameter("si", "0")
            appendQueryParameter("locale", lang)
=======
        Uri.parse("$apiUrl/manga").buildUpon().run {
            appendEncodedPath(manga.url.split('/')[2])
>>>>>>> remotes/keiyoushi/main
            GET(build().toString(), headers)
        }

    override fun chapterListParse(response: Response) =
        SChapter.create().apply {
            val album = response.decode<SHAlbum>().data
            name = "Chapter"
<<<<<<< HEAD
            chapter_number = -1f
=======
>>>>>>> remotes/keiyoushi/main
            url = "${album.path}/all-pages"
            scanlator = album.translators.joinToString { it.title }
            date_upload = dateFormat.parse(album.created_at)?.time ?: 0L
        }.let(::listOf)

    override fun pageListRequest(chapter: SChapter) =
<<<<<<< HEAD
        Uri.parse("$apiUrl/album").buildUpon().run {
            appendEncodedPath(chapter.url.split('/')[2])
            appendEncodedPath("/pages")
            appendQueryParameter("si", "0")
            appendQueryParameter("locale", lang)
=======
        Uri.parse("$apiUrl/manga").buildUpon().run {
            appendEncodedPath(chapter.url.split('/')[2])
            appendEncodedPath("pages")
>>>>>>> remotes/keiyoushi/main
            GET(build().toString(), headers)
        }

    override fun pageListParse(response: Response) =
        response.decode<SHAlbumPages>().data.pages.map {
            Page(it.page_num, "", it.sizes.full)
        }

    override fun getFilterList() = FilterList(
        SortFilter(),
        SeriesFilter(),
        Note("tags"),
        TagsFilter(),
        Note("artists"),
        ArtistsFilter(),
        Note("translators"),
        TranslatorsFilter(),
        Note("characters"),
        CharactersFilter(),
    )

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        EditTextPreference(screen.context).apply {
            key = "blacklist"
            title = "Blacklist"
            summary = "Separate multiple tags with commas (,)"

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString("blacklist", newValue as String).commit()
            }
        }.let(screen::addPreference)
    }

    private inline val blacklist: String
        get() = preferences.getString("blacklist", "")!!

<<<<<<< HEAD
    private inline fun <reified T> Response.decode() =
        json.decodeFromString<T>(body.string())

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")
=======
    private inline fun <reified T> Response.decode(): T =
        json.decodeFromStream(body.byteStream())

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException()
>>>>>>> remotes/keiyoushi/main

    companion object {
        private val dateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)
    }
}
