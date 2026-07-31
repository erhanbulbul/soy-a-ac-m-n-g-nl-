package com.example.data.remote

import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiLegacyService {

    // Kendi gerçek API anahtarınızı buraya, tırnakların içine yapıştırın
    private val GEMINI_API_KEY = "AQ.Ab8RN6IipBRRj2g8d75-lbD9s2UACIgX1_c_lS3keZdvp6M3CQ"

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val endpoint = "https://googleapis.com"
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                return@withContext "Hata: Yanıt çözümlenemedi."
            } else {
                return@withContext "Hata Kodu: $responseCode"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Bağlantı Hatası: ${e.message}"
        }
    }
}
