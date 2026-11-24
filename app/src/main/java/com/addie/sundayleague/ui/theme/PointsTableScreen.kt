package com.addie.sundayleague.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.viewmodel.TournamentViewModel
import androidx.compose.foundation.lazy.itemsIndexed

private const val SEMI_FINAL_SPOTS = 4

@Composable
fun PointsTableScreen(viewModel: TournamentViewModel) {
    val sortedTeams = viewModel.sortedTeams()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Points Table",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (sortedTeams.isEmpty()) {
            EmptyTableState()
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column {
                    TableHeaderRow()

                    LazyColumn {
                        itemsIndexed(sortedTeams) { index, team ->
                            TableDataRow(
                                position = index + 1,
                                team = team,
                                qualifying = index < SEMI_FINAL_SPOTS && sortedTeams.size > SEMI_FINAL_SPOTS,
                                isStriped = index % 2 == 1
                            )
                        }
                    }
                }
            }

            if (sortedTeams.size > SEMI_FINAL_SPOTS) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top $SEMI_FINAL_SPOTS teams qualify for the semi-finals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTableState() {
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
            text = "The table will appear once teams are added",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        HeaderCell(text = "#", weight = 0.5f)
        HeaderCell(text = "TEAM", weight = 2.3f, alignStart = true)
        HeaderCell(text = "P", weight = 0.6f)
        HeaderCell(text = "W", weight = 0.6f)
        HeaderCell(text = "D", weight = 0.6f)
        HeaderCell(text = "L", weight = 0.6f)
        HeaderCell(text = "GF", weight = 0.7f)
        HeaderCell(text = "GA", weight = 0.7f)
        HeaderCell(text = "GD", weight = 0.7f)
        HeaderCell(text = "PTS", weight = 0.8f)
    }
}

@Composable
fun RowScope.HeaderCell(text: String, weight: Float, alignStart: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        letterSpacing = 0.5.sp,
        textAlign = if (alignStart) TextAlign.Start else TextAlign.Center
    )
}

@Composable
fun TableDataRow(position: Int, team: Team, qualifying: Boolean, isStriped: Boolean) {
    val played = team.wins + team.draws + team.losses

    val backgroundColor = when {
        qualifying -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        isStriped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
            if (position == 1) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Leader",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (qualifying) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.weight(2.3f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team.name.trim().firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                softWrap = true
            )
        }

        DataCell(text = "$played", weight = 0.6f)
        DataCell(text = "${team.wins}", weight = 0.6f)
        DataCell(text = "${team.draws}", weight = 0.6f)
        DataCell(text = "${team.losses}", weight = 0.6f)
        DataCell(text = "${team.goalsScored}", weight = 0.7f)
        DataCell(text = "${team.goalsConceded}", weight = 0.7f)
        DataCell(
            text = if (team.goalDifference > 0) "+${team.goalDifference}" else "${team.goalDifference}",
            weight = 0.7f
        )
        DataCell(text = "${team.points}", weight = 0.8f, bold = true, highlight = true)
    }
}

@Composable
fun RowScope.DataCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    highlight: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = if (highlight) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}