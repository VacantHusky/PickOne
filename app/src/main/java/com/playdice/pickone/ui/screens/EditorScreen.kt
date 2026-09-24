@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.playdice.pickone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playdice.pickone.R
import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.model.SoundChoice
import com.playdice.pickone.ui.components.NeuButton
import com.playdice.pickone.ui.components.NeuCard
import com.playdice.pickone.ui.components.NeuColorButton
import com.playdice.pickone.ui.components.NeuDialog
import com.playdice.pickone.ui.components.NeuIconButton
import com.playdice.pickone.ui.components.NeuPathSurface
import com.playdice.pickone.ui.components.NeuSlider
import com.playdice.pickone.ui.components.NeuTextField
import com.playdice.pickone.ui.components.RowSelection
import com.playdice.pickone.ui.components.SelectorTypePill
import com.playdice.pickone.ui.components.selectorIcon
import com.playdice.pickone.ui.components.selectorLabel
import com.playdice.pickone.ui.components.soundLabelRes
import com.playdice.pickone.ui.viewmodel.EditorSection
import com.playdice.pickone.ui.viewmodel.EditorState
import com.playdice.pickone.ui.viewmodel.EditorValidation
import com.playdice.pickone.ui.viewmodel.EditorViewModel
import com.playdice.pickone.ui.viewmodel.EditorWeightRules
import kotlin.math.roundToInt

private val SceneColors = listOf(
    0xFF7479E8, 0xFF4F8DD8, 0xFF4FA8A1, 0xFF65A765,
    0xFFE0A03B, 0xFFE27D60, 0xFFD06091, 0xFF8B6FC0,
    0xFF5E8C74, 0xFFD16C4B,
)
private val EditorPanelShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscard by remember { mutableStateOf(false) }
    val requestBack = { if (state.dirty) showDiscard = true else onBack() }
    BackHandler(onBack = requestBack)

    LaunchedEffect(Unit) { viewModel.saved.collect { onBack() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.id == null) R.string.new_scene else R.string.edit_scene), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = requestBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    NeuIconButton(
                        Icons.Rounded.Save,
                        stringResource(R.string.save),
                        viewModel::save,
                        modifier = Modifier.padding(end = 8.dp).size(42.dp),
                        enabled = !state.loading && !state.saving,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            NeuPathSurface(
                modifier = Modifier.fillMaxSize().padding(padding).padding(top = 8.dp, end = 12.dp, bottom = 12.dp),
                tabWidth = 50.dp,
                // 第一个标签与内容面板的上边缘对齐，因此顶部位置为 0dp。
                // 后续标签按照导航栏的 14dp 顶部内边距和每项 72dp 的步进定位；
                // 72dp 由 58dp 标签高度加 14dp 标签间距组成，必须与 EditorRail
                // 中的布局参数保持一致，否则选中标签的内凹路径会和图标错位。
                tabTop = 72.dp * state.section.ordinal,
                tabHeight = 58.dp,
                cornerRadius = 24.dp,
            ) {
                Row(Modifier.fillMaxSize()) {
                    EditorRail(state, viewModel)
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (state.section) {
                        EditorSection.BASIC -> BasicSection(state, viewModel)
                        EditorSection.OPTIONS -> OptionsSection(state, viewModel)
                        EditorSection.APPEARANCE -> AppearanceSection(state, viewModel)
                    }
                    }
                }
            }
        }
    }

    if (showDiscard) {
        NeuDialog(
            title = stringResource(R.string.discard_title),
            text = stringResource(R.string.discard_body),
            confirmText = stringResource(R.string.discard),
            dismissText = stringResource(R.string.cancel),
            onConfirm = onBack,
            onDismiss = { showDiscard = false },
            destructive = true,
        )
    }
}

