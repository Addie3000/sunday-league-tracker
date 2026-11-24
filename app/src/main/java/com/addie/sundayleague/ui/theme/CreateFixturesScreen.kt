package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.addie.sundayleague.model.Match
import com.addie.sundayleague.model.MatchStatus
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.viewmodel.TournamentViewModel
import com.addie.sundayleague.model.MatchStage

@Composable
fun CreateFixturesScreen(viewModel: TournamentViewModel, onMatchClick: (String) -> Unit) {
    var selectedTeamAId by remember { mutableStateOf("") }
    var selectedTeamBId by remember { mutableStateOf("") }
    var matchPendingDelete by remember { mutableStateOf<Match?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Fixtures",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.teams.size < 2) {
            Text(
                text = "Add at least 2 teams before creating a match.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TeamDropdown(
                        label = "Team A",
                        teams = viewModel.teams,
                        selectedTeamId = selectedTeamAId,
                        onTeamSelected = { selectedTeamAId = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TeamDropdown(
                        label = "Team B",
                        teams = viewModel.teams,
                        selectedTeamId = selectedTeamBId,
                        onTeamSelected = { selectedTeamBId = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.addMatch(selectedTeamAId, selectedTeamBId)
                            selectedTeamAId = ""
                            selectedTeamBId = ""
                        },
                        enabled = selectedTeamAId.isNotEmpty() &&
                                selectedTeamBId.isNotEmpty() &&
                                selectedTeamAId != selectedTeamBId,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Match")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val groupMatches = viewModel.matches.filter { it.stage == MatchStage.GROUP }

        Text(
            text = "${groupMatches.size} match(es) created",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            items(groupMatches) { match ->
                MatchRow(
                    match = match,
                    teamAName = viewModel.getTeamById(match.teamAId)?.name ?: "Unknown",
                    teamBName = viewModel.getTeamById(match.teamBId)?.name ?: "Unknown",
                    onDelete = { matchPendingDelete = match },
                    onClick = { onMatchClick(match.id) }
                )
            }
        }
    }

    matchPendingDelete?.let { match ->
        val teamAName = viewModel.getTeamById(match.teamAId)?.name ?: "Unknown"
        val teamBName = viewModel.getTeamById(match.teamBId)?.name ?: "Unknown"

        AlertDialog(
            onDismissRequest = { matchPendingDelete = null },
            title = { Text("Remove match?") },
            text = { Text("Are you sure you want to remove \"$teamAName vs $teamBName\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMatch(match.id)
                    matchPendingDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDropdown(
    label: String,
    teams: List<Team>,
    selectedTeamId: String,
    onTeamSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeamName = teams.find { it.id == selectedTeamId }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTeamName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    Icons.Filled.SportsSoccer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.name.trim().firstOrNull()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(team.name)
                        }
                    },
                    onClick = {
                        onTeamSelected(team.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MatchRow(
    match: Match,
    teamAName: String,
    teamBName: String,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(70.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchStatusBadge(status = match.status)
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete match",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = teamAName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )

                    if (match.status == MatchStatus.NOT_STARTED) {
                        Text(
                            text = "vs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    } else {
                        Text(
                            text = "${match.scoreA}  -  ${match.scoreB}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }

                    Text(
                        text = teamBName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun MatchStatusBadge(status: MatchStatus) {
    val (label, containerColor, contentColor) = when (status) {
        MatchStatus.NOT_STARTED -> Triple(
            "NOT STARTED",
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        MatchStatus.IN_PROGRESS -> Triple(
            "LIVE",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        MatchStatus.FINISHED -> Triple(
            "FINISHED",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 0.5.sp
        )
    }
}