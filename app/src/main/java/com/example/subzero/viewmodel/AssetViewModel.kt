package com.example.subzero.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subzero.alarm.AlarmScheduler
import com.example.subzero.data.*
import com.example.subzero.utils.EncryptionHelper
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AssetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AssetRepository(database.assetDao())
    private val sharedPrefs = application.getSharedPreferences("subzero_prefs", Context.MODE_PRIVATE)

    // AI Advisor State
    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Reactive list of all assets
    val assets: StateFlow<List<Asset>> = repository.allAssets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Preferences
    private val _currency = MutableStateFlow(sharedPrefs.getString("currency", "USD") ?: "USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isBiometricRequired = MutableStateFlow(sharedPrefs.getBoolean("biometric_required", false))
    val isBiometricRequired: StateFlow<Boolean> = _isBiometricRequired.asStateFlow()

    private val _isAppLocked = MutableStateFlow(sharedPrefs.getBoolean("biometric_required", false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isAutoLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_lock_enabled", false))
    val isAutoLockEnabled: StateFlow<Boolean> = _isAutoLockEnabled.asStateFlow()

    private val _isLocalNotificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("local_notifications", true))
    val isLocalNotificationsEnabled: StateFlow<Boolean> = _isLocalNotificationsEnabled.asStateFlow()

    private val _notificationHour = MutableStateFlow(sharedPrefs.getInt("notification_hour", 9))
    val notificationHour: StateFlow<Int> = _notificationHour.asStateFlow()

    private val _notificationMinute = MutableStateFlow(sharedPrefs.getInt("notification_minute", 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute.asStateFlow()

    private val _defaultNudgeWindows = MutableStateFlow(
        sharedPrefs.getString("default_nudges", "1,3,7")
            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: listOf(1, 3, 7)
    )
    val defaultNudgeWindows: StateFlow<List<Int>> = _defaultNudgeWindows.asStateFlow()

    // Analytics Metrics
    val monthlyDrain: StateFlow<Double> = assets.map { list ->
        calculateMonthlyDrain(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val annualDrain: StateFlow<Double> = monthlyDrain.map { monthly ->
        monthly * 12.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeTrialsCount: StateFlow<Int> = assets.map { list ->
        list.count { it.isTrial && !it.isCanceled && (it.nextBillingDate > System.currentTimeMillis()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAssetsCount: StateFlow<Int> = assets.map { list ->
        list.count { !it.isCanceled }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // AI Advice Generator
    fun fetchAiAdvisorAdvice() {
        val activeAssets = assets.value.filter { !it.isCanceled }
        if (activeAssets.isEmpty()) {
            _aiAdvice.value = "Add some subscriptions to get personalized financial advice!"
            return
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
                val prompt = """
                    You are SubZero, a financial advisor for subscription tracking. 
                    Analyze the following user subscriptions and provide 3 concise bullet points 
                    on how they can optimize their monthly spend. Keep it punchy and professional.
                    
                    Subscriptions:
                    ${activeAssets.joinToString("\n") { "- ${it.name} (${it.category.displayName}): ${it.cost}/${it.billingCycle}" }}
                    
                    Total Monthly Drain: ${monthlyDrain.value}
                """.trimIndent()

                val response = model.generateContent(prompt)
                _aiAdvice.value = response.text
            } catch (e: Exception) {
                _aiAdvice.value = "SubZero Advisor is currently offline. Check your connection or API limits."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Subscription Fatigue & Overlaps Analysis
    data class OverlapResult(
        val bundleName: String,
        val standaloneName: String,
        val overlappingService: String,
        val potentialSavings: Double
    )

    val overlaps: StateFlow<List<OverlapResult>> = assets.map { list ->
        val results = mutableListOf<OverlapResult>()
        val activeAssets = list.filter { !it.isCanceled }
        val bundles = activeAssets.filter { it.category == AssetCategory.BUNDLE }
        val standaloneItems = activeAssets.filter { it.category != AssetCategory.BUNDLE && it.serviceTag.isNotEmpty() }

        for (bundle in bundles) {
            for (service in bundle.nestedServices) {
                val overlapping = standaloneItems.filter { 
                    it.serviceTag.equals(service, ignoreCase = true) 
                }
                for (item in overlapping) {
                    results.add(
                        OverlapResult(
                            bundleName = bundle.name,
                            standaloneName = item.name,
                            overlappingService = service,
                            potentialSavings = item.cost
                        )
                    )
                }
            }
        }
        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPotentialSavings: StateFlow<Double> = overlaps.map { list ->
        list.sumOf { it.potentialSavings }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Distribution (for charts/breakdown)
    data class CategoryBreakdown(
        val category: AssetCategory,
        val amount: Double,
        val percentage: Float
    )

    val categoryBreakdowns: StateFlow<List<CategoryBreakdown>> = assets.map { list ->
        val activeList = list.filter { !it.isCanceled }
        val total = calculateMonthlyDrain(activeList)
        
        if (total <= 0) {
            AssetCategory.values().map { CategoryBreakdown(it, 0.0, 0f) }
        } else {
            AssetCategory.values().map { cat ->
                val catAmount = calculateMonthlyDrain(activeList.filter { it.category == cat })
                val pct = if (total > 0) (catAmount / total).toFloat() else 0f
                CategoryBreakdown(cat, catAmount, pct)
            }.filter { it.amount > 0 }.sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRUD database actions
    fun addAsset(asset: Asset) {
        viewModelScope.launch {
            val finalAsset = if (asset.isTrial) {
                val trialDurationMs = asset.trialLengthDays.toLong() * 24L * 60L * 60L * 1000L
                asset.copy(nextBillingDate = asset.startDate + trialDurationMs)
            } else {
                asset
            }
            val id = repository.insertAsset(finalAsset)
            val savedAsset = finalAsset.copy(id = id.toInt())
            if (_isLocalNotificationsEnabled.value) {
                AlarmScheduler.scheduleAlarmsForAsset(getApplication(), savedAsset)
            }
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
            if (_isLocalNotificationsEnabled.value) {
                AlarmScheduler.scheduleAlarmsForAsset(getApplication(), asset)
            } else {
                AlarmScheduler.cancelAlarmsForAsset(getApplication(), asset)
            }
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarmsForAsset(getApplication(), asset)
            repository.deleteAsset(asset)
        }
    }

    fun markAsRenewed(asset: Asset) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply { timeInMillis = asset.nextBillingDate }
            when (asset.billingCycle) {
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
            }
            val updatedAsset = asset.copy(
                nextBillingDate = calendar.timeInMillis,
                lastRenewedTimestamp = System.currentTimeMillis()
            )
            repository.updateAsset(updatedAsset)
            if (_isLocalNotificationsEnabled.value) {
                AlarmScheduler.scheduleAlarmsForAsset(getApplication(), updatedAsset)
            }
        }
    }

    fun toggleCanceledStatus(asset: Asset) {
        viewModelScope.launch {
            val updatedAsset = asset.copy(isCanceled = !asset.isCanceled)
            repository.updateAsset(updatedAsset)
            if (updatedAsset.isCanceled) {
                AlarmScheduler.cancelAlarmsForAsset(getApplication(), updatedAsset)
            } else {
                if (_isLocalNotificationsEnabled.value) {
                    AlarmScheduler.scheduleAlarmsForAsset(getApplication(), updatedAsset)
                }
            }
        }
    }

    // Settings Modification Actions
    fun setCurrency(newCurrency: String) {
        _currency.value = newCurrency
        sharedPrefs.edit().putString("currency", newCurrency).apply()
    }

    fun setThemeMode(newThemeMode: String) {
        _themeMode.value = newThemeMode
        sharedPrefs.edit().putString("theme_mode", newThemeMode).apply()
    }

    fun setBiometricRequired(required: Boolean) {
        _isBiometricRequired.value = required
        sharedPrefs.edit().putBoolean("biometric_required", required).apply()
        if (!required) {
            _isAppLocked.value = false
        }
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        _isAutoLockEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_lock_enabled", enabled).apply()
    }

    fun setLocalNotificationsEnabled(enabled: Boolean) {
        _isLocalNotificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("local_notifications", enabled).apply()
        viewModelScope.launch {
            val currentAssets = assets.value
            for (asset in currentAssets) {
                if (enabled) {
                    AlarmScheduler.scheduleAlarmsForAsset(getApplication(), asset)
                } else {
                    AlarmScheduler.cancelAlarmsForAsset(getApplication(), asset)
                }
            }
        }
    }

    fun setDefaultNudgeWindows(nudges: List<Int>) {
        _defaultNudgeWindows.value = nudges
        sharedPrefs.edit().putString("default_nudges", nudges.joinToString(",")).apply()
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        _notificationHour.value = hour
        _notificationMinute.value = minute
        sharedPrefs.edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .apply()
        viewModelScope.launch {
            if (_isLocalNotificationsEnabled.value) {
                val currentAssets = assets.value
                for (asset in currentAssets) {
                    AlarmScheduler.scheduleAlarmsForAsset(getApplication(), asset)
                }
            }
        }
    }

    // Helper functions for calculation
    private fun calculateMonthlyDrain(list: List<Asset>): Double {
        return list.filter { !it.isCanceled && it.category != AssetCategory.GIFT_CARD }.sumOf { asset ->
            if (asset.isVariable) {
                asset.cost
            } else {
                when (asset.billingCycle) {
                    "Monthly" -> asset.cost
                    "Yearly" -> asset.cost / 12.0
                    "Weekly" -> asset.cost * 4.33
                    "Quarterly" -> asset.cost / 3.0
                    "One-time" -> 0.0
                    else -> asset.cost
                }
            }
        }
    }

    // Import/Export Engine
    fun exportBackupJson(password: String = ""): String {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val listType = Types.newParameterizedType(List::class.java, Asset::class.java)
        val adapter = moshi.adapter<List<Asset>>(listType)
        val plainJson = adapter.toJson(assets.value)
        val finalPassword = if (password.isEmpty()) "subzero_secure_backup" else password
        return EncryptionHelper.encrypt(plainJson, finalPassword)
    }

    fun exportBackupCsv(): String {
        val sb = StringBuilder()
        sb.append("ID,Name,Category,Cost,BillingCycle,IsVariable,IsCanceled,Notes,ServiceTag,NextBillingDate,RemainingBalance\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (asset in assets.value) {
            val nextDateStr = sdf.format(Date(asset.nextBillingDate))
            sb.append("${asset.id},")
                .append("\"${asset.name.replace("\"", "\"\"")}\",")
                .append("${asset.category.name},")
                .append("${asset.cost},")
                .append("${asset.billingCycle},")
                .append("${asset.isVariable},")
                .append("${asset.isCanceled},")
                .append("\"${asset.notes.replace("\"", "\"\"")}\",")
                .append("\"${asset.serviceTag}\",")
                .append("$nextDateStr,")
                .append("${asset.remainingBalance}\n")
        }
        return sb.toString()
    }

    fun importBackupJson(inputString: String, password: String = ""): Boolean {
        return try {
            val trimmedInput = inputString.trim()
            val jsonToParse = if (trimmedInput.startsWith("[")) {
                trimmedInput
            } else {
                val finalPassword = if (password.isEmpty()) "subzero_secure_backup" else password
                EncryptionHelper.decrypt(trimmedInput, finalPassword)
            }
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, Asset::class.java)
            val adapter = moshi.adapter<List<Asset>>(listType)
            val importedList = adapter.fromJson(jsonToParse) ?: return false
            viewModelScope.launch {
                for (asset in importedList) {
                    val cleanAsset = asset.copy(id = 0)
                    repository.insertAsset(cleanAsset)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
