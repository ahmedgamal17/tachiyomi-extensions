package eu.kanade.tachiyomi.extension.all.simplyhentai

import eu.kanade.tachiyomi.source.SourceFactory

class SimplyHentaiFactory : SourceFactory {
    override fun createSources() = listOf(
<<<<<<< HEAD
        SimplyHentai("en"),
        SimplyHentai("ja"),
        SimplyHentai("zh"),
        SimplyHentai("ko"),
        SimplyHentai("es"),
        SimplyHentai("ru"),
        SimplyHentai("fr"),
        SimplyHentai("de"),
        object : SimplyHentai("pt-BR") {
            // The site uses a Portugal flag for the language,
            // but the contents are in Brazilian Portuguese.
            override val id = 23032005200449651
        },
        SimplyHentai("it"),
        SimplyHentai("pl"),
=======
        SimplyHentai("en", "english"),
        SimplyHentai("ja", "japanese"),
        SimplyHentai("zh", "chinese"),
        SimplyHentai("ko", "korean"),
        SimplyHentai("es", "spanish"),
        SimplyHentai("ru", "russian"),
        SimplyHentai("fr", "french"),
        SimplyHentai("de", "german"),
        SimplyHentai("it", "italian"),
        SimplyHentai("pl", "polish"),
>>>>>>> remotes/keiyoushi/main
    )
}
