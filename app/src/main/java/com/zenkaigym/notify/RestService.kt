package com.zenkaigym.notify

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat

/**
 * Serviço em primeiro plano que existe só enquanto há descanso em andamento. Sem ele o Android congela o
 * processo do app pouco depois de sair da tela (e desliga o wake lock), e o fim do descanso só seria avisado
 * quando o celular fosse destravado. A notificação do serviço é a própria contagem regressiva.
 *
 * Tipo "shortService" (Android 14+): não pede permissão especial e dura até ~3 minutos, mais que qualquer
 * descanso normal. Se o sistema encerrar antes, a contagem na notificação continua e o aviso de fim pode atrasar.
 */
class RestService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rest = RestTimer.state.value
        if (rest == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0
        runCatching {
            ServiceCompat.startForeground(this, RestTimer.ID_COUNTDOWN, RestTimer.countdownNotification(this, rest), type)
        }.onFailure {
            Log.w("RestService", "Sem serviço em primeiro plano; o descanso segue só na notificação", it)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Limite do shortService atingido: encerra sem derrubar o app (a contagem segue na notificação). */
    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        // Descanso ainda correndo (limite do sistema): a contagem fica na tela; terminado ou pulado: some junto.
        val keep = RestTimer.state.value != null
        ServiceCompat.stopForeground(this, if (keep) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
