package no.nav.klage.notifications.exceptions

class MissingAccessException(msg: String) : RuntimeException(msg)

class NotificationNotFoundException(message: String) : RuntimeException(message)

class UnreadNotificationsException(message: String, val count: Int) : RuntimeException(message)
