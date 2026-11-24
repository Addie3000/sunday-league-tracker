package com.addie.sundayleague.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.addie.sundayleague.BuildConfig
import com.addie.sundayleague.data.MAX_SAVED_TOURNAMENTS
import com.addie.sundayleague.data.TournamentStorage
import com.addie.sundayleague.data.WHATS_NEW_HISTORY
import com.addie.sundayleague.data.WhatsNewEntry
import com.addie.sundayleague.model.Match
import com.addie.sundayleague.model.MatchStage
import com.addie.sundayleague.model.MatchStatus
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.model.Tournament
import com.addie.sundayleague.service.MatchTimerService
import java.util.UUID

private const val KNOCKOUT_QUALIFIER_COUNT = 4

class TournamentViewModel(application: Application) : AndroidViewModel(application) {

    var tournaments by mutableStateOf(listOf<Tournament>())
        private set

    var openTournamentId by mutableStateOf<String?>(null)
        private set

    init {
        val context = getApplication<Application>()
        tournaments = TournamentStorage.loadTournaments(context)
        val savedOpenId = TournamentStorage.loadOpenTournamentId(context)
        openTournamentId = if (tournaments.any { it.id == savedOpenId }) savedOpenId else null
    }

    // --- Derived views of the currently open tournament ---

    private fun openTournament(): Tournament? = tournaments.find { it.id == openTournamentId }

    val teams: List<Team> get() = openTournament()?.teams ?: emptyList()
    val matches: List<Match> get() = openTournament()?.matches ?: emptyList()
    val matchDurationMinutes: Int get() = openTournament()?.matchDurationMinutes ?: 10

    fun openTournamentName(): String = openTournament()?.name ?: ""

    private fun updateOpenTournament(transform: (Tournament) -> Tournament) {
        val id = openTournamentId ?: return
        tournaments = tournaments.map { if (it.id == id) transform(it) else it }
        TournamentStorage.saveTournaments(getApplication(), tournaments)
    }

    // --- Home screen: tournament lifecycle ---

    fun createTournament() {
        val context = getApplication<Application>()
        val nextNumber = TournamentStorage.getNextTournamentNumber(context)

        val newTournament = Tournament(
            id = UUID.randomUUID().toString(),
            number = nextNumber,
            name = "Tournament $nextNumber",
            createdAt = System.currentTimeMillis()
        )

        var updatedList = tournaments + newTournament
        if (updatedList.size > MAX_SAVED_TOURNAMENTS) {
            val oldest = updatedList.minByOrNull { it.createdAt }
            if (oldest != null) {
                updatedList = updatedList.filter { it.id != oldest.id }
                if (MatchTimerService.activeMatchId.value != null &&
                    oldest.matches.any { it.id == MatchTimerService.activeMatchId.value }
                ) {
                    MatchTimerService.stop(context)
                }
            }
        }

        tournaments = updatedList
        TournamentStorage.saveTournaments(context, tournaments)
        TournamentStorage.saveNextTournamentNumber(context, nextNumber + 1)

        openTournamentId = newTournament.id
        TournamentStorage.saveOpenTournamentId(context, newTournament.id)
    }

    fun openTournament(tournamentId: String) {
        openTournamentId = tournamentId
        TournamentStorage.saveOpenTournamentId(getApplication(), tournamentId)
    }

    fun closeTournament() {
        openTournamentId = null
        TournamentStorage.saveOpenTournamentId(getApplication(), null)
    }

    fun deleteTournament(tournamentId: String) {
        val target = tournaments.find { it.id == tournamentId }
        if (target != null && MatchTimerService.activeMatchId.value != null &&
            target.matches.any { it.id == MatchTimerService.activeMatchId.value }
        ) {
            MatchTimerService.stop(getApplication())
        }

        tournaments = tournaments.filter { it.id != tournamentId }
        TournamentStorage.saveTournaments(getApplication(), tournaments)

        if (openTournamentId == tournamentId) {
            closeTournament()
        }
    }

    fun renameTournament(tournamentId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        tournaments = tournaments.map {
            if (it.id == tournamentId) it.copy(name = trimmed) else it
        }
        TournamentStorage.saveTournaments(getApplication(), tournaments)
    }

    fun findTournamentIdContainingMatch(matchId: String): String? =
        tournaments.find { t -> t.matches.any { it.id == matchId } }?.id

