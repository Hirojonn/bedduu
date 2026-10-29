package com.hirojonn.bedduu.data.remote.parser

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hirojonn.bedduu.data.model.MangaDto
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

object ComixCatalogParser {
    fun parseHtml(html: String, pageUrl: String): List<MangaDto> {
        val initialData = extractInitialData(html, pageUrl)
            ?: throw ParserException("comix", pageUrl, "Comix catalog initial data was not found")
        return parseInitialData(initialData, pageUrl)
    }

    fun parseInitialData(initialData: String, pageUrl: String): List<MangaDto> {
        val baseUrl = pageUrl.toHttpUrlOrNull()
            ?: throw ParserException("comix", pageUrl, "Invalid Comix catalog URL")

        val root = try {
            JsonParser.parseString(initialData)
        } catch (error: RuntimeException) {
            throw ParserException(
                "comix",
                pageUrl,
                "Comix catalog data was malformed: ${error.message.orEmpty()}"
            )
        }

        val items = collectItems(root)
        val manga = items.mapNotNull { item -> item.toMangaDto(baseUrl) }

        if (manga.isEmpty()) {
            throw ParserException("comix", pageUrl, "No Comix titles were found in catalog data")
        }
        return manga.distinctBy(MangaDto::detailUrl)
    }

    private fun extractInitialData(html: String, pageUrl: String): String? {
        val document = Jsoup.parse(html, pageUrl)
        return document.select("script")
            .asSequence()
            .mapNotNull { script ->
                script.data()
                    .takeIf(String::isNotBlank)
                    ?.let(::normalizeEmbeddedJson)
            }
            .firstOrNull { candidate ->
                candidate.isNotBlank() && try {
                    val parsed = JsonParser.parseString(candidate)
                    parsed.isJsonObject || parsed.isJsonArray
                } catch (_: RuntimeException) {
                    false
                }
            }
    }

    private fun normalizeEmbeddedJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return trimmed

        val assignmentMatch = Regex(
            """(?s)(?:window\.)?[A-Za-z0-9_\$]+\s*=\s*(\{.*\}|\[.*\])\s*;?"""
        ).find(trimmed)
        return assignmentMatch?.groupValues?.getOrNull(1)
    }

    private fun collectItems(element: JsonElement): List<JsonObject> {
        val items = mutableListOf<JsonObject>()
        val seen = mutableSetOf<String>()

        fun walk(node: JsonElement) {
            when {
                node.isJsonArray -> node.asJsonArray.forEach(::walk)
                node.isJsonObject -> {
                    val obj = node.asJsonObject
                    obj.get("items")
                        ?.takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.forEach { item ->
                            if (item.isJsonObject) {
                                val jsonObject = item.asJsonObject
                                val signature = jsonObject.toString()
                                if (signature !in seen) {
                                    seen += signature
                                    items += jsonObject
                                }
                            }
                        }

                    obj.get("result")
                        ?.takeIf { it.isJsonObject }
                        ?.let(::walk)

                    obj.get("queries")
                        ?.takeIf { it.isJsonObject }
                        ?.let(::walk)

                    obj.get("data")
                        ?.let(::walk)

                    obj.entrySet().forEach { (_, value) -> walk(value) }
                }
            }
        }

        walk(element)
        return items
    }

    private fun JsonObject.toMangaDto(baseUrl: okhttp3.HttpUrl): MangaDto? {
        val title = stringValue("title", "name").trim()
        val hid = stringValue("hid", "id", "slug", "mangaId").trim()
        val rawUrl = stringValue("url", "href", "path", "link", "slug").trim()
            .ifBlank { hid.takeIf(String::isNotBlank)?.let { "/title/$it" }.orEmpty() }

        val detailUrl = if (rawUrl.startsWith("http")) rawUrl else baseUrl.resolve(rawUrl)?.toString().orEmpty()
        if (title.isBlank() || detailUrl.isBlank()) return null

        val rawCover = posterUrl()
        val coverUrl = if (rawCover.isBlank()) "" else baseUrl.resolve(rawCover)?.toString().orEmpty()

        return MangaDto(
            id = hid.ifBlank { detailUrl.substringAfterLast('/').ifBlank { title } },
            sourceId = "comix",
            title = title,
            coverUrl = coverUrl,
            detailUrl = detailUrl
        )
    }

    private fun JsonObject.posterUrl(): String {
        val nestedPoster = listOf("poster", "cover", "image")
            .firstNotNullOfOrNull { key ->
                get(key)?.takeIf { it.isJsonObject }?.asJsonObject?.let { image ->
                    image.stringValue("medium", "large", "small", "original", "url", "src")
                }
            }
            ?.takeIf(String::isNotBlank)
        if (nestedPoster != null) return nestedPoster

        return stringValue("coverUrl", "posterUrl", "imageUrl", "thumbnail", "image", "src")
    }

    private fun JsonObject.stringValue(vararg names: String): String {
        for (name in names) {
            val value = get(name)
            val text = value?.takeIf { it.isJsonPrimitive }?.asString
            if (!text.isNullOrBlank()) return text.trim()
        }
        return ""
    }
}
