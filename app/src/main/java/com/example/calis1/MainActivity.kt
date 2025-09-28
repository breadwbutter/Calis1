package com.example.calis1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calis1.data.entity.Nota
import com.example.calis1.navigation.NavigationRoutes
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AlcoholTrackingViewModel
import com.example.calis1.viewmodel.AuthState
import com.example.calis1.viewmodel.AuthViewModel
import com.example.calis1.viewmodel.EventosViewModel
import com.example.calis1.viewmodel.NotasViewModel

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
                            NavigationRoutes.NOTAS -> "Notas"
                            NavigationRoutes.EVENTOS -> "Eventos"
                            NavigationRoutes.EVENTOS_LIST -> "Lista de Eventos"
                            NavigationRoutes.BUSCADOR -> "Buscador de Eventos" // NUEVO: Título para buscador
                            NavigationRoutes.PROFILE -> "Mi Perfil"
                            NavigationRoutes.HISTORY -> "Historial Semanal"
                            else -> "BeerBattle"
                        }
                    )
                },
                actions = {
                    if (currentRoute != NavigationRoutes.PROFILE &&
                        currentRoute != NavigationRoutes.HISTORY &&
                        currentRoute != NavigationRoutes.EVENTOS_LIST &&
                        currentRoute != NavigationRoutes.BUSCADOR) { // NUEVO: Excluir buscador

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

                        // Botón para cambiar entre pantallas principales
                        IconButton(
                            onClick = {
                                val nextRoute = when (currentRoute) {
                                    NavigationRoutes.ALCOHOL_TRACKING -> NavigationRoutes.NOTAS
                                    NavigationRoutes.NOTAS -> NavigationRoutes.EVENTOS
                                    NavigationRoutes.EVENTOS -> NavigationRoutes.ALCOHOL_TRACKING
                                    else -> NavigationRoutes.ALCOHOL_TRACKING
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
                },
                // Botón de navegación hacia atrás para pantallas secundarias
                navigationIcon = {
                    if (currentRoute == NavigationRoutes.EVENTOS_LIST ||
                        currentRoute == NavigationRoutes.BUSCADOR) { // NUEVO: Incluir buscador
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute != NavigationRoutes.PROFILE &&
                currentRoute != NavigationRoutes.HISTORY &&
                currentRoute != NavigationRoutes.EVENTOS_LIST &&
                currentRoute != NavigationRoutes.BUSCADOR) { // NUEVO: Excluir buscador
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
                    navController = navController
                )
            }

            composable(NavigationRoutes.NOTAS) {
                NotasAppWrapper(paddingValues = PaddingValues(0.dp))
            }

            composable(NavigationRoutes.EVENTOS) {
                EventosScreenWrapper(
                    authState = authState,
                    paddingValues = PaddingValues(0.dp),
                    navController = navController
                )
            }

            composable(NavigationRoutes.EVENTOS_LIST) {
                EventosListScreenWrapper(
                    authState = authState,
                    paddingValues = PaddingValues(0.dp)
                )
            }

            // NUEVO: Composable para la pantalla de buscador
            composable(NavigationRoutes.BUSCADOR) {
                BuscadorScreenWrapper(
                    authState = authState,
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
                    imageVector = Icons.Default.Notes,
                    contentDescription = "Notas"
                )
            },
            label = { Text("Notas") },
            selected = currentRoute == NavigationRoutes.NOTAS,
            onClick = { onNavigate(NavigationRoutes.NOTAS) }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Eventos"
                )
            },
            label = { Text("Eventos") },
            selected = currentRoute == NavigationRoutes.EVENTOS,
            onClick = { onNavigate(NavigationRoutes.EVENTOS) }
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
        }
    }
}

@Composable
fun AlcoholTrackingScreenWrapper(
    authState: AuthState,
    paddingValues: PaddingValues,
    navController: NavHostController
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
                navController.navigate(NavigationRoutes.HISTORY)
            }
        )
    }
}

@Composable
fun EventosScreenWrapper(
    authState: AuthState,
    paddingValues: PaddingValues,
    navController: NavHostController
) {
    val eventosViewModel: EventosViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        EventosScreen(
            viewModel = eventosViewModel,
            authState = authState,
            onVerEventosClick = {
                navController.navigate(NavigationRoutes.EVENTOS_LIST)
            },
            // NUEVO: Agregar callback para navegar al buscador
            onBuscarEventosClick = {
                navController.navigate(NavigationRoutes.BUSCADOR)
            }
        )
    }
}

@Composable
fun EventosListScreenWrapper(
    authState: AuthState,
    paddingValues: PaddingValues
) {
    val eventosViewModel: EventosViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        EventosListScreen(
            viewModel = eventosViewModel,
            authState = authState
        )
    }
}

// NUEVO: Wrapper para BuscadorScreen
@Composable
fun BuscadorScreenWrapper(
    authState: AuthState,
    paddingValues: PaddingValues
) {
    val eventosViewModel: EventosViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        BuscadorScreen(
            viewModel = eventosViewModel,
            authState = authState
        )
    }
}

@Composable
fun NotasAppWrapper(paddingValues: PaddingValues) {
    val viewModel: NotasViewModel = viewModel()
    val notas by viewModel.allNotas.collectAsState()

    // Estados para el formulario
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var notaSeleccionadaId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card para agregar o editar notas
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (notaSeleccionadaId == null) "Agregar Nota" else "Editar Nota",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Titulo de Nota") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Nota") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addOrUpdateNota(notaSeleccionadaId, titulo, contenido)
                            // Limpiar formulario
                            titulo = ""
                            contenido = ""
                            notaSeleccionadaId = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (notaSeleccionadaId == null) "Guardar Nota" else "Actualizar Nota")
                    }

                    if (notaSeleccionadaId != null) {
                        TextButton(onClick = {
                            titulo = ""
                            contenido = ""
                            notaSeleccionadaId = null
                        }) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }

        Text(
            text = "Notas (${notas.size})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Lista de notas
        if (notas.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = notas, key = { nota -> nota.id }) { nota ->
                    NotaItem(
                        nota = nota,
                        onEdit = {
                            titulo = nota.title
                            contenido = nota.content
                            notaSeleccionadaId = nota.id
                        },
                        onDelete = { viewModel.deleteNota(nota) }
                    )
                }
            }
        } else {
            // Placeholder si no hay notas
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
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(
                            text = "No hay notas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Agrega la primera nota",
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
fun NotaItem(
    nota: Nota,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nota.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = nota.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Nota",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar Nota",
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