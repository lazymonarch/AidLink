package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import com.aidlink.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
data class ChatDeepLinkInfo(val chatId: String, val userName: String)
data class ReviewDeepLinkInfo(val requestId: String, val revieweeId: String)

sealed class AuthProfileState {
    object Unknown : AuthProfileState()
    object Authenticated : AuthProfileState()
    object Unauthenticated : AuthProfileState()
    object NeedsProfile : AuthProfileState()
}

@HiltViewModel
class AppNavViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel() {
    private val _chatDeepLinkInfo = MutableStateFlow<ChatDeepLinkInfo?>(null)
    val chatDeepLinkInfo: StateFlow<ChatDeepLinkInfo?> = _chatDeepLinkInfo

    private val _reviewDeepLinkInfo = MutableStateFlow<ReviewDeepLinkInfo?>(null)
    val reviewDeepLinkInfo: StateFlow<ReviewDeepLinkInfo?> = _reviewDeepLinkInfo

    @OptIn(ExperimentalCoroutinesApi::class)
    val authProfileState: StateFlow<AuthProfileState> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user == null) {
                flow { emit(AuthProfileState.Unauthenticated) }
            } else {
                flow {
                    if (repository.isUserProfileExists()) {
                        emit(AuthProfileState.Authenticated)
                    } else {
                        emit(AuthProfileState.NeedsProfile)
                    }
                }
            }
        }
        .catch { emit(AuthProfileState.Unauthenticated) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AuthProfileState.Unknown
        )

    fun setChatDeepLink(chatId: String, userName: String) {
        _chatDeepLinkInfo.value = ChatDeepLinkInfo(chatId, userName)
    }

    fun setReviewDeepLink(requestId: String, revieweeId: String) {
        _reviewDeepLinkInfo.value = ReviewDeepLinkInfo(requestId, revieweeId)
    }

    fun consumeDeepLink() {
        _chatDeepLinkInfo.value = null
        _reviewDeepLinkInfo.value = null
    }
}