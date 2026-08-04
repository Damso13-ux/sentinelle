package com.sentinelle.app

import android.os.Bundle
import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sentinelle.app.ui.screen.DashboardScreen
import com.sentinelle.app.ui.screen.HomeScreen
import com.sentinelle.app.ui.screen.ListsScreen
import com.sentinelle.app.ui.screen.LookupScreen
import com.sentinelle.app.ui.screen.MyLabelsScreen
import com.sentinelle.app.ui.screen.ReportScreen
import com.sentinelle.app.ui.screen.SettingsScreen
import com.sentinelle.app.ui.theme.AppTheme
import com.sentinelle.app.util.NotificationUtils
import com.sentinelle.app.worker.ListUpdateWorker

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
)

private val bottomNavItems =
    listOf(
        BottomNavItem("home", "Accueil", Icons.Rounded.Home),
        BottomNavItem("dashboard", "Statistiques", Icons.Rounded.BarChart),
        BottomNavItem("report", "Signaler", Icons.Rounded.Campaign),
        BottomNavItem("lists", "Listes", Icons.AutoMirrored.Rounded.FormatListBulleted),
        BottomNavItem("settings", "Réglages", Icons.Rounded.Settings),
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sentinelle always renders its dark "Garde" theme, so status/nav bar
        // icons stay light regardless of the system's day/night setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        NotificationUtils.createAllNotificationChannels(this)
        downloadListOnLaunchIfNeeded()

        setContent {
            AppTheme {
                SentinelleApp()
            }
        }
    }

    private fun downloadListOnLaunchIfNeeded() {
        Log.d("MainActivity", "Enqueuing one-time list update on app launch")
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val updateRequest =
            OneTimeWorkRequestBuilder<ListUpdateWorker>()
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            ListUpdateWorker.WORK_NAME_LAUNCH,
            ExistingWorkPolicy.KEEP,
            updateRequest,
        )
    }
}

@Preview
@Composable
fun SentinelleApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigationBar(navController) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
        ) {
            composable("home") {
                HomeScreen(
                    onOpenDashboard = { navController.navigate("dashboard") },
                    onOpenLookup = { navController.navigate("lookup") },
                )
            }
            composable("dashboard") {
                DashboardScreen()
            }
            composable(
                "lookup?number={number}",
                arguments =
                    listOf(
                        navArgument("number") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
            ) { backStackEntry ->
                LookupScreen(
                    initialNumber = backStackEntry.arguments?.getString("number"),
                    onOpenMyLabels = { navController.navigate("my-labels") },
                )
            }
            composable("my-labels") {
                MyLabelsScreen(
                    onOpenNumber = { number ->
                        navController.navigate("lookup?number=${android.net.Uri.encode(number)}")
                    },
                )
            }
            composable("report") {
                ReportScreen()
            }
            composable("lists") {
                ListsScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onResetApp = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
