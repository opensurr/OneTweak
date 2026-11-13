package com.oneuitweak.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// ---------- Theme: white + brand blue, navy in dark mode ----------

private val LightColors = lightColorScheme(
    primary = Color(0xFF3468CB), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FB), onPrimaryContainer = Color(0xFF0B1E3F),
    secondary = Color(0xFF4A6FA5), onSecondary = Color.White,
    background = Color(0xFFF3F6FC), onBackground = Color(0xFF0B1E3F),
    surface = Color.White, onSurface = Color(0xFF0B1E3F),
    surfaceVariant = Color(0xFFE8EEF9), onSurfaceVariant = Color(0xFF4A5B7C),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB0FF), onPrimary = Color(0xFF0B1E3F),
    primaryContainer = Color(0xFF1B3A73), onPrimaryContainer = Color(0xFFDCE6FB),
    secondary = Color(0xFFA9C4F5), onSecondary = Color(0xFF0B1E3F),
    background = Color(0xFF0B1E3F), onBackground = Color.White,
    surface = Color(0xFF122A55), onSurface = Color.White,
    surfaceVariant = Color(0xFF1B3A73), onSurfaceVariant = Color(0xFFC9D8F5),
    error = Color(0xFFFFB4AB)
)

private val GoodGreen = Color(0xFF2E9E5B)

@Composable
fun OneUiTweakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

// ---------- State and actions ----------

data class UiState(
    val connected: Boolean,
    val oneUi: OneUiCheck.Info,
    val current: Current,
    val profiles: List<Profile>,
    val busy: Boolean,
    val message: String,
    val notifGranted: Boolean,
    val a11yEnabled: Boolean,
    val model: String
)

class Actions(
    val tweak: (String, () -> AdbBridge.Result) -> Unit,
    val requestNotifications: () -> Unit,
    val openAccessibility: () -> Unit,
    val pairAndConnect: (Int, String, Int) -> Unit,
    val disconnect: () -> Unit,
    val refresh: () -> Unit,
    val setLauncher: (String) -> Unit,
    val saveProfile: (Profile) -> Unit,
    val deleteProfile: (String) -> Unit,
    val setModel: (String) -> Unit
)

enum class Tab(val label: String, val subtitle: String, val icon: ImageVector) {
    HOME("Home", "Setup and quick controls", Icons.Rounded.Home),
    DISPLAY("Display", "Text, density, brightness and screen", Icons.Rounded.Tune),
    MOTION("Motion", "Animation speed and presets", Icons.Rounded.Speed),
    APPS("Device", "Apps, device model and icon", Icons.Rounded.Smartphone),
    PROFILES("Profiles", "Save and apply full setups", Icons.Rounded.Star)
}

private val Models = listOf("Galaxy S26 Ultra", "Galaxy S26", "Galaxy S25 Ultra", "Galaxy S25", "Base / Other")

// ---------- Root ----------

