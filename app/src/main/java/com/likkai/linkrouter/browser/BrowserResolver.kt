package com.likkai.linkrouter.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri

class BrowserResolver(private val context: Context) {

    /**
     * Returns a list of installed browser apps, excluding this app.
     */
    fun getInstalledBrowsers(): List<BrowserApp> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )

        val ownPackage = context.packageName

        return resolveInfos
            .filter { it.activityInfo.packageName != ownPackage }
            .map { resolveInfo ->
                BrowserApp(
                    label = resolveInfo.loadLabel(context.packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }
}
