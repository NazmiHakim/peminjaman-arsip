package com.bpkpad.peminjaman.peminjaman.domain.model

data class DashboardStats(
    val totalTransaksi: Int,
    val menungguPersetujuan: Int,
    val dipinjam: Int,
    val overdueCount: Int,
    val dikembalikan: Int,
    val ditolak: Int,
    val dibatalkan: Int,
    val bypassPerluAcknowledge: Int,
    val perpanjanganPending: Int
)
