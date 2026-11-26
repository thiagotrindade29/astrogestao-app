package com.example.data.repository

object VersionComparator {
    fun parseParts(versionName: String): List<Int> {
        val clean = versionName.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (clean.isBlank()) return emptyList()
        return clean.split(".")
            .mapNotNull { seg -> seg.trim().takeWhile { it.isDigit() }.toIntOrNull() }
    }

    fun compare(currentVersion: String, targetVersion: String): Int {
        val a = parseParts(currentVersion)
        val b = parseParts(targetVersion)
        if (a.isEmpty() && b.isEmpty()) {
            return currentVersion.trim().compareTo(targetVersion.trim(), ignoreCase = true)
        }
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val pa = a.getOrElse(i) { 0 }
            val pb = b.getOrElse(i) { 0 }
            if (pa != pb) return pa.compareTo(pb)
        }
        return 0
    }

    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        if (latestVersion.isBlank()) return false
        return compare(currentVersion, latestVersion) < 0
    }
}
