package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.IptvViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CategoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: IptvViewModel = viewModel()
                var currentTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = PremiumCyan,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = PremiumBlue,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("tab_home")
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Category, contentDescription = "Category") },
                                label = { Text("Category", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = PremiumBlue,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("tab_category")
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.Tv, contentDescription = "Live TV") },
                                label = { Text("Live TV", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = PremiumBlue,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("tab_live_tv")
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Admin") },
                                label = { Text("Admin", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = PremiumBlue,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("tab_admin")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(DarkBg)
                    ) {
                        when (currentTab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToLiveTv = { currentTab = 2 },
                                onNavigateToAdmin = { currentTab = 3 }
                            )
                            1 -> CategoryScreen(
                                viewModel = viewModel,
                                onNavigateToLiveTv = { currentTab = 2 }
                            )
                            2 -> LiveTvScreen(
                                viewModel = viewModel
                            )
                            3 -> AdminPanelScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
