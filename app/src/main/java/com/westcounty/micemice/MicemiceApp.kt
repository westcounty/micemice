package com.westcounty.micemice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.feature.admin.AdminScreen
import com.westcounty.micemice.feature.animals.AnimalDetailScreen
import com.westcounty.micemice.feature.animals.AnimalsScreen
import com.westcounty.micemice.feature.auth.LoginScreen
import com.westcounty.micemice.feature.breeding.BreedingScreen
import com.westcounty.micemice.feature.cages.CageDetailScreen
import com.westcounty.micemice.feature.cages.CagesScreen
import com.westcounty.micemice.feature.cohort.CohortScreen
import com.westcounty.micemice.feature.conflict.ConflictCenterScreen
import com.westcounty.micemice.feature.experiment.ExperimentScreen
import com.westcounty.micemice.feature.genotyping.GenotypingScreen
import com.westcounty.micemice.feature.reports.ReportsScreen
import com.westcounty.micemice.feature.scan.ScanEntryScreen
import com.westcounty.micemice.feature.tasks.NotificationsScreen
import com.westcounty.micemice.feature.tasks.TasksScreen
import com.westcounty.micemice.feature.workbench.WorkbenchScreen
import com.westcounty.micemice.ui.navigation.appDestinations
import com.westcounty.micemice.ui.navigation.canAccess
import com.westcounty.micemice.ui.navigation.primaryDestinations
import com.westcounty.micemice.ui.state.LabViewModel
import com.westcounty.micemice.ui.util.notifyUnreadAlerts
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicemiceApp(
    modifier: Modifier = Modifier,
    viewModel: LabViewModel = viewModel(factory = LabViewModel.factory()),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    if (!session.isLoggedIn) {
        LoginScreen(onLogin = viewModel::login)
        return
    }

    val navController = rememberNavController()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val exportPreview by viewModel.exportPreview.collectAsStateWithLifecycle()
    val canUndoWeaning by viewModel.canUndoWeaning.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotificationPermission = granted },
    )

    val allowedBottomDestinations = primaryDestinations.filter { it.canAccess(session.role) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: allowedBottomDestinations.first().route
    val currentDestination = resolveDestination(currentRoute) ?: allowedBottomDestinations.first()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(currentRoute, session.role) {
        val destination = resolveDestination(currentRoute)
        if (destination != null && !destination.canAccess(session.role)) {
            navController.navigate("workbench") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(unreadNotificationCount, hasNotificationPermission) {
        if (unreadNotificationCount <= 0) {
            notifyUnreadAlerts(context, unreadNotificationCount)
            return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@LaunchedEffect
        }
        notifyUnreadAlerts(context, unreadNotificationCount)
    }

    LaunchedEffect(session.isLoggedIn, session.lastActionAtMillis) {
        if (!session.isLoggedIn) return@LaunchedEffect
        while (true) {
            delay(60_000)
            viewModel.checkSessionTimeout()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Micemice LabOS · ${session.orgCode}")
                        Text(text = currentDestination.label)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "退出")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                allowedBottomDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = allowedBottomDestinations.first().route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("workbench") {
                WorkbenchScreen(
                    snapshot = snapshot,
                    unreadNotificationCount = unreadNotificationCount,
                    onCompleteTask = viewModel::completeTask,
                    onOpenCohort = { navController.navigate("cohort") },
                    onOpenReports = { navController.navigate("reports") },
                    onOpenNotifications = { navController.navigate("notifications") },
                )
            }
            composable("scan") {
                ScanEntryScreen(onOpenCode = { code -> navController.navigate("cage/$code") })
            }
            composable("cages") {
                CagesScreen(
                    snapshot = snapshot,
                    onMoveAnimal = { animalId, targetCageId -> viewModel.moveAnimals(listOf(animalId), targetCageId) },
                    onOpenCageDetail = { cageId -> navController.navigate("cage/$cageId") },
                )
            }
            composable("cage/{cageId}") { entry ->
                val cageId = entry.arguments?.getString("cageId").orEmpty()
                CageDetailScreen(
                    cageId = cageId,
                    snapshot = snapshot,
                    onMoveAnimal = { animalId, targetCageId -> viewModel.moveAnimals(listOf(animalId), targetCageId) },
                    onMergeCages = viewModel::mergeCages,
                    onSplitCage = viewModel::splitCage,
                    onBatchUpdateStatus = viewModel::updateAnimalStatusBatch,
                    onOpenAnimalDetail = { animalId -> navController.navigate("animal/$animalId") },
                )
            }
            composable("animals") {
                AnimalsScreen(
                    snapshot = snapshot,
                    onUpdateStatus = viewModel::updateAnimalStatus,
                    onOpenAnimalDetail = { animalId -> navController.navigate("animal/$animalId") },
                )
            }
            composable("animal/{animalId}") { entry ->
                val animalId = entry.arguments?.getString("animalId").orEmpty()
                AnimalDetailScreen(
                    animalId = animalId,
                    snapshot = snapshot,
                    onUpdateStatus = viewModel::updateAnimalStatus,
                    onAddAnimalEvent = viewModel::addAnimalEvent,
                    onAddAttachment = viewModel::addAnimalAttachment,
                )
            }
            composable("breeding") {
                BreedingScreen(
                    snapshot = snapshot,
                    onCreatePlan = viewModel::createBreedingPlan,
                    onRecordBirth = viewModel::recordBirth,
                    onRecordPlugCheck = viewModel::recordPlugCheck,
                    onCompleteWeaning = viewModel::completeWeaning,
                    onRunWeaningWizard = viewModel::runWeaningWizard,
                    onUndoWeaningWizard = viewModel::undoLastWeaningWizard,
                    canUndoWeaningWizard = canUndoWeaning,
                )
            }
            composable("genotyping") {
                GenotypingScreen(
                    snapshot = snapshot,
                    onRegisterSample = viewModel::registerSample,
                    onCreateBatch = viewModel::createGenotypingBatch,
                    onImportResults = viewModel::importGenotypingResults,
                    onConfirmResult = viewModel::confirmGenotypingResult,
                )
            }
            composable("experiment") {
                ExperimentScreen(
                    snapshot = snapshot,
                    onCreateExperiment = viewModel::createExperiment,
                    onAddEvent = viewModel::addExperimentEvent,
                    onArchiveExperiment = viewModel::archiveExperiment,
                )
            }
            composable("tasks") {
                TasksScreen(
                    tasks = snapshot.tasks,
                    taskTemplates = snapshot.taskTemplates,
                    filterTasks = viewModel::filterTasks,
                    onCompleteTask = viewModel::completeTask,
                    onCompleteBatch = viewModel::completeTasks,
                    onCreateFromTemplate = viewModel::createTaskFromTemplate,
                    onSaveTaskTemplate = viewModel::saveTaskTemplate,
                    onDeleteTaskTemplate = viewModel::deleteTaskTemplate,
                    onReassignTask = viewModel::reassignTask,
                    onReassignBatch = viewModel::reassignTasks,
                    escalationConfig = snapshot.taskEscalationConfig,
                    onSaveEscalationConfig = viewModel::saveTaskEscalationConfig,
                    onApplyEscalationConfig = viewModel::applyTaskEscalation,
                    canManage = snapshot.rolePermissionOverrides.isGranted(session.role, PermissionKey.TaskManage),
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    notifications = notifications,
                    onMarkRead = viewModel::markNotificationRead,
                    onMarkAllRead = viewModel::markAllNotificationsRead,
                )
            }
            composable("admin") {
                AdminScreen(
                    session = session,
                    snapshot = snapshot,
                    exportPreview = exportPreview,
                    onRoleChange = viewModel::switchRole,
                    onProtocolToggle = viewModel::setProtocolState,
                    onOpenConflicts = { navController.navigate("conflicts") },
                    onSyncPending = viewModel::syncPendingEvents,
                    onImportAnimalsCsv = viewModel::importAnimalsCsv,
                    onExportAnimalsCsv = viewModel::exportAnimalsCsv,
                    onExportComplianceCsv = viewModel::exportComplianceCsv,
                    onSaveNotificationPolicy = viewModel::saveNotificationPolicy,
                    onAddStrainToCatalog = viewModel::addStrainToCatalog,
                    onRemoveStrainFromCatalog = viewModel::removeStrainFromCatalog,
                    onAddGenotypeToCatalog = viewModel::addGenotypeToCatalog,
                    onRemoveGenotypeFromCatalog = viewModel::removeGenotypeFromCatalog,
                    onUpsertTrainingRecord = viewModel::upsertTrainingRecord,
                    onRemoveTrainingRecord = viewModel::removeTrainingRecord,
                    onSetRolePermission = viewModel::setRolePermission,
                    onLogoutCurrentSession = viewModel::logout,
                    onCreateCage = viewModel::createCage,
                    onCreateAnimal = viewModel::createAnimal,
                )
            }
            composable("conflicts") {
                ConflictCenterScreen(
                    snapshot = snapshot,
                    onConfirmGenotyping = viewModel::confirmGenotypingResult,
                    onRetrySyncEvent = viewModel::retrySyncEvent,
                )
            }
            composable("cohort") {
                CohortScreen(
                    snapshot = snapshot,
                    onCreateCohort = viewModel::createCohort,
                    onSaveTemplate = viewModel::saveCohortTemplate,
                    onApplyTemplate = viewModel::applyCohortTemplate,
                    onUpdateTemplate = viewModel::updateCohortTemplate,
                    onDeleteTemplate = viewModel::deleteCohortTemplate,
                )
            }
            composable("reports") {
                ReportsScreen(snapshot = snapshot)
            }
        }
    }
}

private fun resolveDestination(route: String): com.westcounty.micemice.ui.navigation.AppDestination? {
    return appDestinations.firstOrNull { it.route == route }
        ?: appDestinations.firstOrNull { destination ->
            val base = destination.route.substringBefore("/{")
            route.startsWith("$base/")
        }
}
