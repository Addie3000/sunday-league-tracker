package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.addie.sundayleague.model.Match
import com.addie.sundayleague.model.MatchStage
import com.addie.sundayleague.model.MatchStatus
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.viewmodel.TournamentViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun KnockoutsScreen(viewModel: TournamentViewModel, onMatchClick: (String) -> Unit) {
    val groupComplete = viewModel.isGroupStageComplete()
    val qualifiers = viewModel.topQualifiers()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // ✅ THIS IS THE FIX
            .padding(10.dp)
    ) {
        Text(
            text = "Knockout Stage",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!groupComplete) {
            LockedKnockoutState(viewModel)
        } else if (qualifiers.size < 4) {
            Text(
                text = "At least 4 teams are needed to run a knockout stage.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Top 4 Qualifiers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            qualifiers.forEachIndexed { index, team ->
                QualifierRow(position = index + 1, team = team)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val semis = viewModel.semiFinalMatches()
            val available = viewModel.teamsAvailableForSemiFinal()

            if (semis.size < 2) {
                SemiFinalCreator(
                    availableTeams = available,
                    onCreate = { a, b -> viewModel.createSemiFinal(a, b) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (semis.isNotEmpty()) {
                Text(
                    text = "Semi-Finals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                semis.forEach { match ->
                    KnockoutMatchCard(
                        match = match,
                        teamAName = viewModel.getTeamById(match.teamAId)?.name ?: "Unknown",
                        teamBName = viewModel.getTeamById(match.teamBId)?.name ?: "Unknown",
                        stageLabel = "SEMI-FINAL",
                        onClick = { onMatchClick(match.id) }
                    )
                }
            }

            val final = viewModel.finalMatch()
            if (final != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Final",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                KnockoutMatchCard(
                    match = final,
                    teamAName = viewModel.getTeamById(final.teamAId)?.name ?: "Unknown",
                    teamBName = viewModel.getTeamById(final.teamBId)?.name ?: "Unknown",
                    stageLabel = "FINAL",
                    onClick = { onMatchClick(final.id) }
                )

                if (final.status == MatchStatus.FINISHED && final.winnerId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ChampionBanner(
                        teamName = viewModel.getTeamById(final.winnerId!!)?.name ?: "Unknown"
                    )
                }
            } else if (semis.size == 2 && semis.any { it.status != MatchStatus.FINISHED }) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "The final will be created automatically once both semi-finals are finished.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LockedKnockoutState(viewModel: TournamentViewModel) {
    val groupMatches = viewModel.matches.filter { it.stage == MatchStage.GROUP }
    val finishedCount = groupMatches.count { it.status == MatchStatus.FINISHED }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Knockouts locked", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (groupMatches.isEmpty())
                "Create and finish group matches first"
            else
                "$finishedCount of ${groupMatches.size} group matches finished",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QualifierRow(position: Int, team: Team) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$position",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp),
                color = MaterialTheme.colorScheme.secondary
            )
            TeamAvatar(name = team.name)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${team.points} pts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemiFinalCreator(availableTeams: List<Team>, onCreate: (String, String) -> Unit) {
    var teamAId by remember { mutableStateOf("") }
    var teamBId by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Create Semi-Final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (availableTeams.size < 2) {
                Text(
                    text = "Not enough unassigned qualifiers left to create another semi-final.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                TeamDropdown(
                    label = "Team A",
                    teams = availableTeams,
                    selectedTeamId = teamAId,
                    onTeamSelected = { teamAId = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TeamDropdown(
                    label = "Team B",
                    teams = availableTeams,
                    selectedTeamId = teamBId,
                    onTeamSelected = { teamBId = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onCreate(teamAId, teamBId)
                        teamAId = ""
                        teamBId = ""
                    },
                    enabled = teamAId.isNotEmpty() && teamBId.isNotEmpty() && teamAId != teamBId,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Semi-Final Match")
                }
            }
        }
    }
}

@Composable
fun KnockoutMatchCard(
    match: Match,
    teamAName: String,
    teamBName: String,
    stageLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    .height(75.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stageLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    MatchStatusBadge(status = match.status)
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
                        modifier = Modifier.weight(1f)
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
                            color = MaterialTheme.colorScheme.primary,
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
fun ChampionBanner(teamName: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CHAMPIONS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = teamName.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}