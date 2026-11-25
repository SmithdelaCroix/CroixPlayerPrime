/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

data class AppIconOption(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val aliasClassName: String,
    val primaryHex: Long,
    val secondaryHex: Long,
)

object AppIconManager {
    val options: List<AppIconOption> = listOf(
        AppIconOption(
            id = "violet",
            labelEs = "Cristal Violeta",
            labelEn = "Violet Glass",
            aliasClassName = "com.croix.player.prime.IconVioletAlias",
            primaryHex = 0xFFA238FF,
            secondaryHex = 0xFFFF2E93,
        ),
        AppIconOption(
            id = "cyan",
            labelEs = "Neón Cian",
            labelEn = "Neon Cyan",
            aliasClassName = "com.croix.player.prime.IconCyanAlias",
            primaryHex = 0xFF00E5FF,
            secondaryHex = 0xFF3D5AFE,
        ),
        AppIconOption(
            id = "obsidian",
            labelEs = "Obsidiana Oro",
            labelEn = "Obsidian Gold",
            aliasClassName = "com.croix.player.prime.IconObsidianAlias",
            primaryHex = 0xFFFFD54F,
            secondaryHex = 0xFF141418,
        ),
        AppIconOption(
            id = "emerald",
            labelEs = "Esmeralda Prime",
            labelEn = "Emerald Prime",
            aliasClassName = "com.croix.player.prime.IconEmeraldAlias",
            primaryHex = 0xFF00E676,
            secondaryHex = 0xFF00BFA5,
        ),
    )

    fun applyIcon(context: Context, iconId: String) {
        val pm = context.packageManager
        val pkg = context.packageName
        val target = options.firstOrNull { it.id == iconId } ?: options.first()

        // Enable target alias FIRST so launcher always sees an active entry point
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, target.aliasClassName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        options.filter { it.id != target.id }.forEach { other ->
            runCatching {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, other.aliasClassName),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
