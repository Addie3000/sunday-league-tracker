package com.addie.sundayleague.model

enum class MatchStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
}

enum class MatchStage {
    GROUP,
    SEMI_FINAL,
    FINAL
}

data class Match(
    val id: String,
    val teamAId: String,
    val teamBId: String,
    var scoreA: Int = 0,
    var scoreB: Int = 0,
    var status: MatchStatus = MatchStatus.NOT_STARTED,
    val stage: MatchStage = MatchStage.GROUP,
    var winnerId: String? = null
)