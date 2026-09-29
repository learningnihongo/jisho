package com.example.data.local

enum class SrsRating(val score: Int, val label: String, val myanmarLabel: String, val intervalHint: String) {
    AGAIN(1, "Again", "မမှတ်မိ / ခက်သည်", "< 1m"),
    HARD(2, "Hard", "အတော်ခက်", "1d"),
    GOOD(3, "Good", "မှတ်မိသည်", "3d"),
    EASY(4, "Easy", "အလွယ်တကူ", "5d+")
}

enum class SrsStage(val label: String, val myanmarLabel: String, val colorHex: Long) {
    NEW("New", "အသစ်", 0xFF607D8B),
    STRUGGLING("Struggling", "ခက်ခဲနေဆဲ ⚠️", 0xFFE53935),
    LEARNING("Learning", "လေ့လာဆဲ 🌱", 0xFFFB8C00),
    REVIEWING("Reviewing", "ပြန်လည်စစ်ဆေးဆဲ 🔄", 0xFF1E88E5),
    MASTERED("Mastered", "ကျွမ်းကျင်ပြီး 🏆", 0xFF43A047)
}

object SrsAlgorithm {

    /**
     * Calculates the next review date and parameters using SuperMemo SM-2 algorithm.
     * When user rates AGAIN, repetition is reset and the word is scheduled immediately (1 minute).
     */
    fun calculateNextReview(word: SavedWord, rating: SrsRating): SavedWord {
        val now = System.currentTimeMillis()
        var rep = word.repetition
        var ease = word.easeFactor
        var interval = word.intervalDays
        var incorrect = word.incorrectCount
        var correct = word.correctCount

        when (rating) {
            SrsRating.AGAIN -> {
                rep = 0
                interval = 0
                incorrect += 1
                ease = maxOf(1.3f, ease - 0.25f)
                // Due immediately (in 1 minute)
                val nextReview = now + (60 * 1000L)
                return word.copy(
                    repetition = rep,
                    intervalDays = interval,
                    easeFactor = ease,
                    nextReviewTimestamp = nextReview,
                    incorrectCount = incorrect,
                    correctCount = correct,
                    lastReviewedTimestamp = now
                )
            }
            SrsRating.HARD -> {
                rep = if (rep == 0) 1 else rep
                interval = if (interval <= 0) 1 else maxOf(1, (interval * 1.2f).toInt())
                ease = maxOf(1.3f, ease - 0.15f)
                incorrect += 1
                val nextReview = now + (interval * 24L * 60L * 60L * 1000L)
                return word.copy(
                    repetition = rep,
                    intervalDays = interval,
                    easeFactor = ease,
                    nextReviewTimestamp = nextReview,
                    incorrectCount = incorrect,
                    correctCount = correct,
                    lastReviewedTimestamp = now
                )
            }
            SrsRating.GOOD -> {
                rep += 1
                correct += 1
                interval = when (rep) {
                    1 -> 1
                    2 -> 3
                    else -> maxOf(4, (interval * ease).toInt())
                }
                val nextReview = now + (interval * 24L * 60L * 60L * 1000L)
                return word.copy(
                    repetition = rep,
                    intervalDays = interval,
                    easeFactor = ease,
                    nextReviewTimestamp = nextReview,
                    incorrectCount = incorrect,
                    correctCount = correct,
                    lastReviewedTimestamp = now
                )
            }
            SrsRating.EASY -> {
                rep += 1
                correct += 1
                ease += 0.15f
                interval = when (rep) {
                    1 -> 3
                    2 -> 6
                    else -> maxOf(7, (interval * ease * 1.3f).toInt())
                }
                val nextReview = now + (interval * 24L * 60L * 60L * 1000L)
                return word.copy(
                    repetition = rep,
                    intervalDays = interval,
                    easeFactor = ease,
                    nextReviewTimestamp = nextReview,
                    incorrectCount = incorrect,
                    correctCount = correct,
                    lastReviewedTimestamp = now
                )
            }
        }
    }

