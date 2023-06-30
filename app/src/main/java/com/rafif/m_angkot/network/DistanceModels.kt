package com.rafif.m_angkot.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DistanceModels(
    val routes: List<RouteFromDb>
) : Parcelable

@Parcelize
data class Route(
    val summary: SummaryDistance
) : Parcelable

@Parcelize
data class SummaryDistance(
    val distance: Double,
    val duration: Double
) : Parcelable