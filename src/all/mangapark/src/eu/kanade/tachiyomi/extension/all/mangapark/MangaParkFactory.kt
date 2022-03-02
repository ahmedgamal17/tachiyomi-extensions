package eu.kanade.tachiyomi.extension.all.mangapark

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class MangaParkFactory : SourceFactory {
    override fun createSources(): List<Source> = languages.map { MangaPark(it.lang, it.siteLang) }
}

class LanguageOption(val lang: String, val siteLang: String = lang)
private val languages = listOf(
    // LanguageOption("<Language Format>","<Language Format used in site.>"),
    LanguageOption("en"),
    LanguageOption("ar"),
    LanguageOption("bg"),
    LanguageOption("zh"),
    LanguageOption("cs"),
    LanguageOption("da"),
    LanguageOption("nl"),
    LanguageOption("fil"),
    LanguageOption("fi"),
    LanguageOption("fr"),
    LanguageOption("de"),
    LanguageOption("el"),
    LanguageOption("he"),
    LanguageOption("hi"),
    LanguageOption("hu"),
    LanguageOption("id"),
    LanguageOption("it"),
    LanguageOption("ja"),
    LanguageOption("ko"),
    LanguageOption("ms"),
    LanguageOption("pl"),
    LanguageOption("pt"),
    LanguageOption("pt-BR", "pt_br"),
    LanguageOption("ro"),
    LanguageOption("ru"),
    LanguageOption("es"),
    LanguageOption("es-419", "es_419"),
    LanguageOption("sv"),
    LanguageOption("th"),
    LanguageOption("tr"),
    LanguageOption("uk"),
    LanguageOption("vi"),
    LanguageOption("af"),
    LanguageOption("sq"),
    LanguageOption("am"),
    LanguageOption("hy"),
    LanguageOption("az"),
    LanguageOption("be"),
    LanguageOption("bn"),
    LanguageOption("bs"),
    LanguageOption("my"),
    LanguageOption("km"),
    LanguageOption("ca"),
    LanguageOption("ceb"),
    LanguageOption("zh-Hans", "zh_hk"),
    LanguageOption("zh-Hant", "zh_tw"),
    LanguageOption("hr"),
    LanguageOption("en-US", "en_us"),
    LanguageOption("eo"),
    LanguageOption("et"),
    LanguageOption("fo"),
    LanguageOption("ka"),
    LanguageOption("gn"),
    LanguageOption("gu"),
    LanguageOption("ht",),
    LanguageOption("ha"),
    LanguageOption("is"),
    LanguageOption("ig"),
    LanguageOption("ga"),
    LanguageOption("jv"),
    LanguageOption("kn"),
    LanguageOption("kk"),
    LanguageOption("ku"),
    LanguageOption("ky"),
    LanguageOption("lo"),
    LanguageOption("lv"),
    LanguageOption("lt"),
    LanguageOption("lb"),
    LanguageOption("mk"),
    LanguageOption("mg"),
    LanguageOption("ml"),
    LanguageOption("mt"),
    LanguageOption("mi"),
    LanguageOption("mr"),
    LanguageOption("mo", "ro-MD"),
    LanguageOption("mn"),
    LanguageOption("ne"),
    LanguageOption("no"),
    LanguageOption("ny"),
    LanguageOption("ps"),
    LanguageOption("fa"),
    LanguageOption("rm"),
    LanguageOption("sm"),
    LanguageOption("sr"),
    LanguageOption("sh",),
    LanguageOption("st"),
    LanguageOption("sn"),
    LanguageOption("sd"),
    LanguageOption("si"),
    LanguageOption("sk"),
    LanguageOption("sl"),
    LanguageOption("so"),
    LanguageOption("sw"),
    LanguageOption("tg"),
    LanguageOption("ta"),
    LanguageOption("ti"),
    LanguageOption("to"),
    LanguageOption("tk"),
    LanguageOption("ur"),
    LanguageOption("uz"),
    LanguageOption("yo"),
    LanguageOption("zu"),
    LanguageOption("other", "_t"),
    LanguageOption("eu"),
    LanguageOption("pt-PT", "pt_pt")
)