@Composable
private fun EditorRail(state: EditorState, viewModel: EditorViewModel) {
    Column(
        modifier = Modifier.width(50.dp).fillMaxHeight().zIndex(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EditorRailItem(Icons.Rounded.Description, stringResource(R.string.section_basic), state.section == EditorSection.BASIC) {
            viewModel.setSection(EditorSection.BASIC)
        }
        EditorRailItem(Icons.AutoMirrored.Rounded.ListAlt, stringResource(R.string.section_options), state.section == EditorSection.OPTIONS) {
            viewModel.setSection(EditorSection.OPTIONS)
        }
        EditorRailItem(Icons.Rounded.ColorLens, stringResource(R.string.section_appearance), state.section == EditorSection.APPEARANCE) {
            viewModel.setSection(EditorSection.APPEARANCE)
        }
    }
}

@Composable
private fun EditorRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier.matchParentSize().clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                // modifier = Modifier.offset(y = (-14).dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLayout(title: String, validation: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (validation != null) {
            NeuCard(modifier = Modifier.fillMaxWidth(), shape = EditorPanelShape, contentPadding = PaddingValues(14.dp)) {
                Text(validation, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BasicSection(state: EditorState, viewModel: EditorViewModel) {
    SectionLayout(stringResource(R.string.section_basic), validationMessage(state.validation)) {
        NeuTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.scene_name),
            placeholder = stringResource(R.string.scene_name_hint),
            supportingText = "${state.name.length}/40",
        )
        Text(stringResource(R.string.selector_type), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SelectorType.entries.forEach { type ->
                SelectorTypePill(
                    text = selectorLabel(type),
                    selected = state.selectorType == type,
                    onClick = { viewModel.setType(type) },
                    modifier = Modifier.weight(1f),
                    leading = {
                        Icon(selectorIcon(type), null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun OptionsSection(state: EditorState, viewModel: EditorViewModel) {
    var deleteTarget by remember { mutableStateOf<ChoiceItem?>(null) }
    var editTarget by remember { mutableStateOf<ChoiceItem?>(null) }
    SectionLayout(stringResource(R.string.section_options), validationMessage(state.validation)) {
        state.choices.forEachIndexed { index, choice ->
            ChoiceEditor(
                choice = choice,
                index = index,
                count = state.choices.size,
                onEdit = { editTarget = choice },
                onRemove = { deleteTarget = choice },
                onMove = { viewModel.moveChoice(choice.id, it) },
            )
        }
        NeuButton(
            text = stringResource(R.string.add_choice),
            onClick = viewModel::addChoice,
            enabled = state.choices.size < 20,
            icon = Icons.Rounded.Add,
        )
    }
    deleteTarget?.let { choice ->
        NeuDialog(
            title = stringResource(R.string.delete_choice_title),
            text = stringResource(R.string.delete_choice_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.removeChoice(choice.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            destructive = true,
        )
    }
    editTarget?.let { choice ->
        ChoiceEditDialog(
            choice = choice,
            onDismiss = { editTarget = null },
            onSave = { name, weight ->
                viewModel.updateChoiceName(choice.id, name)
                viewModel.setWeight(choice.id, weight)
                editTarget = null
            },
        )
    }
}

@Composable
private fun ChoiceEditor(
    choice: ChoiceItem,
    index: Int,
    count: Int,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val rowHeight = 64.dp
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragDistance != 0f) 1f else 0f)
            .offset(y = (dragDistance / 3f).dp)
            .pointerInput(choice.id) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount.y
                        val threshold = rowHeight.toPx() * .72f
                        if (dragDistance > threshold && index < count - 1) {
                            onMove(1)
                            dragDistance -= rowHeight.toPx()
                        } else if (dragDistance < -threshold && index > 0) {
                            onMove(-1)
                            dragDistance += rowHeight.toPx()
                        }
                    },
                    onDragEnd = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                )
            },
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DragHandle, stringResource(R.string.reorder_choice), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = choice.name.ifBlank { stringResource(R.string.choice_name) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text("×${choice.weight}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            NeuIconButton(Icons.Rounded.Edit, stringResource(R.string.edit_choice), onEdit, modifier = Modifier.size(34.dp), minSize = 34.dp)
            NeuIconButton(Icons.Rounded.Delete, stringResource(R.string.remove_choice), onRemove, modifier = Modifier.size(34.dp), enabled = count > 2, minSize = 34.dp, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ChoiceEditDialog(
    choice: ChoiceItem,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var name by remember(choice.id) { mutableStateOf(choice.name) }
    var weight by remember(choice.id) { mutableStateOf(choice.weight) }
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.edit_choice), style = MaterialTheme.typography.headlineSmall)
                NeuTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.choice_name),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.weight), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    NeuIconButton(Icons.Rounded.Remove, stringResource(R.string.decrease_weight), { weight = (weight - 1).coerceAtLeast(EditorWeightRules.MIN_WEIGHT) }, modifier = Modifier.size(36.dp), enabled = EditorWeightRules.canDecrease(weight), minSize = 36.dp)
                    Text(weight.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 18.dp))
                    NeuIconButton(Icons.Rounded.Add, stringResource(R.string.increase_weight), { weight = (weight + 1).coerceAtMost(EditorWeightRules.MAX_WEIGHT) }, modifier = Modifier.size(36.dp), enabled = EditorWeightRules.canIncrease(weight), minSize = 36.dp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    NeuButton(stringResource(R.string.cancel), onDismiss, modifier = Modifier.weight(1f))
                    NeuButton(stringResource(R.string.save), { onSave(name, weight) }, modifier = Modifier.weight(1f), enabled = name.isNotBlank())
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(state: EditorState, viewModel: EditorViewModel) {
    SectionLayout(stringResource(R.string.section_appearance)) {
        Text(stringResource(R.string.scene_color), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            maxItemsInEachRow = 5,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SceneColors.forEach { colorValue ->
                val color = Color(colorValue)
                NeuColorButton(color, state.color == colorValue, { viewModel.setColor(colorValue) }, size = 40.dp)
            }
        }
        Text(stringResource(R.string.sound), style = MaterialTheme.typography.titleMedium)
        RowSelection(
            options = SoundChoice.entries,
            selected = state.sound,
            label = { sound -> stringResource(soundLabelRes(sound)) },
            onSelectionChange = viewModel::setSound,
        )
        NeuButton(stringResource(R.string.preview_sound), viewModel::previewSound, icon = Icons.Rounded.PlayArrow, enabled = state.sound != SoundChoice.NONE)
        Text(stringResource(R.string.animation_duration), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.seconds_value, state.animationDurationMs / 1000f), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        NeuSlider(
            value = state.animationDurationMs.toFloat(),
            onValueChange = { viewModel.setDuration((it / 500f).roundToInt() * 500) },
            valueRange = 1_000f..6_000f,
        )
    }
}

@Composable
private fun validationMessage(validation: EditorValidation?): String? = validation?.let {
    stringResource(
        when (it) {
            EditorValidation.NAME_REQUIRED -> R.string.name_required
            EditorValidation.NAME_TOO_LONG -> R.string.name_too_long
            EditorValidation.CHOICES_REQUIRED -> R.string.choices_required
            EditorValidation.CHOICE_NAME_REQUIRED -> R.string.choice_name_required
            EditorValidation.CHOICES_UNIQUE -> R.string.choices_unique
        },
    )
}
