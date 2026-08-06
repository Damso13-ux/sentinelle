package com.sentinelle.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PatternListItemEntity::class,
        PatternListEntity::class,
        BlockedEventEntity::class,
        CallHistoryEntity::class,
        NumberLabelEntity::class,
        SmsHistoryEntity::class,
        HeuristicShadowEventEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patternListItemDao(): PatternListItemDao

    abstract fun patternListDao(): PatternListDao

    abstract fun blockedEventDao(): BlockedEventDao

    abstract fun callHistoryDao(): CallHistoryDao

    abstract fun numberLabelDao(): NumberLabelDao

    abstract fun smsHistoryDao(): SmsHistoryDao

    abstract fun heuristicShadowEventDao(): HeuristicShadowEventDao

    fun seedUserLists() {
        val dao = patternListDao()
        dao.insertIfAbsent(userList(PatternListEntity.USER_ALLOW_LIST_ID, "user allow", PatternListEntity.TYPE_ALLOW))
        dao.insertIfAbsent(userList(PatternListEntity.USER_BLOCK_LIST_ID, "user block", PatternListEntity.TYPE_BLOCK))
        dao.insertIfAbsent(
            userList(
                PatternListEntity.USER_ALLOW_SMS_LIST_ID,
                "user allow sms",
                PatternListEntity.TYPE_ALLOW,
                PatternListEntity.CHANNEL_SMS,
            ),
        )
        dao.insertIfAbsent(
            userList(
                PatternListEntity.USER_BLOCK_SMS_LIST_ID,
                "user block sms",
                PatternListEntity.TYPE_BLOCK,
                PatternListEntity.CHANNEL_SMS,
            ),
        )
    }

    companion object {
        @Volatile
        @Suppress("PropertyName")
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { db ->
                    INSTANCE = db
                    db.seedUserLists()
                    com.sentinelle.app.util.PatternManager
                        .clearCache()
                }
            }

        private fun userList(
            id: Long,
            name: String,
            type: String,
            channel: String = PatternListEntity.CHANNEL_PHONE,
        ): PatternListEntity =
            PatternListEntity(
                id = id,
                name = name,
                description = null,
                license = null,
                isEnabled = true,
                priority = 0,
                version = "",
                count = 0L,
                channel = channel,
                type = type,
                source = PatternListEntity.SOURCE_USER,
                downloadUrl = "",
                lastDownloaded = 0,
            )

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pattern_list (
                            id INTEGER NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            description TEXT,
                            license TEXT,
                            isEnabled INTEGER NOT NULL DEFAULT 0,
                            priority INTEGER NOT NULL DEFAULT 100,
                            version TEXT NOT NULL DEFAULT '',
                            count INTEGER NOT NULL DEFAULT 0,
                            channel TEXT NOT NULL DEFAULT 'phone',
                            type TEXT NOT NULL DEFAULT 'block',
                            source TEXT NOT NULL DEFAULT 'api',
                            downloadUrl TEXT NOT NULL DEFAULT '',
                            lastDownloaded INTEGER NOT NULL DEFAULT 0
                        )
                        """,
                    )
                    db.execSQL(
                        """
                        INSERT INTO pattern_list (id, name, description, license, isEnabled, priority, version, count, channel, type, source, downloadUrl, lastDownloaded)
                        VALUES
                        (-1, 'user allow', NULL, NULL, 1, 0, '', 0, 'phone', 'allow', 'user', '', 0),
                        (-2, 'user block', NULL, NULL, 1, 0, '', 0, 'phone', 'block', 'user', '', 0)
                        """,
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS pattern_list_item (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            listId INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            pattern TEXT NOT NULL,
                            dateAdded INTEGER NOT NULL,
                            FOREIGN KEY (listId) REFERENCES pattern_list(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """,
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pattern_list_item_pattern ON pattern_list_item(pattern)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pattern_list_item_listId ON pattern_list_item(listId)")
                    db.execSQL(
                        """
                        INSERT INTO pattern_list_item (id, listId, name, pattern, dateAdded)
                        SELECT id,
                               CASE action
                                   WHEN 'allow' THEN -1
                                   WHEN 'identify' THEN -1
                                   ELSE -2
                               END,
                               name, pattern, dateAdded
                        FROM patterns WHERE source = 'user'
                        """,
                    )
                    db.execSQL("DROP TABLE IF EXISTS patterns")
                    db.execSQL("DROP TABLE IF EXISTS list_metadata")
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS blocked_events (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            channel TEXT NOT NULL,
                            phoneNumber INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            reasonType TEXT NOT NULL,
                            reasonListId INTEGER,
                            reasonPatternName TEXT,
                            heuristicScore REAL,
                            heuristicReason TEXT
                        )
                        """,
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_events_timestamp ON blocked_events(timestamp)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_events_channel ON blocked_events(channel)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_events_phoneNumber ON blocked_events(phoneNumber)")
                    db.execSQL(
                        """
                        INSERT INTO blocked_events (channel, phoneNumber, timestamp, reasonType, reasonListId, reasonPatternName, heuristicScore, heuristicReason)
                        SELECT 'phone', phoneNumber, timestamp, 'pattern_list', NULL, NULL, NULL, NULL
                        FROM blocked_calls
                        """,
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS call_history (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            phoneNumber INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            wasBlocked INTEGER NOT NULL
                        )
                        """,
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_call_history_phoneNumber ON call_history(phoneNumber)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_call_history_timestamp ON call_history(timestamp)")
                }
            }

        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS number_labels (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            phoneNumber INTEGER NOT NULL,
                            category TEXT NOT NULL,
                            note TEXT,
                            dateAdded INTEGER NOT NULL
                        )
                        """,
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_number_labels_phoneNumber ON number_labels(phoneNumber)",
                    )
                }
            }

        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS sms_history (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            phoneNumber INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            wasBlocked INTEGER NOT NULL
                        )
                        """,
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_phoneNumber ON sms_history(phoneNumber)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_timestamp ON sms_history(timestamp)")
                }
            }

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS heuristic_shadow_events (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            channel TEXT NOT NULL,
                            phoneNumber INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            score REAL NOT NULL,
                            reason TEXT
                        )
                        """,
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_heuristic_shadow_events_timestamp ON heuristic_shadow_events(timestamp)",
                    )
                }
            }

        // blocked_calls was superseded by blocked_events back in MIGRATION_2_3,
        // which copied every row across. It has had no reader or writer since,
        // so this only drops storage that was already dead — the history it
        // held is still in blocked_events and stays visible in the dashboard.
        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS blocked_calls")
                }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sentinelle.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration(false)
                .allowMainThreadQueries()
                .build()
    }
}
