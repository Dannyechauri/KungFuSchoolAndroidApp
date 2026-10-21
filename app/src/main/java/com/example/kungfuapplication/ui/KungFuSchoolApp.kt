package com.example.kungfuapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.classes.ClassDetailScreen
import com.example.kungfuapplication.ui.classes.ClassesScreen
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.home.HomeScreen
import com.example.kungfuapplication.ui.login.LoginScreen
import com.example.kungfuapplication.ui.profile.ProfileScreen
import com.example.kungfuapplication.ui.progress.ProgressScreen

private enum class TopLevel(val route: String, val icon: ImageVector) {
    Home("home", Icons.Filled.Home),
    Classes("classes", Icons.Filled.CalendarMonth),
    Progress("progress", Icons.Filled.SportsMartialArts),
    Profile("profile", Icons.Filled.Person),
}

private const val CLASS_DETAIL_ROUTE = "class/{classId}"

private fun TopLevel.label(data: SchoolData?): String = when (this) {
    TopLevel.Home -> "Inicio"
    TopLevel.Classes -> "Clases"
    TopLevel.Progress -> if (data?.profile?.student != null) "Progreso" else "Temario"
    TopLevel.Profile -> "Perfil"
}

@Composable
fun KungFuSchoolApp(viewModel: AppViewModel = viewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (session) {
        SessionState.Checking -> LoadingState(message = "Comprobando sesión…")
        SessionState.LoggedOut -> {
            val loginState by viewModel.login.collectAsStateWithLifecycle()
            LoginScreen(state = loginState, onLogin = viewModel::login)
        }
        is SessionState.LoggedIn -> MainScaffold(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(viewModel: AppViewModel) {
    val state by viewModel.data.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevel.entries.firstOrNull { it.route == currentRoute }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(currentTab?.label(state.data) ?: "Detalle de la clase") },
                navigationIcon = {
                    if (currentTab == null && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.isRefreshing && !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevel.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label(state.data)) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val data = state.data
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                data == null && state.error != null -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                data == null -> LoadingState(message = "Cargando datos de la escuela…")
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavHost(navController, data, state.pendingClassIds, viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    data: SchoolData,
    pendingClassIds: Set<Long>,
    viewModel: AppViewModel,
) {
    val openClass: (Long) -> Unit = { navController.navigate("class/$it") }
    NavHost(navController = navController, startDestination = TopLevel.Home.route) {
        composable(TopLevel.Home.route) {
            HomeScreen(
                data = data,
                onOpenClass = openClass,
                onOpenCalendar = { navController.navigateToTab(TopLevel.Classes) },
                onOpenProgress = { navController.navigateToTab(TopLevel.Progress) },
            )
        }
        composable(TopLevel.Classes.route) { ClassesScreen(data, onOpenClass = openClass) }
        composable(TopLevel.Progress.route) { ProgressScreen(data) }
        composable(TopLevel.Profile.route) {
            ProfileScreen(data, baseUrl = viewModel.baseUrl, onLogout = viewModel::logout)
        }
        composable(
            CLASS_DETAIL_ROUTE,
            arguments = listOf(navArgument("classId") { type = NavType.LongType }),
        ) { entry ->
            ClassDetailScreen(
                data = data,
                classId = entry.arguments?.getLong("classId") ?: -1L,
                pendingClassIds = pendingClassIds,
                onConfirm = viewModel::confirmAttendance,
                onCancel = viewModel::cancelAttendance,
            )
        }
    }
}

private fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
