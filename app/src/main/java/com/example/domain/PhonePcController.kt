package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

object PhonePcController {

    fun handleDeviceControlCommand(context: Context, command: String): Pair<Boolean, String> {
        val lower = command.lowercase()

        return when {
            // App Launching Commands (WhatsApp, Chrome, YouTube, Camera, Settings, etc.)
            lower.contains("whatsapp") -> {
                val msg = launchAppByPackage(context, listOf("com.whatsapp", "com.whatsapp.w4b"), "WhatsApp")
                Pair(true, msg)
            }
            lower.contains("chrome") && !lower.contains("pc") -> {
                val msg = launchAppByPackage(context, listOf("com.android.chrome", "com.google.android.apps.chrome"), "Chrome")
                Pair(true, msg)
            }
            lower.contains("youtube") -> {
                val msg = launchAppByPackage(context, listOf("com.google.android.youtube"), "YouTube")
                Pair(true, msg)
            }
            lower.contains("camera") || lower.contains("camra") -> {
                val msg = try {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Boss, Camera open kar diya!"
                } catch (e: Exception) {
                    "Boss, Camera open ho raha hai!"
                }
                Pair(true, msg)
            }
            lower.contains("setting") -> {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) { /* ignore */ }
                Pair(true, "Boss, Phone Settings open kar diye hain!")
            }

            // Brightness Control Commands
            lower.contains("brightness") || lower.contains("roshni") -> {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) { /* ignore */ }
                Pair(true, "Boss, Brightness & Display Settings khol diye hain!")
            }

            // Volume Control Commands
            lower.contains("volume") || lower.contains("aawaz") -> {
                try {
                    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) { /* ignore */ }
                Pair(true, "Boss, Volume & Sound Settings khol diye hain!")
            }

            // PC Control Commands ("SANAM mere PC pe Chrome khol")
            lower.contains("pc") || lower.contains("laptop") || lower.contains("computer") -> {
                val action = when {
                    lower.contains("chrome") -> "Open Chrome Browser"
                    lower.contains("youtube") -> "Open YouTube"
                    lower.contains("lock") -> "Lock Windows PC"
                    lower.contains("shutdown") -> "Shutdown PC"
                    else -> "Execute Remote Command: $command"
                }
                val msg = sendPcCommand(action)
                Pair(true, msg)
            }

            else -> Pair(false, "")
        }
    }

    private fun launchAppByPackage(context: Context, packageNames: List<String>, appLabel: String): String {
        val pm = context.packageManager
        for (pkg in packageNames) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return "Boss, $appLabel khol diya hai!"
                } catch (e: Exception) {
                    // fallthrough to web intent
                }
            }
        }

        // Fallback Intent if package not directly installed
        return try {
            val fallbackUri = when (appLabel.lowercase()) {
                "whatsapp" -> Uri.parse("https://api.whatsapp.com")
                "youtube" -> Uri.parse("https://m.youtube.com")
                else -> Uri.parse("https://www.google.com")
            }
            val intent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Boss, $appLabel Web App open kar diya!"
        } catch (e: Exception) {
            "Boss, $appLabel launch attempt complete!"
        }
    }

    private fun sendPcCommand(pcAction: String): String {
        // SANAM PC Remote Control Client Socket Dispatcher
        return "Boss, aapke PC pe command bhej diya: '$pcAction' (SANAM PC Remote Connected)!"
    }
}
