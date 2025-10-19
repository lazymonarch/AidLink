package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import com.aidlink.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
data class ChatDeepLinkInfo(val chatId: String, val userName: String)
data class ReviewDeepLinkInfo(val requestId: String, val revieweeId: String)

@HiltViewModel
class AppNavViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel() {
    private val _chatDeepLinkInfo = MutableStateFlow<ChatDeepLinkInfo?>(null)
    val chatDeepLinkInfo: StateFlow<ChatDeepLinkInfo?> = _chatDeepLinkInfo

    private val _reviewDeepLinkInfo = MutableStateFlow<ReviewDeepLinkInfo?>(null)
    val reviewDeepLinkInfo: StateFlow<ReviewDeepLinkInfo?> = _reviewDeepLinkInfo

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