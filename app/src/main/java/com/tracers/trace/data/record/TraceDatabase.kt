package com.tracers.trace.data.record

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 앱 로컬 DB. 기기 밖으로 보내지 않는다. */
@Database(
    entities = [
        TripSummaryEntity::class, ChargeSessionEntity::class, TripInProgressEntity::class,
        KmRecordEntity::class, CoinWalletEntity::class, CoinHistoryEntity::class,
        MaintenanceRecordEntity::class, MaintenanceBaselineEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun tripSummaryDao(): TripSummaryDao
    abstract fun chargeSessionDao(): ChargeSessionDao
    abstract fun tripInProgressDao(): TripInProgressDao
    abstract fun kmRecordDao(): KmRecordDao
    abstract fun coinWalletDao(): CoinWalletDao
    abstract fun coinHistoryDao(): CoinHistoryDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        fun create(context: Context): TraceDatabase =
            Room.databaseBuilder(context.applicationContext, TraceDatabase::class.java, "trace.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** 1 → 2: 진행 중 주행 요약 한 줄 테이블 추가 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trip_in_progress` (" +
                        "`id` INTEGER NOT NULL, `startedAtMs` INTEGER NOT NULL, `endedAtMs` INTEGER NOT NULL, " +
                        "`distanceKm` REAL NOT NULL, `energyUsedKwh` REAL NOT NULL, " +
                        "`hardAccelCount` INTEGER NOT NULL, `regenKwh` REAL NOT NULL, `hvacOnSec` INTEGER NOT NULL, " +
                        "`startBatteryPercent` INTEGER, `endBatteryPercent` INTEGER, PRIMARY KEY(`id`))"
                )
            }
        }

        /** 2 → 3: 1km 기록, 안전점수·코인 지갑(한 줄), 코인 기록 테이블 추가 (E-01~E-04) */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `km_record` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tripStartedAtMs` INTEGER NOT NULL, " +
                        "`kmIndex` INTEGER NOT NULL, `startedAtMs` INTEGER NOT NULL, `endedAtMs` INTEGER NOT NULL, " +
                        "`hardAccel` INTEGER NOT NULL, `hardDecel` INTEGER NOT NULL, `hardStop` INTEGER NOT NULL, " +
                        "`speeding` INTEGER NOT NULL, `kmScore` INTEGER NOT NULL, `scoreBefore` REAL NOT NULL, " +
                        "`scoreAfter` REAL NOT NULL, `coinsEarned` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_km_record_tripStartedAtMs` ON `km_record` (`tripStartedAtMs`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `coin_wallet` (" +
                        "`id` INTEGER NOT NULL, `safetyScore` REAL NOT NULL, `unclaimedCoins` INTEGER NOT NULL, " +
                        "`balanceCoins` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `coin_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `atMs` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_coin_history_atMs` ON `coin_history` (`atMs`)")
            }
        }

        /** 3 → 4: 정비 기록, 정비 기준점(앱이 처음 본 날짜·주행거리) 테이블 추가 (D-01·D-05) */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_record` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                        "`odometerKm` INTEGER NOT NULL, `place` TEXT NOT NULL, `items` TEXT NOT NULL, " +
                        "`other` INTEGER NOT NULL, `costWon` INTEGER, `memo` TEXT NOT NULL, " +
                        "`inputMethod` TEXT NOT NULL, `createdAtMs` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_record_epochDay` ON `maintenance_record` (`epochDay`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_baseline` (" +
                        "`id` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, `odometerKm` REAL NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
