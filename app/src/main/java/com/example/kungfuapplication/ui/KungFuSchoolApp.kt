package com.example.kungfuapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.ui.account.AccountScreen
import com.example.kungfuapplication.ui.calendar.CalendarScreen
import com.example.kungfuapplication.ui.calendar.EventDetailScreen
import com.example.kungfuapplication.ui.calendar.ManageScreen
import com.example.kungfuapplication.ui.calendar.ScheduleFormScreen
import com.example.kungfuapplication.ui.calendar.ScheduleTarget
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.home.HomeScreen
import com.example.kungfuapplication.ui.login.LoginScreen
import com.example.kungfuapplication.ui.profile.MyProfileScreen
import com.example.kungfuapplication.ui.performance.PerformanceScreen
import com.example.kungfuapplication.ui.theme.KungFuGoldContainer
import com.example.kungfuapplication.ui.theme.KungFuOnGoldContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate

private enum class Tab(val route: String, val label: String, val title: String, val icon: ImageVector) {
    Home("home", "Inicio", "Escuela de Kung Fu", Icons.Filled.Home),
    Profile("profile", "Mi ficha", "Mi ficha", Icons.Filled.Badge),
    Calendar("calendar", "Calendario", "Calendario", Icons.Filled.CalendarMonth),
    Performance("performance", "Mi desempeño", "Mi desempeño", Icons.Filled.EmojiEvents),
    Manage("manage", "Agenda", "Gestionar agenda", Icons.Filled.EditCalendar),
    Account("account", "Cuenta", "Mi cuenta", Icons.Filled.AccountCircle),
}

/** Mismo orden que el menú de la web: Mi ficha va encima de Calendario. */
private fun tabsFor(user: SessionUser): List<Tab> = buildList {
    add(Tab.Home)
    if (user.hasProfile) add(Tab.Profile)
    add(Tab.Calendar)
    add(Tab.Performance)
    if (user.canManageCalendar && !user.hasProfile) add(Tab.Manage)
    add(Tab.Account)
}

private const val EVENT_ROUTE = "event/{eventId}"
private const val SCHEDULE_ROUTE = "schedule/{kind}?id={id}&date={date}"

private fun NavHostController.openEvent(id: Long) = navigate("event/$id")

private fun NavHostController.openSchedule(kind: String, id: Long? = null, date: LocalDate? = null) {
    navigate("schedule/$kind?id=${id ?: -1}&date=${date?.toString().orEmpty()}")
}

