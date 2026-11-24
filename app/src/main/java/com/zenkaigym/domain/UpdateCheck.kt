package com.zenkaigym.domain

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Última versão publicada no GitHub: [version] sem o "v" e o link direto do APK (ou da página da versão). */
data class LatestRelease(val version: String, val downloadUrl: String)

/** Consulta rápida da última versão do APK. Só roda quando o usuário toca em "Verificar atualização". */
object UpdateCheck {
    const val RELEASES_PAGE = "https://github.com/spardevv/zenkaigym/releases/latest"
    private const val API = "https://api.github.com/repos/spardevv/zenkaigym/releases/latest"

    /** Bloqueante (rede): chamar fora da thread principal. Devolve null se algo falhar. */
    fun fetchLatest(): LatestRelease? = runCatching {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) return null
            parseRelease(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    fun parseRelease(json: String): LatestRelease? {
        val obj = JSONObject(json)
        val tag = obj.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val assets = obj.optJSONArray("assets")
        var apk: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apk = a.optString("browser_download_url").takeIf { it.isNotBlank() }
                    break
                }
            }
        }
        return LatestRelease(tag.removePrefix("v").removePrefix("V"), apk ?: RELEASES_PAGE)
    }

    /** true se [latest] é mais nova que [current] (compara os números de "1.2.3" um a um). */
    fun isNewer(latest: String, current: String): Boolean {
        val a = numbers(latest)
        val b = numbers(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(v: String): List<Int> =
        v.removePrefix("v").removePrefix("V").split('.', '-', '+').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
