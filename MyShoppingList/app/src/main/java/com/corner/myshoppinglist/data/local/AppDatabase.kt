package com.corner.myshoppinglist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.corner.myshoppinglist.data.local.dao.*
import com.corner.myshoppinglist.data.local.entities.*

@Database(
    entities = [ShoppingList::class, ShoppingItem::class, MasterItem::class, Photo::class, AppSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun masterItemDao(): MasterItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun settingsDao(): SettingsDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite doesn't support changing column types, so we must recreate the table
                
                // 1. Create the new table with REAL for quantity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `listId` INTEGER NOT NULL, 
                        `itemName` TEXT NOT NULL, 
                        `estimatedPrice` REAL, 
                        `actualPrice` REAL, 
                        `quantity` REAL NOT NULL DEFAULT 1.0, 
                        `unit` TEXT, 
                        `purchased` INTEGER NOT NULL DEFAULT 0, 
                        `orderIndex` INTEGER NOT NULL DEFAULT 0, 
                        `notes` TEXT, 
                        FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                
                // 2. Copy data from the old table
                db.execSQL("""
                    INSERT INTO `shopping_items_new` (`id`, `listId`, `itemName`, `estimatedPrice`, `actualPrice`, `quantity`, `unit`, `purchased`, `orderIndex`, `notes`)
                    SELECT `id`, `listId`, `itemName`, `estimatedPrice`, `actualPrice`, CAST(`quantity` AS REAL), `unit`, `purchased`, `orderIndex`, `notes`
                    FROM `shopping_items`
                """)
                
                // 3. Drop the old table
                db.execSQL("DROP TABLE `shopping_items`")
                
                // 4. Rename the new table
                db.execSQL("ALTER TABLE `shopping_items_new` RENAME TO `shopping_items`")
                
                // 5. Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_listId` ON `shopping_items` (`listId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_list_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
