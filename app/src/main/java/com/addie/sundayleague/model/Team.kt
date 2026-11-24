package com.addie.sundayleague.model

data class Team(
    val id: String,
    val name: String,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var goalsScored: Int = 0,
    var goalsConceded: Int = 0
) {
    val points: Int
        get() = (wins * 3) + (draws * 1)

    val goalDifference: Int
        get() = goalsScored - goalsConceded
}
