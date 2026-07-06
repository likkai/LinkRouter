package com.likkai.linkrouter.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

class BrowserLauncher(
    private val context: Context,
    private val browserResolver: BrowserResolver
) {

    companion object {
        private const val TAG = "BrowserLauncher"
    }

    /**
     * Launch a specific browser by package name.
     * Returns true if launched successfully, false otherwise.
     */
    fun launch(packageName: String, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Browser not found: $packageName", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser: $packageName", e)
            false
        }
    }

    /**
     * Fallback: find any installed browser (excluding this app) and launch it.
     * Returns true if a browser was found and launched.
     */
    fun launchFallback(url: String): Boolean {
        val browsers = browserResolver.getInstalledBrowsers()
        if (browsers.isEmpty()) {
            Log.e(TAG, "No browsers found on device")
            Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
            return false
        }

        // Try each available browser
        for (browser in browsers) {
            if (launch(browser.packageName, url)) {
                return true
            }
        }

        Log.e(TAG, "Could not launch any available browser")
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        return false
    }
}
