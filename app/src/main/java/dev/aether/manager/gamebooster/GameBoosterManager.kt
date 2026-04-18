package dev.aether.manager.gamebooster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object GameBoosterManager {

    fun isOverlayPermissionGranted(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    fun startBooster(context: Context) {
        val intent = Intent(context, GameBoosterService::class.java)
        context.startForegroundService(intent)
    }

    fun stopBooster(context: Context) {
        val intent = Intent(context, GameBoosterService::class.java).apply {
            action = GameBoosterService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun isRunning(): Boolean = GameBoosterService.isRunning
}
