package com.weartak.plugin.lace

object HostTarget {
    val hostPackageName: String?
        get() = com.weartak.plugin.lace.host.LacePluginRuntime.currentSnapshot().hostPackageName
}
