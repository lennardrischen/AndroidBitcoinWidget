package com.lennardrischen.bitcoinapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
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
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdated() invoked with widget IDs ${appWidgetIds.joinToString()}.")

        scheduleApiRequestWorker(context, appWidgetIds)

        // we also need a OneTimeWorkRequestBuilder for a worker which runs immediately.
        val inputData = Data.Builder()
            .putIntArray(ApiRequestWorker.WIDGET_IDS_KEY, appWidgetIds)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<ApiRequestWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        Log.d(TAG, "Initial ApiRequestWorker scheduled.")

        // There may be multiple widgets active, so update all of them
        /*for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }*/
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "onEnabled() invoked.")
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

    private fun scheduleApiRequestWorker(context: Context, appWidgetIds: IntArray) {
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