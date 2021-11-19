package com.vdotok.one2many.ui.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.vdotok.network.di.module.RetrofitModule
import com.vdotok.network.models.CheckUserModel
import com.vdotok.network.network.Result
import com.vdotok.network.models.LoginUserModel
import com.vdotok.network.repository.AccountRepository
import com.vdotok.network.models.SignUpModel
import com.vdotok.one2many.utils.ApplicationConstants


class AccountViewModel: ViewModel() {

    fun loginUser(email: String, password: String) = liveData {

            val service = RetrofitModule.provideRetrofitService()
            val repo = AccountRepository(service)
            emit(Result.Loading)
            emit(repo.login(LoginUserModel(email, password, ApplicationConstants.SDK_PROJECT_ID)))
    }


    fun checkEmailAlreadyExist(email: String) = liveData {

        val service = RetrofitModule.provideRetrofitService()
        val repo = AccountRepository(service)
        emit(Result.Loading)
        emit(repo.emailAlreadyExist(CheckUserModel(email)))
    }

    fun signUp(signup: SignUpModel) = liveData {

        val service = RetrofitModule.provideRetrofitService()
        val repo = AccountRepository(service)
        emit(Result.Loading)
        emit(repo.signUp(signup))
    }

}