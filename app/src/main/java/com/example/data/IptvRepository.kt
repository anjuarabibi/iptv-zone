package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IptvRepository(private val dao: IptvDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36 IPTVZONE/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists()
    val allChannels: Flow<List<Channel>> = dao.getAllChannelsFlow()

    fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return dao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return dao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(Setting(key, value))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistById(playlistId)
        dao.deleteChannelsByPlaylist(playlistId)
    }

    suspend fun updatePlaylist(playlistId: Long, name: String, url: String) {
        val playlist = Playlist(id = playlistId, name = name, url = url, lastUpdated = System.currentTimeMillis())
        dao.insertPlaylist(playlist)
    }

    suspend fun addPlaylist(name: String, url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Fetch playlist content
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                val content = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                
                // Save playlist
                val playlist = Playlist(name = name, url = url)
                val playlistId = dao.insertPlaylist(playlist)

                // Parse and save channels
                val channels = M3uParser.parse(content, playlistId)
                if (channels.isNotEmpty()) {
                    dao.insertChannels(channels)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshPlaylist(playlistId: Long, url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                val content = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                
                // Delete existing channels for this playlist
                dao.deleteChannelsByPlaylist(playlistId)

                // Parse and insert new channels
                val channels = M3uParser.parse(content, playlistId)
                if (channels.isNotEmpty()) {
                    dao.insertChannels(channels)
                }
                
                // Update playlist timestamp
                val currentPlaylists = dao.getAllPlaylists()
                // Fetch existing playlist to update timestamp
                val playlist = Playlist(id = playlistId, name = "Playlist #$playlistId", url = url)
                dao.insertPlaylist(playlist)
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertChannel(channel: Channel): Long {
        return dao.insertChannel(channel)
    }

    suspend fun updateChannel(channel: Channel) {
        dao.updateChannel(channel)
    }

    suspend fun deleteChannelById(channelId: Long) {
        dao.deleteChannelById(channelId)
    }

    // Auto playlist management: Detect and remove broken channels
    suspend fun verifyAndCleanupChannels(onProgress: (Int, Int) -> Unit): Int = withContext(Dispatchers.IO) {
        val channels = dao.getAllChannels()
        if (channels.isEmpty()) return@withContext 0

        var brokenCount = 0
        val total = channels.size
        
        // Let's check in chunks of 10 channels concurrently for maximum speed
        val chunkSize = 10
        for (i in channels.indices step chunkSize) {
            val chunk = channels.subList(i, minOf(i + chunkSize, total))
            val jobs = chunk.map { channel ->
                async {
                    val working = checkStreamUrlWorking(channel.streamUrl)
                    if (!working) {
                        brokenCount++
                        // Update status, or we can mark isWorking = false, or delete.
                        // The instructions say: "Automatically detect and remove broken or non-working IPTV channels. Keep only working IPTV channels available in the app."
                        // So we delete it or set isWorking = false. Deleting keeps the list absolutely clean!
                        // Let's delete non-working channels, or set isWorking = false so user panel doesn't show them.
                        // Setting isWorking = false is better so the admin can still see they are broken, or delete them.
                        // Wait! "Keep only working IPTV channels available in the app" -> Let's delete them or filter them out in user view.
                        // Let's mark them as isWorking = false and filter them out from the main user panel! 
                        // That way admin can see and try to edit them, but user panel only displays working ones. Or we can just delete.
                        // Let's delete the channel if it's from an M3U playlist, or update isWorking = false. Let's do both: update isWorking = false.
                        val updated = channel.copy(isWorking = false)
                        dao.updateChannel(updated)
                    }
                }
            }
            jobs.awaitAll()
            onProgress(minOf(i + chunkSize, total), total)
        }
        brokenCount
    }

    private suspend fun checkStreamUrlWorking(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) return@withContext true
            }
            // If HEAD fails, fallback to GET (some servers block HEAD)
            val requestGet = Request.Builder().url(url).build()
            client.newCall(requestGet).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
