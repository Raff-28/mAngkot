package com.rafif.m_angkot.ui.register

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rafif.m_angkot.utils.Const.LOGGED_IN
import com.rafif.m_angkot.utils.GeneralUtils.isValidEmail
import com.rafif.m_angkot.utils.PrefUtils


interface RegisterContract {
    fun isLoading(value: Boolean)

    fun onError(error: String)

    fun onRegistered()
}

class RegisterPresenter(
    private val context: Context,
    private val contract: RegisterContract
) {
    private val firestore = Firebase.firestore
    private val prefUtils = PrefUtils(context, LOGGED_IN, MODE_PRIVATE)

    fun register(request: Map<String, String>) {
        contract.isLoading(true)

        //Check if email has registered
        if (verifyData(request["email"]!!, request["password"]!!)) {
            firestore.collection("users").whereEqualTo("email", request["email"]).get()
                .addOnSuccessListener {
                    if (it.isEmpty) {
                        doRegister(request)
                    } else {
                        contract.onError("Email already used!")
                    }
                }
                .addOnFailureListener {
                    contract.onError(it.message ?: "Something happened!")
                }
        }
    }

    private fun verifyData(email: String, password: String): Boolean {
        if (password.length < 6) {
            contract.onError("Password too short!")
            return false
        } else if (!isValidEmail(email)) {
            contract.onError("Email not valid!")
            return false
        }

        return true
    }

    private fun doRegister(request: Map<String, String>) {
        //Add data to firebase
        firestore.collection("users").add(request)
            .addOnSuccessListener {
                //Save id to shared preference
                prefUtils.saveStringToPref(LOGGED_IN, it.id)
                contract.onRegistered()
            }
            .addOnFailureListener {
                contract.onError(it.message ?: "Something happened!")
            }
    }
}