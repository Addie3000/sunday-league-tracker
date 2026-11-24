package com.addie.sundayleague.model

data class Tournament(
    val id: String,
    val number: Int,
    var name: String,
    val createdAt: Long,
    var teams: List<Team> = emptyList(),
    var matches: List<Match> = emptyList(),
    var matchDurationMinutes: Int = 10
)

enum class TournamentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

fun Tournament.status(): TournamentStatus {
    val finalMatch = matches.find { it.stage == MatchStage.FINAL }
    if (finalMatch != null && finalMatch.status == MatchStatus.FINISHED) {
        return TournamentStatus.COMPLETED
    }
    val hasAnyActivity = teams.isNotEmpty() || matches.any { it.status != MatchStatus.NOT_STARTED }
    return if (hasAnyActivity) TournamentStatus.IN_PROGRESS else TournamentStatus.NOT_STARTED
}

fun Tournament.championName(): String? {
    val finalMatch = matches.find { it.stage == MatchStage.FINAL }
    if (finalMatch?.status == MatchStatus.FINISHED && finalMatch.winnerId != null) {
        return teams.find { it.id == finalMatch.winnerId }?.name
    }
    return null
}