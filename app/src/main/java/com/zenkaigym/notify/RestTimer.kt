package com.zenkaigym.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zenkaigym.MainActivity
import com.zenkaigym.R
import com.zenkaigym.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Descanso entre séries que continua com o app em segundo plano ou a tela bloqueada.
 *
 * O estado vive no processo (não na tela): o modo treino mostra a contagem, e uma notificação com cronômetro
 * regressivo, "+30 s" e "Pular" acompanha o descanso. Ela pertence a um serviço em primeiro plano curto
 * ([RestService]) que impede o Android de congelar o app; junto com um wake lock parcial limitado à duração
 * do descanso, o fim é avisado na hora mesmo com a tela apagada (sem permissão de alarme exato).
 * No fim, o celular vibra e, se o app não estiver à vista, aparece o aviso "Descanso acabou".
 */
@SuppressLint("StaticFieldLeak") // guarda só o applicationContext
object RestTimer {
    /** Descanso em andamento: instante (System.currentTimeMillis) em que termina e a duração total. */
    data class Rest(val endsAt: Long, val totalMs: Long)

    const val ADD_SECONDS = 30

    private const val CHANNEL_COUNTDOWN = "rest"
    private const val CHANNEL_DONE = "rest_done"
    internal const val ID_COUNTDOWN = 900
    private const val ID_DONE = 901
    internal const val ACTION_ADD = "com.zenkaigym.REST_ADD"
    internal const val ACTION_SKIP = "com.zenkaigym.REST_SKIP"

    private val _state = MutableStateFlow<Rest?>(null)
    val state: StateFlow<Rest?> = _state

    private var context: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /** App à vista: a notificação de contagem some (a tela do treino já mostra) e o fim só vibra. */
    @Volatile
    private var foreground = false

    fun init(context: Context) {
        this.context = context.applicationContext
        createChannels(context.applicationContext)
    }

    fun start(seconds: Int) {
        val total = seconds.coerceAtLeast(1) * 1000L
        set(Rest(System.currentTimeMillis() + total, total))
    }

    /** Mais [seconds] no descanso em andamento (sem descanso, não faz nada). */
    fun add(seconds: Int = ADD_SECONDS) {
        val r = _state.value ?: return
        val now = System.currentTimeMillis()
        val extra = seconds * 1000L
        set(Rest(maxOf(r.endsAt, now) + extra, r.totalMs + extra))
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = null
        releaseWakeLock()
        context?.let { endCountdown(it) }
    }

    /** Voltar ao app tira o aviso de fim (a tela do treino já mostra); a contagem fica enquanto durar o descanso. */
    fun setForeground(visible: Boolean) {
        foreground = visible
        val ctx = context ?: return
        if (visible) cancel(ctx, ID_DONE)
    }

    private fun set(rest: Rest) {
        job?.cancel()
        _state.value = rest
        val ctx = context ?: return
        acquireWakeLock(ctx, rest.endsAt - System.currentTimeMillis() + 5_000)
        cancel(ctx, ID_DONE)
        startCountdown(ctx, rest)
        job = scope.launch {
            delay((rest.endsAt - System.currentTimeMillis()).coerceAtLeast(0))
            finish(ctx)
        }
    }

    private fun finish(ctx: Context) {
        _state.value = null
        job = null
        endCountdown(ctx)
        vibrate(ctx)
        if (!foreground) showDone(ctx)
        releaseWakeLock()
    }

    /**
     * Mostra a contagem pelo serviço em primeiro plano (mantém o processo vivo até o fim). Se o sistema não
     * deixar iniciar o serviço agora (ex.: app já em segundo plano), fica só a notificação.
     */
    private fun startCountdown(ctx: Context, rest: Rest) {
        val started = runCatching {
            ContextCompat.startForegroundService(ctx, Intent(ctx, RestService::class.java))
        }.isSuccess
        if (!started) showCountdown(ctx, rest)
    }

    private fun endCountdown(ctx: Context) {
        runCatching { ctx.stopService(Intent(ctx, RestService::class.java)) }
        cancel(ctx, ID_COUNTDOWN)
    }

    // ---- Notificações ----

    private fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ctx.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_COUNTDOWN, tr("Descanso entre séries"), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, tr("Fim do descanso"), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = PATTERN
            }
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(ctx: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, RestActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun showCountdown(ctx: Context, rest: Rest) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        runCatching { NotificationManagerCompat.from(ctx).notify(ID_COUNTDOWN, countdownNotification(ctx, rest)) }
    }

    /** Contagem regressiva com "+30 s" e "Pular" (também é a notificação do [RestService]). */
    internal fun countdownNotification(ctx: Context, rest: Rest): android.app.Notification =
        NotificationCompat.Builder(ctx, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(tr("Descansando…"))
            .setContentText(tr("A próxima série começa quando o tempo zerar."))
            .setWhen(rest.endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter((rest.endsAt - System.currentTimeMillis()).coerceAtLeast(1))
            .setContentIntent(openApp(ctx))
            .addAction(0, tr("+{0} s", ADD_SECONDS), action(ctx, ACTION_ADD, 1))
            .addAction(0, tr("Pular"), action(ctx, ACTION_SKIP, 2))
            .build()

    private fun showDone(ctx: Context) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(ctx, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(tr("Descanso acabou!"))
            .setContentText(tr("Hora da próxima série."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(120_000)
            .setContentIntent(openApp(ctx))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(ID_DONE, notification) }
    }

    private fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)

    // ---- Vibração e wake lock ----

    private val PATTERN = longArrayOf(0, 220, 120, 220)

    private fun vibrate(ctx: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, -1))
            else @Suppress("DEPRECATION") vibrator.vibrate(PATTERN, -1)
        }
    }

    private fun acquireWakeLock(ctx: Context, timeoutMs: Long) {
        releaseWakeLock()
        val power = ctx.getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zenkaigym:rest").apply {
            setReferenceCounted(false)
            runCatching { acquire(timeoutMs.coerceIn(1_000, 15 * 60_000L)) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        wakeLock = null
    }
}

/** Botões da notificação de descanso. */
class RestActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RestTimer.ACTION_ADD -> RestTimer.add()
            RestTimer.ACTION_SKIP -> RestTimer.stop()
        }
    }
}
