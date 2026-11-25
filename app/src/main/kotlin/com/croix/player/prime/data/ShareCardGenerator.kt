/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.croix.prime.engine.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates high-DPI shareable PNG cards (1080px wide) using native Android Canvas:
 * 1. Glassmorphism Lyric Card (1-4 selected lines + cover art + track metadata)
 * 2. Retro-modern Musical Receipt ("CROIX RECEIPT" with top tracks, artists, minutes & barcode)
 */
object ShareCardGenerator {
    suspend fun shareLyricCard(
        context: Context,
        track: Track,
        lines: List<String>,
        artwork: Bitmap?,
        primaryArgb: Int,
        secondaryArgb: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val w = 1080
            val h = 1350
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Rich diagonal gradient backdrop
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        blendDark(primaryArgb, 0.55f),
                        0xFF0A0913.toInt(),
                        blendDark(secondaryArgb, 0.50f),
                    ),
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

            // Translucent Glass Card container
            val cardRect = RectF(76f, 110f, w - 76f, h - 110f)
            val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(46, 255, 255, 255)
            }
            canvas.drawRoundRect(cardRect, 56f, 56f, cardFill)

            val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                shader = LinearGradient(
                    cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                    Color.argb(140, 255, 255, 255),
                    primaryArgb,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(cardRect, 56f, 56f, cardBorder)

            // Header: Cover Art + Title + Artist
            val artSize = 148f
            val artRect = RectF(132f, 174f, 132f + artSize, 174f + artSize)
            if (artwork != null) {
                canvas.save()
                val clipPath = Path().apply { addRoundRect(artRect, 28f, 28f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(artwork, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                val ph = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryArgb }
                canvas.drawRoundRect(artRect, 28f, 28f, ph)
            }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 255, 255, 255)
                textSize = 34f
            }
            val textLeft = artRect.right + 32f
            val maxHeaderW = (cardRect.right - 56f - textLeft).toInt().coerceAtLeast(200)
            drawWrappedText(canvas, track.title, titlePaint, textLeft, 194f, maxHeaderW, 2)
            drawWrappedText(canvas, track.artist, artistPaint, textLeft, 272f, maxHeaderW, 1)

            // Quote accent bar
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryArgb }
            canvas.drawRoundRect(RectF(132f, 386f, 224f, 396f), 5f, 5f, accentPaint)

            // Selected Lyrics Body
            val quoteText = lines.take(4).joinToString("\n\n")
            val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = if (quoteText.length > 140) 52f else 62f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawWrappedText(
                canvas = canvas,
                text = "“$quoteText”",
                paint = lyricPaint,
                x = 132f,
                y = 436f,
                width = (cardRect.width() - 112f).toInt(),
                maxLines = 10,
            )

            // Footer Brand Watermark
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(185, 255, 255, 255)
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText("CROIX PLAYER PRIME", 132f, cardRect.bottom - 58f, brandPaint)

            saveAndShareBitmap(context, bmp, "croix_lyric_card.png", "Compartir Tarjeta de Letra")
            true
        }.getOrDefault(false)
    }

    suspend fun shareMusicalReceipt(
        context: Context,
        stats: ListeningStatsSummary,
        listenerName: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val w = 1080
            val h = 1540
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Dark slate background with subtle radial vibe
            canvas.drawColor(0xFF0E0D16.toInt())

            // Crisp off-white thermal receipt paper
            val paperRect = RectF(96f, 84f, w - 96f, h - 84f)
            val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF7F5EE.toInt() }
            canvas.drawRoundRect(paperRect, 26f, 26f, paperPaint)

            val monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val monoReg = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            val centerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF141418.toInt()
                textSize = 54f
                typeface = monoBold
                textAlign = Paint.Align.CENTER
            }
            val centerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF50505C.toInt()
                textSize = 26f
                typeface = monoReg
                textAlign = Paint.Align.CENTER
            }
            val leftText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A20.toInt()
                textSize = 28f
                typeface = monoReg
            }
            val leftBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF121216.toInt()
                textSize = 30f
                typeface = monoBold
            }
            val rightBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF121216.toInt()
                textSize = 28f
                typeface = monoBold
                textAlign = Paint.Align.RIGHT
            }
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF888894.toInt()
                textSize = 24f
                typeface = monoReg
                textAlign = Paint.Align.CENTER
            }

            val cx = w / 2f
            val lx = paperRect.left + 58f
            val rx = paperRect.right - 58f
            var y = paperRect.top + 96f

            canvas.drawText("CROIX RECEIPT", cx, y, centerTitle)
            y += 44f
            canvas.drawText("RESUMEN MUSICAL • CROIX PLAYER PRIME", cx, y, centerSub)
            y += 44f
            val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES")).format(Date())
            canvas.drawText("ORDEN #${(stats.totalPlays * 17 + 104)}  |  $dateStr", cx, y, centerSub)
            y += 38f
            canvas.drawText("OYENTE: ${listenerName.uppercase().take(24)}", cx, y, centerSub)
            y += 46f
            canvas.drawText("--------------------------------------------", cx, y, dashPaint)

            y += 48f
            canvas.drawText("CANT  CANCIONES MÁS ESCUCHADAS", lx, y, leftBold)
            canvas.drawText("PLAYS", rx, y, rightBold)
            y += 44f

            val tracksToPrint = stats.topTracks.take(5)
            if (tracksToPrint.isEmpty()) {
                canvas.drawText("01    Aún sin reproducciones", lx, y, leftText)
                y += 44f
            } else {
                tracksToPrint.forEachIndexed { idx, (track, plays) ->
                    val label = "${(idx + 1).toString().padStart(2, '0')}    ${track.title.take(26)}"
                    canvas.drawText(label, lx, y, leftText)
                    canvas.drawText("${plays}x", rx, y, rightBold)
                    y += 34f
                    val subPaint = Paint(leftText).apply { color = 0xFF666672.toInt(); textSize = 22f }
                    canvas.drawText("      ${track.artist.take(32)}", lx, y, subPaint)
                    y += 44f
                }
            }

            y += 12f
            canvas.drawText("--------------------------------------------", cx, y, dashPaint)
            y += 46f
            canvas.drawText("TOP ARTISTAS DEL PERIODO", lx, y, leftBold)
            y += 44f
            stats.topArtists.take(4).forEachIndexed { idx, (artist, plays) ->
                canvas.drawText("${idx + 1}. ${artist.take(28)}", lx, y, leftText)
                canvas.drawText("${plays} plays", rx, y, rightBold)
                y += 40f
            }

            y += 18f
            canvas.drawText("--------------------------------------------", cx, y, dashPaint)
            y += 50f
            canvas.drawText("TOTAL REPRODUCCIONES:", lx, y, leftBold)
            canvas.drawText("${stats.totalPlays}", rx, y, rightBold)
            y += 44f
            canvas.drawText("TIEMPO ESTIMADO:", lx, y, leftBold)
            canvas.drawText("${stats.estimatedMinutes} MIN", rx, y, rightBold)
            if (stats.peakHour in 0..23) {
                y += 44f
                canvas.drawText("HORA PICO MUSICAL:", lx, y, leftBold)
                canvas.drawText("${stats.peakHour.toString().padStart(2, '0')}:00 H", rx, y, rightBold)
            }

            // Stylized Barcode near bottom of receipt
            val barTop = paperRect.bottom - 184f
            val barBot = paperRect.bottom - 94f
            val barPaint = Paint().apply { color = 0xFF141418.toInt() }
            var bx = lx + 40f
            val pattern = intArrayOf(4, 2, 7, 3, 2, 6, 4, 2, 5, 8, 3, 2, 6, 4, 3, 7, 2, 5, 4, 6, 2, 3, 8, 2, 5, 3, 6, 4)
            var idx = 0
            while (bx < rx - 50f) {
                val width = (pattern[idx % pattern.size] * 3).toFloat()
                if (idx % 2 == 0) {
                    canvas.drawRect(bx, barTop, bx + width, barBot, barPaint)
                }
                bx += width + 4f
                idx++
            }
            canvas.drawText("GRACIAS POR ESCUCHAR EN CROIX PRIME", cx, paperRect.bottom - 44f, centerSub)

            saveAndShareBitmap(context, bmp, "croix_receipt.png", "Compartir Recibo Musical")
            true
        }.getOrDefault(false)
    }

    private fun saveAndShareBitmap(context: Context, bmp: Bitmap, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 96, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
        maxLines: Int,
    ) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun blendDark(argb: Int, ratio: Float): Int {
        val r = (Color.red(argb) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
