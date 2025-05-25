package com.lennardrischen.bitcoinapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit

/**
 * Implementation of App Widget functionality.
 */
class ChartWidget : AppWidgetProvider() {
    companion object {
        const val WIDGET_WORK_NAME = "BitcoinPriceUpdateWork"
        const val TAG = "ChartWidget"
        const val ACTION_REFRESH_WIDGET = "com.lennardrischen.bitcoinapp.ACTION_REFRESH_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdated() invoked with widget IDs ${appWidgetIds.joinToString()}.")

        enqueuePeriodicApiRequestWorker(context, appWidgetIds)
        enqueueOneTimeApiRequestWorker(context, appWidgetIds)

        // There may be multiple widgets active, so update all of them
        /*for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }*/
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "onEnabled() invoked.")

        val alwaysPendingWork = OneTimeWorkRequestBuilder<ApiRequestWorker>()
            .setInitialDelay(5000L, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "always_pending_work",
            ExistingWorkPolicy.KEEP,
            alwaysPendingWork
        )
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "onDisabled() invoked.")
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds != null) {
            Log.d(TAG, "onDeleted() invoked with widget IDs ${appWidgetIds.joinToString()}.")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d(TAG, "onReceive() invoked.")

        if (context != null && intent != null) {
            Log.d(TAG, "context and intent not null.")
            if (intent.action == ACTION_REFRESH_WIDGET) {
                Log.d(TAG, "action == ACTION_REFRESH_WIDGET.")
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d(TAG, "Refresh button clicked for widget ID: $appWidgetId - Triggering Worker.")

                    // TODO Why passing to worker? maybe just fetch them from appwidgetmanager within worker
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, ChartWidget::class.java)
                    )

                    enqueueOneTimeApiRequestWorker(context, appWidgetIds)
                }
            }
        }
    }

    private fun enqueueOneTimeApiRequestWorker(context: Context, appWidgetIds: IntArray) {
        // Worker requires internet connection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putIntArray(ApiRequestWorker.WIDGET_IDS_KEY, appWidgetIds)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<ApiRequestWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        Log.d(TAG, "Initial ApiRequestWorker scheduled.")
    }

    private fun enqueuePeriodicApiRequestWorker(context: Context, appWidgetIds: IntArray) {
        // Worker requires internet connection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putIntArray(ApiRequestWorker.WIDGET_IDS_KEY, appWidgetIds)
            .build()

        val apiWorkRequest = PeriodicWorkRequestBuilder<ApiRequestWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update existing work if it exists
            apiWorkRequest
        )

        Log.d(TAG, "ApiRequestWorker scheduled.")
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    //val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.chart_widget)
    //views.setTextViewText(R.id.appwidget_1_btc_equals_text, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}