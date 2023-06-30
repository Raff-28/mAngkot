package com.rafif.m_angkot.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecommendationModel(
    val routeName: String,
    val route: List<List<Double>>,
    val userRoute: List<List<Double>>,
    val distance: Double,
    val trayek: String
) : Parcelable
