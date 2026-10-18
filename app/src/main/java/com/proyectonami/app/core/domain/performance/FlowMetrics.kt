package com.proyectonami.app.core.domain.performance

import kotlin.math.ceil

/** A deterministic record used to compare the critical flow with its agreed target. */
data class FlowMeasurement(
    val name: String,
    val elapsedMillis: Long,
    val targetMillis: Long,
) {
    init {
        require(name.isNotBlank()) { "El nombre de la medición no puede estar vacío" }
        require(elapsedMillis >= 0L) { "El tiempo transcurrido no puede ser negativo" }
        require(targetMillis > 0L) { "El objetivo debe ser positivo" }
    }
}

data class FlowSafetySnapshot(
    val draft: String?,
    val audioPath: String?,
    val duplicateClientMessageIds: Int,
) {
    init {
        require(duplicateClientMessageIds >= 0) { "Los duplicados no pueden ser negativos" }
    }
}

fun FlowMeasurement.meetsTarget(): Boolean = elapsedMillis <= targetMillis

/**
 * Resumen reproducible de una serie de tiempos del mismo tramo productivo.
 *
 * RNF-03 se expresa como percentil 95. Se exige un mínimo de 20 observaciones
 * para que el percentil no sea simplemente el máximo de una muestra demasiado
 * pequeña. El cálculo usa nearest-rank, por lo que no interpola ni oculta picos.
 */
data class FlowPerformanceSummary(
    val name: String,
    val samplesMillis: List<Long>,
    val targetMillis: Long,
) {
    init {
        require(name.isNotBlank()) { "El nombre de la medición no puede estar vacío" }
        require(samplesMillis.size >= MINIMUM_P95_SAMPLE_COUNT) {
            "Se necesitan al menos $MINIMUM_P95_SAMPLE_COUNT muestras para calcular p95"
        }
        require(samplesMillis.all { it >= 0L }) { "Las muestras no pueden ser negativas" }
        require(targetMillis > 0L) { "El objetivo debe ser positivo" }
    }

    val p95Millis: Long = percentile95(samplesMillis)
    val minimumMillis: Long = samplesMillis.min()
    val maximumMillis: Long = samplesMillis.max()
    val averageMillis: Double = samplesMillis.average()

    fun meetsTarget(): Boolean = p95Millis < targetMillis
}

fun percentile95(samplesMillis: List<Long>): Long {
    require(samplesMillis.isNotEmpty()) { "No se puede calcular p95 sin muestras" }
    val sorted = samplesMillis.sorted()
    val nearestRankIndex = ceil(sorted.size * 0.95).toInt().coerceAtLeast(1) - 1
    return sorted[nearestRankIndex]
}

fun FlowSafetySnapshot.isSafe(): Boolean =
    duplicateClientMessageIds == 0 &&
        (draft == null || draft.isNotBlank()) &&
        (audioPath == null || audioPath.isNotBlank())

const val MINIMUM_P95_SAMPLE_COUNT = 20
