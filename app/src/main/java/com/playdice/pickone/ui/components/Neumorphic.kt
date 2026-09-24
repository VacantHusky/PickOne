package com.playdice.pickone.ui.components

import android.graphics.BlurMaskFilter
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import com.playdice.pickone.ui.theme.LocalNeuColors

enum class NeuDepth { FLAT, SHALLOW, DEEP }

enum class NeuRelief { RAISED, INSET }

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    depth: NeuDepth = NeuDepth.DEEP,
    relief: NeuRelief = NeuRelief.RAISED,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    NeuSurface(
        modifier = modifier,
        shape = shape,
        interaction = interaction,
        pressed = pressed,
        selected = selected,
        depth = depth,
        relief = relief,
        enabled = enabled,
        onClick = onClick,
        contentFillsWidth = true,
        contentPadding = contentPadding,
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * 绘制编辑器左侧导航栏与右侧内容面板共用的一条连续立体路径。
 *
 * 这个组件不是把导航栏和内容区分别画成两个圆角卡片，而是先构造一条
 * 完整的外轮廓，再在左侧选中标签的位置插入两个内凹转角。这样选中的
 * 标签可以自然地“嵌入”内容面板，面板边缘不会出现两条重叠的阴影线。
 *
 * @param tabWidth 左侧导航栏的宽度，也是内容面板左边缘开始的位置。
 * @param tabTop 选中标签顶部相对于组件顶部的距离。第一个标签通常从 0dp 开始，
 *   后续标签需要包含导航栏内边距和标签间距。
 * @param tabHeight 选中标签占用的垂直高度，决定两处内凹转角之间的距离。
 * @param cornerRadius 内容面板四个外角的圆角半径；内部转角会自动限制在可用空间内。
 */
@Composable
fun NeuPathSurface(
    modifier: Modifier = Modifier,
    tabWidth: Dp,
    tabTop: Dp,
    tabHeight: Dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val neu = LocalNeuColors.current
    Box(
        modifier = modifier.drawWithCache {
            val rail = tabWidth.toPx()
            val top = maxOf(0f, tabTop.toPx())
            val height = tabHeight.toPx()
            // 四个外部面板圆角始终使用同一个半径，保证面板四角的视觉一致性。
            // 选中标签与面板连接处属于内部转折，因此单独计算连接圆角。
            val outerRadius = cornerRadius.toPx().coerceAtMost(
                minOf(size.width, size.height) / 2f,
            )

            val path = Path().apply {
                // 这里按顺时针方向拼接一条连续轮廓：先绘制内容面板的上边和右侧，
                // 再经过底部外圆角回到左下角，最后沿左边缘走到选中标签的下方，
                // 绕过两个匹配的内凹转角后回到顶部。面板外角保持统一，
                // 选中标签则像从左侧切入一样与面板连成一个整体。
                moveTo(rail + outerRadius, 0f)
                lineTo(size.width - outerRadius, 0f)  // 内容区上边缘
                quadraticTo(size.width, 0f, size.width, outerRadius)
                lineTo(size.width, size.height - outerRadius)  // 右边缘
                quadraticTo(size.width, size.height, size.width - outerRadius, size.height)
                lineTo(rail + outerRadius, size.height)  // 内容区下边缘
                quadraticTo(rail, size.height, rail, size.height - outerRadius)
                lineTo(rail, top + height + outerRadius)  // 沿内容面板左边缘向上
                quadraticTo(rail, top + height, rail - outerRadius, top + height)
                lineTo(outerRadius, top + height)  // 沿 Tab 底部向左
                quadraticTo(0f, top + height, 0f, top + height - outerRadius)
                lineTo(0f, top + outerRadius)  // 沿 Tab 左边缘向上
                quadraticTo(0f, top, outerRadius, top)
                if (top == 0f) {
                    lineTo(rail + outerRadius, 0f)  // 沿 Tab 顶部向右
                } else {
                    lineTo(rail - outerRadius, top)  // 沿 Tab 顶部向右
                    if (outerRadius < top * 0.5f) {
                        quadraticTo(rail, top, rail, top - outerRadius)
                        lineTo(rail, outerRadius)
                    } else {
                        quadraticTo(rail, top, rail, top * 0.5f)
                    }
                    quadraticTo(rail, 0f, rail + outerRadius, 0f)
                }
                close()
            }
            val androidPath = path.asAndroidPath()
            val offset = 5.dp.toPx()
            val blur = 7.dp.toPx()
            val lightPaint = shadowPaint(neu.highlight, blur, android.graphics.Paint.Style.FILL)
            val darkPaint = shadowPaint(neu.shadow, blur, android.graphics.Paint.Style.FILL)
            onDrawBehind {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.withTranslation(-offset, -offset) {
                        drawPath(androidPath, lightPaint)
                    }
                    native.withTranslation(offset, offset) {
                        drawPath(androidPath, darkPaint)
                    }
                }
                drawPath(path, neu.surface)
            }
        },
        contentAlignment = Alignment.TopStart,
        content = content,
    )
}

