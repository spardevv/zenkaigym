package com.zenkaigym.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.Equipment
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Level
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.TrainingFocus
import java.io.File

/**
 * Cópia de segurança manual: o arquivo do banco inteiro vai para (ou vem de) um local que o usuário escolhe.
 * Nada sai do aparelho sem esse gesto.
 */
object Backup {
    /** Versão do esquema do banco deste app; arquivos de versão maior vêm de um app mais novo e são recusados. */
    private const val MAX_VERSION = 10 // manter igual a AppDatabase.version
    private const val DB_NAME = "zenkaigym.db"
    /** Cópia temporária do arquivo escolhido, aberta à parte enquanto os dados são copiados. */
    private const val STAGING_NAME = "zenkaigym-import.db"
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    /**
     * Tabelas do app na ordem em que são preenchidas: quem é referenciado vem antes de quem referencia.
     * A limpeza segue a ordem inversa. Uma tabela nova no banco precisa entrar aqui.
     */
    private val TABLES = listOf(
        "user_profile", "weight_entry", "availability", "food", "exercise",
        "workout_plan", "workout_day", "workout_item",
        "diet_plan", "meal", "meal_item", "meal_slot", "meal_check",
        "workout_log", "set_log", "workout_swap", "meal_extra", "water_log"
    )

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
     * Confere o arquivo escolhido e, se for um backup válido, troca os dados atuais pelos dele.
     *
     * O banco do app continua aberto: o backup é aberto à parte (o Room aplica as migrações de versões
     * antigas e valida o esquema) e o conteúdo é copiado numa única transação. As telas que observam
     * o banco recebem os dados novos sozinhas, sem reiniciar o app. Se algo falhar, a transação é
     * desfeita e os dados atuais ficam como estavam.
     */
    fun import(context: Context, db: AppDatabase, source: Uri): ImportResult {
        val staging = context.getDatabasePath(STAGING_NAME)
        deleteWithSidecars(staging)
        try {
            val input = context.contentResolver.openInputStream(source) ?: return ImportResult.INVALID
            staging.parentFile?.mkdirs()
            input.use { stream -> staging.outputStream().use { stream.copyTo(it) } }

            val checked = validate(staging)
            if (checked != ImportResult.OK) return checked

            val incoming = try {
                AppDatabase.open(context, STAGING_NAME).also { it.openHelper.readableDatabase }
            } catch (e: Exception) {
                return ImportResult.INVALID
            }
            try {
                val from = incoming.openHelper.readableDatabase
                val extra = tablesOf(from) - TABLES.toSet()
                check(extra.isEmpty()) { "Tabelas sem cópia definida no backup: $extra" }
                // Ao abrir, o Room já aplicou as migrações e AppDatabase.repairCatalog; o que ainda for inválido recusa o arquivo.
                val bad = invalidValues(from)
                if (bad.isNotEmpty()) {
                    Log.w("Backup", "Backup recusado, valores desconhecidos em: $bad")
                    return ImportResult.INVALID
                }
                db.runInTransaction {
                    val to = db.openHelper.writableDatabase
                    TABLES.asReversed().forEach { to.delete(it, null, null) }
                    TABLES.forEach { copyTable(from, to, it) }
                }
            } finally {
                incoming.close()
            }
            return ImportResult.OK
        } finally {
            deleteWithSidecars(staging)
        }
    }

    /** Arquivo SQLite com a tabela de perfil e numa versão que este app sabe abrir. */
    private fun validate(file: File): ImportResult {
        if (!hasSqliteHeader(file)) return ImportResult.INVALID
        val version = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
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
        return ImportResult.OK
    }

    /** Colunas gravadas como nome de enum; um valor fora da lista derrubaria as telas ao ler. */
    private val ENUM_COLUMNS: List<Triple<String, String, List<Enum<*>>>> = listOf(
        Triple("user_profile", "sex", Sex.entries), Triple("user_profile", "goal", Goal.entries),
        Triple("user_profile", "level", Level.entries), Triple("user_profile", "cardio", CardioPref.entries),
        Triple("user_profile", "focus", TrainingFocus.entries), Triple("user_profile", "equipment", Equipment.entries),
        Triple("food", "category", FoodCategory.entries),
        Triple("exercise", "group", MuscleGroup.entries), Triple("exercise", "kind", ExerciseKind.entries),
        Triple("exercise", "equipment", Equipment.entries),
        Triple("workout_plan", "status", PlanStatus.entries), Triple("diet_plan", "status", PlanStatus.entries),
        Triple("meal", "type", MealType.entries), Triple("meal_slot", "type", MealType.entries),
        Triple("meal_check", "type", MealType.entries), Triple("meal_extra", "type", MealType.entries)
    )

    /** "tabela.coluna" de cada coluna de enum que tem algum valor desconhecido. */
    private fun invalidValues(db: SupportSQLiteDatabase): List<String> =
        ENUM_COLUMNS.filter { (table, column, values) ->
            val allowed = values.joinToString(",") { "'${it.name}'" }
            db.query("SELECT 1 FROM `$table` WHERE `$column` NOT IN ($allowed) LIMIT 1").use { it.moveToFirst() }
        }.map { "${it.first}.${it.second}" }

    private fun tablesOf(db: SupportSQLiteDatabase): Set<String> =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }.filterNot { it.startsWith("sqlite_") || it.startsWith("room_") || it == "android_metadata" }.toSet()

    /** Copia as linhas de [table]; nomes de coluna vão entre crases porque há palavras reservadas (ex.: `group`). */
    private fun copyTable(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase, table: String) {
        from.query("SELECT * FROM `$table`").use { c ->
            val columns = c.columnNames.joinToString(",") { "`$it`" }
            val marks = c.columnNames.joinToString(",") { "?" }
            val sql = "INSERT INTO `$table` ($columns) VALUES ($marks)"
            while (c.moveToNext()) {
                val args = Array<Any?>(c.columnCount) { i ->
                    when (c.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                        Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                        else -> c.getString(i)
                    }
                }
                to.execSQL(sql, args)
            }
        }
    }

    private fun deleteWithSidecars(file: File) {
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
    }

    private fun hasSqliteHeader(file: File): Boolean {
        val head = ByteArray(SQLITE_HEADER.size)
        val read = file.inputStream().use { it.read(head) }
        return read == head.size && head.contentEquals(SQLITE_HEADER)
    }
}
