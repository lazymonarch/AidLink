package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import com.aidlink.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel()