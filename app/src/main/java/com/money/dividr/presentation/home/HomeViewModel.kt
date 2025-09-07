package com.money.dividr.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.money.dividr.domain.model.Group
import com.money.dividr.domain.model.UserData
 
import com.money.dividr.domain.usecase.CreateGroupUseCase
import com.money.dividr.domain.usecase.GetGroupsUseCase
import com.money.dividr.domain.usecase.JoinGroupUseCase  
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.money.dividr.data.repository.Result
import com.money.dividr.presentation.home.GroupCreationUiState.*

 
sealed class GroupCreationUiState {
    object Idle : GroupCreationUiState()
    object Loading : GroupCreationUiState()
    data class Success(val groupId: String) : GroupCreationUiState()
    data class Error(val message: String) : GroupCreationUiState()
}

 
sealed class JoinGroupUiState {
    object Idle : JoinGroupUiState()
    object Loading : JoinGroupUiState()
    data class Success(val groupId: String) : JoinGroupUiState()
    data class Error(val message: String) : JoinGroupUiState()
}

 
sealed class GroupsUiState {
    object Idle : GroupsUiState()
    object Loading : GroupsUiState()
    data class Success(val groups: List<Group>) : GroupsUiState()
    data class Error(val message: String) : GroupsUiState()
    object Empty : GroupsUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,  
    val auth: FirebaseAuth
) : ViewModel() {

     
    private val _groupCreationStatus = MutableStateFlow<GroupCreationUiState>(GroupCreationUiState.Idle)
    val groupCreationStatus: StateFlow<GroupCreationUiState> = _groupCreationStatus.asStateFlow()

     
    private val _joinGroupStatus = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.Idle)
    val joinGroupStatus: StateFlow<JoinGroupUiState> = _joinGroupStatus.asStateFlow()

     
    private val _groupsState = MutableStateFlow<GroupsUiState>(GroupsUiState.Idle)
    val groupsState: StateFlow<GroupsUiState> = _groupsState.asStateFlow()

    init {
        if (auth.currentUser != null) {
            loadGroups()
        } else {
            _groupsState.value = GroupsUiState.Error("User not authenticated")
        }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            _groupCreationStatus.value = GroupCreationUiState.Loading
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _groupCreationStatus.value = GroupCreationUiState.Error("User not authenticated.")
                return@launch
            }

            val creatorInfo = UserData(
                userId = currentUser.uid,
                username = currentUser.displayName,
                profilePictureUrl = currentUser.photoUrl?.toString(),
                email = currentUser.email
            )

            val result = createGroupUseCase(groupName, creatorInfo)

            when(result){
                is Result.Success -> {
                    _groupCreationStatus.value = Success(result.data)
                    loadGroups()  
                }
                is Result.Error -> { _groupCreationStatus.value = Error(result.exception.message ?: "Failed to create group.")
                }

                Result.Loading -> _groupCreationStatus.value = Loading
            }
        }
    }

    fun resetGroupCreationStatus() {
        _groupCreationStatus.value = GroupCreationUiState.Idle
    }

    fun joinGroup(groupCode: String) {
        viewModelScope.launch {
            _joinGroupStatus.value = JoinGroupUiState.Loading
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _joinGroupStatus.value = JoinGroupUiState.Error("User not authenticated.")
                return@launch
            }

            val joiningUserInfo = UserData(
                userId = currentUser.uid,
                username = currentUser.displayName,
                profilePictureUrl = currentUser.photoUrl?.toString(),
                email = currentUser.email
            )

             
            val result = joinGroupUseCase(groupCode, joiningUserInfo)
            when(result){
                is Result.Success -> {
                    _joinGroupStatus.value = JoinGroupUiState.Success(result.data)
                    loadGroups()  
                }
                is Result.Error -> {  _joinGroupStatus.value = JoinGroupUiState.Error(result.exception.message ?: "Failed to join group.")
                }
                Result.Loading -> {}
            }
        }
    }

    fun resetJoinGroupStatus() {
        _joinGroupStatus.value = JoinGroupUiState.Idle
    }

    private fun loadGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsUiState.Loading
            getGroupsUseCase()
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading groups", e)
                    _groupsState.value = GroupsUiState.Error(e.message ?: "Failed to load groups")
                }
                .collect { groups ->
                    if (groups.isEmpty()) {
                        _groupsState.value = GroupsUiState.Empty
                    } else {
                        _groupsState.value = GroupsUiState.Success(groups)
                    }
                }
        }
    }
}
