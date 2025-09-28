package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.repository.AlcoholTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlcoholTrackingRepository
    private val userId = MutableStateFlow("")

    init {
        val alcoholRecordDao = AppDatabase.getDatabase(application).alcoholRecordDao()
        repository = AlcoholTrackingRepository(alcoholRecordDao, application.applicationContext)
    }

    val allRecords: Flow<List<AlcoholRecord>> = userId.flatMapLatest { id ->
        if (id.isNotEmpty()) {
            repository.getAllAlcoholRecords(id)
        } else {
            flowOf(emptyList())
        }
    }

    fun setUser(userId: String) {
        this.userId.value = userId
    }
}