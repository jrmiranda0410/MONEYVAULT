package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.MoneyVaultRepository

class MoneyVaultViewModelFactory(
    private val repository: MoneyVaultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MoneyVaultViewModel::class.java) -> {
                MoneyVaultViewModel(repository) as T
            }
            modelClass.isAssignableFrom(LockViewModel::class.java) -> {
                LockViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
