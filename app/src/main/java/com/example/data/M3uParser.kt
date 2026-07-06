package com.example.data

import java.io.BufferedReader
import java.io.StringReader

object M3uParser {
    fun parse(m3uContent: String, playlistId: Long? = null): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String? = reader.readLine()

        var currentName = ""
        var currentLogoUrl: String? = null
        var currentGroup = "Default"

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse EXTINF line
                currentLogoUrl = parseAttribute(trimmed, "tvg-logo")
                currentGroup = parseAttribute(trimmed, "group-title") ?: parseAttribute(trimmed, "group_title") ?: "Default"
                
                // Name is usually after the last comma
                val commaIndex = trimmed.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    parseAttribute(trimmed, "tvg-name") ?: "Unknown Channel"
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This line is the stream URL
                if (currentName.isNotEmpty()) {
                    val streamUrl = trimmed
                    
                    // Fulfill User Panel requirement: Keep FIFA World Cup Live channels pinned at the top
                    val nameLower = currentName.lowercase()
                    val groupLower = currentGroup.lowercase()
                    val isWorldCup = nameLower.contains("fifa") || nameLower.contains("world cup") || nameLower.contains("worldcup") ||
                                     groupLower.contains("fifa") || groupLower.contains("world cup") || groupLower.contains("worldcup")

                    channels.add(
                        Channel(
                            playlistId = playlistId,
                            name = currentName,
                            groupTitle = currentGroup,
                            logoUrl = currentLogoUrl,
                            streamUrl = streamUrl,
                            isFeatured = false,
                            isWorldCup = isWorldCup,
                            isWorking = true,
                            isManuallyAdded = false
                        )
                    )
                    // Reset current info
                    currentName = ""
                    currentLogoUrl = null
                    currentGroup = "Default"
                }
            }
            line = reader.readLine()
        }
        return channels
    }

    private fun parseAttribute(line: String, attributeName: String): String? {
        val searchKey = "$attributeName=\""
        val startIndex = line.indexOf(searchKey)
        if (startIndex == -1) return null
        
        val valueStart = startIndex + searchKey.length
        val endIndex = line.indexOf('"', valueStart)
        if (endIndex == -1) return null
        
        return line.substring(valueStart, endIndex)
    }
}
