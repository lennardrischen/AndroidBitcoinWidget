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
import androidx.core.content.ContextCompat
import okhttp3.Request
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

data class BitcoinData(
    val price: Double,
    val change24hPercent: Double
)

class ApiRequestWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val TAG = "ApiRequestWorker"
        const val WIDGET_IDS_KEY = "widget_ids"
    }

    private val client = OkHttpClient()

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Fetching Bitcoin data...")

            val bitcoinData = fetchBitcoinDataFromApi()
            val bitcoinPrice = bitcoinData.price
            val bitcoinChange24hPercent = bitcoinData.change24hPercent
            val bitcoinWentUpLast24h = bitcoinChange24hPercent >= 0

            Log.d(TAG, "Bitcoin price: ${bitcoinPrice}.")
            Log.d(TAG, "Bitcoin change 24h %: ${bitcoinChange24hPercent}.")
            Log.d(TAG, "Bitcoin went up last 24h: ${bitcoinWentUpLast24h}.")

            val formattedBitcoinPrice = formatBitcoinPrice(bitcoinPrice)
            val formattedBitcoinChange24hPercent = formatBitcoinChange24hPercent(bitcoinChange24hPercent)

            val currentTime = getCurrentTime()

            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            val appWidgetIds = inputData.getIntArray(WIDGET_IDS_KEY) ?: IntArray(0)

            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widget Ids.")
                Result.success()
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(applicationContext.packageName, R.layout.chart_widget)

                val textColor = if (bitcoinWentUpLast24h) {
                    ContextCompat.getColor(applicationContext, R.color.green)
                } else {
                    ContextCompat.getColor(applicationContext, R.color.red)
                }

                views.setTextColor(R.id.appwidget_btc_price_text, textColor)
                views.setTextViewText(R.id.appwidget_btc_price_text, formattedBitcoinPrice)
                views.setTextColor(R.id.appwidget_24h_change_text, textColor)
                views.setTextViewText(R.id.appwidget_24h_change_text, formattedBitcoinChange24hPercent)
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
    private suspend fun fetchBitcoinDataFromApi(): BitcoinData {
        val request = Request.Builder()
            .url("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur&include_24hr_change=true")
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

            val price = bitcoinObject.getDouble("eur")
            val change = bitcoinObject.getDouble("eur_24h_change")
            return BitcoinData(price, change)
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

    private fun formatBitcoinChange24hPercent(change: Double): String {
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        symbols.decimalSeparator = ','
        val formatter = DecimalFormat("#,##0.00", symbols)
        return "${formatter.format(change)} %"
    }
}