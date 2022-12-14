package dev.lucasnlm.antimine.core.models

enum class Difficulty(
    val text: String,
) {
    Standard("STANDARD"),
    Beginner("BEGINNER"),
    Intermediate("INTERMEDIATE"),
    Expert("EXPERT"),
    Custom("CUSTOM"),
    Master("MASTER"),
    Legend("LEGEND"),
    FixedSize("FIXED_SIZE"),
}
