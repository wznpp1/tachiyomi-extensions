package eu.kanade.tachiyomi.multisrc.monochrome

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

open class MonochromeCMS(
    override val name: String,
    override val baseUrl: String,
    override val lang: String
) : HttpSource() {
    override val supportsLatest = false

    protected open val apiUrl by lazy {
        baseUrl.replaceFirst("://", "://api.")
    }

    private val json by injectLazy<Json>()

    override fun headersBuilder() =
        Headers.Builder().set("Referer", "$baseUrl/")

    override fun fetchPopularManga(page: Int) =
        fetchSearchManga(page, "", FilterList())

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$apiUrl/manga?limit=10&offset=${10 * (page - 1)}&title=$query", headers)

    override fun searchMangaParse(response: Response) =
        response.decode<Results>().let { results ->
            val mp = results.map {
                SManga.create().apply {
                    url = it.id
                    title = it.title
                    author = it.author
                    artist = it.artist
                    description = it.description
                    thumbnail_url = apiUrl + it.cover
                    status = when (it.status) {
                        "ongoing", "hiatus" -> SManga.ONGOING
                        "completed", "cancelled" -> SManga.COMPLETED
                        else -> SManga.UNKNOWN
                    }
                }
            }
            MangasPage(mp, results.hasNext)
        }

    // Request the actual manga URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/manga/${manga.url}", headers)

    override fun fetchMangaDetails(manga: SManga) =
        Observable.just(manga.apply { initialized = true })!!

    override fun chapterListRequest(manga: SManga) =
        GET("$apiUrl/manga/${manga.url}/chapters", headers)

    override fun fetchChapterList(manga: SManga) =
        client.newCall(chapterListRequest(manga)).asObservableSuccess().map {
            it.decode<List<Chapter>>().map { ch ->
                SChapter.create().apply {
                    name = ch.title
                    url = manga.url + ch.parts
                    chapter_number = ch.number
                    date_upload = ch.timestamp
                    scanlator = ch.scanGroup
                }
            }
        }!!

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val (uuid, version, length) = chapter.url.split('|')
        val pages = IntRange(1, length.toInt()).map {
            Page(it, "", "$apiUrl/media/$uuid/$it.jpg?version=$version")
        }
        return Observable.just(pages)
    }

    private inline fun <reified T> Response.decode() =
        json.decodeFromString<T>(body!!.string())

    override fun popularMangaRequest(page: Int) =
        throw UnsupportedOperationException("Not used!")

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used!")

    override fun popularMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun mangaDetailsParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun chapterListParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun pageListRequest(chapter: SChapter) =
        throw UnsupportedOperationException("Not used!")

    override fun pageListParse(response: Response) =
        throw UnsupportedOperationException("Not used!")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used!")
}
