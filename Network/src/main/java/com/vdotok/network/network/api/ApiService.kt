package com.vdotok.network.network.api

import com.vdotok.network.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("Login")
    suspend fun loginUser(@Body model: LoginUserModel): LoginResponse

    @POST("SignUp")
    suspend fun signUp(@Body model: SignUpModel): LoginResponse

    @POST("CheckUsername")
    suspend fun checkUserName(@Body model: CheckUserModel): Response<LoginResponse>

    @POST("CheckEmail")
    suspend fun checkEmail(@Body model: CheckUserModel): LoginResponse

    @POST("AllUsers")
    suspend fun getAllUsers(@Header("Authorization") auth_token: String): GetAllUsersResponseModel

    @POST("AllGroups")
    suspend fun getAllGroups(@Header("Authorization") auth_token: String): AllGroupsResponse

    @POST("DeleteGroup")
    suspend fun deleteGroup(@Header("Authorization") auth_token: String, @Body model: DeleteGroupModel): DeleteGroupResponseModel

    @POST("RenameGroup")
    suspend fun updateGroupName(@Header("Authorization") auth_token: String, @Body model: UpdateGroupNameModel): UpdateGroupNameResponseModel

    @POST("CreateGroup")
    suspend fun createGroup(@Header("Authorization") auth_token: String, @Body model: CreateGroupModel): CreateGroupResponse

    @POST("AuthenticateSDK")
    suspend fun authSDK(@Body model: AuthenticationRequest): Response<AuthenticationResponse>
}