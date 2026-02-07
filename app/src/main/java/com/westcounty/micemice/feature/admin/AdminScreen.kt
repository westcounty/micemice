package com.westcounty.micemice.feature.admin

import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.westcounty.micemice.data.model.AnimalSex
import com.westcounty.micemice.data.model.CageStatus
import com.westcounty.micemice.data.model.LabSnapshot
import com.westcounty.micemice.data.model.PermissionKey
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.data.model.UserRole
import com.westcounty.micemice.data.model.roleLevel
import com.westcounty.micemice.ui.components.SectionHeader
import com.westcounty.micemice.ui.state.SessionState
import com.westcounty.micemice.ui.util.daysUntil
import com.westcounty.micemice.ui.util.formatDateTime
import com.westcounty.micemice.ui.util.generateQrCodeBitmap
import com.westcounty.micemice.ui.util.writeBitmapPngToAppFiles
import com.westcounty.micemice.ui.util.writeCompliancePackageZip
import com.westcounty.micemice.ui.util.writeCsvToAppFiles
import com.westcounty.micemice.ui.util.writeTextPdfToAppFiles

@Composable
fun AdminScreen(
    session: SessionState,
    snapshot: LabSnapshot,
    exportPreview: String,
    onRoleChange: (UserRole) -> Unit,
    onProtocolToggle: (protocolId: String, isActive: Boolean) -> Unit,
    onOpenConflicts: () -> Unit,
    onSyncPending: () -> Unit,
    onImportAnimalsCsv: (String) -> Unit,
    onExportAnimalsCsv: () -> Unit,
    onExportComplianceCsv: () -> Unit,
    onSaveNotificationPolicy: (
        enableProtocolExpiry: Boolean,
        protocolExpiryLeadDays: Int,
        enableOverdueTask: Boolean,
        enableCageCapacity: Boolean,
        enableSyncFailure: Boolean,
    ) -> Unit,
    onAddStrainToCatalog: (String) -> Unit,
    onRemoveStrainFromCatalog: (String) -> Unit,
    onAddGenotypeToCatalog: (String) -> Unit,
    onRemoveGenotypeFromCatalog: (String) -> Unit,
    onUpsertTrainingRecord: (username: String, expiresInDays: Int, active: Boolean, note: String) -> Unit,
    onRemoveTrainingRecord: (username: String) -> Unit,
    onSetRolePermission: (role: UserRole, permission: PermissionKey, enabled: Boolean) -> Unit,
    onLogoutCurrentSession: () -> Unit,
    onCreateCage: (cageId: String, roomCode: String, rackCode: String, slotCode: String, capacityLimit: Int) -> Unit,
    onCreateAnimal: (
        identifier: String,
        sex: AnimalSex,
        strain: String,
        genotype: String,
        cageId: String,
        protocolId: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val deviceId = runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull().orEmpty()
    val pendingSyncCount = snapshot.syncEvents.count { it.status == SyncStatus.Pending || it.status == SyncStatus.Failed }
    var enableProtocolExpiry by remember(snapshot.notificationPolicy) { mutableStateOf(snapshot.notificationPolicy.enableProtocolExpiry) }
    var protocolLeadDaysText by remember(snapshot.notificationPolicy) { mutableStateOf(snapshot.notificationPolicy.protocolExpiryLeadDays.toString()) }
    var enableOverdueTask by remember(snapshot.notificationPolicy) { mutableStateOf(snapshot.notificationPolicy.enableOverdueTask) }
    var enableCageCapacity by remember(snapshot.notificationPolicy) { mutableStateOf(snapshot.notificationPolicy.enableCageCapacity) }
    var enableSyncFailure by remember(snapshot.notificationPolicy) { mutableStateOf(snapshot.notificationPolicy.enableSyncFailure) }
    var importCsvText by remember { mutableStateOf("identifier,sex,strain,genotype,cage_id,protocol_id\n") }
    var cageIdText by remember { mutableStateOf("C-${snapshot.cages.size + 101}") }
    var roomCodeText by remember { mutableStateOf("A1") }
    var rackCodeText by remember { mutableStateOf("R1") }
    var slotCodeText by remember { mutableStateOf("99") }
    var cageCapacityText by remember { mutableStateOf("5") }

    var animalIdentifierText by remember { mutableStateOf("") }
    var animalSex by remember { mutableStateOf(AnimalSex.Unknown) }
    var strainText by remember(snapshot.strainCatalog) { mutableStateOf(snapshot.strainCatalog.firstOrNull().orEmpty()) }
    var genotypeText by remember(snapshot.genotypeCatalog) { mutableStateOf(snapshot.genotypeCatalog.firstOrNull().orEmpty()) }
    var animalTargetCageId by remember { mutableStateOf(snapshot.cages.firstOrNull()?.id ?: "") }
    var animalProtocolId by remember { mutableStateOf<String?>(null) }
    var newStrainText by remember { mutableStateOf("") }
    var newGenotypeText by remember { mutableStateOf("") }
    var trainingUsernameText by remember { mutableStateOf("") }
    var trainingDaysText by remember { mutableStateOf("365") }
    var trainingActive by remember { mutableStateOf(true) }
    var trainingNote by remember { mutableStateOf("") }

    val activeCages = snapshot.cages.filter { it.status == CageStatus.Active }
    var selectedCodeCageId by remember { mutableStateOf(activeCages.firstOrNull()?.id ?: "") }
    var qrPayload by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrMessage by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf("") }
    var pendingProtocolToggle by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(activeCages.map { it.id }) {
        if (activeCages.isNotEmpty() && activeCages.none { it.id == selectedCodeCageId }) {
            selectedCodeCageId = activeCages.first().id
        }
    }

    var auditOperatorFilter by remember { mutableStateOf("") }
    var auditEntityFilter by remember { mutableStateOf("") }
    var selectedPermissionRole by remember { mutableStateOf(UserRole.Researcher) }
    val filteredAudits = snapshot.auditEvents.filter { audit ->
        (auditOperatorFilter.isBlank() || audit.operator.contains(auditOperatorFilter, ignoreCase = true)) &&
            (auditEntityFilter.isBlank() ||
                audit.entityId.contains(auditEntityFilter, ignoreCase = true) ||
                audit.entityType.contains(auditEntityFilter, ignoreCase = true) ||
                audit.summary.contains(auditEntityFilter, ignoreCase = true))
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "管理中心",
                subtitle = "在 App 内完成权限、协议、同步、导入导出与审计管理",
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("当前角色", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = snapshot.currentRole == role,
                                onClick = { onRoleChange(role) },
                                label = { Text(role.displayName) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("细粒度权限矩阵", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("按角色启用/禁用具体能力点", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UserRole.entries, key = { it.name }) { role ->
                            FilterChip(
                                selected = selectedPermissionRole == role,
                                onClick = { selectedPermissionRole = role },
                                label = { Text(role.displayName) },
                            )
                        }
                    }
                    PermissionKey.entries.forEach { permission ->
                        val enabled = snapshot.rolePermissionOverrides.isGranted(selectedPermissionRole, permission)
                        val canConfigure = roleLevel(selectedPermissionRole) >= roleLevel(permission.minRole)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(permission.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "最低角色: ${permission.minRole.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = enabled,
                                enabled = canConfigure,
                                onCheckedChange = { checked -> onSetRolePermission(selectedPermissionRole, permission, checked) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("设备与会话", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("当前账号: ${session.username}", style = MaterialTheme.typography.bodySmall)
                    Text("组织: ${session.orgCode}", style = MaterialTheme.typography.bodySmall)
                    Text("设备: $deviceModel", style = MaterialTheme.typography.bodySmall)
                    Text("设备ID: ${if (deviceId.isBlank()) "unknown" else deviceId}", style = MaterialTheme.typography.bodySmall)
                    Text("登录时间: ${formatDateTime(session.loginAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    Text("最近活动: ${formatDateTime(session.lastActionAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onLogoutCurrentSession) {
                        Text("结束当前会话")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("培训资质", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = trainingUsernameText,
                        onValueChange = { trainingUsernameText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = trainingDaysText,
                        onValueChange = { trainingDaysText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("有效期天数（1-3650）") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("培训状态启用")
                        Switch(checked = trainingActive, onCheckedChange = { trainingActive = it })
                    }
                    OutlinedTextField(
                        value = trainingNote,
                        onValueChange = { trainingNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val days = trainingDaysText.toIntOrNull() ?: return@Button
                                onUpsertTrainingRecord(trainingUsernameText, days, trainingActive, trainingNote)
                            },
                            enabled = trainingUsernameText.isNotBlank() && (trainingDaysText.toIntOrNull() ?: 0) in 1..3650,
                        ) {
                            Text("保存培训记录")
                        }
                        Button(
                            onClick = { onRemoveTrainingRecord(trainingUsernameText) },
                            enabled = trainingUsernameText.isNotBlank(),
                        ) {
                            Text("删除培训记录")
                        }
                    }
                    snapshot.trainingRecords
                        .sortedBy { it.username.lowercase() }
                        .take(20)
                        .forEach { record ->
                            Text(
                                text = "${record.username} · ${if (record.isActive) "启用" else "停用"} · 到期${daysUntil(record.expiresAtMillis)}天",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (record.expiresAtMillis < System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("通知策略", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("协议到期提醒")
                        Switch(checked = enableProtocolExpiry, onCheckedChange = { enableProtocolExpiry = it })
                    }
                    OutlinedTextField(
                        value = protocolLeadDaysText,
                        onValueChange = { protocolLeadDaysText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("协议提前提醒天数（1-60）") },
                        singleLine = true,
                        enabled = enableProtocolExpiry,
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("任务逾期提醒")
                        Switch(checked = enableOverdueTask, onCheckedChange = { enableOverdueTask = it })
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("笼位容量提醒")
                        Switch(checked = enableCageCapacity, onCheckedChange = { enableCageCapacity = it })
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("同步失败提醒")
                        Switch(checked = enableSyncFailure, onCheckedChange = { enableSyncFailure = it })
                    }
                    Button(
                        onClick = {
                            val leadDays = protocolLeadDaysText.toIntOrNull() ?: 14
                            onSaveNotificationPolicy(
                                enableProtocolExpiry,
                                leadDays,
                                enableOverdueTask,
                                enableCageCapacity,
                                enableSyncFailure,
                            )
                        },
                        enabled = (protocolLeadDaysText.toIntOrNull() ?: -1) in 1..60,
                    ) {
                        Text("保存通知策略")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("主数据维护", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("品系字典", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = newStrainText,
                        onValueChange = { newStrainText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("新增品系") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onAddStrainToCatalog(newStrainText)
                                newStrainText = ""
                            },
                            enabled = newStrainText.isNotBlank(),
                        ) {
                            Text("添加品系")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.strainCatalog, key = { it }) { strain ->
                            FilterChip(
                                selected = strainText == strain,
                                onClick = { strainText = strain },
                                label = { Text(strain) },
                                trailingIcon = {
                                    Text(
                                        "删",
                                        modifier = Modifier.padding(start = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (strainText.isNotBlank()) onRemoveStrainFromCatalog(strainText) },
                            enabled = strainText.isNotBlank(),
                        ) {
                            Text("删除选中品系")
                        }
                    }

                    Text("基因型模板", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = newGenotypeText,
                        onValueChange = { newGenotypeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("新增基因型模板") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onAddGenotypeToCatalog(newGenotypeText)
                                newGenotypeText = ""
                            },
                            enabled = newGenotypeText.isNotBlank(),
                        ) {
                            Text("添加模板")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.genotypeCatalog, key = { it }) { genotype ->
                            FilterChip(
                                selected = genotypeText == genotype,
                                onClick = { genotypeText = genotype },
                                label = { Text(genotype) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (genotypeText.isNotBlank()) onRemoveGenotypeFromCatalog(genotypeText) },
                            enabled = genotypeText.isNotBlank(),
                        ) {
                            Text("删除选中模板")
                        }
                    }

                    Text("新建笼位", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = cageIdText,
                        onValueChange = { cageIdText = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("笼编号") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomCodeText,
                            onValueChange = { roomCodeText = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("房间") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = rackCodeText,
                            onValueChange = { rackCodeText = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("机架") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = slotCodeText,
                            onValueChange = { slotCodeText = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            label = { Text("位点") },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = cageCapacityText,
                        onValueChange = { cageCapacityText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("容量 1-99") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val cap = cageCapacityText.toIntOrNull() ?: return@Button
                            onCreateCage(cageIdText, roomCodeText, rackCodeText, slotCodeText, cap)
                        },
                        enabled = cageIdText.isNotBlank() && (cageCapacityText.toIntOrNull() ?: 0) in 1..99,
                    ) {
                        Text("创建笼位")
                    }

                    Text("新建个体", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = animalIdentifierText,
                        onValueChange = { animalIdentifierText = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("耳号/RFID") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimalSex.entries.forEach { sex ->
                            FilterChip(
                                selected = animalSex == sex,
                                onClick = { animalSex = sex },
                                label = { Text(sex.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = strainText,
                        onValueChange = { strainText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("品系") },
                        singleLine = true,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.strainCatalog, key = { it }) { strain ->
                            FilterChip(
                                selected = strainText == strain,
                                onClick = { strainText = strain },
                                label = { Text(strain) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = genotypeText,
                        onValueChange = { genotypeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("基因型") },
                        singleLine = true,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshot.genotypeCatalog, key = { it }) { genotype ->
                            FilterChip(
                                selected = genotypeText == genotype,
                                onClick = { genotypeText = genotype },
                                label = { Text(genotype) },
                            )
                        }
                    }
                    Text("入笼", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeCages, key = { it.id }) { cage ->
                            FilterChip(
                                selected = animalTargetCageId == cage.id,
                                onClick = { animalTargetCageId = cage.id },
                                label = { Text(cage.id) },
                            )
                        }
                    }
                    Text("协议（可选）", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = animalProtocolId == null,
                                onClick = { animalProtocolId = null },
                                label = { Text("不绑定") },
                            )
                        }
                        items(snapshot.protocols, key = { it.id }) { protocol ->
                            FilterChip(
                                selected = animalProtocolId == protocol.id,
                                onClick = { animalProtocolId = protocol.id },
                                label = { Text(protocol.id) },
                                enabled = protocol.isActive,
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onCreateAnimal(
                                animalIdentifierText,
                                animalSex,
                                strainText,
                                genotypeText,
                                animalTargetCageId,
                                animalProtocolId,
                            )
                            animalIdentifierText = ""
                        },
                        enabled = animalIdentifierText.isNotBlank() &&
                            animalTargetCageId.isNotBlank() &&
                            strainText.isNotBlank() &&
                            genotypeText.isNotBlank(),
                    ) {
                        Text("创建个体")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("笼码生成", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("用于扫码入口识别，建议内容使用笼编号", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeCages, key = { it.id }) { cage ->
                            FilterChip(
                                selected = selectedCodeCageId == cage.id,
                                onClick = { selectedCodeCageId = cage.id },
                                label = { Text(cage.id) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selectedCodeCageId,
                        onValueChange = { selectedCodeCageId = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("二维码内容") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val payload = selectedCodeCageId.trim().uppercase()
                                qrPayload = payload
                                qrBitmap = generateQrCodeBitmap(payload)
                                qrMessage = if (qrBitmap == null) "生成失败，请检查内容" else ""
                            },
                            enabled = selectedCodeCageId.isNotBlank(),
                        ) {
                            Text("生成二维码")
                        }
                        Button(
                            onClick = {
                                val cages = activeCages
                                var okCount = 0
                                cages.forEach { cage ->
                                    val bmp = generateQrCodeBitmap(cage.id) ?: return@forEach
                                    val result = writeBitmapPngToAppFiles(context, "cage_qr_${cage.id}", bmp)
                                    if (result.isSuccess) okCount += 1
                                }
                                qrMessage = "批量导出完成：$okCount/${cages.size}"
                            },
                            enabled = activeCages.isNotEmpty(),
                        ) {
                            Text("批量导出")
                        }
                    }
                    val bmp = qrBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "笼码二维码",
                            modifier = Modifier.size(180.dp),
                        )
                        Text("当前内容：$qrPayload", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                val result = writeBitmapPngToAppFiles(
                                    context = context,
                                    prefix = "cage_qr_${qrPayload.ifBlank { "code" }}",
                                    bitmap = bmp,
                                )
                                qrMessage = result.getOrNull()?.let { "已保存：$it" } ?: "保存失败"
                            },
                        ) {
                            Text("保存当前二维码PNG")
                        }
                    }
                    if (qrMessage.isNotBlank()) {
                        Text(qrMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("同步队列", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("待同步事件: $pendingSyncCount", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSyncPending, enabled = pendingSyncCount > 0) {
                            Text("立即同步")
                        }
                        Button(onClick = onOpenConflicts) {
                            Text("冲突中心")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("导入导出", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        label = { Text("动物 CSV") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onImportAnimalsCsv(importCsvText) }) {
                            Text("导入个体")
                        }
                        Button(onClick = onExportAnimalsCsv) {
                            Text("生成个体CSV")
                        }
                        Button(onClick = onExportComplianceCsv) {
                            Text("生成合规CSV")
                        }
                    }
                    if (exportPreview.isNotBlank()) {
                        OutlinedTextField(
                            value = exportPreview,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("导出预览") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 8,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val result = writeCsvToAppFiles(context, "micemice_export", exportPreview)
                                    exportMessage = result.getOrNull()?.let { "CSV已保存：$it" } ?: "CSV保存失败"
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("保存CSV")
                            }
                            Button(
                                onClick = {
                                    val result = writeTextPdfToAppFiles(
                                        context = context,
                                        prefix = "micemice_compliance",
                                        title = "Micemice Compliance Export",
                                        content = exportPreview,
                                    )
                                    exportMessage = result.getOrNull()?.let { "PDF已保存：$it" } ?: "PDF保存失败"
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("保存PDF")
                            }
                        }
                        Button(
                            onClick = {
                                val result = writeCompliancePackageZip(
                                    context = context,
                                    prefix = "micemice_compliance_pkg",
                                    csvContent = exportPreview,
                                    pdfTitle = "Micemice Compliance Export",
                                    pdfContent = exportPreview,
                                )
                                exportMessage = result.getOrNull()?.let { "导出包已保存：$it" } ?: "导出包保存失败"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存合规导出包ZIP")
                        }
                        if (exportMessage.isNotBlank()) {
                            Text(exportMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "协议状态",
                subtitle = "临期协议优先续期，过期协议禁止关键操作",
            )
        }

        items(snapshot.protocols, key = { it.id }) { protocol ->
            val days = daysUntil(protocol.expiresAtMillis)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(protocol.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(protocol.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "到期剩余 ${days} 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (days < 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = protocol.isActive,
                        onCheckedChange = { checked -> pendingProtocolToggle = protocol.id to checked },
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "最近同步事件",
                subtitle = "离线动作进入队列后可集中上送",
            )
        }

        items(snapshot.syncEvents.take(10), key = { it.id }) { sync ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(sync.eventType, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(sync.payloadSummary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${sync.status.name} · retry=${sync.retryCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "最近审计记录",
                subtitle = "关键操作自动留痕，支持内部检查",
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = auditOperatorFilter,
                    onValueChange = { auditOperatorFilter = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("按操作者过滤") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = auditEntityFilter,
                    onValueChange = { auditEntityFilter = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("按对象过滤") },
                    singleLine = true,
                )
            }
        }

        items(filteredAudits.take(20), key = { it.id }) { audit ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(audit.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(audit.summary, style = MaterialTheme.typography.bodyMedium)
                    if (audit.beforeFields.isNotEmpty()) {
                        Text(
                            text = "Before: ${audit.beforeFields.entries.joinToString(" | ") { "${it.key}=${it.value}" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (audit.afterFields.isNotEmpty()) {
                        Text(
                            text = "After: ${audit.afterFields.entries.joinToString(" | ") { "${it.key}=${it.value}" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${audit.operator} · ${formatDateTime(audit.createdAtMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    val pending = pendingProtocolToggle
    if (pending != null) {
        val (protocolId, targetState) = pending
        AlertDialog(
            onDismissRequest = { pendingProtocolToggle = null },
            title = { Text("确认修改协议状态") },
            text = {
                Text("是否将 $protocolId ${if (targetState) "启用" else "停用"}？该操作会影响关键流程可执行性。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onProtocolToggle(protocolId, targetState)
                        pendingProtocolToggle = null
                    },
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingProtocolToggle = null }) {
                    Text("取消")
                }
            },
        )
    }
}
