package com.rafif.m_angkot.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RouteModels(
    val features: List<Feature>
) : Parcelable

@Parcelize
data class Feature(
    val geometry: Geometry,
    val properties: Property
) : Parcelable

@Parcelize
data class Property(
    val summary: Summary
) : Parcelable

@Parcelize
data class Summary(
    val distance: Double,
    val duration: Double
) : Parcelable

@Parcelize
data class Geometry(
    val coordinates: List<List<Double>>
) : Parcelable


