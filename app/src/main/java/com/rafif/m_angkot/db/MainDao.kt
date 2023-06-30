package com.rafif.m_angkot.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rafif.m_angkot.network.RouteFromDb

@Dao
interface MainDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoute(routes: RouteFromDb)

    @Query("select * from route")
    fun getRoutes(): LiveData<List<RouteFromDb>>

    @Query("select * from route where name in (:names)")
    fun getRouteByNames(names: List<String>): LiveData<List<RouteFromDb>>
}