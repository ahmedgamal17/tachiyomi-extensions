package eu.kanade.tachiyomi.extension.es.ikigaimangas

import eu.kanade.tachiyomi.source.model.Filter

class Genre(title: String, val id: Long) : Filter.CheckBox(title)
class GenreFilter(title: String, genres: List<Genre>) : Filter.Group<Genre>(title, genres)

class Status(title: String, val id: Long) : Filter.CheckBox(title)
class StatusFilter(title: String, statuses: List<Status>) : Filter.Group<Status>(title, statuses)

<<<<<<< HEAD
class SortByFilter(title: String, private val sortProperties: List<IkigaiMangas.SortProperty>) : Filter.Sort(
=======
class SortByFilter(title: String, private val sortProperties: List<SortProperty>) : Filter.Sort(
>>>>>>> remotes/keiyoushi/main
    title,
    sortProperties.map { it.name }.toTypedArray(),
    Selection(0, ascending = true),
) {
    val selected: String
        get() = sortProperties[state!!.index].value
}
<<<<<<< HEAD
=======

class SortProperty(val name: String, val value: String) {
    override fun toString(): String = name
}
>>>>>>> remotes/keiyoushi/main
