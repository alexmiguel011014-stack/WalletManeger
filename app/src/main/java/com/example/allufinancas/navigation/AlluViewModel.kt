package com.example.allufinancas.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.allufinancas.data.DemoDataSource
import com.example.allufinancas.data.DisplayPreferences
import com.example.allufinancas.feature.alerts.AlertsViewModel
import com.example.allufinancas.feature.assistant.AssistantViewModel
import com.example.allufinancas.feature.budgets.BudgetsViewModel
import com.example.allufinancas.feature.home.HomeViewModel
import com.example.allufinancas.feature.settings.SettingsViewModel
import com.example.allufinancas.feature.transactions.TransactionsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide actions and theme preferences; feature data belongs to feature ViewModels. */
class AlluViewModel(internal val dataSource: DemoDataSource) : ViewModel() {
    val preferences = dataSource.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayPreferences(),
    )

    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    fun updateBudget(categoryId: String, limitCents: Long) = viewModelScope.launch {
        runCatching { dataSource.updateBudget(categoryId, limitCents) }
            .onSuccess { _notice.value = "Limite salvo" }
            .onFailure { _notice.value = "Não foi possível salvar o limite" }
    }

    fun saveTransaction(transaction: com.example.allufinancas.data.Transaction) = viewModelScope.launch {
        runCatching { dataSource.saveTransaction(transaction) }
            .onSuccess { _notice.value = "Lançamento salvo neste dispositivo" }
            .onFailure { _notice.value = it.message ?: "Não foi possível salvar o lançamento" }
    }

    fun deleteTransaction(id: String) = viewModelScope.launch {
        runCatching { dataSource.deleteTransaction(id) }
            .onSuccess { _notice.value = "Lançamento excluído" }
            .onFailure { _notice.value = it.message ?: "Não foi possível excluir o lançamento" }
    }

    fun setCategory(transactionId: String, categoryId: String) = viewModelScope.launch {
        runCatching { dataSource.setTransactionCategory(transactionId, categoryId) }
            .onSuccess { _notice.value = "Categoria atualizada" }
            .onFailure { _notice.value = "Não foi possível alterar a categoria" }
    }

    fun decideReview(caseId: String) = viewModelScope.launch {
        runCatching { dataSource.decideReconciliationCase(caseId) }
            .onSuccess { _notice.value = "Lançamentos marcados como conferidos" }
            .onFailure { _notice.value = "Não foi possível salvar a decisão" }
    }

    fun reopenReview(caseId: String) = viewModelScope.launch {
        runCatching { dataSource.reopenReconciliationCase(caseId) }
            .onSuccess { _notice.value = "Item reaberto; dados de origem preservados" }
            .onFailure { _notice.value = "Não foi possível reabrir o item" }
    }

    fun setAlertDismissed(alertId: String, dismissed: Boolean) = viewModelScope.launch {
        runCatching { dataSource.setAlertDismissed(alertId, dismissed) }
            .onFailure { _notice.value = "Não foi possível atualizar o aviso" }
    }

    fun sendMessage(text: String) = viewModelScope.launch {
        runCatching { dataSource.sendMessage(text) }
            .onFailure { _notice.value = "Não foi possível responder agora" }
    }

    fun resetDemo() = viewModelScope.launch {
        runCatching { dataSource.resetDemoData() }
            .onSuccess { _notice.value = "Dados financeiros locais excluídos" }
            .onFailure { _notice.value = "Não foi possível excluir os dados" }
    }

    fun setDarkTheme(value: Boolean) = viewModelScope.launch { dataSource.setDarkTheme(value) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { dataSource.setReduceMotion(value) }
    fun setShowCents(value: Boolean) = viewModelScope.launch { dataSource.setShowCents(value) }
    fun clearNotice() { _notice.value = null }
}

class AlluViewModelFactory(private val dataSource: DemoDataSource) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val model = when {
            modelClass.isAssignableFrom(AlluViewModel::class.java) -> AlluViewModel(dataSource)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(dataSource)
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(dataSource)
            modelClass.isAssignableFrom(BudgetsViewModel::class.java) -> BudgetsViewModel(dataSource)
            modelClass.isAssignableFrom(AlertsViewModel::class.java) -> AlertsViewModel(dataSource)
            modelClass.isAssignableFrom(AssistantViewModel::class.java) -> AssistantViewModel(dataSource)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(dataSource)
            else -> error("ViewModel não registrado: ${modelClass.name}")
        }
        return model as T
    }
}
