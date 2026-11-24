package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.addie.sundayleague.model.Tournament
import com.addie.sundayleague.model.TournamentStatus
import com.addie.sundayleague.model.championName
import com.addie.sundayleague.model.status
import com.addie.sundayleague.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import com.addie.sundayleague.R

@Composable
fun HomeScreen(viewModel: TournamentViewModel, onTournamentOpened: () -> Unit) {
    var tournamentPendingDelete by remember { mutableStateOf<Tournament?>(null) }
    var tournamentPendingRename by remember { mutableStateOf<Tournament?>(null) }

    val sortedTournaments = viewModel.tournaments.sortedByDescending { it.createdAt }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader()

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tournaments",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sortedTournaments.size}/10",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    viewModel.createTournament()
                    onTournamentOpened()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create New Tournament")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sortedTournaments.isEmpty()) {
                EmptyHomeState()
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sortedTournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onOpen = {
                                viewModel.openTournament(tournament.id)
                                onTournamentOpened()
                            },
                            onRename = { tournamentPendingRename = tournament },
                            onDelete = { tournamentPendingDelete = tournament }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }

    tournamentPendingDelete?.let { tournament ->
        AlertDialog(
            onDismissRequest = { tournamentPendingDelete = null },
            title = { Text("Delete tournament?") },
            text = {
                Text("Are you sure you want to permanently delete \"${tournament.name}\"? All teams, fixtures, and results will be lost. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTournament(tournament.id)
                    tournamentPendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tournamentPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    tournamentPendingRename?.let { tournament ->
        RenameTournamentDialog(
            tournament = tournament,
            onDismiss = { tournamentPendingRename = null },
            onConfirm = { newName ->
                viewModel.renameTournament(tournament.id, newName)
                tournamentPendingRename = null
            }
        )
    }
}

@Composable
fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(top = 40.dp, bottom = 24.dp, start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_ball_net),
            contentDescription = null,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SUNDAY LEAGUE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "TRACKER",
                style = MaterialTheme.typography.titleSmall,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )
        }

        Text(
            text = "7",
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            color = Color.White
        )
    }
}

@Composable
fun EmptyHomeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tournaments yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Create your first tournament to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val status = tournament.status()
    val champion = tournament.championName()
    val dateText = remember(tournament.createdAt) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tournament.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onOpen
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Rename tournament",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete tournament",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${tournament.teams.size} team(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TournamentStatusBadge(status = status, championName = champion)
            }
        }
    }
}

@Composable
fun TournamentStatusBadge(status: TournamentStatus, championName: String?) {
    val (label, containerColor, contentColor) = when (status) {
        TournamentStatus.NOT_STARTED -> Triple(
            "NOT STARTED",
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TournamentStatus.IN_PROGRESS -> Triple(
            "IN PROGRESS",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        TournamentStatus.COMPLETED -> Triple(
            if (championName != null) "CHAMPION: ${championName.uppercase()}" else "COMPLETED",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun RenameTournamentDialog(
    tournament: Tournament,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(tournament.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename tournament") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    if (it.length <= 30) {
                        nameInput = it
                        error = null
                    }
                },
                label = { Text("Tournament name") },
                isError = error != null,
                supportingText = { if (error != null) Text(error!!) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameInput.trim().isEmpty()) {
                    error = "Name cannot be empty"
                } else {
                    onConfirm(nameInput.trim())
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}