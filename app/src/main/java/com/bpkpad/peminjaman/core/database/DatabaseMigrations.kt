package com.bpkpad.peminjaman.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `audit_log_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `transaksi_id` INTEGER,
                    `user_id` INTEGER NOT NULL,
                    `aksi` TEXT NOT NULL,
                    `detail` TEXT,
                    `catatan` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    FOREIGN KEY(`transaksi_id`) REFERENCES `transaksi_peminjaman`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`) REFERENCES `users`(`id`)
                        ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `audit_log_new`
                    (`id`, `transaksi_id`, `user_id`, `aksi`, `detail`, `catatan`, `timestamp`)
                SELECT
                    `id`,
                    CASE WHEN `transaksi_id` = 0 THEN NULL ELSE `transaksi_id` END,
                    `user_id`,
                    `aksi`,
                    `detail`,
                    `catatan`,
                    `timestamp`
                FROM `audit_log`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `audit_log`")
            db.execSQL("ALTER TABLE `audit_log_new` RENAME TO `audit_log`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_transaksi_id` ON `audit_log` (`transaksi_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_user_id` ON `audit_log` (`user_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_aksi` ON `audit_log` (`aksi`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_timestamp` ON `audit_log` (`timestamp`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transaksi_peminjaman` ADD COLUMN `remote_id` TEXT")
            db.execSQL("ALTER TABLE `transaksi_peminjaman` ADD COLUMN `sync_key` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `transaksi_peminjaman` ADD COLUMN `sync_state` TEXT NOT NULL DEFAULT 'pending'")
            db.execSQL("ALTER TABLE `transaksi_peminjaman` ADD COLUMN `last_sync_error` TEXT")
            db.execSQL(
                """
                UPDATE `transaksi_peminjaman`
                SET
                    `sync_key` = lower(hex(randomblob(16))),
                    `sync_state` = CASE
                        WHEN `id` BETWEEN 1 AND 7 THEN 'local_only'
                        ELSE 'pending'
                    END
                WHERE `sync_key` = ''
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_transaksi_peminjaman_sync_key` " +
                    "ON `transaksi_peminjaman` (`sync_key`)"
            )
            db.execSQL("ALTER TABLE `master_dokumen` ADD COLUMN `remote_id` TEXT")
            db.execSQL("ALTER TABLE `instansi_peminjam` ADD COLUMN `remote_id` TEXT")
        }
    }
}
