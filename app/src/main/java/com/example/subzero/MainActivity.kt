package com.example.subzero

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subzero.ui.screens.*
import com.example.subzero.ui.theme.MyApplicationTheme
import com.example.subzero.viewmodel.AssetViewModel
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.core.content.edit

enum class AppScreen {
    ONBOARDING,
    DASHBOARD,
    INSIGHTS,
    ALERTS,
    VAULT,
    ADD_EDIT
}

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: AssetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = androidx.lifecycle.ViewModelProvider(this)[AssetViewModel::class.java]

        // Add lifecycle observer to auto-lock on background
        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (viewModel.isAutoLockEnabled.value) {
                    viewModel.setAppLocked(true)
                }
            }
        })

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                SubZeroAppContainer(viewModel = viewModel)
            }
        }
    }

    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.setAppLocked(false)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SubZero")
            .setSubtitle("Confirm security verification to continue")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun SecureLockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1210)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secured",
                tint = Color(0xFF81C784),
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SUBZERO SECURED",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your subscription vault is protected by offline device encryption.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF909194),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E5F3B),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("unlock_vault_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SubZeroAppContainer(viewModel: AssetViewModel = viewModel()) {
    val context = LocalContext.current
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    LaunchedEffect(isAppLocked) {
        if (isAppLocked) {
            (context as? MainActivity)?.showBiometricPrompt()
        }
    }

    if (isAppLocked) {
        SecureLockScreen(
            onUnlockClick = {
                (context as? MainActivity)?.showBiometricPrompt()
            }
        )
        return
    }

    val sharedPrefs = remember { context.getSharedPreferences("subzero_prefs", Context.MODE_PRIVATE) }
    var currentScreen by remember { 
        mutableStateOf(
            if (sharedPrefs.getBoolean("onboarding_completed", false)) AppScreen.DASHBOARD else AppScreen.ONBOARDING
        ) 
    }
    
    var lastActiveScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var editAssetId by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            if (currentScreen != AppScreen.ONBOARDING && currentScreen != AppScreen.ADD_EDIT) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { currentScreen = AppScreen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.INSIGHTS,
                        onClick = { currentScreen = AppScreen.INSIGHTS },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Insights") },
                        label = { Text("Insights") },
                        modifier = Modifier.testTag("nav_tab_insights")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ALERTS,
                        onClick = { currentScreen = AppScreen.ALERTS },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                        label = { Text("Alerts") },
                        modifier = Modifier.testTag("nav_tab_alerts")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.VAULT,
                        onClick = { currentScreen = AppScreen.VAULT },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                        label = { Text("Vault") },
                        modifier = Modifier.testTag("nav_tab_vault")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.ONBOARDING -> {
                    OnboardingScreen(
                        onGetStarted = {
                            sharedPrefs.edit { putBoolean("onboarding_completed", true) }
                            currentScreen = AppScreen.DASHBOARD
                        }
                    )
                }
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onAddAsset = {
                            editAssetId = 0
                            lastActiveScreen = AppScreen.DASHBOARD
                            currentScreen = AppScreen.ADD_EDIT
                        },
                        onEditAsset = { id ->
                            editAssetId = id
                            lastActiveScreen = AppScreen.DASHBOARD
                            currentScreen = AppScreen.ADD_EDIT
                        },
                        onNavigateToAlerts = {
                            currentScreen = AppScreen.ALERTS
                        }
                    )
                }
                AppScreen.INSIGHTS -> {
                    InsightsScreen(viewModel = viewModel)
                }
                AppScreen.ALERTS -> {
                    AlertsScreen(viewModel = viewModel)
                }
                AppScreen.VAULT -> {
                    VaultScreen(viewModel = viewModel)
                }
                AppScreen.ADD_EDIT -> {
                    AddEditAssetScreen(
                        viewModel = viewModel,
                        assetId = editAssetId,
                        onBack = {
                            currentScreen = lastActiveScreen
                        }
                    )
                }
            }
        }
    }
}
