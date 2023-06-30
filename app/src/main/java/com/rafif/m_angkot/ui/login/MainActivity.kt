package com.rafif.m_angkot.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rafif.m_angkot.databinding.ActivityMainBinding
import com.rafif.m_angkot.ui.home.HomeActivity
import com.rafif.m_angkot.ui.register.RegisterActivity
import com.rafif.m_angkot.utils.GeneralUtils.disable
import com.rafif.m_angkot.utils.GeneralUtils.enable
import com.rafif.m_angkot.utils.GeneralUtils.gone
import com.rafif.m_angkot.utils.GeneralUtils.showToast
import com.rafif.m_angkot.utils.GeneralUtils.visible

class MainActivity : AppCompatActivity(), LoginContract {
    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: LoginPresenter

    private var isRelogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = LoginPresenter(this, this)
        presenter.getRouteDetails()

        binding.apply {
            btnLoginLogin.setOnClickListener {
                isRelogin = true
                presenter.getRouteDetails()
            }
            tvLoginRegister.setOnClickListener {
                val i = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivity(i)
            }
        }
    }

    override fun isLoading(value: Boolean) {
        if (value) {
            binding.btnLoginLogin.disable()
            binding.tvLoginRegister.disable()
            binding.pbLogin.visible()
        } else {
            binding.btnLoginLogin.enable()
            binding.tvLoginRegister.enable()
            binding.pbLogin.gone()
        }
    }

    override fun onError(error: String) {
        showToast(this, error)
        isLoading(false)
    }

    override fun onGetRoute() {
        isLoading(false)

        if (isRelogin) {
            presenter.login(
                mapOf(
                    "email" to binding.etLoginEmail.text.toString(),
                    "password" to binding.etLoginPassword.text.toString()
                )
            )
        } else {
            if (presenter.isLoggedIn().isNotEmpty()) {
                onLoggedIn()
            }
        }
    }

    override fun onLoggedIn() {
        val i = Intent(this, HomeActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
}