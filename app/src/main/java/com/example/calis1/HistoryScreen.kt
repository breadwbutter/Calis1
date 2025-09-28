package com.example.calis1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.viewmodel.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = viewModel(),
    userId: String
) {
    historyViewModel.setUser(userId)
    val records by historyViewModel.allRecords.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(records) { record ->
            HistoryCard(record = record)
        }
    }
}

@Composable
fun HistoryCard(record: AlcoholRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val date = LocalDate.parse(record.fecha)
            val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.getNombreDia(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = date.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "${record.nombreBebida} - ${record.mililitros}ml (${record.porcentajeAlcohol}%)",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Alcohol puro: ${"%.2f".format(record.calcularAlcoholPuro())} ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}