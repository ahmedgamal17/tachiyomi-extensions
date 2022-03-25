package eu.kanade.tachiyomi.extension.ar.teamx

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class TeamX : ParsedHttpSource() {

    override val name = "TeamX"
    
    private val defaultBaseUrl = "https://teamx.fun"

    private val BASE_URL_PREF = "overrideBaseUrl_v${BuildConfig.VERSION_NAME}"

    override val baseUrl by lazy { getPrefBaseUrl() }
    //override val baseUrl = "http://teamxmanga.com"

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val baseUrlPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = BASE_URL_PREF_TITLE
            title = BASE_URL_PREF_TITLE
            summary = BASE_URL_PREF_SUMMARY
            this.setDefaultValue(defaultBaseUrl)
            dialogTitle = BASE_URL_PREF_TITLE
            dialogMessage = "Default: $defaultBaseUrl"

            setOnPreferenceChangeListener { _, newValue ->
                try {
                    val res = preferences.edit().putString(BASE_URL_PREF, newValue as String).commit()
                    res
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }

        screen.addPreference(baseUrlPref)
    }

    private fun getPrefBaseUrl(): String = preferences.getString(BASE_URL_PREF, defaultBaseUrl)!!

    companion object {
        private const val BASE_URL_PREF_TITLE = "Override BaseUrl"
        private const val BASE_URL_PREF_SUMMARY = "Override default domain with a different one"
    }

    // Decreases calls, helps with Cloudflare
    private fun String.addTrailingSlash() = if (!this.endsWith("/")) "$this/" else this

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/page/$page/")
    }

    override fun popularMangaSelector() = "div > div.last-post-manga"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href").addTrailingSlash())
            }
            element.select("div.thumb").let {
                title = element.select("h3").text()
                thumbnail_url = element.select("img").attr("abs:src")
            }
        }
    }

    override fun popularMangaNextPageSelector() = "div.wp-pagenavi > span.next_text > a"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/last-chapters/page/$page/")
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href").addTrailingSlash())
            }
            thumbnail_url = element.attr("style").substringAfter("url('").substringBefore("')")
            element.select("div.thumb").let {
                title = element.select("h5").text()
            }
        }
    }

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank()) {
            GET("$baseUrl/page/$page/?s=$query")
        } else {
            val url = "$baseUrl/manga/page/$page/?".toHttpUrlOrNull()!!.newBuilder()
            filters.forEach { filter ->
                when (filter) {
                    is StatusFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("st", it.id) }
                    }
                    is TypeFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("ty", it.id) }
                    }
                    is GenreFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("ge", it.id) }
                    }
                }
            }
            GET(url.build().toString())
        }
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
            }
            element.select("div.thumb").let {
                title = element.select("div.info > h5 > a, div.info > h3 > a").text()
                // thumbnail_url = element.select("img").attr("abs:src") + element.attr("style").substringAfter("background-image: url('").substringBeforeLast("')")
            }
            thumbnail_url = element.select("div.thumb img").attr("abs:src") + element.attr("style").substringAfter("background-image: url('").substringBeforeLast("')")
        }
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.story").first().let { info ->
                description = info.select("p").text()
            }

            thumbnail_url = document.select("div.thumb > img").attr("src")

            author = document.select("div.container > div:nth-child(1) > div:nth-child(2) > div:nth-child(6) > a").firstOrNull()?.ownText()
            artist = author

            genre = document.select("div.container > div:nth-child(1) > div:nth-child(2) > div:nth-child(5) > a").joinToString(", ") { it.text() }

            // add series Status to manga description
            document.select("div.container > div:nth-child(1) > div:nth-child(2) > div:nth-child(4) > a")?.first()?.text()?.also { statusText ->
                when {
                    statusText.contains("مستمرة", true) -> status = SManga.ONGOING
                    statusText.contains("مكتملة", true) -> status = SManga.COMPLETED
                    else -> status = SManga.UNKNOWN
                }
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "div.single-manga-chapter > div.container > div > div > a"

    private fun chapterNextPageSelector() = "div.wp-pagenavi > span.nextx_text > a"

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()

        // Chapter list may be paginated, get recursively
        fun addChapters(document: Document) {
            document.select(chapterListSelector()).map { chapters.add(chapterFromElement(it)) }
            document.select("${chapterNextPageSelector()}").firstOrNull()
                ?.let { addChapters(client.newCall(GET(it.attr("abs:href").addTrailingSlash())).execute().asJsoup()) }
        }

        addChapters(response.asJsoup())
        return chapters
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            setUrlWithoutDomain(element.attr("href").addTrailingSlash())
            name = "${element.text()}"
        }
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div#translationPageall embed[src*=content]").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Separator(),
        StatusFilter(getStatusFilters()),
        Filter.Separator(),
        TypeFilter(getTypeFilter()),
        Filter.Separator(),
        GenreFilter(getGenreFilters()),
    )

    class Type(name: String, val id: String = name) : Filter.TriState(name)
    private class TypeFilter(types: List<Type>) : Filter.Group<Type>("Type", types)
    class Status(name: String, val id: String = name) : Filter.TriState(name)
    private class StatusFilter(statuses: List<Status>) : Filter.Group<Status>("Status", statuses)
    class Genre(name: String, val id: String = name) : Filter.TriState(name)
    private class GenreFilter(genres: List<Genre>) : Filter.Group<Genre>("Genre", genres)

    open fun getGenreFilters(): List<Genre> = listOf(
        Genre("+15", "1466"),
        Genre("+18", "2307"),
        Genre("اكشن", "2"),
        Genre("إثارة", "3"),
        Genre("إعادة إحياء", "2535"),
        Genre("اتشي", "2364"),
        Genre("اعمار/بناء", "2772"),
        Genre("الحياة اليومية", "36"),
        Genre("السفر عبر الزمن", "2797"),
        Genre("العاب", "142"),
        Genre("الواقع الافتراضي", "1261"),
        Genre("ايسكاي", "1980"),
        Genre("بطل غير إعتيادي", "2206"),
        Genre("بوليسي", "1509"),
        Genre("تاريخي", "112"),
        Genre("تراجيدي", "108"),
        Genre("ثأر", "1068"),
        Genre("جوسيه", "596"),
        Genre("حريم", "35"),
        Genre("حياة مدرسية", "92"),
        Genre("خارق للطبيعة", "87"),
        Genre("خيال", "39"),
        Genre("خيال علمي", "90"),
        Genre("داخل اللعبة", "143"),
        Genre("داخل رواية", "1777"),
        Genre("دراما", "91"),
        Genre("دموي", "89"),
        Genre("ديني", "1995"),
        Genre("راشد", "114"),
        Genre("رعب", "109"),
        Genre("رومانسي", "93"),
        Genre("رياضة", "110"),
        Genre("زمكاني", "6"),
        Genre("زومبي", "496"),
        Genre("سحر", "88"),
        Genre("سنين", "115"),
        Genre("سينين", "111"),
        Genre("شريحة من الحياة", "947"),
        Genre("شوجو", "157"),
        Genre("شونين", "33"),
        Genre("شياطين", "159"),
        Genre("صيد", "1124"),
        Genre("طبخ", "2346"),
        Genre("طبى", "1234"),
        Genre("طبي", "176"),
        Genre("عسكري", "1978"),
        Genre("عنف", "1235"),
        Genre("غموض", "38"),
        Genre("فانتازيا", "164"),
        Genre("فنون قتال", "5"),
        Genre("قوة خارقة", "7"),
        Genre("كل الاعمار", "1992"),
        Genre("كوميدي", "37"),
        Genre("مصاص دماء", "2201"),
        Genre("مغامرات", "4"),
        Genre("ملائكة", "2203"),
        Genre("نبالة", "158"),
        Genre("نظام", "2733"),
        Genre("نفسي", "1319"),
        Genre("هندسة", "2770"),
        Genre("وحوش", "113"),
        Genre("ويب تون", "34")
    )

    open fun getTypeFilter(): List<Type> = listOf(
        Type("قرآن كريم", "1996"),
        Type("مانجا يابانية", "106"),
        Type("مانها صينية", "8"),
        Type("مانهوا روسية", "2196"),
        Type("مانهوا كورية", "9"),
        Type("ويب تون", "2304"),
        Type("ويب كوميك", "2305")
    )

    open fun getStatusFilters(): List<Status> = listOf(
        Status("مكتملة", "10"),
        Status("مستمرة", "11"),
        Status("متوقفة", "964"),
        Status("شريف", "1997"),
        Status("برومو", "1778"),
        Status("ون شوت", "2306")
    )
}
