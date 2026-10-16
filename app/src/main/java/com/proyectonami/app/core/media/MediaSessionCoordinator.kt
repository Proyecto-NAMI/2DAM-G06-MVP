package com.proyectonami.app.core.media

fun interface ActiveMediaSession {
    fun stop()
}

interface MediaSessionController {
    fun stopAll(): Boolean
}

class MediaSessionCoordinator : MediaSessionController {
    private val activeSessions = linkedSetOf<ActiveMediaSession>()

    @Synchronized
    fun register(session: ActiveMediaSession): AutoCloseable {
        activeSessions += session
        return AutoCloseable {
            synchronized(this) {
                activeSessions -= session
            }
        }
    }

    override fun stopAll(): Boolean {
        val sessions =
            synchronized(this) {
                activeSessions.toList().also { activeSessions.clear() }
            }
        var allStopped = true
        sessions.forEach { session ->
            try {
                session.stop()
            } catch (_: Exception) {
                allStopped = false
            }
        }
        return allStopped
    }
}