@Composable
fun KungFuSchoolApp(viewModel: AppViewModel = viewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (val current = session) {
        SessionState.Checking -> LoadingState(message = "Comprobando sesión…")
        SessionState.LoggedOut -> {
            val loginState by viewModel.login.collectAsStateWithLifecycle()
            LoginScreen(state = loginState, onLogin = viewModel::login)
        }
        is SessionState.LoggedIn -> MainScaffold(viewModel, current.user)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(viewModel: AppViewModel, user: SessionUser) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabs = remember(user) { tabsFor(user) }
    val currentTab = tabs.firstOrNull { it.route == currentRoute }
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val performance by viewModel.performance.collectAsStateWithLifecycle()
    val pendingCount = if (user.canParticipate) performance.inbox.size else 0
    val cancellationCount = if (user.canParticipate) calendar.cancellationNotifications.size else 0
    val calendarNoticeCount = cancellationCount + calendar.eventNotifications.count
    val promotionCount = if (user.hasProfile) profile.promotionNotifications.size else 0
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, user.userId, user.role) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                viewModel.refreshPerformanceRequests()
                viewModel.refreshNotifications()
                delay(30_000)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val title = currentTab?.title ?: when (currentRoute) {
        Tab.Manage.route -> "Gestionar agenda"
        EVENT_ROUTE -> "Actividad"
        SCHEDULE_ROUTE -> when (backStackEntry?.arguments?.getString("kind")) {
            ScheduleTarget.EVENT_EDIT -> "Editar sesión"
            ScheduleTarget.RULE_EDIT -> "Editar horario habitual"
            ScheduleTarget.RULE_NEW -> "Nuevo horario habitual"
            else -> "Programar actividad"
        }
        else -> ""
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (currentTab == null && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    if (currentTab == Tab.Calendar && user.canManageCalendar && Tab.Manage !in tabs) {
                        IconButton(onClick = { navController.navigate(Tab.Manage.route) }) {
                            Icon(Icons.Filled.EditCalendar, contentDescription = "Gestionar agenda")
                        }
                    }
                    if (currentTab != null && currentTab != Tab.Account) {
                        IconButton(onClick = viewModel::refresh, enabled = !isRefreshing) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val badgeCount = when (tab) {
                            Tab.Calendar -> calendarNoticeCount
                            Tab.Profile -> promotionCount
                            Tab.Performance -> pendingCount
                            else -> 0
                        }
                        val badgeLabel = when (tab) {
                            Tab.Calendar -> "avisos de calendario pendientes"
                            Tab.Profile -> "avisos de ascenso pendientes"
                            else -> "actividades pendientes de confirmar o rechazar"
                        }
                        val hasPending = badgeCount > 0
                        NavigationBarItem(
                            modifier = Modifier.background(if (hasPending) KungFuGoldContainer else Color.Transparent, RoundedCornerShape(8.dp)),
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = {
                                BadgedBox(badge = {
                                    if (hasPending) Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.semantics {
                                            contentDescription = "$badgeCount $badgeLabel"
                                        },
                                    ) { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
                                }) {
                                    Icon(tab.icon, contentDescription = null,
                                        tint = if (hasPending) KungFuOnGoldContainer else Color.Unspecified)
                                }
                            },
                            label = {
                                Text(tab.label, maxLines = 2, textAlign = TextAlign.Center,
                                    color = if (hasPending) KungFuOnGoldContainer else Color.Unspecified)
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == Tab.Calendar && user.canManageCalendar) {
                ExtendedFloatingActionButton(
                    onClick = { navController.openSchedule(ScheduleTarget.EVENT_NEW, date = calendar.selectedDate) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Sesión") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AppNavHost(navController, user, viewModel, isRefreshing)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Refreshable(isRefreshing: Boolean, onRefresh: () -> Unit, content: @Composable () -> Unit) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        content()
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    user: SessionUser,
    viewModel: AppViewModel,
    isRefreshing: Boolean,
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    NavHost(navController = navController, startDestination = if (user.hasProfile) Tab.Profile.route else Tab.Home.route) {
        composable(Tab.Home.route) {
            Refreshable(isRefreshing, viewModel::refresh) {
                HomeScreen(
                    user = user,
                    viewModel = viewModel,
                    onOpenEvent = navController::openEvent,
                    onOpenCalendar = { navController.navigateToTab(Tab.Calendar) },
                    onOpenProfile = { navController.navigateToTab(Tab.Profile) },
                )
            }
        }
        composable(Tab.Profile.route) {
            Refreshable(isRefreshing, viewModel::refresh) { MyProfileScreen(viewModel) }
        }
        composable(Tab.Calendar.route) {
            Refreshable(isRefreshing, viewModel::refresh) {
                CalendarScreen(user = user, viewModel = viewModel, onOpenEvent = navController::openEvent)
            }
        }
        composable(Tab.Performance.route) {
            Refreshable(isRefreshing, viewModel::refresh) { PerformanceScreen(user, viewModel) }
        }
        composable(Tab.Manage.route) {
            Refreshable(isRefreshing, viewModel::refresh) {
                ManageScreen(
                    user = user,
                    viewModel = viewModel,
                    onNewEvent = { navController.openSchedule(ScheduleTarget.EVENT_NEW, date = schoolToday()) },
                    onNewRule = { navController.openSchedule(ScheduleTarget.RULE_NEW, date = schoolToday()) },
                    onEditRule = { navController.openSchedule(ScheduleTarget.RULE_EDIT, id = it) },
                    onOpenEvent = navController::openEvent,
                )
            }
        }
        composable(Tab.Account.route) {
            AccountScreen(
                user = user,
                baseUrl = viewModel.baseUrl,
                state = account,
                onChangePassword = viewModel::changePassword,
                onLogout = viewModel::logout,
            )
        }
        composable(EVENT_ROUTE, arguments = listOf(navArgument("eventId") { type = NavType.LongType })) { entry ->
            EventDetailScreen(
                eventId = entry.arguments?.getLong("eventId") ?: -1L,
                user = user,
                viewModel = viewModel,
                onEditEvent = { navController.openSchedule(ScheduleTarget.EVENT_EDIT, id = it) },
                onEditRule = { navController.openSchedule(ScheduleTarget.RULE_EDIT, id = it) },
            )
        }
        composable(
            SCHEDULE_ROUTE,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val args = entry.arguments
            val target = ScheduleTarget(
                kind = args?.getString("kind") ?: ScheduleTarget.EVENT_NEW,
                id = args?.getLong("id")?.takeIf { it > 0 },
                date = args?.getString("date")?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            )
            ScheduleFormScreen(
                target = target,
                user = user,
                viewModel = viewModel,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
