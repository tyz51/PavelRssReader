package com.pavel.pavelrssreader.presentation.webview

import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.text.Html
import android.widget.TextView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.Target

/**
 * Html.ImageGetter that loads <img> tags asynchronously using Coil.
 * After each image loads it triggers a TextView re-layout by reassigning textView.text.
 */
class CoilImageGetter(
    textView: TextView,
    private val imageLoader: ImageLoader,
    private val baseUrl: String
) : Html.ImageGetter {

    private val textViewRef = java.lang.ref.WeakReference(textView)

    override fun getDrawable(source: String?): Drawable {
        if (source == null) return LevelListDrawable()
        val resolvedUrl = resolveUrl(source)
        val container = LevelListDrawable()
        container.setBounds(0, 0, 1, 1) // tiny placeholder so parsing doesn't stall

        val request = ImageRequest.Builder(textViewRef.get()?.context ?: return LevelListDrawable())
            .data(resolvedUrl)
            .target(object : Target {
                override fun onSuccess(result: Drawable) {
                    val tv = textViewRef.get() ?: return
                    val availableWidth = tv.width
                        .takeIf { it > 0 }
                        ?: (tv.context.resources.displayMetrics.widthPixels - 64)

                    val scaledWidth: Int
                    val scaledHeight: Int
                    if (result.intrinsicWidth > 0 && result.intrinsicHeight > 0) {
                        scaledWidth = minOf(availableWidth, result.intrinsicWidth)
                        scaledHeight = (scaledWidth * result.intrinsicHeight.toFloat() /
                                result.intrinsicWidth).toInt()
                    } else {
                        scaledWidth = availableWidth
                        scaledHeight = availableWidth / 2
                    }

                    result.setBounds(0, 0, scaledWidth, scaledHeight)
                    container.addLevel(1, 1, result)
                    container.level = 1
                    container.setBounds(0, 0, scaledWidth, scaledHeight)

                    // Reassigning text forces the TextView to re-measure with the new drawable
                    tv.post { tv.text = tv.text }
                }
            })
            .build()

        imageLoader.enqueue(request)
        return container
    }

    private fun resolveUrl(source: String): String {
        if (source.startsWith("http://") || source.startsWith("https://")) return source
        if (source.startsWith("//")) return "https:$source"
        return try {
            java.net.URL(java.net.URL(baseUrl), source).toString()
        } catch (_: Exception) {
            source
        }
    }
}
