package com.rafif.m_angkot.network

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "route")
@Parcelize
data class RouteFromDb(
    @PrimaryKey
    val name: String,
    val lat: Double,
    val lng: Double
) : Parcelable