@Composable
fun AppRoot(state: UiState, actions: Actions) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val tab = Tab.entries[tabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (tab == Tab.HOME) Hero(state) else PageHeader(tab)
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            MessageBar(state.message)

            when (tab) {
                Tab.HOME -> HomeTab(state, actions)
                Tab.DISPLAY -> DisplayTab(state, actions)
                Tab.MOTION -> MotionTab(state, actions)
                Tab.APPS -> DeviceTab(state, actions)
                Tab.PROFILES -> ProfilesTab(state, actions)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Hero(state: UiState) {
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Image(
            painter = painterResource(R.drawable.banner_onetweak),
            contentDescription = "OneUI Tweak banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60B1E3F))))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "OneUI Tweak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${state.model} · ${state.oneUi.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            StatusPill(connected = state.connected)
        }
    }
}

@Composable
private fun StatusPill(connected: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (connected) GoodGreen else Color(0xFFFFB4AB))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (connected) "ADB connected" else "ADB not connected",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PageHeader(tab: Tab) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp)) {
        Text(tab.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(tab.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageBar(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ---------- Shared pieces ----------

@Composable
private fun TweakCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    subtitle: String,
    done: Boolean,
    buttonLabel: String?,
    onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (done) GoodGreen else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (buttonLabel != null) {
            FilledTonalButton(onClick = onAction) { Text(buttonLabel) }
        }
    }
}

@Composable
private fun PairDialog(onDismiss: () -> Unit, onConfirm: (Int, String, Int) -> Unit) {
    var pairPort by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var connectPort by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with Wireless debugging") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "On your phone: Settings → Developer options → Wireless debugging. " +
                        "Tap \"Pair device with pairing code\" for the first two fields. " +
                        "Use the port on the main screen for the last field.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = pairPort,
                    onValueChange = { pairPort = it.filter(Char::isDigit) },
                    label = { Text("Pairing port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit pairing code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = connectPort,
                    onValueChange = { connectPort = it.filter(Char::isDigit) },
                    label = { Text("Connect port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = pairPort.isNotBlank() && code.length == 6 && connectPort.isNotBlank(),
                onClick = {
                    onConfirm(pairPort.toIntOrNull() ?: 0, code, connectPort.toIntOrNull() ?: 0)
                }
            ) { Text("Pair & connect") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------- Home ----------

@Composable
private fun HomeTab(state: UiState, actions: Actions) {
    var showPair by remember { mutableStateOf(false) }
    var bluetooth by remember { mutableStateOf(true) }
    var mobileData by remember { mutableStateOf(true) }
    val canUse = state.connected && state.oneUi.supported && !state.busy

    TweakCard("Setup", Icons.Rounded.CheckCircle, "Finish these once to unlock every tweak.") {
        ChecklistRow(
            "Notifications", "Shows a message when ADB connects",
            state.notifGranted, if (state.notifGranted) null else "Allow"
        ) { actions.requestNotifications() }
        ChecklistRow(
            "Accessibility", "Turn on OneUI Tweak in accessibility settings",
            state.a11yEnabled, if (state.a11yEnabled) null else "Enable"
        ) { actions.openAccessibility() }
        ChecklistRow(
            "ADB connection", "Pair with Wireless debugging",
            state.connected, if (state.connected) "Disconnect" else "Connect"
        ) { if (state.connected) actions.disconnect() else showPair = true }
        if (!state.oneUi.supported) {
            Text(
                "This app supports One UI 8 and One UI 9 only.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showPair) {
        PairDialog(
            onDismiss = { showPair = false },
            onConfirm = { pp, code, cp ->
                showPair = false
                actions.pairAndConnect(pp, code, cp)
            }
        )
    }

    TweakCard(
        "Quick toggles",
        Icons.Rounded.Bolt,
        "Bluetooth and mobile data. Wi-Fi is not here because turning it off would cut the wireless ADB link."
    ) {
        SwitchRow(Icons.Rounded.Bluetooth, "Bluetooth", bluetooth, canUse) { on ->
            bluetooth = on
            actions.tweak("Bluetooth") { Tweaks.setBluetooth(on) }
        }
        SwitchRow(Icons.Rounded.SignalCellularAlt, "Mobile data", mobileData, canUse) { on ->
            mobileData = on
            actions.tweak("Mobile data") { Tweaks.setMobileData(on) }
        }
    }

    TweakCard("Reset", Icons.Rounded.Refresh, "Restores motion, text size, density, dark mode, rotation, timeout and touch overlays.") {
        Button(
            enabled = canUse,
            onClick = { actions.tweak("Reset") { Tweaks.resetAll() } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset everything") }
    }

    TweakCard("About", Icons.Rounded.Info) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
            )
            Column {
                Text("OneUI Tweak 3.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Native ADB, no root, no Shizuku.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------- Display ----------

@Composable
private fun DisplayTab(state: UiState, actions: Actions) {
    val current = state.current
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var font by remember(current.font) { mutableFloatStateOf(current.font) }
    var dpi by remember { mutableStateOf("") }
    var brightness by remember { mutableFloatStateOf(128f) }

    TweakCard("Text size", Icons.Rounded.TextFields, "System font scale.") {
        Text("Scale: ${"%.2f".format(font)}x", style = MaterialTheme.typography.bodyLarge)
        Slider(value = font, onValueChange = { font = it }, valueRange = 0.85f..1.3f, steps = 8, enabled = canUse)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Text size") { Tweaks.setFontScale(font) } }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                font = 1f
                actions.tweak("Text size") { Tweaks.setFontScale(1f) }
            }) { Text("Normal") }
        }
    }

    TweakCard("Screen density", Icons.Rounded.AspectRatio, "Changes how large everything looks. Range 120 to 800.") {
        OutlinedTextField(
            value = dpi,
            onValueChange = { dpi = it.filter(Char::isDigit) },
            label = { Text("DPI") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = {
                actions.tweak("Density") { Tweaks.setDensity(dpi.toIntOrNull()) }
            }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Density") { Tweaks.resetDensity() } }) {
                Text("Reset")
            }
        }
    }

    TweakCard("Dark mode", Icons.Rounded.DarkMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("yes") } }) { Text("On") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("no") } }) { Text("Off") }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Dark mode") { Tweaks.setNightMode("auto") } }) { Text("Auto") }
        }
    }

    TweakCard("Brightness", Icons.Rounded.Brightness6, "Turns off auto-brightness and sets a fixed level.") {
        Text("Level: ${brightness.toInt()} / 255", style = MaterialTheme.typography.bodyLarge)
        Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0f..255f, enabled = canUse)
        Button(enabled = canUse, onClick = {
            val level = brightness.toInt()
            actions.tweak("Brightness") { Tweaks.setBrightness(level) }
        }) { Text("Apply") }
    }

    TweakCard("Screen behavior", Icons.Rounded.ScreenRotation) {
        SwitchRow(Icons.Rounded.ScreenRotation, "Auto-rotate", current.autoRotate, canUse) { on ->
            actions.tweak("Auto-rotate") { Tweaks.setAutoRotate(on) }
        }
        SwitchRow(Icons.Rounded.TouchApp, "Show touches", false, canUse) { on ->
            actions.tweak("Show touches") { Tweaks.setShowTouches(on) }
        }
        SwitchRow(Icons.Rounded.Palette, "Pointer location", false, canUse) { on ->
            actions.tweak("Pointer location") { Tweaks.setPointerLocation(on) }
        }
        SwitchRow(Icons.Rounded.LocalCafe, "Stay awake while charging", false, canUse) { on ->
            actions.tweak("Stay awake") { Tweaks.setStayAwake(on) }
        }
    }

    TweakCard("Screen timeout", Icons.Rounded.Timer, "Current: ${current.timeoutSec}s") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30 to "30s", 60 to "1m", 300 to "5m", 600 to "10m").forEach { (sec, label) ->
                OutlinedButton(enabled = canUse, onClick = {
                    actions.tweak("Screen timeout") { Tweaks.setScreenTimeout(sec) }
                }) { Text(label) }
            }
        }
    }
}

