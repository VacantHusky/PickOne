package com.playdice.pickone.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playdice.pickone.data.SceneRepository
import com.playdice.pickone.data.SettingsRepository
import com.playdice.pickone.data.AppUpdate
import com.playdice.pickone.data.UpdateChecker
import com.playdice.pickone.data.AppUpdateDownloader
import com.playdice.pickone.domain.SelectionEngine
import com.playdice.pickone.domain.SoundEffectPlayer
import com.playdice.pickone.model.AppLanguage
import com.playdice.pickone.model.AppSettings
import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.HomeLayout
import com.playdice.pickone.model.Scene
import com.playdice.pickone.model.SelectionOutcome
import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.model.SoundChoice
import com.playdice.pickone.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val scenes: SceneRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker,
    private val updateDownloader: AppUpdateDownloader,
) : ViewModel() {
    private val _availableUpdate = MutableStateFlow<AppUpdate?>(null)
    val availableUpdate: StateFlow<AppUpdate?> = _availableUpdate.asStateFlow()
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()
    private val _downloadedUpdate = MutableStateFlow<java.io.File?>(null)
    val downloadedUpdate: StateFlow<java.io.File?> = _downloadedUpdate.asStateFlow()
    private val _downloadError = MutableStateFlow(false)
    val downloadError: StateFlow<Boolean> = _downloadError.asStateFlow()
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    init {
        viewModelScope.launch {
            if (!settingsRepository.settings.first().seeded) {
                scenes.seedIfEmpty(defaultScenes())
                settingsRepository.markSeeded()
            }
        }
        viewModelScope.launch {
            _availableUpdate.value = updateChecker.check()
        }
    }

    fun dismissUpdate() {
        if (_downloadProgress.value != null) return
        _availableUpdate.value = null
    }

    fun downloadUpdate() {
        val update = _availableUpdate.value ?: return
        if (_downloadProgress.value != null) return
        viewModelScope.launch {
            _downloadError.value = false
            _downloadProgress.value = 0
            runCatching { updateDownloader.download(update) { progress -> _downloadProgress.value = progress } }
                .onSuccess { file ->
                    _downloadProgress.value = null
                    _downloadedUpdate.value = file
                }
                .onFailure {
                    _downloadProgress.value = null
                    _downloadError.value = true
                }
        }
    }

    fun clearDownloadedUpdate() {
        _downloadedUpdate.value = null
        _availableUpdate.value = null
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sceneRepository: SceneRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val scenes: StateFlow<List<Scene>> = sceneRepository.scenes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    fun setLayout(layout: HomeLayout) = viewModelScope.launch { settingsRepository.setHomeLayout(layout) }
    fun delete(id: String) = viewModelScope.launch { sceneRepository.delete(id) }
    fun duplicate(id: String) = viewModelScope.launch { sceneRepository.duplicate(id) }
}

enum class EditorSection { BASIC, OPTIONS, APPEARANCE }

enum class EditorValidation { NAME_REQUIRED, NAME_TOO_LONG, CHOICES_REQUIRED, CHOICE_NAME_REQUIRED, CHOICES_UNIQUE }

data class EditorState(
    val loading: Boolean = true,
    val id: String? = null,
    val name: String = "",
    val selectorType: SelectorType = SelectorType.WHEEL,
    val choices: List<ChoiceItem> = listOf(
        ChoiceItem(name = "", position = 0),
        ChoiceItem(name = "", position = 1),
    ),
    val color: Long = 0xFF7479E8,
    val sound: SoundChoice = SoundChoice.CRISP,
    val animationDurationMs: Int = 3_000,
    val createdAt: Long = System.currentTimeMillis(),
    val section: EditorSection = EditorSection.BASIC,
    val navigationExpanded: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val validation: EditorValidation? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SceneRepository,
    private val sounds: SoundEffectPlayer,
) : ViewModel() {
    private val sceneId: String = checkNotNull(savedStateHandle["sceneId"])
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()
    private val _saved = MutableSharedFlow<Unit>()
    val saved = _saved.asSharedFlow()
    private var editRevision = 0L

    init {
        viewModelScope.launch {
            val existing = if (sceneId == "new") null else repository.getScene(sceneId)
            _state.value = if (existing == null) {
                EditorState(loading = false)
            } else {
                EditorState(
                    loading = false,
                    id = existing.id,
                    name = existing.name,
                    selectorType = existing.selectorType,
                    choices = existing.choices,
                    color = existing.color,
                    sound = existing.sound,
                    animationDurationMs = existing.animationDurationMs,
                    createdAt = existing.createdAt,
                )
            }
        }
    }

    fun setSection(value: EditorSection) = update { copy(section = value) }
    fun toggleNavigation() = update { copy(navigationExpanded = !navigationExpanded) }
    fun setName(value: String) = updateDirty { copy(name = value.take(40), validation = null) }
    fun setType(value: SelectorType) = updateDirty { copy(selectorType = value) }
    fun setColor(value: Long) = updateDirty { copy(color = value) }
    fun setSound(value: SoundChoice) = updateDirty { copy(sound = value) }
    fun previewSound() = sounds.play(_state.value.sound)
    fun setDuration(value: Int) = updateDirty { copy(animationDurationMs = value.coerceIn(1_000, 6_000)) }

    fun addChoice() {
        if (_state.value.choices.size >= 20) return
        updateDirty {
            copy(choices = choices + ChoiceItem(name = "", position = choices.size), validation = null)
        }
    }

    fun updateChoiceName(id: String, value: String) = updateDirty {
        copy(
            choices = choices.map { if (it.id == id) it.copy(name = value.take(40)) else it },
            validation = null,
        )
    }

    fun updateWeight(id: String, delta: Int) = updateDirty {
        copy(choices = choices.map { if (it.id == id) it.copy(weight = EditorWeightRules.adjust(it.weight, delta)) else it })
    }

    fun setWeight(id: String, value: Int) = updateDirty {
        copy(choices = choices.map { if (it.id == id) it.copy(weight = value.coerceIn(EditorWeightRules.MIN_WEIGHT, EditorWeightRules.MAX_WEIGHT)) else it })
    }

    fun removeChoice(id: String) {
        if (_state.value.choices.size <= 2) return
        updateDirty { copy(choices = choices.filterNot { it.id == id }.repositioned(), validation = null) }
    }

    fun moveChoice(id: String, delta: Int) = updateDirty {
        val from = choices.indexOfFirst { it.id == id }
        val to = (from + delta).coerceIn(0, choices.lastIndex)
        if (from < 0 || from == to) this else copy(
            choices = choices.toMutableList().apply { add(to, removeAt(from)) }.repositioned(),
        )
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val validation = validate(current)
        if (validation != null) {
            _state.value = current.copy(validation = validation, section = sectionFor(validation))
            return
        }
        val revisionAtSave = editRevision
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            try {
                val now = System.currentTimeMillis()
                repository.save(
                    Scene(
                        id = current.id ?: UUID.randomUUID().toString(),
                        name = current.name,
                        selectorType = current.selectorType,
                        color = current.color,
                        sound = current.sound,
                        animationDurationMs = current.animationDurationMs,
                        choices = current.choices,
                        createdAt = current.createdAt,
                        updatedAt = now,
                    ),
                )
                val changedWhileSaving = editRevision != revisionAtSave
                _state.value = _state.value.copy(
                    saving = false,
                    dirty = changedWhileSaving,
                )
                if (!changedWhileSaving) _saved.emit(Unit)
            } catch (error: Throwable) {
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    private fun validate(state: EditorState): EditorValidation? {
        val names = state.choices.map { it.name.trim() }
        return when {
            state.name.isBlank() -> EditorValidation.NAME_REQUIRED
            state.name.trim().length > 40 -> EditorValidation.NAME_TOO_LONG
            state.choices.size < 2 -> EditorValidation.CHOICES_REQUIRED
            names.any(String::isBlank) -> EditorValidation.CHOICE_NAME_REQUIRED
            names.map(String::lowercase).distinct().size != names.size -> EditorValidation.CHOICES_UNIQUE
            else -> null
        }
    }

    private fun sectionFor(validation: EditorValidation) = when (validation) {
        EditorValidation.NAME_REQUIRED, EditorValidation.NAME_TOO_LONG -> EditorSection.BASIC
        else -> EditorSection.OPTIONS
    }

    private fun update(block: EditorState.() -> EditorState) { _state.value = _state.value.block() }
    private fun updateDirty(block: EditorState.() -> EditorState) {
        editRevision++
        _state.value = _state.value.block().copy(dirty = true)
    }
}

@HiltViewModel
class PlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: SceneRepository,
    private val engine: SelectionEngine,
    private val sounds: SoundEffectPlayer,
) : ViewModel() {
    private val sceneId: String = checkNotNull(savedStateHandle["sceneId"])
    val scene: StateFlow<Scene?> = repository.observeScene(sceneId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    fun choose(): SelectionOutcome? = scene.value?.let(engine::select)
    fun playResultSound() { scene.value?.let { sounds.play(it.sound) } }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repository.setThemeMode(value) }
    fun setThemeColor(value: Long) = viewModelScope.launch { repository.setThemeColor(value) }
    fun setLanguage(value: AppLanguage) = viewModelScope.launch { repository.setLanguage(value) }
}

private fun List<ChoiceItem>.repositioned() = mapIndexed { index, item -> item.copy(position = index) }

private fun defaultScenes(): List<Scene> {
    fun choices(vararg names: String) = names.mapIndexed { index, name ->
        ChoiceItem(name = name, position = index)
    }
    return listOf(
        Scene(
            name = "去哪吃饭？",
            selectorType = SelectorType.WHEEL,
            color = 0xFF7479E8,
            choices = choices(
                "木莲食堂一楼",
                "木莲食堂二楼",
                "木莲食堂三楼",
                "木莲食堂四楼",
                "海桐食堂一楼",
                "海桐食堂四楼",
                "夜市",
            ),
        ),
        Scene(
            name = "吃什么？",
            selectorType = SelectorType.WHEEL,
            color = 0xFF4FA8A1,
            choices = choices(
                "自选",
                "柳州螺蛳粉",
                "小锅米线",
                "面条",
                "馄饨",
                "馄饨面",
                "水饺",
                "饭团",
                "螺蛳粉",
                "瓦老表瓦香鸡",
                "满味缘",
                "烤肉拌饭",
                "黄焖鸡米饭",
                "上海小馄饨",
                "朱家小馆",
                "阿婆味道",
                "卤肉饭",
                "缘味先石锅饭",
                "麻辣烫",
                "隆江猪脚饭",
                "砂锅冒菜",
                "铁板炒饭炒面",
                "有缘见面",
                "口水鸡",
                "德州汉堡",
                "南国一豆",
                "手抓饼",
                "闫阿妈拌饭",
                "渔粉",
                "清真菜",
                "轻小莳",
                "大酥牛肉面",
                "台北鸡油饭",
                "炒饭先生",
                "土豆泥拌饭",
                "柏氏麻辣烫",
                "文山米线",
                "馄饨",
                "饵块",
                "炸洋芋",
                "杨福记麻辣烫",
                "瓦香鸡",
                "饭香居",
                "缘味石锅饭",
                "嘉思汀汉堡",
                "烤盘饭",
                "老仓醋米线",
                "螺蛳粉",
                "猪脚饭",
                "虾仁捞饭",
                "嘟嘟鱼",
                "小锅米线",
            ),
        ),
    )
}
