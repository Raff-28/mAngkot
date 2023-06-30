package com.rafif.m_angkot.ui.login

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rafif.m_angkot.db.MainDb
import com.rafif.m_angkot.network.RouteFromDb
import com.rafif.m_angkot.utils.Const.LOGGED_IN
import com.rafif.m_angkot.utils.PrefUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


interface LoginContract {
    fun isLoading(value: Boolean)

    fun onError(error: String)

    fun onGetRoute()

    fun onLoggedIn()
}

class LoginPresenter(
    private val context: Context,
    private val contract: LoginContract
) {
    private val firestore = Firebase.firestore
    private val prefUtils = PrefUtils(context, LOGGED_IN, MODE_PRIVATE)

    //Check if user has logged in
    fun isLoggedIn() =
        prefUtils.getStringFromPref(LOGGED_IN)

    fun login(request: Map<String, String>) {
        contract.isLoading(true)

        //Authenticate user data from firebase
        firestore
            .collection("users")
            .whereEqualTo("email", request["email"])
            .whereEqualTo("password", request["password"])
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    contract.onError("Invalid email/password!")
                } else {
                    prefUtils.saveStringToPref(LOGGED_IN, it.documentChanges.first().document.id)
                    contract.onLoggedIn()
                }
            }
            .addOnFailureListener {
                contract.onError(it.message ?: "Something happened!")
            }
    }

    private val db = MainDb.getDb(context)
    private val dao = db?.mainDao

    fun getRouteDetails() {
        contract.isLoading(true)

        val firestore = Firebase.firestore
        firestore
            .collection("route-detail")
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    contract.onError("No data found!")
                } else {
                    it.documents.forEach { snapshot ->
                        val data = snapshot.data
                        val lat = data!!["lat"] as String
                        val lng = data["lng"] as String

                        val localRoute = RouteFromDb(
                            data["name"] as String,
                            lat.toDouble(),
                            lng.toDouble()
                        )

                        GlobalScope.launch {
                            dao?.insertRoute(localRoute)
                        }
                    }
                    contract.onGetRoute()
                }
            }
            .addOnFailureListener {
                contract.onError(it.message ?: "Something happened!")
            }
    }

}