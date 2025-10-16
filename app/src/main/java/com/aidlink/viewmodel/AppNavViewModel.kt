package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import com.aidlink.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class DeepLinkInfo(val chatId: String, val userName: String)

@HiltViewModel
class AppNavViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel() {
    private val _deepLinkInfo = MutableStateFlow<DeepLinkInfo?>(null)
    val deepLinkInfo: StateFlow<DeepLinkInfo?> = _deepLinkInfo

    fun setDeepLink(chatId: String, userName: String) {
        _deepLinkInfo.value = DeepLinkInfo(chatId, userName)
    }

    fun consumeDeepLink() {
        _deepLinkInfo.value = null
    }
}