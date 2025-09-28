package com.example.calis1

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthState
import com.example.calis1.viewmodel.SettingsViewModel
import com.example.calis1.viewmodel.TipoTema
import com.example.calis1.viewmodel.SistemaUnidades

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    // Estados del ViewModel
    val configuraciones by settingsViewModel.configuraciones.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()

    // Estados locales para diálogos
    var mostrarDialogoBorrado by remember { mutableStateOf(false) }
    var mostrarProgresoBorrado by remember { mutableStateOf(false) }

    // Configurar usuario actual basado en el estado de autenticación
    LaunchedEffect(authState) {
        val userId = when (authState) {
            is AuthState.SignedIn -> authState.user.uid
            is AuthState.TraditionalSignedIn -> "admin"
            is AuthState.EmailSignedIn -> authState.email
            else -> ""
        }
        settingsViewModel.configurarUsuario(userId)
    }

    // Auto-limpiar mensajes después de 3 segundos
    LaunchedEffect(uiState.lastAction, uiState.error) {
        if (uiState.lastAction != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            settingsViewModel.limpiarMensajes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuraciones",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mensaje de estado
            item {
                AnimatedVisibility(
                    visible = uiState.error != null || uiState.lastAction != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
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
            }

            // Sección: Apariencia
            item {
                SeccionConfiguracion(titulo = "Apariencia") {
                    // Cambio de tema
                    TarjetaConfiguracion(
                        titulo = "Tema de la aplicación",
                        descripcion = "Cambiar entre modo claro y oscuro",
                        icono = Icons.Default.Palette
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                onClick = { settingsViewModel.cambiarTema(TipoTema.CLARO) },
                                label = { Text("Claro") },
                                selected = configuraciones.tema == TipoTema.CLARO,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            FilterChip(
                                onClick = { settingsViewModel.cambiarTema(TipoTema.OSCURO) },
                                label = { Text("Oscuro") },
                                selected = configuraciones.tema == TipoTema.OSCURO,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            FilterChip(
                                onClick = { settingsViewModel.cambiarTema(TipoTema.SISTEMA) },
                                label = { Text("Sistema") },
                                selected = configuraciones.tema == TipoTema.SISTEMA,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBrightness,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Sección: Unidades
            item {
                SeccionConfiguracion(titulo = "Unidades de medida") {
                    TarjetaConfiguracion(
                        titulo = "Sistema de unidades",
                        descripcion = "Cambiar entre mililitros y onzas",
                        icono = Icons.Default.Straighten
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                onClick = { settingsViewModel.cambiarUnidades(SistemaUnidades.MILILITROS) },
                                label = { Text("Mililitros (ml)") },
                                selected = configuraciones.sistemaUnidades == SistemaUnidades.MILILITROS,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalDrink,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            FilterChip(
                                onClick = { settingsViewModel.cambiarUnidades(SistemaUnidades.ONZAS) },
                                label = { Text("Onzas (oz)") },
                                selected = configuraciones.sistemaUnidades == SistemaUnidades.ONZAS,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Información de conversión
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Información de conversión",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "• 1 onza (oz) = 29.57 mililitros (ml)\n• 100 ml = 3.38 oz aproximadamente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Sección: Datos
            item {
                SeccionConfiguracion(titulo = "Gestión de datos") {
                    // Botón de borrar todos los datos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Zona de peligro",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Acciones irreversibles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Text(
                                text = "Esta acción eliminará permanentemente todos tus datos incluyendo:" +
                                        "\n• Todos los registros de alcohol" +
                                        "\n• Todas las notas guardadas" +
                                        "\n• Todos los eventos creados" +
                                        "\n• Configuraciones de la aplicación",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Button(
                                onClick = { mostrarDialogoBorrado = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Eliminando...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Eliminar todos los datos")
                                }
                            }
                        }
                    }
                }
            }

            // Espacio adicional al final
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Diálogo de confirmación para borrar datos
    if (mostrarDialogoBorrado) {
        DialogoBorrarDatos(
            onConfirmar = {
                settingsViewModel.borrarTodosLosDatos()
                mostrarDialogoBorrado = false
            },
            onCancelar = { mostrarDialogoBorrado = false }
        )
    }
}

@Composable
fun SeccionConfiguracion(
    titulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        contenido()
    }
}

@Composable
fun TarjetaConfiguracion(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            contenido()
        }
    }
}

@Composable
fun DialogoBorrarDatos(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    var textoConfirmacion by remember { mutableStateOf("") }
    val textoRequerido = "ELIMINAR"
    val puedeConfirmar = textoConfirmacion.equals(textoRequerido, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "¿Eliminar todos los datos?",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    buildAnnotatedString {
                        append("Esta acción eliminará permanentemente:\n\n")
                        append("🗃️ Todos los registros de alcohol\n")
                        append("📝 Todas las notas guardadas\n")
                        append("📅 Todos los eventos creados\n")
                        append("⚙️ Configuraciones de la app\n\n")
                        append("Esta acción NO SE PUEDE DESHACER.")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Para confirmar, escribe: $textoRequerido",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = textoConfirmacion,
                        onValueChange = { textoConfirmacion = it },
                        placeholder = { Text("Escribe $textoRequerido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (puedeConfirmar)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = puedeConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar Todo")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    Calis1Theme {
        SettingsScreen(
            authState = AuthState.TraditionalSignedIn("admin"),
            onNavigateBack = {}
        )
    }
}