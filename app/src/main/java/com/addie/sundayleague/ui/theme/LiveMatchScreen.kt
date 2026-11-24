package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.addie.sundayleague.model.MatchStatus
import com.addie.sundayleague.service.MatchTimerService
import com.addie.sundayleague.viewmodel.TournamentViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip

private val DURATION_OPTIONS = listOf(1, 5, 10, 15, 20, 25, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMatchScreen(
    matchId: String,
    viewModel: TournamentViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val match = viewModel.getMatchById(matchId)

    if (match == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val teamAName = viewModel.getTeamById(match.teamAId)?.name ?: "Team A"
    val teamBName = viewModel.getTeamById(match.teamBId)?.name ?: "Team B"

    var selectedDuration by remember { mutableStateOf(viewModel.matchDurationMinutes) }
    var durationExpanded by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }

    val serviceActiveMatchId by MatchTimerService.activeMatchId.collectAsState()
    val serviceRemainingSeconds by MatchTimerService.remainingSeconds.collectAsState()
    val serviceIsRunning by MatchTimerService.isRunning.collectAsState()

    val isThisMatchActive = serviceActiveMatchId == matchId
    val remainingSeconds = if (isThisMatchActive) serviceRemainingSeconds else selectedDuration * 60
    val isRunning = isThisMatchActive && serviceIsRunning

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Live Match",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MatchScoreboardCard(teamAName = teamAName, teamBName = teamBName, match = match)

            Spacer(modifier = Modifier.height(28.dp))

            if (match.status == MatchStatus.NOT_STARTED) {
                StartMatchCard(
                    selectedDuration = selectedDuration,
                    durationExpanded = durationExpanded,
                    onExpandedChange = { durationExpanded = it },
                    onDurationSelected = { selectedDuration = it },
                    onStart = { viewModel.startMatchWithDuration(matchId, selectedDuration) }
                )
            } else {
                TimerCard(timeText = timeText, isRunning = isRunning, isFinished = match.status == MatchStatus.FINISHED)

                if (match.status == MatchStatus.IN_PROGRESS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TimerControls(
                        isRunning = isRunning,
                        remainingSeconds = remainingSeconds,
                        onPauseResume = {
                            if (isRunning) showPauseConfirm = true else viewModel.resumeTimer()
                        },
                        onReset = { showResetConfirm = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreCounter(
                    teamName = teamAName,
                    score = match.scoreA,
                    onIncrement = { viewModel.incrementScoreA(matchId) },
                    onDecrement = { viewModel.decrementScoreA(matchId) },
                    enabled = match.status == MatchStatus.IN_PROGRESS,
                    modifier = Modifier.weight(1f)
                )
                ScoreCounter(
                    teamName = teamBName,
                    score = match.scoreB,
                    onIncrement = { viewModel.incrementScoreB(matchId) },
                    onDecrement = { viewModel.decrementScoreB(matchId) },
                    enabled = match.status == MatchStatus.IN_PROGRESS,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (match.status != MatchStatus.FINISHED) {
                Button(
                    onClick = {
                        if (match.status == MatchStatus.IN_PROGRESS &&
                            match.scoreA == match.scoreB &&
                            requiresPenaltyDecision(match.id, viewModel)
                        ) {
                            showPenaltyDialog = true
                        } else {
                            viewModel.finishMatch(matchId)
                            onFinished()
                        }
                    },
                    enabled = match.status == MatchStatus.IN_PROGRESS,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("FINISH MATCH", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            } else {
                FinishedBanner()
            }
        }
    }

    if (showPenaltyDialog) {
        AlertDialog(
            onDismissRequest = { showPenaltyDialog = false },
            title = { Text("Match drawn") },
            text = {
                Text("This knockout match finished level (${match.scoreA} - ${match.scoreB}). Who won on penalties?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.finishMatch(matchId, match.teamAId)
                    showPenaltyDialog = false
                    onFinished()
                }) {
                    Text(teamAName)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.finishMatch(matchId, match.teamBId)
                    showPenaltyDialog = false
                    onFinished()
                }) {
                    Text(teamBName)
                }
            }
        )
    }
    if (showPauseConfirm) {
        AlertDialog(
            onDismissRequest = { showPauseConfirm = false },
            title = { Text("Pause timer?") },
            text = { Text("The match clock will stop counting down until you resume it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pauseTimer()
                    showPauseConfirm = false
                }) {
                    Text("Pause")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset timer?") },
            text = { Text("This will reset the clock back to $selectedDuration:00. The current score will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetTimer()
                    showResetConfirm = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun requiresPenaltyDecision(matchId: String, viewModel: TournamentViewModel): Boolean {
    val match = viewModel.getMatchById(matchId) ?: return false
    return match.stage != com.addie.sundayleague.model.MatchStage.GROUP
}

@Composable
fun MatchScoreboardCard(
    teamAName: String,
    teamBName: String,
    match: com.addie.sundayleague.model.Match
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamAvatar(name = teamAName)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = teamAName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            if (match.status == MatchStatus.NOT_STARTED) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Text(
                    text = "${match.scoreA} - ${match.scoreB}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamAvatar(name = teamBName)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = teamBName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartMatchCard(
    selectedDuration: Int,
    durationExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onStart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set match duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = durationExpanded,
                onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = "$selectedDuration minutes",
                    onValueChange = {},
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.menuAnchor().width(200.dp)
                )
                ExposedDropdownMenu(
                    expanded = durationExpanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    DURATION_OPTIONS.forEach { mins ->
                        DropdownMenuItem(
                            text = { Text("$mins minutes") },
                            onClick = {
                                onDurationSelected(mins)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("START MATCH", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun TimerCard(timeText: String, isRunning: Boolean, isFinished: Boolean) {
    val borderColor = when {
        isFinished -> MaterialTheme.colorScheme.secondary
        isRunning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val textColor = when {
        isFinished -> MaterialTheme.colorScheme.secondary
        isRunning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(BorderStroke(3.dp, borderColor), CircleShape)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = if (isFinished) "FULL TIME" else if (isRunning) "LIVE" else "PAUSED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    remainingSeconds: Int,
    onPauseResume: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onPauseResume,
            enabled = remainingSeconds > 0,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(46.dp)
        ) {
            Icon(
                if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isRunning) "Pause" else "Resume", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(46.dp)
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
        }
    }
}

@Composable
fun ScoreCounter(
    teamName: String,
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$score",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = onDecrement,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease score", modifier = Modifier.size(16.dp))
                }
                FilledIconButton(
                    onClick = onIncrement,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase score", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FinishedBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "MATCH FINISHED",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}