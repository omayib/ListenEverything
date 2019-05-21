package com.hepicar.listeneverything.repository

import android.arch.persistence.room.*
import java.util.*

@Entity
class RawDataSensor(
    val uuid:String,val name:String,val timestampString:String,val raw_hrm_ir:String){
}
@Entity
data class SensorData(
    @PrimaryKey var uid: Int,
    @ColumnInfo(name = "date") var date: String?,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "timestamp") var timestampString: String,
    @ColumnInfo(name = "hrm_ir") var hrm_ir: String?,
    @ColumnInfo(name = "hrm_red") var hrm_red: String?
)

@Dao
interface SensorDataQuery {
    @Query("SELECT * FROM SensorData")
    fun getAll(): List<SensorData>

    @Query("SELECT * FROM SensorData WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<SensorData>

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): SensorData

    @Insert
    fun insertAll(vararg users: SensorData)

    @Delete
    fun delete(user: SensorData)
}