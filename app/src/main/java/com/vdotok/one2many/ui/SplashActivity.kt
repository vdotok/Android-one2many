package com.vdotok.one2many.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.ActivitySplashBinding
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.account.AccountsActivity.Companion.createAccountsActivity
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.ViewUtils.setStatusBarGradient

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.ic_account_gradient_bg)
        init()

    }

    private fun init() {
        prefs = Prefs(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        moveToAccountsActivity()
    }

    private fun moveToAccountsActivity() {
        Handler(Looper.getMainLooper()).postDelayed({

            prefs.loginInfo?.let {
                startActivity(applicationContext?.let { DashBoardActivity.createDashboardActivity(it) })
                finish()

            } ?: kotlin.run {
                startActivity(createAccountsActivity(this))
                finish()

            }

        }, 2000)

    }
}