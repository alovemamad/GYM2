package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.util.JalaliCalendar
import com.example.util.NotificationHelper
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

class ActiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val workoutDao = db.workoutDao()

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _currentPlanId = MutableStateFlow<Long?>(null)
    val currentPlanId: StateFlow<Long?> = _currentPlanId.asStateFlow()

    private val _currentPlanName = MutableStateFlow("")
    val currentPlanName: StateFlow<String> = _currentPlanName.asStateFlow()

    private val _workoutDate = MutableStateFlow(JalaliCalendar.getTodayGregorian())
    val workoutDate: StateFlow<String> = _workoutDate.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _exercisesState = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val exercisesState: StateFlow<List<ActiveExerciseState>> = _exercisesState.asStateFlow()

    // Rest timer state
    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _totalRestSecondsUsed = MutableStateFlow(0L)
    val totalRestSecondsUsed: StateFlow<Long> = _totalRestSecondsUsed.asStateFlow()

    private val _activeExerciseIndex = MutableStateFlow(0)
    val activeExerciseIndex: StateFlow<Int> = _activeExerciseIndex.asStateFlow()

    // Global custom metric mode toggle
    private val _isGlobalCustomMetrics = MutableStateFlow(false)
    val isGlobalCustomMetrics: StateFlow<Boolean> = _isGlobalCustomMetrics.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var exerciseTimerJob: Job? = null

    fun startWorkout(plan: WorkoutPlan, exercises: List<WorkoutExercise>) {
        val today = JalaliCalendar.getTodayGregorian()
        _currentPlanId.value = plan.id
        _currentPlanName.value = plan.name
        _workoutDate.value = today
        _isWorkoutActive.value = true
        _elapsedSeconds.value = 0L
        _totalRestSecondsUsed.value = 0L
        _activeExerciseIndex.value = 0

        val initialStates = exercises.map { ex ->
            val setList = (1..ex.sets).map { sNum ->
                val initialTimer = if (ex.isTimedExercise) (if (ex.reps > 0) ex.reps else 60) else 0
                ActiveSetState(
                    setNumber = sNum,
                    weight = ex.weight,
                    reps = ex.reps,
                    completed = false,
                    timerSecondsRemaining = initialTimer,
                    customValues = mapOf("وزن (kg)" to ex.weight.toString(), "تکرار" to ex.reps.toString(), "زمان (ثانیه)" to initialTimer.toString())
                )
            }
            ActiveExerciseState(
                exercise = ex,
                sets = setList,
                isCustomMetricsMode = !ex.customMetrics.isNullOrBlank()
            )
        }
        _exercisesState.value = initialStates

        startWorkoutTicker()
    }

    private fun startWorkoutTicker() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun toggleGlobalCustomMetrics() {
        _isGlobalCustomMetrics.value = !_isGlobalCustomMetrics.value
    }

    fun setActiveExerciseIndex(index: Int) {
        _activeExerciseIndex.value = index.coerceIn(0, (_exercisesState.value.size - 1).coerceAtLeast(0))
    }

    fun updateSetValues(exerciseIndex: Int, setIndex: Int, weight: Double, reps: Int) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val s = sets.getOrNull(setIndex) ?: return

        sets[setIndex] = s.copy(weight = weight, reps = reps)
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list
    }

    fun updateCustomMetricValue(exerciseIndex: Int, setIndex: Int, metricKey: String, value: String) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val s = sets.getOrNull(setIndex) ?: return

        val map = s.customValues.toMutableMap()
        map[metricKey] = value
        sets[setIndex] = s.copy(customValues = map)
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, defaultRestSeconds: Int = 60) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val s = sets.getOrNull(setIndex) ?: return

        val newCompleted = !s.completed
        sets[setIndex] = s.copy(completed = newCompleted)
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list

        // Immediately persist to WorkoutSetLog in Room
        viewModelScope.launch {
            val customStr = s.customValues.entries.joinToString(";") { "${it.key}:${it.value}" }
            val log = WorkoutSetLog(
                id = s.setLogId,
                date = _workoutDate.value,
                planId = _currentPlanId.value ?: 0L,
                planName = _currentPlanName.value,
                exerciseName = ex.exercise.name,
                setNumber = s.setNumber,
                weight = s.weight,
                reps = s.reps,
                completed = newCompleted,
                timestamp = System.currentTimeMillis(),
                restSecondsUsed = s.restUsedSeconds,
                customMetricValues = customStr
            )
            val insertedId = workoutDao.insertSetLog(log)
            if (s.setLogId == 0L) {
                sets[setIndex] = sets[setIndex].copy(setLogId = insertedId)
                list[exerciseIndex] = ex.copy(sets = sets)
                _exercisesState.value = list
            }
        }

        // Trigger rest timer if set was just completed
        if (newCompleted) {
            startRestTimer(defaultRestSeconds)
        }
    }

    fun addExtraSet(exerciseIndex: Int) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
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
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list
    }

    // Timed exercise controls
    fun toggleExerciseTimer(exerciseIndex: Int, setIndex: Int) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val s = sets.getOrNull(setIndex) ?: return

        val willBeActive = !s.isTimerActive
        val initialDuration = if (s.timerSecondsRemaining <= 0) {
            if (ex.exercise.isTimedExercise && ex.exercise.reps > 0) ex.exercise.reps else 60
        } else {
            s.timerSecondsRemaining
        }
        sets[setIndex] = s.copy(
            isTimerActive = willBeActive,
            timerSecondsRemaining = initialDuration
        )
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list

        exerciseTimerJob?.cancel()
        if (willBeActive) {
            exerciseTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    val curList = _exercisesState.value.toMutableList()
                    val curEx = curList.getOrNull(exerciseIndex) ?: break
                    val curSets = curEx.sets.toMutableList()
                    val curS = curSets.getOrNull(setIndex) ?: break

                    if (!curS.isTimerActive) break
                    if (curS.timerSecondsRemaining > 1) {
                        curSets[setIndex] = curS.copy(timerSecondsRemaining = curS.timerSecondsRemaining - 1)
                        curList[exerciseIndex] = curEx.copy(sets = curSets)
                        _exercisesState.value = curList
                    } else {
                        // Finished timer!
                        curSets[setIndex] = curS.copy(timerSecondsRemaining = 0, isTimerActive = false)
                        curList[exerciseIndex] = curEx.copy(sets = curSets)
                        _exercisesState.value = curList

                        val app = getApplication<Application>()
                        NotificationHelper.playAlarmSoundAndVibrate(app)
                        NotificationHelper.showNotification(
                            context = app,
                            notificationId = 888,
                            channelId = NotificationHelper.CHANNEL_REST_TIMER,
                            title = "پایان حرکت ${curEx.exercise.name}!",
                            message = "زمان این ست به پایان رسید. آفرین، ست با موفقیت ثبت شد!"
                        )

                        completeSet(exerciseIndex, setIndex)
                        break
                    }
                }
            }
        }
    }

    fun updateTimedDuration(exerciseIndex: Int, setIndex: Int, seconds: Int) {
        val list = _exercisesState.value.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val s = sets.getOrNull(setIndex) ?: return
        val newSec = seconds.coerceIn(5, 600)
        sets[setIndex] = s.copy(timerSecondsRemaining = newSec, reps = newSec)
        list[exerciseIndex] = ex.copy(sets = sets)
        _exercisesState.value = list
    }

    // Rest timer
    fun startRestTimer(seconds: Int) {
        _restSecondsRemaining.value = seconds
        _isRestTimerActive.value = true

        val app = getApplication<Application>()
        ReminderScheduler.scheduleRestTimer(app, seconds.toLong())

        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restSecondsRemaining.value > 0 && _isRestTimerActive.value) {
                delay(1000)
                _restSecondsRemaining.value -= 1
                _totalRestSecondsUsed.value += 1
            }
            _isRestTimerActive.value = false
            val app = getApplication<Application>()
            NotificationHelper.playAlarmSoundAndVibrate(app)
        }
    }

    fun addRestTime(secondsToAdd: Int = 15) {
        _restSecondsRemaining.value += secondsToAdd
    }

    fun cancelRestTimer() {
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
        restTimerJob?.cancel()
        ReminderScheduler.cancelReminder(getApplication(), ReminderScheduler.ID_REST_TIMER)
    }

    fun finishWorkout(onFinished: () -> Unit) {
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

            workoutDao.insertHistories(summaries)
            workoutDao.clearSetLogsForDateAndPlan(date, planId)

            _isWorkoutActive.value = false
            workoutTimerJob?.cancel()
            cancelRestTimer()

            onFinished()
        }
    }

    fun cancelAndResetWorkout() {
        viewModelScope.launch {
            val date = _workoutDate.value
            val planId = _currentPlanId.value ?: 0L
            workoutDao.clearSetLogsForDateAndPlan(date, planId)
            _isWorkoutActive.value = false
            workoutTimerJob?.cancel()
            cancelRestTimer()
        }
    }
}
