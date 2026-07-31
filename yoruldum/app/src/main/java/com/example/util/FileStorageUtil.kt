package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileStorageUtil {

    /**
     * Kopyalama işlemi ile Content URI'lerini uygulamanın özel dahili depolama alanına
     * (filesDir/media_vault) kopyalar.
     * Bu sayede uygulama kapatılıp açıldığında veya cihaz yeniden başlatıldığında
     * resimler ve belgeler kaybolmaz (file:// URI olarak kalıcı hale gelir).
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri, prefix: String = "media"): String? {
        return try {
            // Eğer zaten dahili dosya (file://) ise tekrar kopyalamaya gerek yok
            if (sourceUri.scheme == "file") {
                return sourceUri.toString()
            }

            val mediaDir = File(context.filesDir, "media_vault").apply {
                if (!exists()) mkdirs()
            }

            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when {
                mimeType?.contains("pdf", ignoreCase = true) == true -> "pdf"
                mimeType?.contains("png", ignoreCase = true) == true -> "png"
                mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
                else -> "jpg"
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension"
            val destFile = File(mediaDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return sourceUri.toString()

            val permanentUriStr = Uri.fromFile(destFile).toString()
            Log.d("FileStorageUtil", "Görsel/Belge dahili depolamaya kaydedildi: $permanentUriStr")
            permanentUriStr
        } catch (e: Exception) {
            Log.e("FileStorageUtil", "Görsel kaydedilirken hata oluştu: ${e.message}", e)
            sourceUri.toString()
        }
    }
}
