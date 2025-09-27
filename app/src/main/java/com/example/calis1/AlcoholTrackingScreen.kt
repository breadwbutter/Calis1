package com.example.calis1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AlcoholTrackingViewModel
import com.example.calis1.viewmodel.AuthState
import com.example.calis1.viewmodel.EstadoSalud
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AlcoholTrackingScreen(
    viewModel: AlcoholTrackingViewModel = viewModel(),
    authState: AuthState,
    onLogout: () -> Unit,
    onHistorialClick: () -> Unit = {} // Función para historial (por ahora sin implementar)
) {
    // States del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val semanaActual by viewModel.semanaActual.collectAsState()
    val registrosSemana by viewModel.registrosSemana.collectAsState()
    val totalAlcoholPuro by viewModel.totalAlcoholPuro.collectAsState()

    val context = LocalContext.current

    // Configurar usuario actual basado en el estado de autenticación
    LaunchedEffect(authState) {
        val userId = when (authState) {
            is AuthState.SignedIn -> authState.user.uid
            is AuthState.TraditionalSignedIn -> "admin"
            is AuthState.EmailSignedIn -> authState.email
            else -> ""
        }
        viewModel.setCurrentUser(userId)
    }

    // Observar estado de WorkManager
    val workManager = WorkManager.getInstance(context)
    var workStatus by remember { mutableStateOf("Inactivo") }

    LaunchedEffect(Unit) {
        workManager.getWorkInfosForUniqueWorkLiveData("periodic_alcohol_sync").observeForever { workInfos ->
            workStatus = when {
                workInfos.any { it.state == WorkInfo.State.RUNNING } -> "Sincronizando..."
                workInfos.any { it.state == WorkInfo.State.ENQUEUED } -> "Programado"
                workInfos.any { it.state == WorkInfo.State.SUCCEEDED } -> "Completado"
                else -> "Inactivo"
            }
        }
    }

    // Auto-limpiar mensajes después de 3 segundos
    LaunchedEffect(uiState.lastAction, uiState.error) {
        if (uiState.lastAction != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    // Column principal SIN Scaffold (se usa el del MainActivity)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Snackbar para mensajes de estado
        if (uiState.error != null || uiState.lastAction != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.error != null)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = uiState.error ?: uiState.lastAction ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = if (uiState.error != null)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header con navegación de semana
            item {
                SemanaNavigationCard(
                    rangoSemana = viewModel.getRangoSemana(),
                    esSemanaActual = viewModel.esSemanaActual(),
                    onSemanaAnterior = { viewModel.irSemanaAnterior() },
                    onSemanaSiguiente = { viewModel.irSemanaSiguiente() },
                    onSemanaActual = { viewModel.irSemanaActual() }
                )
            }

            // Resumen de alcohol y estado de salud
            item {
                ResumenAlcoholCard(
                    totalAlcoholPuro = totalAlcoholPuro,
                    estadoSalud = uiState.estadoSalud
                )
            }

            // Días de la semana (Domingo a Sábado)
            items(7) { index ->
                val diaSemana = index + 1 // 1=Domingo, 2=Lunes, etc.
                val registrosDia = uiState.registrosPorDia[diaSemana] ?: emptyList() // CAMBIO: Lista de registros
                val fechaDia = semanaActual.plusDays(index.toLong())

                DiaAlcoholCard(
                    diaSemana = diaSemana,
                    fecha = fechaDia,
                    registros = registrosDia, // CAMBIO: Pasar lista de registros
                    isLoading = uiState.isLoading,
                    onActualizar = { nombre, ml, porcentaje ->
                        viewModel.actualizarRegistro(diaSemana, nombre, ml, porcentaje)
                    },
                    onEliminar = { registroId -> // CAMBIO: Eliminar por ID
                        viewModel.eliminarRegistroEspecifico(registroId)
                    },
                    onEditar = { registroId, nombre, ml, porcentaje -> // NUEVO: Editar registro
                        viewModel.editarRegistro(registroId, nombre, ml, porcentaje)
                    }
                )
            }

            // Botón de historial
            item {
                Button(
                    onClick = onHistorialClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historial"
                        )
                        Text(
                            text = "Historial Semanal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Espacio adicional al final
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SemanaNavigationCard(
    rangoSemana: String,
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onSemanaActual: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Semana del $rangoSemana",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSemanaAnterior
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Semana anterior",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (!esSemanaActual) {
                    TextButton(
                        onClick = onSemanaActual
                    ) {
                        Text(
                            text = "Ir a semana actual",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Text(
                        text = "Semana actual",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onSemanaSiguiente,
                    enabled = !esSemanaActual // CAMBIO: Deshabilitar si ya estamos en semana actual
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Semana siguiente",
                        tint = if (esSemanaActual)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ResumenAlcoholCard(
    totalAlcoholPuro: Double,
    estadoSalud: EstadoSalud
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (estadoSalud) {
                EstadoSalud.SALUDABLE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                EstadoSalud.EN_RIESGO -> Color(0xFFFF9800).copy(alpha = 0.1f)
                EstadoSalud.EXCESO -> Color(0xFFD32F2F).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icono y estado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (estadoSalud) {
                        EstadoSalud.SALUDABLE -> Icons.Default.CheckCircle
                        EstadoSalud.EN_RIESGO -> Icons.Default.Warning
                        EstadoSalud.EXCESO -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (estadoSalud) {
                        EstadoSalud.SALUDABLE -> Color(0xFF4CAF50)
                        EstadoSalud.EN_RIESGO -> Color(0xFFFF9800)
                        EstadoSalud.EXCESO -> Color(0xFFD32F2F)
                    }
                )
                Text(
                    text = estadoSalud.mensaje,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (estadoSalud) {
                        EstadoSalud.SALUDABLE -> Color(0xFF4CAF50)
                        EstadoSalud.EN_RIESGO -> Color(0xFFFF9800)
                        EstadoSalud.EXCESO -> Color(0xFFD32F2F)
                    }
                )
            }

            Text(
                text = estadoSalud.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Total de alcohol puro
            Text(
                text = "Total de alcohol puro: ${String.format("%.1f", totalAlcoholPuro)} ml",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            // Barra de progreso visual
            LinearProgressIndicator(
                progress = (totalAlcoholPuro / 280.0).toFloat().coerceAtMost(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (estadoSalud) {
                    EstadoSalud.SALUDABLE -> Color(0xFF4CAF50)
                    EstadoSalud.EN_RIESGO -> Color(0xFFFF9800)
                    EstadoSalud.EXCESO -> Color(0xFFD32F2F)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "Límite recomendado: 140 ml/semana",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DiaAlcoholCard(
    diaSemana: Int,
    fecha: LocalDate,
    registros: List<AlcoholRecord>, // CAMBIO: Lista de registros en lugar de uno solo
    isLoading: Boolean,
    onActualizar: (String, String, String) -> Unit,
    onEliminar: (String) -> Unit, // CAMBIO: Eliminar por ID específico
    onEditar: (String, String, String, String) -> Unit // NUEVO: Editar registro específico
) {
    var expanded by remember { mutableStateOf(false) }
    var editingRegistroId by remember { mutableStateOf<String?>(null) }

    // Estados para formulario (siempre limpios para nuevo registro)
    var nombreBebida by remember { mutableStateOf("") }
    var mililitros by remember { mutableStateOf("") }
    var porcentajeAlcohol by remember { mutableStateOf("") }

    // Estados para edición
    var editNombreBebida by remember { mutableStateOf("") }
    var editMililitros by remember { mutableStateOf("") }
    var editPorcentajeAlcohol by remember { mutableStateOf("") }

    val nombreDia = when (diaSemana) {
        1 -> "Domingo"
        2 -> "Lunes"
        3 -> "Martes"
        4 -> "Miércoles"
        5 -> "Jueves"
        6 -> "Viernes"
        7 -> "Sábado"
        else -> "Día"
    }

    // Calcular total de alcohol del día
    val totalAlcoholDia = registros.sumOf { it.calcularAlcoholPuro() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (registros.isNotEmpty())
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = nombreDia,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = AlcoholRecord.formatearFecha(fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (registros.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${registros.size} registro${if (registros.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${String.format("%.1f", totalAlcoholDia)} ml alcohol",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { expanded = !expanded }
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Contraer" else "Expandir"
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { expanded = true }
                        ) {
                            Text("Agregar")
                        }
                    }
                }
            }

            // Lista de registros existentes (cuando está colapsado)
            if (registros.isNotEmpty() && !expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                registros.forEach { registro ->
                    Text(
                        text = "${registro.nombreBebida} • ${registro.mililitros}ml • ${registro.porcentajeAlcohol}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Contenido expandido
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar registros existentes con opciones de editar/eliminar
                if (registros.isNotEmpty()) {
                    Text(
                        text = "Registros del día:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    registros.forEach { registro ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            if (editingRegistroId == registro.id) {
                                // Modo edición
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editNombreBebida,
                                        onValueChange = { editNombreBebida = it },
                                        label = { Text("Nombre") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editMililitros,
                                            onValueChange = { editMililitros = it },
                                            label = { Text("ml") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = editPorcentajeAlcohol,
                                            onValueChange = { editPorcentajeAlcohol = it },
                                            label = { Text("% Alcohol") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onEditar(registro.id, editNombreBebida, editMililitros, editPorcentajeAlcohol)
                                                editingRegistroId = null
                                            },
                                            enabled = !isLoading
                                        ) {
                                            Text("Guardar")
                                        }

                                        TextButton(
                                            onClick = { editingRegistroId = null }
                                        ) {
                                            Text("Cancelar")
                                        }
                                    }
                                }
                            } else {
                                // Modo visualización
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = registro.nombreBebida,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${registro.mililitros}ml • ${registro.porcentajeAlcohol}% • ${String.format("%.1f", registro.calcularAlcoholPuro())}ml alcohol",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingRegistroId = registro.id
                                                editNombreBebida = registro.nombreBebida
                                                editMililitros = registro.mililitros.toString()
                                                editPorcentajeAlcohol = registro.porcentajeAlcohol.toString()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { onEliminar(registro.id) },
                                            enabled = !isLoading
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Agregar nuevo registro:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Formulario para NUEVO registro (siempre limpio)
                OutlinedTextField(
                    value = nombreBebida,
                    onValueChange = { nombreBebida = it },
                    label = { Text("Nombre de la bebida") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = mililitros,
                        onValueChange = { mililitros = it },
                        label = { Text("Mililitros") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = porcentajeAlcohol,
                        onValueChange = { porcentajeAlcohol = it },
                        label = { Text("% Alcohol") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onActualizar(nombreBebida, mililitros, porcentajeAlcohol)
                            // Limpiar formulario después de guardar
                            nombreBebida = ""
                            mililitros = ""
                            porcentajeAlcohol = ""
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Agregar")
                        }
                    }

                    TextButton(
                        onClick = {
                            expanded = false
                            editingRegistroId = null
                            // Limpiar formulario al cancelar
                            nombreBebida = ""
                            mililitros = ""
                            porcentajeAlcohol = ""
                        }
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlcoholTrackingScreenPreview() {
    Calis1Theme {
        AlcoholTrackingScreen(
            authState = AuthState.TraditionalSignedIn("admin"),
            onLogout = {}
        )
    }
}