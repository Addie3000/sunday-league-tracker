package com.addie.sundayleague.data

import android.content.Context
import com.addie.sundayleague.model.Match
import com.addie.sundayleague.model.MatchStage
import com.addie.sundayleague.model.MatchStatus
import com.addie.sundayleague.model.Team
import com.addie.sundayleague.model.Tournament
import org.json.JSONArray
import org.json.JSONObject
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

const val MAX_SAVED_TOURNAMENTS = 10

object TournamentStorage {

    private const val PREFS_NAME = "tournament_prefs"
    private const val KEY_TOURNAMENTS = "tournaments_json"
    private const val KEY_NEXT_NUMBER = "next_tournament_number"

    private const val KEY_OPEN_TOURNAMENT_ID = "open_tournament_id"

    private const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"

    fun getLastSeenVersionCode(context: Context): Int =
        prefs(context).getInt(KEY_LAST_SEEN_VERSION, 0)

    fun saveLastSeenVersionCode(context: Context, versionCode: Int) {
        prefs(context).edit().putInt(KEY_LAST_SEEN_VERSION, versionCode).apply()
    }

    fun saveOpenTournamentId(context: Context, id: String?) {
        val editor = prefs(context).edit()
        if (id == null) editor.remove(KEY_OPEN_TOURNAMENT_ID) else editor.putString(KEY_OPEN_TOURNAMENT_ID, id)
        editor.apply()
    }

    fun loadOpenTournamentId(context: Context): String? =
        prefs(context).getString(KEY_OPEN_TOURNAMENT_ID, null)

    fun saveTournaments(context: Context, tournaments: List<Tournament>) {
        val array = JSONArray()
        tournaments.forEach { t -> array.put(tournamentToJson(t)) }
        prefs(context).edit().putString(KEY_TOURNAMENTS, array.toString()).apply()
    }

    fun loadTournaments(context: Context): List<Tournament> {
        val json = prefs(context).getString(KEY_TOURNAMENTS, null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<Tournament>()
        for (i in 0 until array.length()) {
            result.add(tournamentFromJson(array.getJSONObject(i)))
        }
        return result
    }

    fun getNextTournamentNumber(context: Context): Int {
        return prefs(context).getInt(KEY_NEXT_NUMBER, 1)
    }

    fun saveNextTournamentNumber(context: Context, number: Int) {
        prefs(context).edit().putInt(KEY_NEXT_NUMBER, number).apply()
    }

    private fun tournamentToJson(t: Tournament): JSONObject {
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("number", t.number)
        obj.put("name", t.name)
        obj.put("createdAt", t.createdAt)
        obj.put("matchDurationMinutes", t.matchDurationMinutes)

        val teamsArray = JSONArray()
        t.teams.forEach { team ->
            val teamObj = JSONObject()
            teamObj.put("id", team.id)
            teamObj.put("name", team.name)
            teamObj.put("wins", team.wins)
            teamObj.put("draws", team.draws)
            teamObj.put("losses", team.losses)
            teamObj.put("goalsScored", team.goalsScored)
            teamObj.put("goalsConceded", team.goalsConceded)
            teamsArray.put(teamObj)
        }
        obj.put("teams", teamsArray)

        val matchesArray = JSONArray()
        t.matches.forEach { match ->
            val matchObj = JSONObject()
            matchObj.put("id", match.id)
            matchObj.put("teamAId", match.teamAId)
            matchObj.put("teamBId", match.teamBId)
            matchObj.put("scoreA", match.scoreA)
            matchObj.put("scoreB", match.scoreB)
            matchObj.put("status", match.status.name)
            matchObj.put("stage", match.stage.name)
            matchObj.put("winnerId", match.winnerId ?: "")
            matchesArray.put(matchObj)
        }
        obj.put("matches", matchesArray)

        return obj
    }

    private fun tournamentFromJson(obj: JSONObject): Tournament {
        val teams = mutableListOf<Team>()
        val teamsArray = obj.getJSONArray("teams")
        for (i in 0 until teamsArray.length()) {
            val teamObj = teamsArray.getJSONObject(i)
            teams.add(
                Team(
                    id = teamObj.getString("id"),
                    name = teamObj.getString("name"),
                    wins = teamObj.getInt("wins"),
                    draws = teamObj.getInt("draws"),
                    losses = teamObj.getInt("losses"),
                    goalsScored = teamObj.getInt("goalsScored"),
                    goalsConceded = teamObj.getInt("goalsConceded")
                )
            )
        }

        val matches = mutableListOf<Match>()
        val matchesArray = obj.getJSONArray("matches")
        for (i in 0 until matchesArray.length()) {
            val matchObj = matchesArray.getJSONObject(i)
            matches.add(
                Match(
                    id = matchObj.getString("id"),
                    teamAId = matchObj.getString("teamAId"),
                    teamBId = matchObj.getString("teamBId"),
                    scoreA = matchObj.getInt("scoreA"),
                    scoreB = matchObj.getInt("scoreB"),
                    status = MatchStatus.valueOf(matchObj.getString("status")),
                    stage = MatchStage.valueOf(matchObj.optString("stage", "GROUP")),
                    winnerId = matchObj.optString("winnerId", "").ifEmpty { null }
                )
            )
        }

        return Tournament(
            id = obj.getString("id"),
            number = obj.getInt("number"),
            name = obj.getString("name"),
            createdAt = obj.getLong("createdAt"),
            teams = teams,
            matches = matches,
            matchDurationMinutes = obj.optInt("matchDurationMinutes", 10)
        )
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}