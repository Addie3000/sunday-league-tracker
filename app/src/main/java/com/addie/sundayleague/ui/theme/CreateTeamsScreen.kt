package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.viewmodel.TournamentViewModel
import androidx.compose.foundation.border

private const val MAX_TEAM_NAME_LENGTH = 20

@Composable
fun CreateTeamsScreen(viewModel: TournamentViewModel) {
    var teamNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teamPendingDelete by remember { mutableStateOf<Team?>(null) }
    var teamPendingEdit by remember { mutableStateOf<Team?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Teams",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = teamNameInput,
            onValueChange = {
                if (it.length <= MAX_TEAM_NAME_LENGTH) {
                    teamNameInput = it
                    errorMessage = null
                }
            },
            label = { Text("Team name") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage!!)
                } else {
                    Text("${teamNameInput.length}/$MAX_TEAM_NAME_LENGTH")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val trimmed = teamNameInput.trim()
                when {
                    trimmed.isEmpty() -> errorMessage = "Team name cannot be empty"
                    viewModel.teams.any { it.name.equals(trimmed, ignoreCase = true) } ->
                        errorMessage = "A team with this name already exists"
                    else -> {
                        viewModel.addTeam(trimmed)
                        teamNameInput = ""
                        errorMessage = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Team")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.teams.isEmpty()) {
            EmptyTeamsState()
        } else {
            Text(
                text = "${viewModel.teams.size} team(s) added",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(viewModel.teams) { team ->
                    TeamRow(
                        team = team,
                        onEdit = { teamPendingEdit = team },
                        onDelete = { teamPendingDelete = team }
                    )
                }
            }
        }
    }

    teamPendingDelete?.let { team ->
        AlertDialog(
            onDismissRequest = { teamPendingDelete = null },
            title = { Text("Remove team?") },
            text = { Text("Are you sure you want to remove \"${team.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTeam(team.id)
                    teamPendingDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { teamPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    teamPendingEdit?.let { team ->
        EditTeamDialog(
            team = team,
            existingNames = viewModel.teams
                .filter { it.id != team.id }
                .map { it.name },
            onDismiss = { teamPendingEdit = null },
            onConfirm = { newName ->
                viewModel.renameTeam(team.id, newName)
                teamPendingEdit = null
            }
        )
    }
}

@Composable
fun EditTeamDialog(
    team: Team,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(team.name) }
    var editError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename team") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    if (it.length <= MAX_TEAM_NAME_LENGTH) {
                        nameInput = it
                        editError = null
                    }
                },
                label = { Text("Team name") },
                isError = editError != null,
                supportingText = {
                    if (editError != null) {
                        Text(editError!!)
                    }else {
                        Text("${nameInput.length}/$MAX_TEAM_NAME_LENGTH")
                    }
                },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = nameInput.trim()
                when {
                    trimmed.isEmpty() -> editError = "Team name cannot be empty"
                    existingNames.any { it.equals(trimmed, ignoreCase = true) } ->
                        editError = "A team with this name already exists"
                    else -> onConfirm(trimmed)
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

@Composable
fun EmptyTeamsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "No teams yet", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add your first team using the field above",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TeamRow(team: Team, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamAvatar(name = team.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = team.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Tournament squad",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit team",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete team",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .then(
                Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                    CircleShape
                )
        )
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}