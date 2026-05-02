package com.isharaai.isl.core.db
import androidx.room.*

// Sign data access object
@Dao
interface SignDao {
    @Query("SELECT * FROM signs WHERE signId = :signId")
    suspend fun getSign(signId: String): SignEntity?

    @Query("SELECT * FROM signs")
    suspend fun getAllSigns(): List<SignEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(signs: List<SignEntity>)
}
