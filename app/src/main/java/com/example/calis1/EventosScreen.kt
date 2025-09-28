package com.example.calis1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthState
import com.example.calis1.viewmodel.EventosViewModel

@Composable
fun EventosScreen(
    viewModel: EventosViewModel = viewModel(),
    authState: AuthState,
    onVerEventosClick: () -> Unit,
    onBuscarEventosClick: () -> Unit // NUEVO: Callback para navegar al buscador
) {
    // States del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val eventosCount by viewModel.eventosCount.collectAsState()

    val context = LocalContext.current

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    var tituloError by remember { mutableStateOf("") }
    var descripcionError by remember { mutableStateOf("") }
    var fechaError by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        val userId = when (authState) {
            is AuthState.SignedIn -> authState.user.uid
            is AuthState.TraditionalSignedIn -> "admin"
            is AuthState.EmailSignedIn -> authState.email
            else -> ""
        }
        viewModel.setCurrentUser(userId)
    }

    LaunchedEffect(uiState.lastAction, uiState.error) {
        if (uiState.lastAction != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(titulo) {
        tituloError = when {
            titulo.isNotEmpty() && titulo.trim().length < 3 -> "El título debe tener al menos 3 caracteres"
            titulo.trim().length > 100 -> "El título no puede exceder 100 caracteres"
            else -> ""
        }
    }

    LaunchedEffect(descripcion) {
        descripcionError = when {
            descripcion.isNotEmpty() && descripcion.trim().length < 5 -> "La descripción debe tener al menos 5 caracteres"
            descripcion.trim().length > 500 -> "La descripción no puede exceder 500 caracteres"
            else -> ""
        }
    }

    LaunchedEffect(fecha) {
        fechaError = when {
            fecha.isNotEmpty() && fecha.trim().length < 5 -> "La fecha debe ser válida"
            else -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // NUEVO: Header con título y botón de búsqueda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Eventos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Botón de búsqueda
            FloatingActionButton(
                onClick = onBuscarEventosClick,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar eventos",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.error != null) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (uiState.error != null)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = uiState.error ?: uiState.lastAction ?: "",
                        color = if (uiState.error != null)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Card del formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Agregar Nuevo Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Campo de título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        viewModel.clearMessages()
                    },
                    label = { Text("Título del evento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Título"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    isError = tituloError.isNotEmpty(),
                    supportingText = if (tituloError.isNotEmpty()) {
                        { Text(tituloError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Campo de descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        viewModel.clearMessages()
                    },
                    label = { Text("Descripción del evento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Descripción"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    enabled = !uiState.isLoading,
                    maxLines = 4,
                    isError = descripcionError.isNotEmpty(),
                    supportingText = if (descripcionError.isNotEmpty()) {
                        { Text(descripcionError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Campo de fecha
                OutlinedTextField(
                    value = fecha,
                    onValueChange = {
                        fecha = it
                        viewModel.clearMessages()
                    },
                    label = { Text("Fecha del evento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Fecha"
                        )
                    },
                    placeholder = { Text("Ej: 25 de Diciembre, 2024") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    isError = fechaError.isNotEmpty(),
                    supportingText = if (fechaError.isNotEmpty()) {
                        { Text(fechaError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para agregar evento
                Button(
                    onClick = {
                        when {
                            titulo.isBlank() -> {
                                tituloError = "El título es obligatorio"
                            }
                            descripcion.isBlank() -> {
                                descripcionError = "La descripción es obligatoria"
                            }
                            fecha.isBlank() -> {
                                fechaError = "La fecha es obligatoria"
                            }
                            tituloError.isNotEmpty() || descripcionError.isNotEmpty() || fechaError.isNotEmpty() -> {
                                // No hacer nada, los errores ya se muestran
                            }
                            else -> {
                                viewModel.insertEvento(titulo, descripcion, fecha)
                                // Limpiar formulario después de enviar
                                titulo = ""
                                descripcion = ""
                                fecha = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agregar Evento",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Card de información y botones de acción
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Eventos Registrados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "$eventosCount evento${if (eventosCount != 1) "s" else ""} guardado${if (eventosCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )

                // NUEVO: Fila de botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón de búsqueda (versión alternativa)
                    OutlinedButton(
                        onClick = onBuscarEventosClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buscar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Botón para ver todos los eventos
                    Button(
                        onClick = onVerEventosClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver Todo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Espacio adicional
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun EventosScreenPreview() {
    Calis1Theme {
        EventosScreen(
            authState = AuthState.TraditionalSignedIn("admin"),
            onVerEventosClick = {},
            onBuscarEventosClick = {}
        )
    }
}