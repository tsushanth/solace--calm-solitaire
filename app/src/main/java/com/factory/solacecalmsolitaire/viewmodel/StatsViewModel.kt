package com.factory.solacecalmsolitaire.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.solacecalmsolitaire.SolaceApplication
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.data.repository.StatsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val statsRepository: StatsRepository =
        (application as SolaceApplication).statsRepository

    val summary: StateFlow<StatsSummary> = statsRepository.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsSummary())

    fun clearHistory() {
        viewModelScope.launch { statsRepository.clearHistory() }
    }
}
