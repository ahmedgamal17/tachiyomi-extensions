package eu.kanade.tachiyomi.extension.ar.teamx

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class TeamX : ParsedHttpSource() {

    override val name = "TeamX"

    override val baseUrl = "https://team1x1.com"

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0")
        .add("Content-Encoding", "identity")

    // Decreases calls, helps with Cloudflare
    private fun String.addTrailingSlash() = if (!this.endsWith("/")) "$this/" else this

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/page/$page/", headers)
    }

    override fun popularMangaSelector() = "div > div.last-post-manga"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href").addTrailingSlash())
            }
            element.select("div.thumb").let {
                title = element.select("h3").text()
                thumbnail_url = element.select("img").attr("src")
            }
        }
    }

    override fun popularMangaNextPageSelector() = "div.wp-pagenavi > span.next_text > a"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/last-chapters/page/$page/", headers)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank()) {
            GET("$baseUrl/page/$page/?s=$query", headers)
        } else {
            val url = "$baseUrl/manga/page/$page/?".toHttpUrlOrNull()!!.newBuilder()
            filters.forEach { filter ->
                when (filter) {
                    is GenreFilter -> url.addQueryParameter("ge", filter.toUriPart())
                    is TypeFilter -> url.addQueryParameter("ty", filter.toUriPart())
                    is StatusFilter -> url.addQueryParameter("st", filter.toUriPart())
                }
            }
            GET(url.build().toString(), headers)
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
                //thumbnail_url = element.select("img").attr("abs:src") + element.attr("style").substringAfter("background-image: url('").substringBeforeLast("')")
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
                ?.let { addChapters(client.newCall(GET(it.attr("abs:href").addTrailingSlash(), headers)).execute().asJsoup()) }
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
        return document.select("div.container > div > div > div:nth-child(2) > embed").mapIndexed { i, img ->
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

    private class GenreFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Genre", vals)
    private class TypeFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Type", vals)
    private class StatusFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Status", vals)

    open fun getGenreFilters(): Array<Pair<String?, String>> = arrayOf(
      Pair(null, "<Select>"),
      Pair("1466", "+15"),
      Pair("2307", "+18"),
      Pair("2", "اكشن"),
      Pair("3", "إثارة"),
      Pair("2535", "إعادة إحياء"),
      Pair("2364", "اتشي"),
      Pair("2772", "اعمار/بناء"),
      Pair("36", "الحياة اليومية"),
      Pair("2797", "السفر عبر الزمن"),
      Pair("142", "العاب"),
      Pair("1261", "الواقع الافتراضي"),
      Pair("1980", "ايسكاي"),
      Pair("2206", "بطل غير إعتيادي"),
      Pair("1509", "بوليسي"),
      Pair("112", "تاريخي"),
      Pair("108", "تراجيدي"),
      Pair("1068", "ثأر"),
      Pair("596", "جوسيه"),
      Pair("35", "حريم"),
      Pair("92", "حياة مدرسية"),
      Pair("87", "خارق للطبيعة"),
      Pair("39", "خيال"),
      Pair("90", "خيال علمي"),
      Pair("143", "داخل اللعبة"),
      Pair("1777", "داخل رواية"),
      Pair("91", "دراما"),
      Pair("89", "دموي"),
      Pair("1995", "ديني"),
      Pair("114", "راشد"),
      Pair("109", "رعب"),
      Pair("93", "رومانسي"),
      Pair("110", "رياضة"),
      Pair("6", "زمكاني"),
      Pair("496", "زومبي"),
      Pair("88", "سحر"),
      Pair("115", "سنين"),
      Pair("111", "سينين"),
      Pair("947", "شريحة من الحياة"),
      Pair("157", "شوجو"),
      Pair("33", "شونين"),
      Pair("159", "شياطين"),
      Pair("1124", "صيد"),
      Pair("2346", "طبخ"),
      Pair("1234", "طبى"),
      Pair("176", "طبي"),
      Pair("1978", "عسكري"),
      Pair("1235", "عنف"),
      Pair("38", "غموض"),
      Pair("164", "فانتازيا"),
      Pair("5", "فنون قتال"),
      Pair("7", "قوة خارقة"),
      Pair("1992", "كل الاعمار"),
      Pair("37", "كوميدي"),
      Pair("2201", "مصاص دماء"),
      Pair("4", "مغامرات"),
      Pair("2203", "ملائكة"),
      Pair("158", "نبالة"),
      Pair("2733", "نظام"),
      Pair("1319", "نفسي"),
      Pair("2770", "هندسة"),
      Pair("113", "وحوش"),
      Pair("34", "ويب تون")
    )

    open fun getTypeFilter(): Array<Pair<String?, String>> = arrayOf(
        Pair("", "<Select>"),
        Pair("1996", "قرآن كريم"),
        Pair("106", "مانجا يابانية"),
        Pair("8", "مانها صينية"),
        Pair("2196", "مانهوا روسية"),
        Pair("9", "مانهوا كورية"),
        Pair("2304", "ويب تون"),
        Pair("2305", "ويب كوميك")
    )

    open fun getStatusFilters(): Array<Pair<String?, String>> = arrayOf(
        Pair("", "<Select>"),
        Pair("10", "مكتملة"),
        Pair("11", "مستمرة"),
        Pair("964", "متوقفة"),
        Pair("1997", "شريف"),
        Pair("1778", "برومو"),
        Pair("2306", "ون شوت")
    )

    open class UriPartFilter(displayName: String, private val vals: Array<Pair<String?, String>>) :
        Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()) {
        fun toUriPart() = vals[state].first
    }
}
