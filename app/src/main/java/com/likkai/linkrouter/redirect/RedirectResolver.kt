package com.likkai.linkrouter.redirect

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class RedirectResolver {

    companion object {
        private const val TAG = "RedirectResolver"
        private const val MAX_REDIRECTS = 10
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 5000
    }

    /**
     * Resolves a potentially shortened URL to its final destination.
     * Follows HTTP redirects (301, 302, 303, 307, 308) up to MAX_REDIRECTS times.
     * Returns the final URL, or the original URL if resolution fails.
     */
    suspend fun resolve(url: String, debug: Boolean = false): String = withContext(Dispatchers.IO) {
        var currentUrl = url
        var redirectCount = 0

        try {
            while (redirectCount < MAX_REDIRECTS) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "HEAD"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("User-Agent", "LinkRouter/1.0")

                try {
                    val responseCode = connection.responseCode

                    if (debug) {
                        Log.d(TAG, "Redirect #$redirectCount: $currentUrl -> HTTP $responseCode")
                    }

                    if (responseCode in 300..399) {
                        val location = connection.getHeaderField("Location")
                        if (location.isNullOrBlank()) {
                            if (debug) Log.d(TAG, "No Location header, stopping")
                            break
                        }

                        // Handle relative URLs
                        currentUrl = try {
                            URL(URL(currentUrl), location).toString()
                        } catch (e: Exception) {
                            location
                        }

                        redirectCount++
                    } else {
                        // Not a redirect, we've reached the final URL
                        break
                    }
                } finally {
                    connection.disconnect()
                }
            }

            if (debug) {
                Log.d(TAG, "Resolved: $url -> $currentUrl (after $redirectCount redirects)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve redirect for $url", e)
            // Return whatever we have so far (might be partially resolved)
        }

        currentUrl
    }
}
