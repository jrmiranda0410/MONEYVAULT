package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MoneyVaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val pin: String = "",
    val isUnlocked: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
    val errorMessage: String? = null,
    val biometricEnabled: Boolean = false
)

class LockViewModel(
    private val repository: MoneyVaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeIfEmpty()
            repository.userSettings.collect { settings ->
                if (settings != null) {
                    val now = System.currentTimeMillis()
                    if (now < settings.lockoutUntilMillis) {
                        val remaining = ((settings.lockoutUntilMillis - now) / 1000).toInt().coerceAtLeast(1)
                        startLockoutCountdown(remaining)
                    }
                    _uiState.update { it.copy(biometricEnabled = settings.biometricEnabled) }
                }
            }
        }
    }

    fun onDigitEntered(digit: Char) {
        if (_uiState.value.isLockedOut) return
        val currentPin = _uiState.value.pin
        if (currentPin.length >= 4) return

        val newPin = currentPin + digit
        _uiState.update { it.copy(pin = newPin, errorMessage = null) }

        if (newPin.length == 4) {
            submitPin(newPin)
        }
    }

    fun onDelete() {
        if (_uiState.value.isLockedOut) return
        val current = _uiState.value.pin
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(pin = current.dropLast(1), errorMessage = null) }
        }
    }

    fun onClear() {
        _uiState.update { it.copy(pin = "", errorMessage = null) }
    }

    fun lockApp() {
        _uiState.update { it.copy(pin = "", isUnlocked = false, errorMessage = null) }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(isUnlocked = true, errorMessage = null, pin = "") }
    }

    private fun submitPin(pin: String) {
        viewModelScope.launch {
            val isCorrect = repository.verifyPin(pin)
            if (isCorrect) {
                _uiState.update { it.copy(isUnlocked = true, errorMessage = null, pin = "") }
            } else {
                val settings = repository.userSettingsDao.getSettings()
                val now = System.currentTimeMillis()
                if (settings != null && now < settings.lockoutUntilMillis) {
                    val remaining = ((settings.lockoutUntilMillis - now) / 1000).toInt().coerceAtLeast(1)
                    startLockoutCountdown(remaining)
                } else {
                    _uiState.update {
                        it.copy(
                            pin = "",
                            errorMessage = "Incorrect PIN. Please try again."
                        )
                    }
                }
            }
        }
    }

    private fun startLockoutCountdown(seconds: Int) {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                isLockedOut = true,
                lockoutSecondsRemaining = seconds,
                pin = "",
                errorMessage = "Too many attempts. Locked for $seconds seconds."
            )
        }
        countdownJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                delay(1000)
                current--
                _uiState.update {
                    it.copy(
                        lockoutSecondsRemaining = current,
                        errorMessage = if (current > 0) "Too many attempts. Locked for $current seconds." else null
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isLockedOut = false,
                    lockoutSecondsRemaining = 0,
                    errorMessage = null
                )
            }
        }
    }
}
