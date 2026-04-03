package com.example.urbaneye.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbaneye.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _userState.value = UserState.LoggedOut
        } else {
            fetchUserProfile(currentUser.uid)
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                val user = document.toObject(User::class.java) ?: User(uid = uid, email = auth.currentUser?.email ?: "")
                _userState.value = UserState.LoggedIn(user)
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun login(email: String, pass: String) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                checkUserStatus()
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            try {
                println("DEBUG: Starting Auth Create")
                val result = auth.createUserWithEmailAndPassword(email, pass).await()

                println("DEBUG: Auth Success, Starting Firestore Write")
                val user = User(uid = result.user!!.uid, name = name, email = email)
                firestore.collection("users").document(user.uid).set(user).await()

                println("DEBUG: Firestore Success")
                _userState.value = UserState.LoggedIn(user)
            } catch (e: Exception) {
                println("DEBUG: Caught Error: ${e.message}")
                _userState.value = UserState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _userState.value = UserState.LoggedOut
    }

    fun updateUserProfile(name: String) {
        val currentState = _userState.value
        if (currentState is UserState.LoggedIn) {
            viewModelScope.launch {
                try {
                    val updatedUser = currentState.user.copy(name = name)
                    firestore.collection("users").document(updatedUser.uid).set(updatedUser).await()
                    _userState.value = UserState.LoggedIn(updatedUser)
                } catch (e: Exception) {
                    // Handle update error
                }
            }
        }
    }
}

sealed class UserState {
    object Loading : UserState()
    object LoggedOut : UserState()
    data class LoggedIn(val user: User) : UserState()
    data class Error(val message: String) : UserState()
}