// ---------- Motion ----------

@Composable
private fun MotionTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var anim by remember(state.current.anim) { mutableFloatStateOf(state.current.anim) }

    TweakCard("Animation speed", Icons.Rounded.Speed, "Scales window, transition and in-app animations.") {
        Text("Scale: ${"%.2f".format(anim)}x", style = MaterialTheme.typography.bodyLarge)
        Slider(value = anim, onValueChange = { anim = it }, valueRange = 0f..2f, steps = 19, enabled = canUse)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = {
                actions.tweak("Animation speed") { Tweaks.setAnimationScale(anim) }
            }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                anim = 1f
                actions.tweak("Animation speed") { Tweaks.setAnimationScale(1f) }
            }) { Text("Normal") }
        }
    }

    TweakCard("Presets", Icons.Rounded.Bolt) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Off" to 0f, "Fast" to 0.5f, "Normal" to 1f, "Slow" to 1.5f).forEach { (label, v) ->
                FilledTonalButton(enabled = canUse, onClick = {
                    anim = v
                    actions.tweak("Preset $label") { Tweaks.setAnimationScale(v) }
                }) { Text(label) }
            }
        }
    }
}

// ---------- Device ----------

@Composable
private fun DeviceTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var pkg by remember { mutableStateOf("") }

    TweakCard("Device model", Icons.Rounded.Smartphone, "Shown in the header and saved on this phone. Your phone's real hardware is not changed.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Models.take(2).forEach { m ->
                FilterChip(selected = state.model == m, onClick = { actions.setModel(m) }, label = { Text(m) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Models.drop(2).forEach { m ->
                FilterChip(selected = state.model == m, onClick = { actions.setModel(m) }, label = { Text(m) })
            }
        }
    }

    TweakCard(
        "Enable or disable an app",
        Icons.Rounded.Apps,
        "Uses ADB for user 0. Disabling system apps can break things."
    ) {
        OutlinedTextField(
            value = pkg,
            onValueChange = { pkg = it.trim() },
            label = { Text("Package name, e.g. com.example.app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.tweak("Disable $pkg") { Tweaks.setAppEnabled(pkg, false) } }) {
                Text("Disable")
            }
            OutlinedButton(enabled = canUse, onClick = { actions.tweak("Enable $pkg") { Tweaks.setAppEnabled(pkg, true) } }) {
                Text("Enable")
            }
        }
    }

    TweakCard("OneUI Tweak icon", Icons.Rounded.Palette, "Changes only this app's icon. No ADB needed.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.setLauncher("LauncherDefault") }) { Text("Blue") }
            OutlinedButton(onClick = { actions.setLauncher("LauncherDark") }) { Text("Navy") }
        }
    }
}

// ---------- Profiles ----------

@Composable
private fun ProfilesTab(state: UiState, actions: Actions) {
    val canUse = state.connected && state.oneUi.supported && !state.busy
    var name by remember { mutableStateOf("") }
    var night by remember { mutableStateOf("auto") }

    TweakCard(
        "Save current setup",
        Icons.Rounded.Save,
        "Stores motion, text size, dark mode, rotation and timeout. Connect first so values load."
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("yes", "no", "auto").forEach { m ->
                FilterChip(selected = night == m, onClick = { night = m }, label = { Text(m) })
            }
        }
        Button(
            enabled = canUse && name.isNotBlank(),
            onClick = {
                val c = state.current
                actions.saveProfile(Profile(name.trim(), c.anim, c.font, night, c.autoRotate, c.timeoutSec))
                name = ""
            }
        ) { Text("Save profile") }
    }

    if (state.profiles.isEmpty()) {
        TweakCard("No profiles yet", Icons.Rounded.Star) {
            Text("Save your favorite setup once, then apply it in one tap.")
        }
    }

    state.profiles.forEach { p ->
        TweakCard(
            p.name,
            Icons.Rounded.Star,
            "Motion ${"%.2f".format(p.anim)}x · Text ${"%.2f".format(p.font)}x · Dark ${p.nightMode} · " +
                "Rotate ${if (p.autoRotate) "on" else "off"} · Screen ${p.timeoutSec}s"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = { actions.tweak("Profile ${p.name}") { Tweaks.applyProfile(p) } }) {
                    Text("Apply")
                }
                TextButton(onClick = { actions.deleteProfile(p.name) }) { Text("Delete") }
            }
        }
    }
}
