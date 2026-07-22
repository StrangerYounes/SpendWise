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
    entities = [
        ShoppingList::class, 
        ShoppingItem::class, 
        MasterItem::class, 
        Photo::class, 
        AppSettings::class,
        Store::class,
        StoreItemPrice::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun masterItemDao(): MasterItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun settingsDao(): SettingsDao
    abstract fun statsDao(): StatsDao
    abstract fun storeDao(): StoreDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create stores table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stores` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL
                    )
                """)

                // 2. Create store_item_prices table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `store_item_prices` (
                        `masterItemId` INTEGER NOT NULL, 
                        `storeId` INTEGER NOT NULL, 
                        `lastPrice` REAL NOT NULL, 
                        `lowestPrice` REAL NOT NULL, 
                        `highestPrice` REAL NOT NULL, 
                        `lastUpdated` INTEGER NOT NULL, 
                        PRIMARY KEY(`masterItemId`, `storeId`), 
                        FOREIGN KEY(`masterItemId`) REFERENCES `master_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_item_prices_masterItemId` ON `store_item_prices` (`masterItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_store_item_prices_storeId` ON `store_item_prices` (`storeId`)")

                // 3. Update shopping_lists table (recreate to add storeId with FK)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_lists_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `createdDate` INTEGER NOT NULL, 
                        `completedDate` INTEGER, 
                        `isCompleted` INTEGER NOT NULL, 
                        `storeId` INTEGER, 
                        FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `shopping_lists_new` (`id`, `name`, `createdDate`, `completedDate`, `isCompleted`, `storeId`)
                    SELECT `id`, `name`, `createdDate`, `completedDate`, `isCompleted`, NULL FROM `shopping_lists`
                """)
                db.execSQL("DROP TABLE `shopping_lists`")
                db.execSQL("ALTER TABLE `shopping_lists_new` RENAME TO `shopping_lists`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_lists_storeId` ON `shopping_lists` (`storeId`)")

                // 4. Update shopping_items table (recreate to add storeId with FK)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `listId` INTEGER NOT NULL, 
                        `itemName` TEXT NOT NULL, 
                        `estimatedPrice` REAL, 
                        `actualPrice` REAL, 
                        `quantity` REAL NOT NULL, 
                        `unit` TEXT, 
                        `purchased` INTEGER NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        `notes` TEXT, 
                        `storeId` INTEGER, 
                        FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO `shopping_items_new` (`id`, `listId`, `itemName`, `estimatedPrice`, `actualPrice`, `quantity`, `unit`, `purchased`, `orderIndex`, `notes`, `storeId`)
                    SELECT `id`, `listId`, `itemName`, `estimatedPrice`, `actualPrice`, `quantity`, `unit`, `purchased`, `orderIndex`, `notes`, NULL FROM `shopping_items`
                """)
                db.execSQL("DROP TABLE `shopping_items`")
                db.execSQL("ALTER TABLE `shopping_items_new` RENAME TO `shopping_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_listId` ON `shopping_items` (`listId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_storeId` ON `shopping_items` (`storeId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_list_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
