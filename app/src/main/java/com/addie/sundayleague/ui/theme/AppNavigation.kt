package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.addie.sundayleague.data.WhatsNewEntry
import com.addie.sundayleague.service.MatchTimerService
import com.addie.sundayleague.viewmodel.TournamentViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp



sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Teams : Screen("teams", "Teams")
    object Fixtures : Screen("fixtures", "Fixtures")
    object Table : Screen("table", "Table")
    object Knockouts : Screen("knockouts", "Knockouts")
    object LiveMatch : Screen("live_match/{matchId}", "Live Match")
}

private val TOURNAMENT_TAB_ROUTES = setOf(
    Screen.Teams.route, Screen.Fixtures.route, Screen.Table.route, Screen.Knockouts.route
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: TournamentViewModel) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Teams, Screen.Fixtures, Screen.Table, Screen.Knockouts)

    var pendingWhatsNew by remember { mutableStateOf(viewModel.getPendingWhatsNew()) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route


    val isTournamentTabScreen = currentDestination?.hierarchy?.any {
        it.route in TOURNAMENT_TAB_ROUTES
    } == true
    val isLiveMatchScreen = currentDestination?.hierarchy?.any {
        it.route == Screen.LiveMatch.route
    } == true

    fun goHome() {
        viewModel.closeTournament()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        val activeMatchId = MatchTimerService.activeMatchId.value

        if (activeMatchId != null) {
            val owningTournamentId = viewModel.findTournamentIdContainingMatch(activeMatchId)
            if (owningTournamentId != null) {
                if (viewModel.openTournamentId != owningTournamentId) {
                    viewModel.openTournament(owningTournamentId)
                }
                navController.navigate("live_match/$activeMatchId") {
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
        }

        if (viewModel.openTournamentId != null) {
            navController.navigate(Screen.Teams.route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (isTournamentTabScreen) {
                TopAppBar(
                    title = {
                        Text(text = viewModel.openTournamentName(), maxLines = 1)
                    },
                    navigationIcon = {
                        IconButton(onClick = { goHome() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isTournamentTabScreen) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Teams.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                val icon = when (screen) {
                                    Screen.Teams -> Icons.Filled.Group
                                    Screen.Fixtures -> Icons.Filled.SportsSoccer
                                    Screen.Table -> Icons.Filled.EmojiEvents
                                    Screen.Knockouts -> Icons.Filled.Whatshot
                                    else -> Icons.Filled.Group
                                }
                                Icon(icon, contentDescription = screen.label)
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = if (isLiveMatchScreen) {
            Modifier
        } else {
            Modifier.padding(innerPadding)
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = contentModifier
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onTournamentOpened = {
                        navController.navigate(Screen.Teams.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Teams.route) {
                CreateTeamsScreen(viewModel = viewModel)
            }

            composable(Screen.Fixtures.route) {
                CreateFixturesScreen(
                    viewModel = viewModel,
                    onMatchClick = { matchId ->
                        navController.navigate("live_match/$matchId")
                    }
                )
            }


            composable(Screen.Table.route) {
                PointsTableScreen(viewModel = viewModel)
            }

            composable(Screen.Knockouts.route) {
                KnockoutsScreen(
                    viewModel = viewModel,
                    onMatchClick = { matchId ->
                        navController.navigate("live_match/$matchId")
                    }
                )
            }


            composable(
                route = Screen.LiveMatch.route,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                LiveMatchScreen(
                    matchId = matchId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onFinished = {navController.popBackStack()}
                )
            }
        }
    }

    pendingWhatsNew?.let { entry ->
        WhatsNewDialog(
            entry = entry,
            onDismiss = {
                viewModel.markWhatsNewSeen()
                pendingWhatsNew = null
            }
        )
    }
}

@Composable
fun WhatsNewDialog(entry: WhatsNewEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                VersionBadge(versionName = entry.versionName)
            }
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                entry.features.forEachIndexed { index, feature ->
                    val accentColor = if (index % 2 == 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (index % 2 == 0)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    )
}

@Composable
fun VersionBadge(versionName: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "V $versionName",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSecondary,
            letterSpacing = 0.5.sp
        )
    }
}