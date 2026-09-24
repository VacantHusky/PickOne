package com.playdice.pickone.domain

import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.Scene
import com.playdice.pickone.model.SelectionOutcome
import com.playdice.pickone.model.SelectorType
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

fun interface RandomSource {
    fun nextInt(bound: Int): Int
}

@Singleton
class SelectionEngine @Inject constructor() {
    private val secureRandom = SecureRandom()

    fun select(scene: Scene, random: RandomSource = RandomSource(secureRandom::nextInt)): SelectionOutcome {
        require(scene.choices.size >= 2) { "A scene needs at least two choices." }
        require(scene.choices.all { it.weight > 0 }) { "Weights must be positive." }

        val divisor = scene.choices.map(ChoiceItem::weight).reduce(::gcd)
        val normalized = scene.choices.map { it.weight / divisor }
        val totalLong = normalized.fold(0L) { total, weight ->
            total.checkedAdd(weight.toLong(), "Total weight exceeds supported range.")
        }
        require(totalLong <= Int.MAX_VALUE) { "Total weight exceeds supported range." }
        val total = totalLong.toInt()

        return when (scene.selectorType) {
            SelectorType.DICE -> selectWithUnits(scene.choices, normalized, total, 6, random)
            SelectorType.COIN -> selectWithUnits(scene.choices, normalized, total, 2, random)
            else -> {
                val sample = random.nextInt(total)
                SelectionOutcome(
                    selected = scene.choices[itemIndexForSample(normalized, sample)],
                    normalizedWeights = normalized,
                    sampledIndex = sample.toInt(),
                )
            }
        }
    }

    private fun selectWithUnits(
        choices: List<ChoiceItem>,
        normalized: List<Int>,
        total: Int,
        base: Int,
        random: RandomSource,
    ): SelectionOutcome {
        val unitCount = unitCountFor(total, base)
        var rejections = 0
        while (true) {
            val units = List(unitCount) { random.nextInt(base) }
            val sample = units.fold(0L) { value, digit -> value * base + digit }
            if (sample < total) {
                return SelectionOutcome(
                    selected = choices[itemIndexForSample(normalized, sample.toInt())],
                    normalizedWeights = normalized,
                    sampledIndex = sample.toInt(),
                    unitValues = units,
                    rejectedRolls = rejections,
                )
            }
            rejections++
        }
    }

    companion object {
        fun unitCountFor(total: Int, base: Int): Int {
            require(total > 0 && base > 1)
            var count = 1
            var outcomes = base.toLong()
            while (outcomes < total.toLong()) {
                count++
                outcomes = outcomes.checkedMultiply(base.toLong(), "Unit count exceeds supported range.")
            }
            return count
        }

        fun itemIndexForSample(weights: List<Int>, sample: Int): Int {
            require(weights.isNotEmpty() && weights.all { it > 0 })
            val total = weights.fold(0L) { sum, weight ->
                sum.checkedAdd(weight.toLong(), "Total weight exceeds supported range.")
            }
            require(sample >= 0 && sample.toLong() < total)
            var boundary = 0L
            weights.forEachIndexed { index, weight ->
                boundary += weight.toLong()
                if (sample.toLong() < boundary) return index
            }
            error("Unreachable weighted sample")
        }

        private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        private fun Long.checkedAdd(value: Long, message: String): Long =
            Math.addExact(this, value).also { require(it <= Int.MAX_VALUE) { message } }

        private fun Long.checkedMultiply(value: Long, message: String): Long =
            Math.multiplyExact(this, value).also { require(it <= Int.MAX_VALUE) { message } }
    }
}
