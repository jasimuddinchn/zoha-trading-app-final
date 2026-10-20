package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettings
import com.example.data.model.ConditionGrade
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE phones ADD COLUMN itemCondition TEXT NOT NULL DEFAULT 'Used'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'LIGHT'")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN dynamicColor INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE phones ADD COLUMN cashReceivedBdt REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE phones ADD COLUMN dueAmountBdt REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptUrl TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [PhoneItem::class, ExpenseItem::class, AppSettings::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phoneDao(): PhoneDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "zoha_trading_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            /**
             * onCreate is ONLY called when the database file is first created on a fresh install.
             * It never runs during app launches, updates, or reinstalls when the DB file exists.
             */
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialSettingsOnly(database)
                    }
                }
            }

            /**
             * onOpen is called whenever the database is opened.
             * STRICT RULE: Never clear, alter, or inject dummy/sample records in onOpen.
             */
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Strictly no-op: user data is 100% preserved without any automatic dummy injections.
            }
        }

        /**
         * Seeds default business profile and security configuration on brand-new fresh database creation.
         * Dummy phones and expenses are strictly NOT injected so user data remains clean and pristine.
         */
        suspend fun populateInitialSettingsOnly(database: AppDatabase) {
            val settingsDao = database.settingsDao()

            if (settingsDao.getSettingsDirect() == null) {
                settingsDao.insertOrUpdateSettings(
                    AppSettings(
                        id = 1,
                        rmbToBdtRate = 17.50,
                        ownerPin = "1111",
                        staff1Pin = "2222",
                        staff2Pin = "3333",
                        isAdminPinSetupCompleted = true,
                        adminNameBn = "Jasim",
                        adminNameEn = "Jasim",
                        staff1NameBn = "Sumon",
                        staff1NameEn = "Sumon",
                        staff2NameBn = "Deedat",
                        staff2NameEn = "Deedat",
                        shopName = "Zoha Trading",
                        shopSubtitle = "Trading Worldwide Since 2012",
                        shopAddress = "Shop #204, Level 3, Eastern Plaza, Dhaka",
                        shopPhone = "01755-987654",
                        staff1Name = "Sumon",
                        staff2Name = "Deedat",
                        defaultWarrantyText = "10 days replacement warranty and 2 years free service warranty.",
                        appLanguage = "en",
                        themeMode = "LIGHT",
                        dynamicColor = true
                    )
                )
            }
        }
    }
}
