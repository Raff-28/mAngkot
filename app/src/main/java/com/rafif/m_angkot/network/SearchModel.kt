package com.rafif.m_angkot.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchModel(
    val polies: RouteModels,
    val trayek: String,
    val routeCoordinates: List<RouteFromDb>
) : Parcelable
