package com.example.calis1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.calis1.data.entity.Usuario
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthViewModel
import com.example.calis1.viewmodel.UsuarioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Calis1Theme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    if (isLoggedIn) {
        UsuarioApp(
            onLogout = {
                coroutineScope.launch {
                    // Primero hacer logout completo en el ViewModel
                    authViewModel.signOut(credentialManager)
                    // Luego cambiar el estado local
                    isLoggedIn = false
                }
            }
        )
    } else {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioApp(
    viewModel: UsuarioViewModel = viewModel(),
    onLogout: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var isManualSync by remember { mutableStateOf(false) }
    val usuarios: List<Usuario> by viewModel.allUsuarios.observeAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observar estado de WorkManager
    val workManager = WorkManager.getInstance(context)
    var workStatus by remember { mutableStateOf("Inactivo") }

    LaunchedEffect(Unit) {
        // Observar trabajos de sincronización
        workManager.getWorkInfosForUniqueWorkLiveData("periodic_sync").observeForever { workInfos ->
            workStatus = when {
                workInfos.any { it.state == WorkInfo.State.RUNNING } -> "Sincronizando..."
                workInfos.any { it.state == WorkInfo.State.ENQUEUED } -> "Programado"
                workInfos.any { it.state == WorkInfo.State.SUCCEEDED } -> "Completado"
                else -> "Inactivo"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Usuarios - Room + Firebase")
                        Text(
                            text = "WorkManager: $workStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Botón de sincronización con WorkManager
                    IconButton(
                        onClick = {
                            viewModel.forceSync()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sincronizar con WorkManager",
                            tint = if (workStatus == "Sincronizando...")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Botón de sincronización manual tradicional
                    IconButton(
                        onClick = {
                            isManualSync = true
                            coroutineScope.launch {
                                viewModel.manualSync()
                                isManualSync = false
                            }
                        },
                        enabled = !isManualSync
                    ) {
                        if (isManualSync) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronización manual"
                            )
                        }
                    }

                    // Botón de logout
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mensaje informativo sobre sincronización
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🔄 Sincronización Automática Activada",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "• Tiempo real mientras la app está abierta\n• Cada 15 min en segundo plano con WorkManager\n• Al recuperar conectividad automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Formulario de entrada
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Agregar Usuario",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edad,
                        onValueChange = { edad = it },
                        label = { Text("Edad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (nombre.isNotBlank() && edad.isNotBlank()) {
                                viewModel.insertUsuario(nombre, edad.toIntOrNull() ?: 0)
                                nombre = ""
                                edad = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Usuario")
                    }
                }
            }

            // Lista de usuarios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usuarios Guardados (${usuarios.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Indicador de estado de WorkManager
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (workStatus) {
                            "Sincronizando..." -> MaterialTheme.colorScheme.primary
                            "Completado" -> MaterialTheme.colorScheme.tertiary
                            "Programado" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = workStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (workStatus) {
                            "Sincronizando..." -> MaterialTheme.colorScheme.onPrimary
                            "Completado" -> MaterialTheme.colorScheme.onTertiary
                            "Programado" -> MaterialTheme.colorScheme.onSecondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = usuarios,
                    key = { usuario -> usuario.id }
                ) { usuario ->
                    UsuarioItem(
                        usuario = usuario,
                        onDelete = { viewModel.deleteUsuario(usuario) }
                    )
                }
            }
        }
    }
}

@Composable
fun UsuarioItem(
    usuario: Usuario,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${usuario.edad} años",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "ID: ${usuario.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar usuario",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    Calis1Theme {
        MainApp()
    }
}