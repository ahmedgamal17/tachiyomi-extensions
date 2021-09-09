package eu.kanade.tachiyomi.animeextension.en.animepahe

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale

class AnimePahe : AnimeHttpSource() {

    override val name = "AnimePahe"

    override val baseUrl = "https://animepahe.com"

    override val lang = "en"

    override val supportsLatest = false

    override fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Aniyomi")
        add("Referer", "https://kwik.cx")
    }

    override val client: OkHttpClient = network.cloudflareClient

    override fun animeDetailsParse(response: Response): SAnime {
        val jsoup = response.asJsoup()
        val anime = SAnime.create()
        anime.title = jsoup.selectFirst("div.title-wrapper h1").text()
        anime.author = jsoup.selectFirst("div.col-sm-4.anime-info p:contains(Studio:)").text().replace("Studio: ", "")
        anime.status = parseStatus(jsoup.selectFirst("div.col-sm-4.anime-info p:contains(Status:) a").text())
        anime.thumbnail_url = jsoup.selectFirst("div.anime-poster a").attr("href")
        anime.genre = jsoup.select("div.anime-genre ul li").joinToString { it.text() }
        anime.description = jsoup.select("div.anime-summary").text()
        return anime
    }

    override fun latestUpdatesRequest(page: Int) = throw Exception("not supported")

    override fun latestUpdatesParse(response: Response) = throw Exception("not supported")

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/api?m=search&l=8&q=$query")

    override fun searchAnimeParse(response: Response): AnimesPage {
        val responseString = response.body!!.string()
        return parseSearchJson(responseString)
    }

    private fun parseSearchJson(jsonLine: String?): AnimesPage {
        val jElement: JsonElement = JsonParser.parseString(jsonLine)
        val jObject: JsonObject = jElement.asJsonObject
        val data = jObject.get("data") ?: return AnimesPage(emptyList(), false)
        val array = data.asJsonArray
        val animeList = mutableListOf<SAnime>()
        for (item in array) {
            val anime = SAnime.create()
            anime.title = item.asJsonObject.get("title").asString
            val animeId = item.asJsonObject.get("id").asInt
            val session = item.asJsonObject.get("session").asString
            anime.setUrlWithoutDomain("$baseUrl/anime/$session?anime_id=$animeId")
            animeList.add(anime)
        }
        return AnimesPage(animeList, false)
    }

    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/api?m=airing&page=$page")

    override fun popularAnimeParse(response: Response): AnimesPage {
        val responseString = response.body!!.string()
        return parsePopularAnimeJson(responseString)
    }

    private fun parsePopularAnimeJson(jsonLine: String?): AnimesPage {
        val jElement: JsonElement = JsonParser.parseString(jsonLine)
        val jObject: JsonObject = jElement.asJsonObject
        val lastPage = jObject.get("last_page").asInt
        val page = jObject.get("current_page").asInt
        val hasNextPage = page < lastPage
        val array = jObject.get("data").asJsonArray
        val animeList = mutableListOf<SAnime>()
        for (item in array) {
            val anime = SAnime.create()
            anime.title = item.asJsonObject.get("anime_title").asString
            val animeId = item.asJsonObject.get("anime_id").asInt
            val session = item.asJsonObject.get("anime_session").asString
            anime.setUrlWithoutDomain("$baseUrl/anime/$session?anime_id=$animeId")
            anime.artist = item.asJsonObject.get("fansub").asString
            animeList.add(anime)
        }
        return AnimesPage(animeList, hasNextPage)
    }

    private fun parseStatus(statusString: String): Int {
        return when (statusString) {
            "Currently Airing" -> SAnime.ONGOING
            "Finished Airing" -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
    }

    override fun episodeListRequest(anime: SAnime): Request {
        val animeId = anime.url.substringAfterLast("?anime_id=")
        return GET("$baseUrl/api?m=release&id=$animeId&sort=episode_desc&page=1")
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        return recursivePages(response)
    }

    private fun parseEpisodePage(jsonLine: String?): MutableList<SEpisode> {
        val jElement: JsonElement = JsonParser.parseString(jsonLine)
        val jObject: JsonObject = jElement.asJsonObject
        val array = jObject.get("data").asJsonArray
        val episodeList = mutableListOf<SEpisode>()
        for (item in array) {
            val itemO = item.asJsonObject
            val episode = SEpisode.create()
            episode.date_upload = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .parse(itemO.get("created_at").asString)!!.time
            val animeId = itemO.get("anime_id").asInt
            val session = itemO.get("session").asString
            episode.setUrlWithoutDomain("$baseUrl/api?m=links&id=$animeId&session=$session&p=kwik")
            val epNum = itemO.get("episode").asInt
            episode.episode_number = epNum.toFloat()
            episode.name = "Episode $epNum"
            episodeList.add(episode)
        }
        return episodeList
    }

    private fun recursivePages(response: Response): List<SEpisode> {
        val responseString = response.body!!.string()
        val jElement: JsonElement = JsonParser.parseString(responseString)
        val jObject: JsonObject = jElement.asJsonObject
        val lastPage = jObject.get("last_page").asInt
        val page = jObject.get("current_page").asInt
        val hasNextPage = page < lastPage
        val returnList = parseEpisodePage(responseString)
        if (hasNextPage) {
            val nextPage = nextPageRequest(response.request.url.toString(), page + 1)
            returnList += recursivePages(nextPage)
        }
        return returnList
    }

    private fun nextPageRequest(url: String, page: Int): Response {
        val request = GET(url.substringBeforeLast("&page=") + "&page=$page")
        return client.newCall(request).execute()
    }

    override fun videoListParse(response: Response): List<Video> {
        val array = JsonParser.parseString(response.body!!.string())
            .asJsonObject.get("data").asJsonArray
        val videos = mutableListOf<Video>()
        for (item in array.reversed()) {
            val quality = item.asJsonObject.keySet().first()
            val kwikLink = item.asJsonObject.get(quality)
                .asJsonObject.get("kwik").asString
            videos.add(getVideo(kwikLink, quality))
        }
        return videos
    }

    private fun getVideo(adflyUrl: String, quality: String): Video {
        val videoUrl = getStreamUrlFromKwik(adflyUrl)
        return Video(videoUrl, "${quality}p", videoUrl, null)
    }

    private fun getStreamUrlFromKwik(kwikLink: String): String {
        val eContent = client.newCall(
            GET(kwikLink, Headers.headersOf("referer", baseUrl))
        ).execute().asJsoup()
        val eContentString = eContent.select("script").filter {
            it.data().startsWith("eval")
        }.firstOrNull()?.data()
        val data = eContentString?.substringBeforeLast("'.split('|')")
            ?.substringAfterLast("'")
        val url = data?.substring(data.indexOf("m3u8|uwu"))
        val list = url?.split("|")?.reversed()
        if (list?.lastIndex == 10) {
            return "${list[0]}://${list[1]}-${list[2]}.${list[3]}.${list[4]}.${list[5]}/${list[6]}/${list[7]}/${list[8]}/${list[9]}.${list[10]}"
        }
        throw Exception("error getting links")
    }
}
