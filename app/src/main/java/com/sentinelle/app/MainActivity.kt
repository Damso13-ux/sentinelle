package com.sentinelle.app

import android.content.Intent
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.sentinelle.app.ui.screen.OnboardingScreen
import com.sentinelle.app.ui.screen.ReportScreen
import com.sentinelle.app.ui.screen.SettingsScreen
import com.sentinelle.app.ui.theme.AppTheme
import com.sentinelle.app.util.NotificationUtils
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.worker.ListUpdateWorker

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
)

// Three tabs, down from five.
//
// "Signaler" was a tab holding a blank form you had to retype a number
// into from memory. It's now reached from a number you actually saw
// blocked, which is both faster and more accurate — plus an entry in
// Réglages for the rare case of reporting something out of the blue.
//
// "Listes" exposed prefixes, wildcards and priorities: internal machinery
// a general-audience user shouldn't have to meet to use the app. It lives
// in Réglages as "Mes filtres".
//
// Route ids stay as they were. They're internal, and "dashboard" still
// describes what that screen is even though its tab now reads "Activité" —
// renaming them would churn the shortcuts XML and the widget for no
// user-visible gain.
private val bottomNavItems =
    listOf(
        BottomNavItem("home", "Accueil", Icons.Rounded.Home),
        BottomNavItem("dashboard", "Activité", Icons.Rounded.History),
        BottomNavItem("settings", "Réglages", Icons.Rounded.Settings),
    )

class MainActivity : ComponentActivity() {
    private var shortcutDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // auto(), not dark(): the app follows the system day/night setting
        // now, and forcing the dark style would leave light system-bar icons
        // sitting invisibly on a light background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )

        NotificationUtils.createAllNotificationChannels(this)
        downloadListOnLaunchIfNeeded()
        shortcutDestination = intent?.getStringExtra(EXTRA_SHORTCUT_DESTINATION)

        setContent {
            val themeVariant by PreferencesManager
                .getEffectiveThemeVariantFlow(this)
                .collectAsState(initial = com.sentinelle.app.ui.theme.ThemeVariant.INDIGO)

            // null while DataStore is still being read on first frame. Showing
            // the app and then yanking it away for the walkthrough would be
            // worse than a blank frame, so wait for the real value.
            val onboardingCompleted by PreferencesManager
                .getOnboardingCompletedFlow(this)
                .collectAsState(initial = null)

            AppTheme(themeVariant = themeVariant) {
                when (onboardingCompleted) {
                    null -> Unit

                    false ->
                        OnboardingScreen(
                            // setOnboardingCompleted is written by the screen
                            // itself; the flow above then flips this to true.
                            onFinished = {},
                        )

                    true ->
                        SentinelleApp(
                            shortcutDestination = shortcutDestination,
                            onShortcutDestinationConsumed = { shortcutDestination = null },
                        )
                }
            }
        }
    }

    // Handles the app shortcuts (long-press the launcher icon) when the
    // Activity is already running — onCreate only fires on a cold start.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutDestination = intent.getStringExtra(EXTRA_SHORTCUT_DESTINATION)
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

    companion object {
        // Matches the <extra android:name="shortcut_destination"> in
        // res/xml/shortcuts.xml.
        const val EXTRA_SHORTCUT_DESTINATION = "shortcut_destination"
    }
}

// MainActivity is exported (required — it's the launcher), so this extra
// can arrive from any app on the device, not just our own shortcuts.
// navController.navigate() throws on an unknown route, so an unvalidated
// value here is a free crash-on-demand for whoever wants to send it. Keep
// this in sync with shortcuts.xml.
private val VALID_SHORTCUT_DESTINATIONS = setOf("lookup", "report")

// Every navigation *to a bottom-nav destination* has to go through this,
// including the ones triggered from inside a screen (InfoSheet's "Voir les
// statistiques", for instance). A plain navigate() to the same route pushes
// it without the saveState/restoreState bookkeeping the tab bar depends on,
// so the two end up disagreeing about the back stack and the Accueil tab
// stops responding — reachable only with the system Back button.
//
// Destinations that are *not* tabs (lookup, report, my-labels, lists) keep
// using a plain navigate(): they're meant to stack on top of the current
// tab and be dismissed with Back.
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview
@Composable
fun SentinelleApp(
    shortcutDestination: String? = null,
    onShortcutDestinationConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(shortcutDestination) {
        if (shortcutDestination != null) {
            if (shortcutDestination in VALID_SHORTCUT_DESTINATIONS) {
                navController.navigate(shortcutDestination) {
                    popUpTo(navController.graph.findStartDestination().id)
                }
            }
            onShortcutDestinationConsumed()
        }
    }

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
                    // "dashboard" is a tab, so it must not be pushed with a
                    // plain navigate() — see navigateToTab.
                    onOpenDashboard = { navController.navigateToTab("dashboard") },
                    onOpenLookup = { navController.navigate("lookup") },
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    onOpenNumber = { phoneNumber ->
                        navController.navigate("lookup?number=${android.net.Uri.encode("+$phoneNumber")}")
                    },
                )
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
                    onReportNumber = { number ->
                        navController.navigate("report?number=${android.net.Uri.encode(number)}")
                    },
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
            composable(
                "report?number={number}",
                arguments =
                    listOf(
                        navArgument("number") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
            ) { backStackEntry ->
                ReportScreen(initialNumber = backStackEntry.arguments?.getString("number"))
            }
            composable("lists") {
                ListsScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onOpenFilters = { navController.navigate("lists") },
                    onOpenReport = { navController.navigate("report") },
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
                onClick = { navController.navigateToTab(item.route) },
            )
        }
    }
}
