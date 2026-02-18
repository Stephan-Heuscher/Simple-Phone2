package ch.heuscher.simplephone.workers

import android.app.NotificationManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class MissedCallNotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val notificationId = inputData.getInt("notification_id", -1)

        if (notificationId != -1) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            return Result.success()
        }

        return Result.failure()
    }
}