@Composable
fun NeuInset(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    depth: NeuDepth = NeuDepth.SHALLOW,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    NeuSurface(
        modifier = modifier,
        shape = shape,
        interaction = remember { MutableInteractionSource() },
        pressed = false,
        depth = depth,
        relief = NeuRelief.INSET,
        contentFillsWidth = true,
        onClick = null,
        contentPadding = contentPadding,
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun NeuIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    minSize: Dp = 48.dp,
    tint: Color? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    NeuSurface(
        modifier = modifier.defaultMinSize(minSize, minSize),
        shape = shape,
        interaction = interaction,
        pressed = pressed,
        selected = selected,
        depth = NeuDepth.SHALLOW,
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)
                tint != null -> tint
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    supportingText: String? = null,
    singleLine: Boolean = true,
) {
    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 7.dp),
            )
        }
        NeuInset(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp),
            shape = RoundedCornerShape(17.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface),
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = VisualTransformation.None,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(top = 5.dp, end = 6.dp),
            )
        }
    }
}

@Composable
fun NeuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var widthPx by remember { mutableIntStateOf(1) }
    val update: (Float) -> Unit = { x ->
        val nextFraction = (x / widthPx).coerceIn(0f, 1f)
        onValueChange(valueRange.start + nextFraction * (valueRange.endInclusive - valueRange.start))
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(valueRange, widthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position.x)
                    do {
                        val change = awaitPointerEvent().changes.first()
                        update(change.position.x)
                        change.consume()
                    } while (change.pressed)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        NeuInset(
            modifier = Modifier.fillMaxWidth().height(14.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(.001f))
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            )
        }
        NeuCard(
            modifier = Modifier.offset(x = (maxWidth - 32.dp) * fraction).size(32.dp),
            depth = NeuDepth.SHALLOW,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun NeuColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    NeuCard(
        modifier = modifier.size(size),
        onClick = onClick,
        selected = selected,
        depth = NeuDepth.SHALLOW,
        shape = CircleShape,
        contentPadding = PaddingValues(6.dp),
    ) {
        Box(
            Modifier.size((size - 12.dp).coerceAtLeast(20.dp)).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
fun NeuDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val neu = LocalNeuColors.current
    val shape = RoundedCornerShape(18.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .outerNeuShadow(shape, neu.highlight, neu.shadow, NeuDepth.DEEP)
            .clip(shape)
            .background(neu.surface),
        content = content,
    )
}

@Composable
fun NeuDialog(
    title: String,
    text: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(LocalNeuColors.current.surface)
                .padding(24.dp),
        ) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        depth = NeuDepth.SHALLOW,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                    ) {
                        Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        selected = true,
                        enabled = confirmEnabled,
                        depth = NeuDepth.SHALLOW,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                    ) {
                        Text(
                            confirmText,
                            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    NeuSurface(
        modifier = modifier.defaultMinSize(minHeight = 54.dp),
        shape = shape,
        interaction = interaction,
        pressed = pressed,
        accent = enabled,
        depth = NeuDepth.SHALLOW,
        enabled = enabled,
        onClick = onClick,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Text(
                text = text,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun SelectionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: NeuDepth = NeuDepth.SHALLOW,
    relief: NeuRelief = NeuRelief.RAISED,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    selectedSurface: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val selectedRelief = if (selected) NeuRelief.INSET else relief
    NeuSurface(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = shape,
        interaction = interaction,
        pressed = pressed,
        selected = selected && selectedSurface,
        depth = depth,
        relief = selectedRelief,
        onClick = onClick,
        role = Role.RadioButton,
        contentPadding = contentPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke(this)
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Selector-type control: selected items remain raised and use the accent surface. */
@Composable
fun SelectorTypePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    NeuSurface(
        modifier = modifier.defaultMinSize(minHeight = 54.dp),
        shape = shape,
        interaction = interaction,
        pressed = pressed,
        selected = selected,
        depth = NeuDepth.SHALLOW,
        relief = NeuRelief.RAISED,
        onClick = onClick,
        role = Role.RadioButton,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke(this)
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun NeuSurface(
    modifier: Modifier,
    shape: Shape,
    interaction: MutableInteractionSource,
    pressed: Boolean,
    selected: Boolean = false,
    accent: Boolean = false,
    depth: NeuDepth = NeuDepth.DEEP,
    relief: NeuRelief = NeuRelief.RAISED,
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
    role: Role = Role.Button,
    contentFillsWidth: Boolean = false,
    contentPadding: PaddingValues,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val neu = LocalNeuColors.current
    val primary = MaterialTheme.colorScheme.primary
    val foreground = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        accent -> primary
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> neu.surface
    }
    val coloredSurface = enabled && (accent || selected)
    val lightShadow = if (coloredSurface) foreground.shiftToward(Color.White, .36f) else neu.highlight
    val darkShadow = if (coloredSurface) foreground.scaleRgb(.84f) else neu.shadow
    val currentRelief = if (pressed && relief == NeuRelief.RAISED) NeuRelief.INSET else relief
    val clickableModifier = if (onClick == null) Modifier else Modifier.clickable(
        enabled = enabled,
        interactionSource = interaction,
        indication = null,
        role = role,
        onClick = onClick,
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (depth != NeuDepth.FLAT && currentRelief == NeuRelief.RAISED) {
            Box(
                Modifier
                    .matchParentSize()
                    .outerNeuShadow(
                        shape = shape,
                        light = lightShadow,
                        dark = darkShadow,
                        depth = depth,
                    ),
            )
        }
        Box(
            modifier = Modifier.matchParentSize().clip(shape).background(foreground),
        )
        if (depth != NeuDepth.FLAT && currentRelief == NeuRelief.INSET) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .innerNeuShadow(
                        shape = shape,
                        dark = darkShadow,
                        light = lightShadow,
                        depth = depth,
                    ),
            )
        }
        Box(modifier = Modifier.matchParentSize().then(clickableModifier))
        Box(
            modifier = (if (contentFillsWidth) Modifier.fillMaxWidth() else Modifier).padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * ShadeCraft-style raised surface: two compact, fully colored blurred shapes
 * on opposite sides of one fixed upper-left light source.
 */
private fun Modifier.outerNeuShadow(
    shape: Shape,
    light: Color,
    dark: Color,
    depth: NeuDepth,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = outline.toPath()
    val offsetPx = when (depth) {
        NeuDepth.FLAT -> 0.dp
        NeuDepth.SHALLOW -> 2.5.dp
        NeuDepth.DEEP -> 5.dp
    }.toPx()
    val blurPx = when (depth) {
        NeuDepth.FLAT -> 0.dp
        NeuDepth.SHALLOW -> 4.dp
        NeuDepth.DEEP -> 7.dp
    }.toPx()

    val lightPaint = shadowPaint(light, blurPx, android.graphics.Paint.Style.FILL)
    val darkPaint = shadowPaint(dark, blurPx, android.graphics.Paint.Style.FILL)

    onDrawBehind {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val androidPath = path.asAndroidPath()

            nativeCanvas.withTranslation(-offsetPx, -offsetPx) {
                drawPath(androidPath, lightPaint)
            }
            nativeCanvas.withTranslation(offsetPx, offsetPx) {
                drawPath(androidPath, darkPaint)
            }
        }
    }
}

/**
 * Draws a real recessed edge. With a light source at the upper left, an inset
 * surface is dark at its upper-left edge and bright at its lower-right edge.
 */
private fun Modifier.innerNeuShadow(
    shape: Shape,
    dark: Color,
    light: Color,
    depth: NeuDepth,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = outline.toPath()
    val offsetPx = when (depth) {
        NeuDepth.FLAT -> 0.dp
        NeuDepth.SHALLOW -> 2.dp
        NeuDepth.DEEP -> 4.dp
    }.toPx()
    val blurPx = when (depth) {
        NeuDepth.FLAT -> 0.dp
        NeuDepth.SHALLOW -> 2.dp
        NeuDepth.DEEP -> 3.dp
    }.toPx()
    val strokePx = when (depth) {
        NeuDepth.FLAT -> 0.dp
        NeuDepth.SHALLOW -> 5.dp
        NeuDepth.DEEP -> 7.dp
    }.toPx()

    val darkPaint = shadowPaint(dark, blurPx, android.graphics.Paint.Style.STROKE).apply {
        strokeWidth = strokePx
    }
    val lightPaint = shadowPaint(light, blurPx, android.graphics.Paint.Style.STROKE).apply {
        strokeWidth = strokePx
    }

    onDrawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val androidPath = path.asAndroidPath()

            nativeCanvas.withClip(androidPath) {
                withTranslation(offsetPx, offsetPx) {
                    drawPath(androidPath, darkPaint)
                }
            }
            nativeCanvas.withClip(androidPath) {
                withTranslation(-offsetPx, -offsetPx) {
                    drawPath(androidPath, lightPaint)
                }
            }
        }
    }
}

private fun shadowPaint(
    color: Color,
    blurPx: Float,
    style: android.graphics.Paint.Style,
) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    isAntiAlias = true
    isDither = true
    this.style = style
    this.color = color.toArgb()
    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(Rect(rect.left, rect.top, rect.right, rect.bottom)) }
}

private fun Color.shiftToward(target: Color, amount: Float) = Color(
    red = red + (target.red - red) * amount,
    green = green + (target.green - green) * amount,
    blue = blue + (target.blue - blue) * amount,
    alpha = alpha,
)

private fun Color.scaleRgb(factor: Float) = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha,
)
