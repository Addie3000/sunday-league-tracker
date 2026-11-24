package com.addie.sundayleague.data

data class WhatsNewEntry(
    val versionName: String,
    val features: List<String>
)

// Add one new entry here per future release — nothing else needs to change
// for the "What's New" dialog to pick it up automatically.
val WHATS_NEW_HISTORY: Map<Int, WhatsNewEntry> = mapOf(
    2 to WhatsNewEntry(
        versionName = "2.0",
        features = listOf(
            "All-new Home screen — create and manage up to 10 tournaments, each saved separately",
            "Rename or delete old tournaments any time",
            "Knockout stage: create semi-finals from your top 4 teams, with the final created automatically",
            "Match timer now keeps running in the background, even if the app is closed",
            "Redesigned Live Match screen with a cleaner scoreboard layout",
            "Points Table now fully displays long team names",
            "Confirmation prompts added before pausing or resetting a live match timer"
        )
    )
)