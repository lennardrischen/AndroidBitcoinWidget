package com.lennardrischen.bitcoinapp

import android.appwidget.AppWidgetManager
import android.content.Context
import android.icu.text.NumberFormat
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Locale
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import okhttp3.Request
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ApiRequestWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val TAG = "ApiRequestWorker"
        const val WIDGET_IDS_KEY = "widget_ids"
    }

    private val client = OkHttpClient()

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Fetching Bitcoin price...")

            val bitcoinPrice = fetchBitcoinPriceFromApi()

            Log.d(TAG, "Bitcoin price: ${bitcoinPrice}.")

            val formattedBitcoinPrice = formatBitcoinPrice(bitcoinPrice)

            val currentTime = getCurrentTime()

            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            val appWidgetIds = inputData.getIntArray(WIDGET_IDS_KEY) ?: IntArray(0)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widget Ids.")
                Result.success()
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(applicationContext.packageName, R.layout.chart_widget)
                views.setTextViewText(R.id.appwidget_btc_price_text, formattedBitcoinPrice)
                views.setTextViewText(R.id.appwidget_last_updated_text, "Last update: $currentTime")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Updated TextViews.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error while fetching price and updating TextViews.: ${e.message}", e)
            Result.retry()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTime(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return currentTime.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private suspend fun fetchBitcoinPriceFromApi(): Double {
        val request = Request.Builder()
            .url("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("API request failed with code: ${response.code()} and message: ${response.message()}!")
        }

        val responseBody = response.body()?.string()
            ?: throw IOException("API response body is empty!")

        try {
            val jsonObject = JSONObject(responseBody)
            val bitcoinObject = jsonObject.getJSONObject("bitcoin")
            return bitcoinObject.getDouble("eur")
        } catch (e: JSONException) {
            throw JSONException("JSON parsing failed with message ${e.message}!")
        }
    }

    private fun formatBitcoinPrice(price: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        return formatter.format(price)
    }
}