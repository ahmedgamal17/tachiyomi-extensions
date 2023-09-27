package eu.kanade.tachiyomi.extension.ar.teamx

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Locale

class TeamX : ConfigurableSource, ParsedHttpSource() {

    override val name = "TeamX"

    private val defaultBaseUrl = "https://team1x12.com"

    private val BASE_URL_PREF = "overrideBaseUrl_v${AppInfo.getVersionName()}"

    override val baseUrl by lazy { getPrefBaseUrl() }

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/series?page=$page")
    }

    override fun popularMangaSelector() = "div.bs > div.bsx"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                title = it.attr("title")
            }
            element.select("div.limit").let {
                thumbnail_url = element.select("img").attr("abs:src")
            }
        }
    }

    override fun popularMangaNextPageSelector() = "a[rel=next]"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/?page=$page")
    }

    override fun latestUpdatesSelector() = "div.box div.uta"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("div.imgu a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                thumbnail_url = it.select("img").attr("src")
                title = element.select("img").attr("alt")
            }
            /*element.select("div.thumb").let {
                title = element.select("h5").text()
            }*/
        }
    }

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val postHeaders = headers.newBuilder()
            .add("Referer", "$baseUrl/")
            .build()
        return if (query.isNotBlank()) {
            GET("$baseUrl/ajax/search?keyword=$query", postHeaders)
        } else {
            val url = "$baseUrl/series?page=$page".toHttpUrlOrNull()!!.newBuilder()
            filters.forEach { filter ->
                when (filter) {
                    is StatusFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("status", it.id) }
                    }
                    is TypeFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("type", it.id) }
                    }
                    is GenreFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("genre", it.id) }
                    }
                    else -> {}
                }
            }
            GET(url.build().toString())
        }
    }

    override fun searchMangaSelector() = "div.bs > div.bsx, li.list-group-item"

    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                title = it.attr("href").substringAfterLast("/").replace("-", " ")
            }

            thumbnail_url = element.select("img").attr("abs:src")
        }
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.review-content").first()!!.let { info ->
                description = info.select("p").text()
            }
            title = document.select("div.author-info-title > h1").text()

            thumbnail_url = document.select("img[alt=Manga Image]").attr("src")

            author = document.select("div.full-list-info:contains(الرسام) > small > a").firstOrNull()?.ownText()
            artist = author

            genre = document.select("div.review-author-info > a, div.full-list-info:contains(النوع) > small > a").joinToString(", ") { it.text() }

            // add series Status to manga description
            document.select("div.full-list-info:contains(الحالة) > small > a")?.first()!!.text()?.also { statusText ->
                when {
                    statusText.contains("مستمرة", true) -> status = SManga.ONGOING
                    statusText.contains("مكتملة", true) -> status = SManga.COMPLETED
                    else -> status = SManga.UNKNOWN
                }
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "div.eplisterfull > ul > li > a"

    private fun chapterNextPageSelector() = "ul.pagination li:last-child a" // "a[rel=next]"

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()

        // Chapter list may be paginated, get recursively
        fun addChapters(document: Document) {
            document.select(chapterListSelector()).map { chapters.add(chapterFromElement(it)) }
            document.select("${chapterNextPageSelector()}").firstOrNull()
                ?.let { addChapters(client.newCall(GET(it.attr("href"))).execute().asJsoup()) }
        }

        addChapters(response.asJsoup())
        return chapters
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            setUrlWithoutDomain(element.attr("href"))
            name = element.select("div.epl-title").text()
            date_upload = element.select("div.epl-date").first()!!.text()?.let { parseChapterDate(it) } ?: 0
            val epNum = getNumberFromEpsString(element.select("div.epl-num").text())
            chapter_number = when {
                (epNum.isNotEmpty()) -> epNum.toFloat()
                else -> 1F
            }
        }
    }

    private fun parseChapterDate(date: String): Long {
        return SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).parse(date)?.time ?: 0L
    }

    private fun getNumberFromEpsString(epsStr: String): String {
        return epsStr.filter { it.isDigit() }
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.reading-content div.page-break img[src*=uploads]").mapIndexed { i, img ->
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
        Genre("اكشن", "أكشن"),
        Genre("إثارة", "إثارة"),
        Genre("إيسيكاي", "إيسيكاي"),
        Genre("إعادة إحياء", "إعادة إحياء"),
        Genre("اعمار/بناء", "اعمار"),
        Genre("الحياة اليومية", "الحياة اليومية"),
        Genre("داخل لعبة", "داخل لعبة"),
        Genre("بطل غير إعتيادي", "بطل غير إعتيادي"),
        Genre("بوليسي", "بوليسي"),
        Genre("تاريخي", "تاريخي"),
        Genre("تاريخ", "تاريخ"),
        Genre("أكاديمية", "أكاديمية"),
        Genre("أرواح", "أرواح"),
        Genre("ابراج", "ابراج"),
        Genre("حرب", "حرب"),
        Genre("تراجيدي", "تراجيدي"),
        Genre("رياضي", "رياضي"),
        Genre("ثأر", "ثأر"),
        Genre("زنزانات", "زنزانات"),
        Genre("حريم", "حريم"),
        Genre("حياة مدرسية", "حياة مدرسية"),
        Genre("واقع افتراضي", "واقع افتراضي"),
        Genre("خارق للطبيعة", "خارق للطبيعة"),
        Genre("خيال", "خيال"),
        Genre("خيال علمي", "خيال علمي"),
        Genre("داخل اللعبة", "143"),
        Genre("داخل رواية", "داخل رواية"),
        Genre("دراما", "دراما"),
        Genre("دموي", "دموي"),
        Genre("رعب", "رعب"),
        Genre("رومانسي", "رومانسي"),
        Genre("زمكاني", "زمكاني"),
        Genre("زومبي", "زومبي"),
        Genre("سحر", "سحر"),
        Genre("سنين", "سنين"),
        Genre("شريحة من الحياة", "شريحة من الحياة"),
        Genre("شوجو", "شوجو"),
        Genre("معالج", "معالج"),
        Genre("شونين", "شونين"),
        Genre("شياطين", "شياطين"),
        Genre("طبخ", "طبخ"),
        Genre("مدرسي", "مدرسي"),
        Genre("عسكري", "عسكري"),
        Genre("عنف", "عنف"),
        Genre("غموض", "غموض"),
        Genre("فانتازيا", "فانتازيا"),
        Genre("فنون قتال", "فنون قتال"),
        Genre("قوة خارقة", "قوة خارقة"),
        Genre("خارق", "خارق"),
        Genre("كوميدي", "كوميدي"),
        Genre("مغامرات", "مغامرات"),
        Genre("ملائكة", "ملائكة"),
        Genre("بعد الكارثة", "بعد الكارثة"),
        Genre("نظام", "نظام"),
        Genre("صقل", "صقل"),
        Genre("نفسي", "نفسي"),
        Genre("وحوش", "وحوش"),
        Genre("ويب تون", "ويب تون"),
    )

    open fun getTypeFilter(): List<Type> = listOf(
        Type("مانجا يابانية", "مانجا ياباني"),
        Type("مانها صينية", "مانها صيني"),
        Type("مانها أندونيسية", "مانها أندونيسية"),
        Type("مانهوا كورية", "مانهوا كورية"),
        Type("ويب تون انجليزية", "ويب تون انجليزية"),
    )

    open fun getStatusFilters(): List<Status> = listOf(
        Status("مكتملة", "مكتمل"),
        Status("مستمرة", "مستمرة"),
        Status("متوقفة", "متوقف"),
        Status("قادم قريبًا", "قادم قريبًا"),
    )

    // settings

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
}