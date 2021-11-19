package com.vdotok.one2many.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.vdotok.network.di.module.RetrofitModule
import com.vdotok.network.models.CreateGroupModel
import com.vdotok.network.models.LoginUserModel
import com.vdotok.network.models.UpdateGroupNameModel
import com.vdotok.network.network.Result
import com.vdotok.network.repository.AccountRepository
import com.vdotok.network.repository.GroupRepository
import com.vdotok.network.repository.UserListRepository


class AllUserViewModel: ViewModel() {

    private val service = RetrofitModule.provideRetrofitService()
    private val groupRepo = GroupRepository(service)
    private val userListRepo = UserListRepository(service)

    fun getAllUsers(context: Context, token: String) = liveData {
            emit(Result.Loading)
            emit(userListRepo.getAllUsers(token))
    }

    fun createGroup(token: String, model: CreateGroupModel) = liveData {
        emit(Result.Loading)
        emit(groupRepo.createGroup(token, model))
    }

    fun updateGroupName(token: String, model: UpdateGroupNameModel) = liveData {
        emit(Result.Loading)
        emit(groupRepo.updateGroupName(token, model))
    }


}