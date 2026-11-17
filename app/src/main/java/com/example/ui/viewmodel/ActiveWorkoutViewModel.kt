package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.util.AlertType
import com.example.util.JalaliCalendar
import com.example.util.NotificationHelper
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveExerciseState(
    val exercise: WorkoutExercise,
    val sets: List<ActiveSetState>,
    val isCustomMetricsMode: Boolean = false,
    val activeCustomMetrics: List<String> = listOf("وزن (kg)", "تکرار", "زمان (ثانیه)")
)

data class ActiveSetState(
    val setLogId: Long = 0,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val completed: Boolean = false,
    val restUsedSeconds: Long = 0,
    val customValues: Map<String, String> = emptyMap(),
    // Timed exercise timer
    val timerSecondsRemaining: Int = 0,
    val isTimerActive: Boolean = false
)

object ActiveWorkoutRestoreHelper {
    fun parseCustomMetrics(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val result = linkedMapOf<String, String>()
        raw.split(";").forEach { entry ->
            val idx = entry.indexOf(':')
            if (idx > 0) {
                val k = entry.substring(0, idx).trim()
                val v = entry.substring(idx + 1).trim()
                if (k.isNotEmpty()) result[k] = v
            }
        }
        return result
    }

    fun buildExerciseStatesWithLogs(
        exercises: List<WorkoutExercise>,
        savedLogs: List<WorkoutSetLog>
    ): List<ActiveExerciseState> {
        val logsByExercise = savedLogs.groupBy { it.exerciseName }
        return exercises.map { ex ->
            val exLogs = logsByExercise[ex.name].orEmpty().sortedBy { it.setNumber }
            val maxSets = maxOf(ex.sets.coerceAtLeast(1), exLogs.maxOfOrNull { it.setNumber } ?: 0)
            val setList = (1..maxSets).map { sNum ->
                val saved = exLogs.firstOrNull { it.setNumber == sNum }
                if (saved != null) {
                    val initialTimer = if (ex.isTimedExercise) {
                        if (saved.reps > 0) saved.reps else 60
                    } else 0
                    val parsedCustom = parseCustomMetrics(saved.customMetricValues.orEmpty()).ifEmpty {
                        mapOf(
                            "وزن (kg)" to saved.weight.toString(),
                            "تکرار" to saved.reps.toString(),
                            "زمان (ثانیه)" to initialTimer.toString()
                        )
                    }
                    ActiveSetState(
                        setLogId = saved.id,
                        setNumber = sNum,
                        weight = saved.weight,
                        reps = saved.reps,
                        completed = saved.completed,
                        restUsedSeconds = saved.restSecondsUsed,
                        customValues = parsedCustom,
                        timerSecondsRemaining = initialTimer
                    )
                } else {
                    val initialTimer = if (ex.isTimedExercise) (if (ex.reps > 0) ex.reps else 60) else 0
                    ActiveSetState(
                        setNumber = sNum,
                        weight = ex.weight,
                        reps = ex.reps,
                        completed = false,
                        timerSecondsRemaining = initialTimer,
                        customValues = mapOf(
                            "وزن (kg)" to ex.weight.toString(),
                            "تکرار" to ex.reps.toString(),
                            "زمان (ثانیه)" to initialTimer.toString()
                        )
                    )
                }
            }
            ActiveExerciseState(
                exercise = ex,
                sets = setList,
                isCustomMetricsMode = !ex.customMetrics.isNullOrBlank()
            )
        }
    }
}

class ActiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val PREFS_NAME = "gym_active_session"
        const val KEY_IS_ACTIVE = "is_active"
        const val KEY_PLAN_ID = "plan_id"
        const val KEY_PLAN_NAME = "plan_name"
        const val KEY_WORKOUT_DATE = "workout_date"
        const val KEY_STARTED_AT_MS = "started_at_ms"
        const val KEY_TOTAL_REST_USED = "total_rest_used"
        const val KEY_REST_END_MS = "rest_end_ms"
    }

    private val db = AppDatabase.getInstance(application)
    private val workoutDao = db.workoutDao()
    private val sessionPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val initialActive = sessionPrefs.getBoolean(KEY_IS_ACTIVE, false)
    private val initialPlanId = sessionPrefs.getLong(KEY_PLAN_ID, -1L).takeIf { initialActive && it > 0L }
    private val initialPlanName = if (initialPlanId != null) (sessionPrefs.getString(KEY_PLAN_NAME, "") ?: "") else ""
    private val initialDate = if (initialPlanId != null) {
        sessionPrefs.getString(KEY_WORKOUT_DATE, JalaliCalendar.getTodayGregorian()) ?: JalaliCalendar.getTodayGregorian()
    } else {
        JalaliCalendar.getTodayGregorian()
    }
    private var sessionStartedAtMs: Long = if (initialPlanId != null) {
        sessionPrefs.getLong(KEY_STARTED_AT_MS, System.currentTimeMillis()).takeIf { it > 0L } ?: System.currentTimeMillis()
    } else 0L

    private val _isWorkoutActive = MutableStateFlow(initialPlanId != null)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _currentPlanId = MutableStateFlow<Long?>(initialPlanId)
    val currentPlanId: StateFlow<Long?> = _currentPlanId.asStateFlow()

    private val _currentPlanName = MutableStateFlow(initialPlanName)
    val currentPlanName: StateFlow<String> = _currentPlanName.asStateFlow()

    private val _workoutDate = MutableStateFlow(initialDate)
    val workoutDate: StateFlow<String> = _workoutDate.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(
        if (initialPlanId != null) ((System.currentTimeMillis() - sessionStartedAtMs) / 1000L).coerceAtLeast(0L) else 0L
    )
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _exercisesState = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val exercisesState: StateFlow<List<ActiveExerciseState>> = _exercisesState.asStateFlow()

    // Rest timer state
    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _totalRestSecondsUsed = MutableStateFlow(
        if (initialPlanId != null) sessionPrefs.getLong(KEY_TOTAL_REST_USED, 0L) else 0L
    )
    val totalRestSecondsUsed: StateFlow<Long> = _totalRestSecondsUsed.asStateFlow()

    private val _activeExerciseIndex = MutableStateFlow(0)
    val activeExerciseIndex: StateFlow<Int> = _activeExerciseIndex.asStateFlow()

    // Global custom metric mode toggle
    private val _isGlobalCustomMetrics = MutableStateFlow(false)
    val isGlobalCustomMetrics: StateFlow<Boolean> = _isGlobalCustomMetrics.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var exerciseTimerJob: Job? = null
    private val pendingSaveJobs = mutableMapOf<Pair<Int, Int>, Job>()

    init {
        if (initialPlanId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                restoreActiveSession(initialPlanId, initialDate)
            }
        }
    }

    private suspend fun restoreActiveSession(planId: Long, date: String) {
        try {
            val plan = workoutDao.getPlanById(planId)
            val exercises = workoutDao.getExercisesForPlanSync(planId)
            if (plan == null || exercises.isEmpty()) {
                clearSessionPrefs()
                _isWorkoutActive.value = false
                _currentPlanId.value = null
                return
            }
            val savedLogs = workoutDao.getSetLogsForDateAndPlan(date, planId).first()
            _currentPlanName.value = plan.name
            _exercisesState.value = ActiveWorkoutRestoreHelper.buildExerciseStatesWithLogs(exercises, savedLogs)
            startWorkoutTicker()

            val savedRestEndMs = sessionPrefs.getLong(KEY_REST_END_MS, 0L)
            val remSec = ((savedRestEndMs - System.currentTimeMillis()) / 1000L).toInt()
            if (remSec > 0) {
                launchRestCountdown(remSec)
            }
        } catch (_: Exception) {
        }
    }

    private fun saveSessionSnapshot() {
        sessionPrefs.edit()
            .putBoolean(KEY_IS_ACTIVE, _isWorkoutActive.value)
            .putLong(KEY_PLAN_ID, _currentPlanId.value ?: -1L)
            .putString(KEY_PLAN_NAME, _currentPlanName.value)
            .putString(KEY_WORKOUT_DATE, _workoutDate.value)
            .putLong(KEY_STARTED_AT_MS, sessionStartedAtMs)
            .putLong(KEY_TOTAL_REST_USED, _totalRestSecondsUsed.value)
            .apply()
    }

    private fun saveRestEndMillis(restEndMs: Long) {
        sessionPrefs.edit().putLong(KEY_REST_END_MS, restEndMs).apply()
    }

    private fun clearSessionPrefs() {
        sessionPrefs.edit().clear().apply()
    }

    suspend fun startWorkout(plan: WorkoutPlan, exercises: List<WorkoutExercise>) {
        val today = JalaliCalendar.getTodayGregorian()

        // اگر همین برنامه امروز هم‌اکنون فعال است، هرگز پیشرفت فعلی ریست نشود
        if (_isWorkoutActive.value &&
            _currentPlanId.value == plan.id &&
            _workoutDate.value == today &&
            _exercisesState.value.isNotEmpty()
        ) {
            if (workoutTimerJob?.isActive != true) {
                startWorkoutTicker()
            }
            return
        }

        // اگر از قبل ست‌های ذخیره‌شده برای امروز و این برنامه در دیتابیس وجود دارد، آن‌ها را بازیابی کن
        val savedLogs = withContext(Dispatchers.IO) {
            workoutDao.getSetLogsForDateAndPlan(today, plan.id).first()
        }

        val reuseStart = sessionPrefs.getBoolean(KEY_IS_ACTIVE, false) &&
            sessionPrefs.getLong(KEY_PLAN_ID, -1L) == plan.id &&
            sessionPrefs.getString(KEY_WORKOUT_DATE, "") == today &&
            sessionPrefs.getLong(KEY_STARTED_AT_MS, 0L) > 0L

        sessionStartedAtMs = if (reuseStart) {
            sessionPrefs.getLong(KEY_STARTED_AT_MS, System.currentTimeMillis())
        } else {
            System.currentTimeMillis()
        }

        _currentPlanId.value = plan.id
        _currentPlanName.value = plan.name
        _workoutDate.value = today
        _isWorkoutActive.value = true
        _elapsedSeconds.value = ((System.currentTimeMillis() - sessionStartedAtMs) / 1000L).coerceAtLeast(0L)
        if (!reuseStart) {
            _totalRestSecondsUsed.value = 0L
        }
        _activeExerciseIndex.value = 0
        _exercisesState.value = ActiveWorkoutRestoreHelper.buildExerciseStatesWithLogs(exercises, savedLogs)

        saveSessionSnapshot()
        startWorkoutTicker()
    }

    private fun startWorkoutTicker() {
        workoutTimerJob?.cancel()
        if (sessionStartedAtMs <= 0L) {
            sessionStartedAtMs = System.currentTimeMillis() - (_elapsedSeconds.value * 1000L)
        }
        workoutTimerJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                _elapsedSeconds.value = ((System.currentTimeMillis() - sessionStartedAtMs) / 1000L).coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    fun toggleGlobalCustomMetrics() {
        _isGlobalCustomMetrics.value = !_isGlobalCustomMetrics.value
    }

    fun setActiveExerciseIndex(index: Int) {
        _activeExerciseIndex.value = index.coerceIn(0, (_exercisesState.value.size - 1).coerceAtLeast(0))
    }

    private fun scheduleSetSnapshotSave(exerciseIndex: Int, setIndex: Int) {
        val key = exerciseIndex to setIndex
        pendingSaveJobs[key]?.cancel()
        pendingSaveJobs[key] = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            persistSetImmediate(exerciseIndex, setIndex)
        }
    }

    private suspend fun persistSetImmediate(exerciseIndex: Int, setIndex: Int) {
        val exState = _exercisesState.value.getOrNull(exerciseIndex) ?: return
        val s = exState.sets.getOrNull(setIndex) ?: return
        val customStr = s.customValues.entries.joinToString(";") { "${it.key}:${it.value}" }
        val log = WorkoutSetLog(
            id = s.setLogId,
            date = _workoutDate.value,
            planId = _currentPlanId.value ?: 0L,
            planName = _currentPlanName.value,
            exerciseName = exState.exercise.name,
            setNumber = s.setNumber,
            weight = s.weight,
            reps = s.reps,
            completed = s.completed,
            timestamp = System.currentTimeMillis(),
            restSecondsUsed = s.restUsedSeconds,
            customMetricValues = customStr
        )
        val insertedId = workoutDao.insertSetLog(log)
        if (s.setLogId == 0L && insertedId > 0L) {
            _exercisesState.update { currentList ->
                val curEx = currentList.getOrNull(exerciseIndex) ?: return@update currentList
                val curSet = curEx.sets.getOrNull(setIndex) ?: return@update currentList
                if (curSet.setLogId == insertedId) return@update currentList
                val newSets = curEx.sets.toMutableList()
                newSets[setIndex] = curSet.copy(setLogId = insertedId)
                val newList = currentList.toMutableList()
                newList[exerciseIndex] = curEx.copy(sets = newSets)
                newList
            }
        }
    }

    fun updateSetValues(exerciseIndex: Int, setIndex: Int, weight: Double, reps: Int) {
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val s = ex.sets.getOrNull(setIndex) ?: return@update list
            val sets = ex.sets.toMutableList()
            sets[setIndex] = s.copy(weight = weight, reps = reps)
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }
        scheduleSetSnapshotSave(exerciseIndex, setIndex)
    }

    fun updateCustomMetricValue(exerciseIndex: Int, setIndex: Int, metricKey: String, value: String) {
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val s = ex.sets.getOrNull(setIndex) ?: return@update list
            val map = s.customValues.toMutableMap()
            map[metricKey] = value
            val sets = ex.sets.toMutableList()
            sets[setIndex] = s.copy(customValues = map)
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }
        scheduleSetSnapshotSave(exerciseIndex, setIndex)
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, defaultRestSeconds: Int = 60) {
        var justCompleted = false
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val s = ex.sets.getOrNull(setIndex) ?: return@update list
            val newCompleted = !s.completed
            justCompleted = newCompleted
            val sets = ex.sets.toMutableList()
            sets[setIndex] = s.copy(completed = newCompleted)
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }

        val key = exerciseIndex to setIndex
        pendingSaveJobs[key]?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            persistSetImmediate(exerciseIndex, setIndex)
        }

        if (justCompleted) {
            startRestTimer(defaultRestSeconds)
        }
    }

    fun addExtraSet(exerciseIndex: Int) {
        var addedSetIndex = -1
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val sets = ex.sets.toMutableList()
            val lastSet = sets.lastOrNull()
            val newSetNum = sets.size + 1
            sets.add(
                ActiveSetState(
                    setNumber = newSetNum,
                    weight = lastSet?.weight ?: ex.exercise.weight,
                    reps = lastSet?.reps ?: ex.exercise.reps,
                    completed = false,
                    customValues = lastSet?.customValues ?: emptyMap()
                )
            )
            addedSetIndex = sets.lastIndex
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }
        if (addedSetIndex >= 0) {
            scheduleSetSnapshotSave(exerciseIndex, addedSetIndex)
        }
    }

    // Timed exercise controls
    fun toggleExerciseTimer(exerciseIndex: Int, setIndex: Int) {
        var willBeActive = false
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val s = ex.sets.getOrNull(setIndex) ?: return@update list
            willBeActive = !s.isTimerActive
            val initialDuration = if (s.timerSecondsRemaining <= 0) {
                if (ex.exercise.isTimedExercise && ex.exercise.reps > 0) ex.exercise.reps else 60
            } else {
                s.timerSecondsRemaining
            }
            val sets = ex.sets.toMutableList()
            sets[setIndex] = s.copy(
                isTimerActive = willBeActive,
                timerSecondsRemaining = initialDuration
            )
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }

        exerciseTimerJob?.cancel()
        if (willBeActive) {
            exerciseTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    var finishedNow = false
                    var finishedExName = ""
                    var stillRunning = false

                    _exercisesState.update { curList ->
                        val curEx = curList.getOrNull(exerciseIndex) ?: return@update curList
                        val curS = curEx.sets.getOrNull(setIndex) ?: return@update curList
                        if (!curS.isTimerActive) return@update curList

                        val curSets = curEx.sets.toMutableList()
                        if (curS.timerSecondsRemaining > 1) {
                            curSets[setIndex] = curS.copy(timerSecondsRemaining = curS.timerSecondsRemaining - 1)
                            stillRunning = true
                        } else {
                            curSets[setIndex] = curS.copy(timerSecondsRemaining = 0, isTimerActive = false)
                            finishedNow = true
                            finishedExName = curEx.exercise.name
                        }
                        val newList = curList.toMutableList()
                        newList[exerciseIndex] = curEx.copy(sets = curSets)
                        newList
                    }

                    if (finishedNow) {
                        val app = getApplication<Application>()
                        NotificationHelper.playAlarmSoundAndVibrate(app, AlertType.EXERCISE_END)
                        NotificationHelper.showNotification(
                            context = app,
                            notificationId = 888,
                            channelId = NotificationHelper.CHANNEL_EXERCISE_END,
                            title = "پایان حرکت $finishedExName!",
                            message = "زمان این ست به پایان رسید. آفرین، ست با موفقیت ثبت شد!",
                            silent = true
                        )
                        completeSet(exerciseIndex, setIndex)
                        break
                    }
                    if (!stillRunning) break
                }
            }
        }
    }

    fun updateTimedDuration(exerciseIndex: Int, setIndex: Int, seconds: Int) {
        val newSec = seconds.coerceIn(5, 600)
        _exercisesState.update { list ->
            val ex = list.getOrNull(exerciseIndex) ?: return@update list
            val s = ex.sets.getOrNull(setIndex) ?: return@update list
            val sets = ex.sets.toMutableList()
            sets[setIndex] = s.copy(timerSecondsRemaining = newSec, reps = newSec)
            val newList = list.toMutableList()
            newList[exerciseIndex] = ex.copy(sets = sets)
            newList
        }
        scheduleSetSnapshotSave(exerciseIndex, setIndex)
    }

    // Rest timer
    fun startRestTimer(seconds: Int) {
        val app = getApplication<Application>()
        ReminderScheduler.scheduleRestTimer(app, seconds.toLong())
        saveRestEndMillis(System.currentTimeMillis() + seconds * 1000L)
        launchRestCountdown(seconds)
    }

    private fun launchRestCountdown(seconds: Int) {
        _restSecondsRemaining.value = seconds
        _isRestTimerActive.value = true

        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restSecondsRemaining.value > 0 && _isRestTimerActive.value) {
                delay(1000)
                _restSecondsRemaining.value -= 1
                _totalRestSecondsUsed.value += 1
                if (_restSecondsRemaining.value % 5 == 0) {
                    saveSessionSnapshot()
                }
            }
            val finishedNaturally = _isRestTimerActive.value && _restSecondsRemaining.value <= 0
            _isRestTimerActive.value = false
            saveRestEndMillis(0L)
            if (finishedNaturally) {
                val app = getApplication<Application>()
                ReminderScheduler.cancelReminder(app, ReminderScheduler.ID_REST_TIMER)
                NotificationHelper.playAlarmSoundAndVibrate(app, AlertType.REST_END)
            }
        }
    }

    fun addRestTime(secondsToAdd: Int = 15) {
        _restSecondsRemaining.value += secondsToAdd
        if (_isRestTimerActive.value) {
            saveRestEndMillis(System.currentTimeMillis() + _restSecondsRemaining.value * 1000L)
            ReminderScheduler.scheduleRestTimer(getApplication(), _restSecondsRemaining.value.toLong())
        }
    }

    fun cancelRestTimer() {
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
        restTimerJob?.cancel()
        saveRestEndMillis(0L)
        ReminderScheduler.cancelReminder(getApplication(), ReminderScheduler.ID_REST_TIMER)
    }

    fun finishWorkout(onFinished: () -> Unit) {
        pendingSaveJobs.values.forEach { it.cancel() }
        pendingSaveJobs.clear()

        viewModelScope.launch {
            val date = _workoutDate.value
            val planName = _currentPlanName.value
            val planId = _currentPlanId.value ?: 0L
            val duration = _elapsedSeconds.value
            val totalRest = _totalRestSecondsUsed.value

            val summaries = mutableListOf<WorkoutHistory>()
            _exercisesState.value.forEach { exState ->
                val completedSets = exState.sets.filter { it.completed }
                if (completedSets.isNotEmpty()) {
                    val avgReps = (completedSets.map { it.reps }.average()).toInt()
                    val maxWeight = completedSets.maxOfOrNull { it.weight } ?: 0.0
                    val customDataSummary = completedSets.mapIndexed { idx, s ->
                        "ست ${idx + 1}: " + s.customValues.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    }.joinToString(" | ")

                    summaries.add(
                        WorkoutHistory(
                            date = date,
                            planName = planName,
                            exerciseName = exState.exercise.name,
                            sets = completedSets.size,
                            reps = avgReps,
                            weight = maxWeight,
                            notes = exState.exercise.notes,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = duration,
                            totalRestSecondsUsed = totalRest,
                            customData = if (customDataSummary.isNotBlank()) customDataSummary else null
                        )
                    )
                }
            }

            withContext(Dispatchers.IO) {
                workoutDao.insertHistories(summaries)
                workoutDao.clearSetLogsForDateAndPlan(date, planId)
            }

            clearSessionPrefs()
            _isWorkoutActive.value = false
            _currentPlanId.value = null
            _currentPlanName.value = ""
            _exercisesState.value = emptyList()
            workoutTimerJob?.cancel()
            exerciseTimerJob?.cancel()
            cancelRestTimer()

            onFinished()
        }
    }

    fun cancelAndResetWorkout() {
        pendingSaveJobs.values.forEach { it.cancel() }
        pendingSaveJobs.clear()

        viewModelScope.launch {
            val date = _workoutDate.value
            val planId = _currentPlanId.value ?: 0L
            withContext(Dispatchers.IO) {
                workoutDao.clearSetLogsForDateAndPlan(date, planId)
            }
            clearSessionPrefs()
            _isWorkoutActive.value = false
            _currentPlanId.value = null
            _currentPlanName.value = ""
            _exercisesState.value = emptyList()
            workoutTimerJob?.cancel()
            exerciseTimerJob?.cancel()
            cancelRestTimer()
        }
    }
}
