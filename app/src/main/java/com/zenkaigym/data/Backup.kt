package com.zenkaigym.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File

/**
 * Cópia de segurança manual: o arquivo do banco inteiro vai para (ou vem de) um local que o usuário escolhe.
 * Nada sai do aparelho sem esse gesto.
 */
object Backup {
    /** Versão do esquema do banco deste app; arquivos de versão maior vêm de um app mais novo e são recusados. */
    private const val MAX_VERSION = 8
    private const val DB_NAME = "zenkaigym.db"
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    enum class ImportResult { OK, INVALID, TOO_NEW }

    fun suggestedName(): String =
        "zenkaigym-backup-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}.db"

    /** Grava o banco em [target]. Antes, descarrega o log de escrita (WAL) para o arquivo principal. */
    fun export(context: Context, db: AppDatabase, target: Uri) {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val source = context.getDatabasePath(DB_NAME)
        val out = context.contentResolver.openOutputStream(target) ?: error("Não foi possível abrir o destino")
        out.use { sink -> source.inputStream().use { it.copyTo(sink) } }
    }

    /**
     * Confere o arquivo escolhido e, se for um backup válido, troca o banco atual por ele.
     * Depois de [ImportResult.OK] o banco está fechado: o chamador precisa reiniciar o app.
     */
    fun import(context: Context, db: AppDatabase, source: Uri): ImportResult {
        val temp = File(context.cacheDir, "import.db")
        try {
            val input = context.contentResolver.openInputStream(source) ?: return ImportResult.INVALID
            input.use { stream -> temp.outputStream().use { stream.copyTo(it) } }

            if (!hasSqliteHeader(temp)) return ImportResult.INVALID
            val version = try {
                SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
                    val hasProfile = check.rawQuery(
                        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'user_profile'", null
                    ).use { it.moveToFirst() }
                    if (!hasProfile) return ImportResult.INVALID
                    check.version
                }
            } catch (e: Exception) {
                return ImportResult.INVALID
            }
            if (version > MAX_VERSION) return ImportResult.TOO_NEW
            if (version < 1) return ImportResult.INVALID

            db.close()
            val target = context.getDatabasePath(DB_NAME)
            listOf("-wal", "-shm", "-journal").forEach { File(target.path + it).delete() }
            temp.copyTo(target, overwrite = true)
            return ImportResult.OK
        } finally {
            temp.delete()
        }
    }

    private fun hasSqliteHeader(file: File): Boolean {
        val head = ByteArray(SQLITE_HEADER.size)
        val read = file.inputStream().use { it.read(head) }
        return read == head.size && head.contentEquals(SQLITE_HEADER)
    }
}