    // --- Teams (operate on the currently open tournament) ---

    fun addTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateOpenTournament { t ->
            t.copy(teams = t.teams + Team(id = UUID.randomUUID().toString(), name = trimmed))
        }
    }

    fun removeTeam(teamId: String) {
        updateOpenTournament { t -> t.copy(teams = t.teams.filter { it.id != teamId }) }
    }

    fun renameTeam(teamId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        updateOpenTournament { t ->
            t.copy(teams = t.teams.map { if (it.id == teamId) it.copy(name = trimmed) else it })
        }
    }

    fun getTeamById(teamId: String): Team? = teams.find { it.id == teamId }

    fun sortedTeams(): List<Team> = teams.sortedWith(
        compareByDescending<Team> { it.points }
            .thenByDescending { it.goalDifference }
            .thenByDescending { it.goalsScored }
    )

    // --- Group matches ---

    fun addMatch(teamAId: String, teamBId: String) {
        if (teamAId == teamBId || teamAId.isEmpty() || teamBId.isEmpty()) return
        updateOpenTournament { t ->
            t.copy(
                matches = t.matches + Match(
                    id = UUID.randomUUID().toString(),
                    teamAId = teamAId,
                    teamBId = teamBId,
                    stage = MatchStage.GROUP
                )
            )
        }
    }

    fun removeMatch(matchId: String) {
        updateOpenTournament { t -> t.copy(matches = t.matches.filter { it.id != matchId }) }
        if (MatchTimerService.activeMatchId.value == matchId) {
            MatchTimerService.stop(getApplication())
        }
    }

    fun getMatchById(matchId: String): Match? = matches.find { it.id == matchId }

    private fun markInProgress(matchId: String) {
        updateOpenTournament { t ->
            t.copy(matches = t.matches.map {
                if (it.id == matchId) it.copy(status = MatchStatus.IN_PROGRESS) else it
            })
        }
    }

    // --- Knockout stage ---

    fun isGroupStageComplete(): Boolean {
        val groupMatches = matches.filter { it.stage == MatchStage.GROUP }
        return groupMatches.isNotEmpty() && groupMatches.all { it.status == MatchStatus.FINISHED }
    }

    fun topQualifiers(): List<Team> {
        if (!isGroupStageComplete() || teams.size < KNOCKOUT_QUALIFIER_COUNT) return emptyList()
        return sortedTeams().take(KNOCKOUT_QUALIFIER_COUNT)
    }

    fun semiFinalMatches(): List<Match> = matches.filter { it.stage == MatchStage.SEMI_FINAL }

    fun finalMatch(): Match? = matches.find { it.stage == MatchStage.FINAL }

    fun teamsAvailableForSemiFinal(): List<Team> {
        val alreadyUsed = semiFinalMatches().flatMap { listOf(it.teamAId, it.teamBId) }.toSet()
        return topQualifiers().filter { it.id !in alreadyUsed }
    }

    fun createSemiFinal(teamAId: String, teamBId: String) {
        if (semiFinalMatches().size >= 2) return
        if (teamAId == teamBId || teamAId.isEmpty() || teamBId.isEmpty()) return
        val qualifierIds = topQualifiers().map { it.id }.toSet()
        if (teamAId !in qualifierIds || teamBId !in qualifierIds) return

        updateOpenTournament { t ->
            t.copy(
                matches = t.matches + Match(
                    id = UUID.randomUUID().toString(),
                    teamAId = teamAId,
                    teamBId = teamBId,
                    stage = MatchStage.SEMI_FINAL
                )
            )
        }
    }

    private fun maybeCreateFinal() {
        val semis = semiFinalMatches()
        if (semis.size == 2 && semis.all { it.status == MatchStatus.FINISHED } && finalMatch() == null) {
            val winnerOne = semis[0].winnerId
            val winnerTwo = semis[1].winnerId
            if (winnerOne != null && winnerTwo != null) {
                updateOpenTournament { t ->
                    t.copy(
                        matches = t.matches + Match(
                            id = UUID.randomUUID().toString(),
                            teamAId = winnerOne,
                            teamBId = winnerTwo,
                            stage = MatchStage.FINAL
                        )
                    )
                }
            }
        }
    }

    // --- Timer control: delegates to the background service ---

    fun setMatchDuration(minutes: Int) {
        if (minutes <= 0) return
        updateOpenTournament { t -> t.copy(matchDurationMinutes = minutes) }
    }

    fun startMatchWithDuration(matchId: String, minutes: Int) {
        setMatchDuration(minutes)
        markInProgress(matchId)
        MatchTimerService.start(getApplication(), matchId, minutes)
    }

    fun resumeTimer() {
        MatchTimerService.resume(getApplication())
    }

    fun pauseTimer() {
        MatchTimerService.pause(getApplication())
    }

    fun resetTimer() {
        MatchTimerService.reset(getApplication(), matchDurationMinutes)
    }

    // --- Score control ---

    fun incrementScoreA(matchId: String) {
        updateOpenTournament { t ->
            t.copy(matches = t.matches.map { if (it.id == matchId) it.copy(scoreA = it.scoreA + 1) else it })
        }
    }

    fun decrementScoreA(matchId: String) {
        updateOpenTournament { t ->
            t.copy(matches = t.matches.map {
                if (it.id == matchId && it.scoreA > 0) it.copy(scoreA = it.scoreA - 1) else it
            })
        }
    }

    fun incrementScoreB(matchId: String) {
        updateOpenTournament { t ->
            t.copy(matches = t.matches.map { if (it.id == matchId) it.copy(scoreB = it.scoreB + 1) else it })
        }
    }

    fun decrementScoreB(matchId: String) {
        updateOpenTournament { t ->
            t.copy(matches = t.matches.map {
                if (it.id == matchId && it.scoreB > 0) it.copy(scoreB = it.scoreB - 1) else it
            })
        }
    }

    fun finishMatch(matchId: String, penaltyWinnerId: String? = null) {
        val tournament = openTournament() ?: return
        val match = tournament.matches.find { it.id == matchId } ?: return
        if (match.status == MatchStatus.FINISHED) return

        if (match.stage == MatchStage.GROUP) {
            val updatedTeams = tournament.teams.map { team ->
                when (team.id) {
                    match.teamAId -> team.copy(
                        goalsScored = team.goalsScored + match.scoreA,
                        goalsConceded = team.goalsConceded + match.scoreB,
                        wins = team.wins + if (match.scoreA > match.scoreB) 1 else 0,
                        draws = team.draws + if (match.scoreA == match.scoreB) 1 else 0,
                        losses = team.losses + if (match.scoreA < match.scoreB) 1 else 0
                    )
                    match.teamBId -> team.copy(
                        goalsScored = team.goalsScored + match.scoreB,
                        goalsConceded = team.goalsConceded + match.scoreA,
                        wins = team.wins + if (match.scoreB > match.scoreA) 1 else 0,
                        draws = team.draws + if (match.scoreA == match.scoreB) 1 else 0,
                        losses = team.losses + if (match.scoreB < match.scoreA) 1 else 0
                    )
                    else -> team
                }
            }
            val updatedMatches = tournament.matches.map {
                if (it.id == matchId) it.copy(status = MatchStatus.FINISHED) else it
            }
            updateOpenTournament { it.copy(teams = updatedTeams, matches = updatedMatches) }
        } else {
            val winnerId = when {
                match.scoreA > match.scoreB -> match.teamAId
                match.scoreB > match.scoreA -> match.teamBId
                else -> penaltyWinnerId
            }
            val updatedMatches = tournament.matches.map {
                if (it.id == matchId) it.copy(status = MatchStatus.FINISHED, winnerId = winnerId) else it
            }
            updateOpenTournament { it.copy(matches = updatedMatches) }
            maybeCreateFinal()
        }

        if (MatchTimerService.activeMatchId.value == matchId) {
            MatchTimerService.stop(getApplication())
        }
    }

    // --- What's New dialog ---

    fun getPendingWhatsNew(): WhatsNewEntry? {
        val lastSeen = TournamentStorage.getLastSeenVersionCode(getApplication())
        val current = BuildConfig.VERSION_CODE
        if (lastSeen >= current) return null
        return WHATS_NEW_HISTORY[current]
    }

    fun markWhatsNewSeen() {
        TournamentStorage.saveLastSeenVersionCode(getApplication(), BuildConfig.VERSION_CODE)
    }
}