    /**
     * Prioritizes words for review:
     * 1. Struggling words (highest failure rate, lowest easeFactor)
     * 2. Due words (nextReviewTimestamp <= now)
     * 3. Unstudied words (lastReviewedTimestamp == 0L)
     * 4. Future due words
     */
    fun prioritizeForQuiz(words: List<SavedWord>): List<SavedWord> {
        val now = System.currentTimeMillis()
        return words.sortedWith(
            compareBy<SavedWord> { word ->
                val isDue = word.nextReviewTimestamp <= now
                val isNeverStudied = word.lastReviewedTimestamp == 0L
                val isStruggling = word.incorrectCount > word.correctCount || (word.incorrectCount > 0 && word.repetition == 0)

                when {
                    isDue && isStruggling -> 0
                    isDue && !isNeverStudied -> 1
                    isNeverStudied -> 2
                    isStruggling -> 3
                    else -> 4
                }
            }
            // Secondary sort: prioritize words with most failed attempts and lower ease
            .thenByDescending { (it.incorrectCount * 3) - it.correctCount }
            .thenBy { it.easeFactor }
            .thenBy { it.nextReviewTimestamp }
        )
    }

    fun getSrsStage(word: SavedWord): SrsStage {
        return when {
            word.lastReviewedTimestamp == 0L -> SrsStage.NEW
            word.incorrectCount > word.correctCount || (word.repetition == 0 && word.incorrectCount > 0) -> SrsStage.STRUGGLING
            word.repetition in 1..2 -> SrsStage.LEARNING
            word.repetition in 3..4 -> SrsStage.REVIEWING
            else -> SrsStage.MASTERED
        }
    }

    /**
     * Filters and sorts the quiz queue based on user selection:
     * - Struggling: words with high failure rate or struggling stage
     * - JLPT Level: sorted by N5 -> N4 -> N3 -> N2 -> N1
     * - Recently Added: latest saved timestamp first
     * - Due Today: words that need review right now
     * - All: all words with SRS prioritization
     */
    fun filterAndSortQuizQueue(
        words: List<SavedWord>,
        sortMode: QuizSortMode,
        jlptLevelFilter: String = "ALL"
    ): List<SavedWord> {
        val now = System.currentTimeMillis()
        var filtered = words

        if (jlptLevelFilter != "ALL") {
            filtered = filtered.filter {
                it.jlptLevel.contains(jlptLevelFilter, ignoreCase = true)
            }
        }

        return when (sortMode) {
            QuizSortMode.STRUGGLING -> {
                val struggling = filtered.filter { getSrsStage(it) == SrsStage.STRUGGLING }
                if (struggling.isNotEmpty()) prioritizeForQuiz(struggling) else prioritizeForQuiz(filtered)
            }
            QuizSortMode.JLPT_LEVEL -> {
                filtered.sortedWith(
                    compareBy<SavedWord> { word ->
                        val lvl = word.jlptLevel.lowercase()
                        when {
                            lvl.contains("n5") -> 1
                            lvl.contains("n4") -> 2
                            lvl.contains("n3") -> 3
                            lvl.contains("n2") -> 4
                            lvl.contains("n1") -> 5
                            else -> 6
                        }
                    }.thenByDescending { it.incorrectCount }
                    .thenBy { it.nextReviewTimestamp }
                )
            }
            QuizSortMode.RECENTLY_ADDED -> {
                filtered.sortedByDescending { it.timestamp }
            }
            QuizSortMode.DUE_TODAY -> {
                val due = filtered.filter { it.nextReviewTimestamp <= now }
                if (due.isNotEmpty()) prioritizeForQuiz(due) else prioritizeForQuiz(filtered)
            }
            QuizSortMode.ALL -> {
                prioritizeForQuiz(filtered)
            }
        }
    }
}

enum class QuizSortMode(
    val id: String,
    val title: String,
    val myanmarTitle: String,
    val iconEmoji: String
) {
    STRUGGLING("struggling", "Struggling", "ခက်ခဲနေဆဲ ဦးစားပေး", "⚠️"),
    JLPT_LEVEL("jlpt", "JLPT Level", "JLPT အဆင့်အလိုက် (N5 → N1)", "🎓"),
    RECENTLY_ADDED("recent", "Recently Added", "လတ်တလော သိမ်းထားသည်များ", "🕒"),
    DUE_TODAY("due", "Due for Review", "ယနေ့ စစ်ဆေးရန် ရှိသည်များ", "⏰"),
    ALL("all", "All Words", "စကားလုံး အားလုံး", "📚")
}
