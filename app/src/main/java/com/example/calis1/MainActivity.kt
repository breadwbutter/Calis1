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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.calis1.data.entity.Usuario
import com.example.calis1.navigation.NavigationRoutes
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthState
import com.example.calis1.viewmodel.AuthViewModel
import com.example.calis1.viewmodel.UsuarioViewModel
import com.example.calis1.viewmodel.AlcoholTrackingViewModel

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
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val navController = rememberNavController()

    // Estado para controlar splash screen
    var showSplash by remember { mutableStateOf(true) }

    // Estados existentes
    val authState by authViewModel.authState.collectAsState()

    // Mostrar splash screen primero
    if (showSplash) {
        SplashScreen(
            onSplashFinished = {
                showSplash = false
                authViewModel.initialize(context)
            }
        )
        return
    }

    // Determinar si mostrar la pantalla principal o login
    when (authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.SignedIn,
        is AuthState.TraditionalSignedIn,
        is AuthState.EmailSignedIn -> {
            MainAppWithNavigation(
                navController = navController,
                authState = authState,
                onLogout = {
                    authViewModel.signOut(context)
                }
            )
        }
        is AuthState.SignedOut,
        is AuthState.Error,
        is AuthState.RegistrationSuccess -> {
            LoginScreen(
                authViewModel = authViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppWithNavigation(
    navController: NavHostController,
    authState: AuthState,
    onLogout: () -> Unit
) {
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            NavigationRoutes.ALCOHOL_TRACKING -> "Control Semanal"
                            NavigationRoutes.USUARIOS -> "Gestión Usuarios"
                            NavigationRoutes.PROFILE -> "Mi Perfil"
                            NavigationRoutes.HISTORY -> "Historial Semanal" // Título para historial
                            else -> "BeerBattle"
                        }
                    )
                },
                actions = {
                    if (currentRoute != NavigationRoutes.PROFILE && currentRoute != NavigationRoutes.HISTORY) {
                        // Botón de perfil
                        IconButton(
                            onClick = {
                                navController.navigate(NavigationRoutes.PROFILE)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Mi perfil"
                            )
                        }

                        // Botón para cambiar entre alcohol y usuarios
                        IconButton(
                            onClick = {
                                val nextRoute = if (currentRoute == NavigationRoutes.ALCOHOL_TRACKING) {
                                    NavigationRoutes.USUARIOS
                                } else {
                                    NavigationRoutes.ALCOHOL_TRACKING
                                }
                                navController.navigate(nextRoute) {
                                    popUpTo(NavigationRoutes.ALCOHOL_TRACKING) { inclusive = false }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Cambiar pantalla"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute != NavigationRoutes.PROFILE && currentRoute != NavigationRoutes.HISTORY) {
                NavigationBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavigationRoutes.ALCOHOL_TRACKING) { inclusive = false }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.ALCOHOL_TRACKING,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavigationRoutes.ALCOHOL_TRACKING) {
                AlcoholTrackingScreenWrapper(
                    authState = authState,
                    paddingValues = PaddingValues(0.dp),
                    navController = navController // Pasar NavController
                )
            }

            composable(NavigationRoutes.USUARIOS) {
                UsuarioAppWrapper(
                    authState = authState,
                    onLogout = onLogout,
                    paddingValues = PaddingValues(0.dp)
                )
            }

            composable(NavigationRoutes.PROFILE) {
                UserProfileScreen(
                    authState = authState,
                    onLogout = {
                        onLogout()
                        navController.navigate(NavigationRoutes.ALCOHOL_TRACKING) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Nueva ruta para el historial
            composable(NavigationRoutes.HISTORY) {
                val userId = when (authState) {
                    is AuthState.SignedIn -> authState.user.uid
                    is AuthState.TraditionalSignedIn -> "admin"
                    is AuthState.EmailSignedIn -> authState.email
                    else -> ""
                }
                HistoryScreen(userId = userId)
            }
        }
    }
}

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.LocalBar,
                    contentDescription = "Control semanal"
                )
            },
            label = { Text("Control") },
            selected = currentRoute == NavigationRoutes.ALCOHOL_TRACKING,
            onClick = { onNavigate(NavigationRoutes.ALCOHOL_TRACKING) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Usuarios"
                )
            },
            label = { Text("Usuarios") },
            selected = currentRoute == NavigationRoutes.USUARIOS,
            onClick = { onNavigate(NavigationRoutes.USUARIOS) }
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Verificando sesión...",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Si se demora mucho, verifica tu conexión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AlcoholTrackingScreenWrapper(
    authState: AuthState,
    paddingValues: PaddingValues,
    navController: NavHostController // Recibir NavController
) {
    val alcoholViewModel: AlcoholTrackingViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        AlcoholTrackingScreen(
            viewModel = alcoholViewModel,
            authState = authState,
            onLogout = { /* Ya manejado en el nivel superior */ },
            onHistorialClick = {
                // Implementar navegación
                navController.navigate(NavigationRoutes.HISTORY)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioAppWrapper(
    authState: AuthState,
    onLogout: () -> Unit,
    paddingValues: PaddingValues
) {
    val viewModel: UsuarioViewModel = viewModel()
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }

    val usuarios by viewModel.allUsuarios.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var workStatus by remember { mutableStateOf("Inactivo") }

    LaunchedEffect(Unit) {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("periodic_sync").observeForever { workInfos ->
            workStatus = when {
                workInfos.any { it.state == WorkInfo.State.RUNNING } -> "Sincronizando..."
                workInfos.any { it.state == WorkInfo.State.ENQUEUED } -> "Programado"
                workInfos.any { it.state == WorkInfo.State.SUCCEEDED } -> "Completado"
                else -> "Inactivo"
            }
        }
    }

    LaunchedEffect(uiState.lastAction, uiState.error) {
        if (uiState.lastAction != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    onValueChange = {
                        nombre = it
                        viewModel.clearMessages()
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.error?.contains("nombre", ignoreCase = true) == true
                )

                OutlinedTextField(
                    value = edad,
                    onValueChange = {
                        edad = it
                        viewModel.clearMessages()
                    },
                    label = { Text("Edad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.error?.contains("edad", ignoreCase = true) == true
                )

                Button(
                    onClick = {
                        if (nombre.isNotBlank() && edad.isNotBlank()) {
                            viewModel.insertUsuario(nombre, edad.toIntOrNull() ?: 0)
                            nombre = ""
                            edad = ""
                        } else {
                            viewModel.insertUsuario(nombre, edad.toIntOrNull() ?: 0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Guardar Usuario")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Usuarios (${usuarios.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isLoading) {
                    Text(
                        text = "Cargando...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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

                IconButton(
                    onClick = {
                        viewModel.manualSync()
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
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

                Text(
                    text = workStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (workStatus) {
                        "Sincronizando..." -> MaterialTheme.colorScheme.primary
                        "Completado" -> MaterialTheme.colorScheme.tertiary
                        "Programado" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }
        }

        if (uiState.hasUsers) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = usuarios,
                    key = { usuario -> usuario.id }
                ) { usuario ->
                    UsuarioItem(
                        usuario = usuario,
                        onDelete = { viewModel.deleteUsuario(usuario) },
                        isLoading = uiState.isLoading
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "No hay usuarios",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Agrega el primer usuario",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsuarioItem(
    usuario: Usuario,
    onDelete: () -> Unit,
    isLoading: Boolean = false
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

            IconButton(
                onClick = onDelete,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar usuario",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
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