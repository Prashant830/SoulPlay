package com.souljoy.soulmasti.domain.model

enum class MatchOutcome {
    WIN,
    LOST,
    /** Could not infer (missing winner, tie scores, unclear text). */
    UNKNOWN,
}
