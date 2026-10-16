package com.proyectonami.app.core.domain.performance

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

fun FlowSafetySnapshot.isSafe(): Boolean =
    duplicateClientMessageIds == 0 &&
        (draft == null || draft.isNotBlank()) &&
        (audioPath == null || audioPath.isNotBlank())
