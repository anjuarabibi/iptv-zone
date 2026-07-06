package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.Playlist
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Manage Channels", "Settings & Auto-Cleanup")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Admin header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "IPTV",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "ZONE",
                    color = PremiumBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    " ADMIN PANEL",
                    color = SecondaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = PremiumCyan
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) Color.White else SecondaryText
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> PlaylistTabContent(viewModel)
                1 -> ChannelTabContent(viewModel)
                2 -> SettingsTabContent(viewModel)
            }
        }
    }
}

// TAB 1: Playlist Management
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistTabContent(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()

    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // Dialog for Editing Playlist
    if (editingPlaylist != null) {
        var editName by remember { mutableStateOf(editingPlaylist!!.name) }
        var editUrl by remember { mutableStateOf(editingPlaylist!!.url) }

        AlertDialog(
            onDismissRequest = { editingPlaylist = null },
            title = { Text("Edit Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1B1A22),
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Playlist Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("M3U Playlist URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editUrl.isBlank()) {
                            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updatePlaylist(editingPlaylist!!.id, editName, editUrl)
                        Toast.makeText(context, "Playlist updated successfully!", Toast.LENGTH_SHORT).show()
                        editingPlaylist = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlaylist = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form to Add M3U Playlist
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Import M3U Playlist",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        label = { Text("M3U Playlist URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (playlistName.isBlank() || playlistUrl.isBlank()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isImporting = true
                            viewModel.addPlaylist(playlistName, playlistUrl) { result ->
                                isImporting = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Playlist imported successfully!", Toast.LENGTH_LONG).show()
                                    playlistName = ""
                                    playlistUrl = ""
                                } else {
                                    Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_playlist_button"),
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Import and Sync Channels", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List of Imported Playlists
        item {
            Text(
                "Imported M3U Playlists",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No playlists imported yet", color = Color.Gray)
                }
            }
        } else {
            items(playlists) { playlist ->
                PlaylistItemCard(
                    playlist = playlist,
                    onEdit = { editingPlaylist = playlist },
                    onRefresh = {
                        Toast.makeText(context, "Refreshing channels...", Toast.LENGTH_SHORT).show()
                        viewModel.refreshPlaylist(playlist.id, playlist.url) { result ->
                            if (result.isSuccess) {
                                Toast.makeText(context, "Playlist refreshed successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Refresh failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onDelete = {
                        viewModel.deletePlaylist(playlist.id)
                        Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: Playlist,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        playlist.url,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_playlist_${playlist.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PremiumCyan)
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_playlist_${playlist.id}")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Green)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_playlist_${playlist.id}")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}

// TAB 2: Channels Management (Manual Insertion & Star/FIFA toggling)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelTabContent(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val allChannels by viewModel.allChannels.collectAsState()

    var chName by remember { mutableStateOf("") }
    var chStreamUrl by remember { mutableStateOf("") }
    var chLogoUrl by remember { mutableStateOf("") }
    var chGroup by remember { mutableStateOf("") }
    var chIsFeatured by remember { mutableStateOf(false) }
    var chIsWorldCup by remember { mutableStateOf(false) }

    var editingChannel by remember { mutableStateOf<Channel?>(null) }

    val manualImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            chLogoUrl = uri.toString()
        }
    }

    // Dialog for Editing Channel
    if (editingChannel != null) {
        var editName by remember { mutableStateOf(editingChannel!!.name) }
        var editStreamUrl by remember { mutableStateOf(editingChannel!!.streamUrl) }
        var editLogoUrl by remember { mutableStateOf(editingChannel!!.logoUrl ?: "") }
        var editGroup by remember { mutableStateOf(editingChannel!!.groupTitle) }
        var editIsFeatured by remember { mutableStateOf(editingChannel!!.isFeatured) }
        var editIsWorldCup by remember { mutableStateOf(editingChannel!!.isWorldCup) }

        val editImagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                editLogoUrl = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { editingChannel = null },
            title = { Text("Edit Channel", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1B1A22),
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Channel Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = editStreamUrl,
                        onValueChange = { editStreamUrl = it },
                        label = { Text("Stream URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editLogoUrl,
                            onValueChange = { editLogoUrl = it },
                            label = { Text("Logo / Image URL") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Button(
                            onClick = { editImagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Image")
                        }
                    }

                    OutlinedTextField(
                        value = editGroup,
                        onValueChange = { editGroup = it },
                        label = { Text("Category / Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = editIsFeatured,
                                onCheckedChange = { editIsFeatured = it }
                            )
                            Text("Featured (Star)", color = Color.LightGray, fontSize = 14.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = editIsWorldCup,
                                onCheckedChange = { editIsWorldCup = it }
                            )
                            Text("FIFA World Cup", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editStreamUrl.isBlank()) {
                            Toast.makeText(context, "Name and Stream URL are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val updatedChannel = editingChannel!!.copy(
                            name = editName,
                            streamUrl = editStreamUrl,
                            logoUrl = if (editLogoUrl.isBlank()) null else editLogoUrl,
                            groupTitle = editGroup.ifBlank { "Manual" },
                            isFeatured = editIsFeatured,
                            isWorldCup = editIsWorldCup
                        )
                        viewModel.updateManualChannel(updatedChannel)
                        Toast.makeText(context, "Channel updated successfully!", Toast.LENGTH_SHORT).show()
                        editingChannel = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingChannel = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form to Manually Add Channel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Add Channel Manually",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = chName,
                        onValueChange = { chName = it },
                        label = { Text("Channel Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chStreamUrl,
                        onValueChange = { chStreamUrl = it },
                        label = { Text("Stream URL (M3U8, MP4, etc.)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chLogoUrl,
                            onValueChange = { chLogoUrl = it },
                            label = { Text("Logo / Image URL (Optional)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("channel_logo_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Button(
                            onClick = { manualImagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chGroup,
                        onValueChange = { chGroup = it },
                        label = { Text("Category / Group Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_group_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Featured & WorldCup checkboxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = chIsFeatured,
                                onCheckedChange = { chIsFeatured = it },
                                modifier = Modifier.testTag("channel_featured_checkbox")
                            )
                            Text("Featured (Star)", color = Color.LightGray, fontSize = 14.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = chIsWorldCup,
                                onCheckedChange = { chIsWorldCup = it },
                                modifier = Modifier.testTag("channel_world_cup_checkbox")
                            )
                            Text("FIFA World Cup Live Pinned", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (chName.isBlank() || chStreamUrl.isBlank()) {
                                Toast.makeText(context, "Name and Stream URL are required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addManualChannel(
                                name = chName,
                                streamUrl = chStreamUrl,
                                logoUrl = chLogoUrl,
                                groupTitle = chGroup,
                                isFeatured = chIsFeatured,
                                isWorldCup = chIsWorldCup
                            )
                            Toast.makeText(context, "Channel added successfully!", Toast.LENGTH_SHORT).show()
                            chName = ""
                            chStreamUrl = ""
                            chLogoUrl = ""
                            chGroup = ""
                            chIsFeatured = false
                            chIsWorldCup = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_channel_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add to Channel List", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of all active channels with Star and FIFA quick toggles
        item {
            Text(
                "App Channel Database (${allChannels.size} Channels)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (allChannels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No channels registered", color = Color.Gray)
                }
            }
        } else {
            items(allChannels) { channel ->
                AdminChannelCard(
                    channel = channel,
                    onEdit = { editingChannel = channel },
                    onToggleFeatured = { viewModel.toggleFeatureChannel(channel) },
                    onToggleWorldCup = { viewModel.toggleWorldCupChannel(channel) },
                    onDelete = {
                        viewModel.deleteChannel(channel.id)
                        Toast.makeText(context, "Channel removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AdminChannelCard(
    channel: Channel,
    onEdit: () -> Unit,
    onToggleFeatured: () -> Unit,
    onToggleWorldCup: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "Channel logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Tv, contentDescription = "TV", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    channel.groupTitle,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!channel.isWorking) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "⚠️ Connection Broken",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row {
                // Edit Button
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Channel",
                        tint = PremiumCyan
                    )
                }

                // Feature/Star Toggle
                IconButton(onClick = onToggleFeatured) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Featured Star",
                        tint = if (channel.isFeatured) Color.Yellow else Color.Gray
                    )
                }

                // WorldCup/FIFA Toggle
                IconButton(onClick = onToggleWorldCup) {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = "World Cup",
                        tint = if (channel.isWorldCup) Color(0xFFE5A93B) else Color.Gray
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

// TAB 3: Settings Page & Playlist Health Auto-cleanup
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabContent(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val bannerUrl by viewModel.bannerUrl.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val progress by viewModel.cleaningProgress.collectAsState()

    var inputBannerUrl by remember(bannerUrl) { mutableStateOf(bannerUrl) }

    val bannerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            inputBannerUrl = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App settings section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Home Page Header Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputBannerUrl,
                            onValueChange = { inputBannerUrl = it },
                            label = { Text("Banner Image URL") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("banner_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Button(
                            onClick = { bannerImagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Upload Banner")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateBannerUrl(inputBannerUrl)
                            Toast.makeText(context, "Banner image updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_banner_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Banner", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Automatic Playlist Management (Verify & Cleanup)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Automatic Playlist Health Check",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This tool scans all imported and manually added channels in the database to detect and mark broken or unreachable streams, ensuring users only see working channels.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCleaning) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Analyzing Stream Connections...", color = Color.LightGray, fontSize = 14.sp)
                                Text("${progress.first}/${progress.second}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f
                                },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.verifyAndCleanupChannels()
                                Toast.makeText(context, "Scanning stream connections...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_cleanup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = "Clean")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan and Remove Broken Channels", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
