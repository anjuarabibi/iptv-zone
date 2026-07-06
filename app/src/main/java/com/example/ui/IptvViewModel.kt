package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = IptvRepository(db.iptvDao())

    val playlists = repository.allPlaylists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allChannels = repository.allChannels.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    // Search and Category filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Settings States
    private val _bannerUrl = MutableStateFlow("https://images.unsplash.com/photo-1542281286-9e0a16bb7366")
    val bannerUrl: StateFlow<String> = _bannerUrl.asStateFlow()

    // Cleaning State
    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _cleaningProgress = MutableStateFlow(Pair(0, 0))
    val cleaningProgress: StateFlow<Pair<Int, Int>> = _cleaningProgress.asStateFlow()

    init {
        // Load settings
        viewModelScope.launch {
            val savedBanner = repository.getSetting("banner_url", "https://images.unsplash.com/photo-1542281286-9e0a16bb7366")
            _bannerUrl.value = savedBanner

            // Insert some default mock working IPTV channels to make the first launch incredible
            val current = repository.allChannels.first()
            if (current.isEmpty()) {
                // Add some free test channels
                val demoChannels = listOf(
                    Channel(
                        name = "Sintel HLS Demo",
                        groupTitle = "Movies",
                        logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba",
                        streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                        isFeatured = true,
                        isWorldCup = false,
                        isManuallyAdded = true
                    ),
                    Channel(
                        name = "Big Buck Bunny Stream",
                        groupTitle = "Cartoons",
                        logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        isFeatured = true,
                        isWorldCup = false,
                        isManuallyAdded = true
                    ),
                    Channel(
                        name = "FIFA World Cup Live Match (Demo)",
                        groupTitle = "Sports",
                        logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        isFeatured = true,
                        isWorldCup = true, // World Cup channel pinned
                        isManuallyAdded = true
                    ),
                    Channel(
                        name = "Tears of Steel Movie",
                        groupTitle = "Movies",
                        logoUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        isFeatured = false,
                        isWorldCup = false,
                        isManuallyAdded = true
                    )
                )
                repository.insertChannel(demoChannels[0])
                repository.insertChannel(demoChannels[1])
                repository.insertChannel(demoChannels[2])
                repository.insertChannel(demoChannels[3])
            }
        }
    }

    // Filtered channels
    val filteredChannels = combine(
        allChannels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        channels.filter { channel ->
            // Only show working channels in the main User Panel!
            val matchesWorking = channel.isWorking
            val matchesQuery = channel.name.contains(query, ignoreCase = true) ||
                    channel.groupTitle.contains(query, ignoreCase = true)
            val matchesCategory = category == null || channel.groupTitle == category
            matchesWorking && matchesQuery && matchesCategory
        }.sortedWith(
            compareByDescending<Channel> { it.isWorldCup }
                .thenByDescending { it.isFeatured }
                .thenBy { it.name }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Extracted categories from working channels
    val categories = allChannels.map { list ->
        list.filter { it.isWorking }.map { it.groupTitle }.distinct().sorted()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Playback functions
    fun selectChannel(channel: Channel?) {
        _currentChannel.value = channel
    }

    // Search and filter actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Playlist Actions
    fun addPlaylist(name: String, url: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.addPlaylist(name, url)
            onComplete(result)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun updatePlaylist(playlistId: Long, name: String, url: String) {
        viewModelScope.launch {
            repository.updatePlaylist(playlistId, name, url)
        }
    }

    fun refreshPlaylist(playlistId: Long, url: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.refreshPlaylist(playlistId, url)
            onComplete(result)
        }
    }

    // Channel actions
    fun toggleFeatureChannel(channel: Channel) {
        viewModelScope.launch {
            repository.updateChannel(channel.copy(isFeatured = !channel.isFeatured))
        }
    }

    fun toggleWorldCupChannel(channel: Channel) {
        viewModelScope.launch {
            repository.updateChannel(channel.copy(isWorldCup = !channel.isWorldCup))
        }
    }

    fun addManualChannel(
        name: String,
        streamUrl: String,
        logoUrl: String?,
        groupTitle: String,
        isFeatured: Boolean,
        isWorldCup: Boolean
    ) {
        viewModelScope.launch {
            val channel = Channel(
                name = name,
                streamUrl = streamUrl,
                logoUrl = logoUrl?.ifBlank { null },
                groupTitle = groupTitle.ifBlank { "Manual" },
                isFeatured = isFeatured,
                isWorldCup = isWorldCup,
                isManuallyAdded = true,
                isWorking = true
            )
            repository.insertChannel(channel)
        }
    }

    fun updateManualChannel(channel: Channel) {
        viewModelScope.launch {
            repository.updateChannel(channel)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch {
            repository.deleteChannelById(channelId)
        }
    }

    // Settings actions
    fun updateBannerUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("banner_url", url)
            _bannerUrl.value = url
        }
    }

    // Auto playlist management: Detect and remove broken channels
    fun verifyAndCleanupChannels() {
        if (_isCleaning.value) return
        _isCleaning.value = true
        _cleaningProgress.value = Pair(0, 0)
        
        viewModelScope.launch {
            val brokenCount = repository.verifyAndCleanupChannels { checked, total ->
                _cleaningProgress.value = Pair(checked, total)
            }
            _isCleaning.value = false
        }
    }
}
