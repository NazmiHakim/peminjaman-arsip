package com.bpkpad.peminjaman.core.common

object Constants {
    const val DB_NAME = "bpkpad_peminjaman.db"
    const val SESSION_DATASTORE = "bpkpad_session"
    const val NOTIFICATION_CHANNEL_ID = "bpkpad_overdue_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Pengingat Overdue Peminjaman"
    const val DEEP_LINK_SCHEME = "bpkpad"
    const val DEEP_LINK_TRANSAKSI_HOST = "transaksi"
    const val DEEP_LINK_OVERDUE_HOST = "overdue"
    const val MAX_IMAGE_WIDTH = 1024
    const val MAX_IMAGE_HEIGHT = 1024
    const val IMAGE_COMPRESSION_QUALITY = 85
    const val QR_TOKEN_PREFIX = "QR-"
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val WHATSAPP_API_URL = "https://api.whatsapp.com/send"
}
