package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long? = null,
    val name: String,
    val groupTitle: String = "Default",
    val logoUrl: String? = null,
    val streamUrl: String,
    val isFeatured: Boolean = false,
    val isWorldCup: Boolean = false,
    val isManuallyAdded: Boolean = false,
    val isWorking: Boolean = true,
    val orderIndex: Int = 0
)
