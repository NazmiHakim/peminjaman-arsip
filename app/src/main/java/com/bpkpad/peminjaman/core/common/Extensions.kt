package com.bpkpad.peminjaman.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
private val DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id", "ID"))

fun LocalDate.toDisplayString(): String = this.format(DATE_FORMATTER)
fun LocalDate.toShortString(): String = this.format(DATE_SHORT)
fun LocalDateTime.toDisplayString(): String = this.format(DATETIME_FORMATTER)

fun LocalDate.daysUntil(): Long = ChronoUnit.DAYS.between(LocalDate.now(), this)
fun LocalDate.isOverdue(): Boolean = this.isBefore(LocalDate.now())
fun LocalDate.daysOverdue(): Long = ChronoUnit.DAYS.between(this, LocalDate.now())

fun Double.toCurrencyString(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(this)
}

fun String.isValidPhoneNumber(): Boolean {
    val digits = this.filter { it.isDigit() }
    return digits.length >= 10
}

fun String.toPhoneE164(): String {
    val digits = this.filter { it.isDigit() }
    return if (digits.startsWith("0")) "62" + digits.substring(1) else digits
}
