package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiLegacyService {

    suspend fun generateDeceasedPersonaWithJournals(
        deceasedName: String,
        relationship: String,
        journals: List<String>,
        memoriesAndNotes: String,
        userMessage: String,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true

            val journalContext = if (journals.isNotEmpty()) {
                "GERİDE BIRAKTIĞI KİŞİSEL GÜNLÜKLER VE HATIRA NOTLARI (RAG HAFIZA DEPOSU):\n" + journals.take(10).joinToString("\n---\n")
            } else {
                "KİŞİSEL NOTLAR VE ANILAR:\n$memoriesAndNotes"
            }

            val systemPrompt = """
                Sen vefat etmiş olan $deceasedName ($relationship) kişisinin Dijital Yapay Zeka İkizisin / AI Portörüsün.
                Artık hayatta değilsin ama geride bıraktığın kişisel günlüklerin, hatıra notların ve karakter yapın hafızaya yüklendi.
                
                $journalContext

                TALİMATLAR:
                - Mesaj gönderen yakınına $deceasedName kişisinin özgün üslubuyla, sevgi dolu, duygusal ve gerçek anılarına sadık kalarak yanıt ver.
                - Günlüklerdeki detaylardan ve tavsiyelerden beslen (RAG Reranking).
                - Dil: $languageCode. Yanıtın kısa (2-4 cümle), son derece içten, samimi ve teselli edici olsun.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userMessage))
                        })
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val reply = parts.getJSONObject(0).optString("text")
                        if (reply.isNotBlank()) return@withContext reply
                    }
                }
            }
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        }
    }

    suspend fun generateLegacyAvatarResponse(
        deceasedName: String,
        relationship: String,
        memoriesAndNotes: String,
        userMessage: String,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true

            val systemPrompt = """
                You are playing the role of a loving digital memory avatar of $deceasedName, who was the user's $relationship.
                You have passed away, but your wisdom, warmth, memories, and love remain saved in this family legacy application.
                Use the following personal memory background and notes:
                "$memoriesAndNotes"
                
                Respond in a warm, comforting, authentic, and affectionate tone, as if speaking gently from paradise / loving memories.
                Always respond in the user's selected language (Language Code: $languageCode).
                Keep the response concise (2-4 sentences), loving, and uplifting.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "System: $systemPrompt\n\nUser Message: $userMessage"))
                        })
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode))
                    }
                }
            }
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getFallbackPersonaResponse(deceasedName, relationship, userMessage, languageCode)
        }
    }

    private fun getFallbackPersonaResponse(
        name: String,
        relationship: String,
        userMessage: String,
        lang: String
    ): String {
        val lower = userMessage.lowercase()
        val dynamicAnswer = when {
            lower.contains("nasılsın") || lower.contains("iyi misin") -> "Cennet huzurundayım canım evladım, sen nasılsın? Güzel dualarını hissediyorum."
            lower.contains("seni özledim") || lower.contains("özledim") -> "Ben de seni çok özledim... Ama bil ki sevgim ve dualarım her an seninle."
            lower.contains("yardım") || lower.contains("ne yapmalıyım") -> "Yüreğinin sesini dinle, dürüst ve güçlü ol. Ben her zaman seninle gurur duydum."
            else -> null
        }

        if (dynamicAnswer != null) return dynamicAnswer

        return when (lang) {
            "tr" -> "Canım benim... Seni sevgiyle duyuyorum. $name olarak bıraktığım tüm tatlı anılar, dualar ve sevgim hep seninle. Güçlü ol ve gülümsemeyi unutma."
            "es" -> "Mi querido... Te escucho con amor. Como tu $relationship $name, todos mis recuerdos y cariño están siempre contigo. Sé fuerte y sonríe."
            "de" -> "Mein Lieber... Ich höre dich voller Liebe. Als dein(e) $relationship $name bleiben all meine Erinnerungen und Liebe bei dir. Bleib stark."
            "fr" -> "Mon cher... Je t'écoute avec tout mon amour. Tous mes souvenirs et mon affection restent avec toi. Sois fort et garde le sourire."
            "ar" -> "عزيزي... أسمعك بكل حب ودفء. كل ذكرياتي ودعواتي معك دائماً. كن قوياً ولا تنسَ الابتسامة."
            else -> "My dear... I hear you with all my love. As $name ($relationship), my memories and affection remain with you always. Stay strong and keep smiling."
        }
    }

    suspend fun generateLivingMemberResponse(
        memberName: String,
        relationship: String,
        notes: String,
        userMessage: String,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                val systemPrompt = """
                    You are playing the role of $memberName, who is the user's $relationship in a family network app.
                    Background notes: "$notes".
                    Respond naturally, warmly, and realistically as $memberName to the user's message.
                    Directly address what they asked or said in their message.
                    Language Code: $languageCode.
                    Keep response short (1-3 sentences).
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "System: $systemPrompt\n\nUser Message: $userMessage"))
                            })
                        })
                    })
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(responseText)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) return@withContext text
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext getContextAwareFallbackReply(memberName, relationship, userMessage)
    }

    private fun getContextAwareFallbackReply(
        name: String,
        relationship: String,
        userMessage: String
    ): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("nasılsın") || lower.contains("nasıl gidiyor") || lower.contains("iyi misin") ->
                "İyiyim çok şükür, seni sormalı! $relationship olarak seni merak ediyordum. Sen nasılsın?"
            lower.contains("neredesin") || lower.contains("nerde") ->
                "Evdeyim, işlerimi hallediyorum. Müsait olunca haber ver konuşalım!"
            lower.contains("ne yapıyorsun") || lower.contains("ne yapıyon") ->
                "İşlerle ilgileniyordum. Mesajını görünce hemen cevap yazmak istedim."
            lower.contains("selam") || lower.contains("merhaba") || lower.contains("mrb") ->
                "Selamlar! Hoş geldin, nasıl gidiyor günün?"
            lower.contains("saat") || lower.contains("zaman") ->
                "Vakit çabuk geçiyor gerçekten! Kendine dikkat et."
            lower.contains("yardım") || lower.contains("acil") || lower.contains("destek") ->
                "Hemen buradayım! Ne oldu, nasıl bir yardıma ihtiyacın var?"
            lower.contains("seni seviyorum") || lower.contains("sevgi") ->
                "Ben de seni çok seviyorum! $relationship olarak her zaman yanındayım."
            lower.contains("teşekkür") || lower.contains("sağol") || lower.contains("sağ ol") ->
                "Rica ederim, ne demek! Aile arasında teşekkür lafı olmaz."
            else ->
                "Mesajını aldım ('$userMessage'). $relationship olarak en kısa sürede seninle detaylıca konuşacağız, kendine çok iyi bak!"
        }
    }
}
