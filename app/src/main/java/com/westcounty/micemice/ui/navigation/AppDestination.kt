package com.westcounty.micemice.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.westcounty.micemice.data.model.UserRole

data class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val requiredRole: UserRole = UserRole.Researcher,
    val showInBottomBar: Boolean = true,
)

private fun UserRole.level(): Int = when (this) {
    UserRole.Researcher -> 0
    UserRole.PrincipalInvestigator -> 1
    UserRole.Admin -> 2
}

fun AppDestination.canAccess(role: UserRole): Boolean = role.level() >= requiredRole.level()

val appDestinations = listOf(
    AppDestination("workbench", "工作台", Icons.Filled.Home, Icons.Outlined.Home),
    AppDestination("scan", "扫码", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    AppDestination("cages", "笼位", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    AppDestination("cage/{cageId}", "笼卡详情", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, showInBottomBar = false),
    AppDestination("animals", "个体", Icons.Filled.People, Icons.Outlined.People),
    AppDestination("animal/{animalId}", "个体详情", Icons.Filled.People, Icons.Outlined.People, showInBottomBar = false),
    AppDestination("breeding", "繁育", Icons.Filled.Science, Icons.Outlined.Science),
    AppDestination("genotyping", "分型", Icons.Filled.Science, Icons.Outlined.Science),
    AppDestination("experiment", "实验", Icons.Filled.Task, Icons.Outlined.Task),
    AppDestination("tasks", "任务", Icons.Filled.Task, Icons.Outlined.Task),
    AppDestination("notifications", "通知中心", Icons.Filled.Notifications, Icons.Outlined.Notifications, showInBottomBar = false),
    AppDestination("admin", "管理", Icons.Filled.Tune, Icons.Outlined.Tune, requiredRole = UserRole.Admin),
    AppDestination("conflicts", "冲突中心", Icons.Filled.Tune, Icons.Outlined.Tune, requiredRole = UserRole.Admin, showInBottomBar = false),
    AppDestination("cohort", "Cohort", Icons.Filled.GroupWork, Icons.Outlined.GroupWork, showInBottomBar = false),
    AppDestination("reports", "报表", Icons.Filled.Assessment, Icons.Outlined.Assessment, requiredRole = UserRole.PrincipalInvestigator, showInBottomBar = false),
)

val primaryDestinations = appDestinations.filter { it.showInBottomBar }
