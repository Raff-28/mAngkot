package com.rafif.m_angkot.ui.register

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rafif.m_angkot.databinding.ActivityRegisterBinding
import com.rafif.m_angkot.utils.GeneralUtils.disable
import com.rafif.m_angkot.utils.GeneralUtils.enable
import com.rafif.m_angkot.utils.GeneralUtils.gone
import com.rafif.m_angkot.utils.GeneralUtils.showToast
import com.rafif.m_angkot.utils.GeneralUtils.visible

class RegisterActivity : AppCompatActivity(), RegisterContract {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val presenter = RegisterPresenter(this, this)
        binding.apply {
            btnRegisterRegister.setOnClickListener {
                presenter.register(
                    mapOf(
                        "email" to etRegisterEmail.text.toString(),
                        "password" to etRegisterPassword.text.toString()
                    )
                )
            }
            tvRegisterLogin.setOnClickListener { finish() }
        }
    }

    override fun isLoading(value: Boolean) {
        if (value) {
            binding.btnRegisterRegister.disable()
            binding.pbRegister.visible()
        } else {
            binding.btnRegisterRegister.enable()
            binding.pbRegister.gone()
        }
    }

    override fun onError(error: String) {
        showToast(this, error)
        isLoading(false)
    }

    override fun onRegistered() {
        finish()
    }
}