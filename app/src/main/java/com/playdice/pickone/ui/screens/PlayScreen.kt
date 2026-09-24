@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.playdice.pickone.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playdice.pickone.R
import com.playdice.pickone.domain.SelectionVisualMapping
import com.playdice.pickone.model.Scene
import com.playdice.pickone.model.SelectionOutcome
import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.ui.components.NeuCard
import com.playdice.pickone.ui.components.NeuDepth
import com.playdice.pickone.ui.components.NeuInset
import com.playdice.pickone.ui.viewmodel.PlayViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onBack: () -> Unit,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    var outcome by remember { mutableStateOf<SelectionOutcome?>(null) }
    var animating by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    var chargePower by remember { mutableFloatStateOf(.5f) }
    val progress = remember { Animatable(0f) }

    val startChoice: (Float) -> Unit = { power ->
        if (!animating) {
            viewModel.choose()?.let {
                chargePower = power.coerceIn(.12f, 1f)
                outcome = it
                animating = true
                runId++
            }
        }
    }

    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        progress.snapTo(0f)
        val baseDuration = scene?.animationDurationMs ?: 3_000
        val duration = (baseDuration * (.78f + chargePower * .46f)).toInt()
        val easing = when (scene?.selectorType) {
            SelectorType.WHEEL -> CubicBezierEasing(.08f, .72f, .14f, 1f)
            SelectorType.SLOT_MACHINE -> CubicBezierEasing(.10f, .64f, .16f, 1f)
            SelectorType.DICE, SelectorType.COIN -> LinearOutSlowInEasing
            else -> FastOutSlowInEasing
        }
        progress.animateTo(1f, tween(duration, easing = easing))
        animating = false
        viewModel.playResultSound()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(scene?.name.orEmpty(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val current = scene
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NeuCard(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    SelectorStage(current, outcome, progress.value, animating, chargePower)
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(84.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (outcome != null && !animating) {
                        outcome?.let { ResultCard(it, current.color) }
                    }
                }
                if (outcome?.rejectedRolls ?: 0 > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.rerolling), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(18.dp))
                ChargeButton(
                    idleText = stringResource(if (outcome == null) R.string.roll_now else R.string.charge_again),
                    chargingText = { percent -> stringResource(R.string.charging_percent, percent) },
                    busyText = stringResource(R.string.choosing),
                    onCharged = startChoice,
                    enabled = !animating,
                    hasResult = outcome != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ChargeButton(
    idleText: String,
    chargingText: @Composable (Int) -> String,
    busyText: String,
    onCharged: (Float) -> Unit,
    enabled: Boolean,
    hasResult: Boolean,
    modifier: Modifier = Modifier,
) {
    val charge = remember { Animatable(0f) }
    var charging by remember { mutableStateOf(false) }
    val currentOnCharged by rememberUpdatedState(onCharged)
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val label = when {
        !enabled -> busyText
        charging -> chargingText((charge.value * 100).toInt())
        else -> idleText
    }
    val gestureModifier = Modifier
        .semantics {
            role = Role.Button
            contentDescription = label
            onClick {
                if (enabled) currentOnCharged(1f)
                enabled
            }
        }
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    if (enabled) {
                        charging = true
                        coroutineScope {
                            val animation = launch {
                                charge.snapTo(0f)
                                charge.animateTo(1f, tween(1_450, easing = LinearEasing))
                            }
                            val released = tryAwaitRelease()
                            val power = charge.value
                            animation.cancel()
                            charging = false
                            if (released && power >= .12f) currentOnCharged(power)
                            charge.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                        }
                    }
                },
            )
        }

    NeuCard(
        modifier = modifier.height(68.dp).then(gestureModifier),
        depth = NeuDepth.SHALLOW,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(charge.value.coerceAtLeast(.001f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = .34f), accent.copy(alpha = .82f)),
                        ),
                    )
                    .align(Alignment.CenterStart),
            )
            Row(
                modifier = Modifier.graphicsLayer {
                    val pulse = 1f + charge.value * .10f
                    scaleX = pulse
                    scaleY = pulse
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(42.dp)) {
                        drawCircle(onSurface.copy(alpha = .10f), style = Stroke(3.dp.toPx()))
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = charge.value * 360f,
                            useCenter = false,
                            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Icon(
                        imageVector = if (hasResult) Icons.Rounded.Refresh else Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectorStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, animating: Boolean, power: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (scene.selectorType) {
            SelectorType.WHEEL -> WheelStage(scene, outcome, progress, power)
            SelectorType.SLOT_MACHINE -> SlotStage(scene, outcome, progress, animating, power)
            SelectorType.DICE -> DiceStage(scene, outcome, progress, animating, power)
            SelectorType.COIN -> CoinStage(scene, outcome, progress, animating, power)
            SelectorType.RUSSIAN_ROULETTE -> RouletteStage(scene, outcome, progress, animating, power)
        }
    }
}

@Composable
private fun WheelStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, power: Float) {
    val weights = outcome?.normalizedWeights ?: scene.choices.map { it.weight }
    val total = weights.sum().toFloat()
    val choiceCount = scene.choices.size
    val wheelScale = (
        1f + sqrt((choiceCount - 8).coerceAtLeast(0).toFloat()) * .075f
    ).coerceAtMost(1.5f)
    val labelMaxLength = when {
        choiceCount >= 40 -> 5
        choiceCount >= 24 -> 6
        else -> 7
    }
    val labelSize = when {
        choiceCount >= 40 -> 11.dp
        choiceCount >= 24 -> 12.dp
        else -> 13.dp
    }
    val labelRadius = when {
        choiceCount >= 24 -> .40f
        choiceCount >= 12 -> .34f
        else -> .30f
    }
    val selectedIndex = outcome?.let { result -> scene.choices.indexOfFirst { it.id == result.selected.id } }
    val rotation = if (selectedIndex == null) 0f else progress * SelectionVisualMapping.wheelStopRotation(weights, selectedIndex, power)
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
    Box(
        Modifier
            .fillMaxWidth(.94f)
            .aspectRatio(1f),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Larger wheels are allowed to extend beyond the card so dense option
        // sets retain readable spacing. The pointer stays above the wheel.
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = wheelScale
                    scaleY = wheelScale
                },
        ) {
            var start = -90f
            drawCircle(Color(scene.color).copy(alpha = .24f), radius = size.minDimension * .49f)
            rotate(rotation, center) {
                weights.forEachIndexed { index, weight ->
                    val sweep = 360f * weight / total
                    drawArc(
                        color = wheelColor(Color(scene.color), index),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size,
                    )
                    val mid = Math.toRadians((start + sweep / 2f).toDouble())
                    val textCenter = Offset(
                        center.x + cos(mid).toFloat() * size.minDimension * labelRadius,
                        center.y + sin(mid).toFloat() * size.minDimension * labelRadius,
                    )
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = Color.White.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = labelSize.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        }
                        val label = scene.choices[index].name.take(labelMaxLength)
                        val canvasRotation = Math.toDegrees(mid).toFloat().let { angle ->
                            if (angle > 90f || angle < -90f) angle + 180f else angle
                        }
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.rotate(canvasRotation, textCenter.x, textCenter.y)
                        canvas.nativeCanvas.drawText(label, textCenter.x, textCenter.y - (paint.ascent() + paint.descent()) / 2f, paint)
                        canvas.nativeCanvas.restore()
                    }
                    start += sweep
                }
                drawCircle(surface, radius = size.minDimension * .115f)
                drawCircle(Color(scene.color), radius = size.minDimension * .055f)
            }
            drawCircle(Color.White.copy(alpha = .45f), style = Stroke(width = 5.dp.toPx()))
            drawCircle(outline, radius = size.minDimension * .47f, style = Stroke(width = 2.dp.toPx()))
        }
        Canvas(Modifier.size(42.dp, 58.dp).offset(y = (-44).dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, Color(scene.color))
        }
    }
}

@Composable
private fun SlotStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, animating: Boolean, power: Float) {
    val selectedIndex = outcome?.let { out -> scene.choices.indexOfFirst { it.id == out.selected.id } } ?: 0
    val slotSurface = MaterialTheme.colorScheme.surface
    val slotBackground = MaterialTheme.colorScheme.background
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(64.dp).height(54.dp)) {
            drawRoundRect(Color(scene.color), topLeft = Offset(size.width * .2f, 0f), size = Size(size.width * .6f, size.height * .72f), cornerRadius = CornerRadius(16.dp.toPx()))
            drawCircle(slotSurface, radius = size.width * .17f, center = Offset(size.width / 2f, size.height * .12f))
        }
        NeuInset(modifier = Modifier.fillMaxWidth(), depth = NeuDepth.DEEP, shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { reel ->
                    // Animate from a different part of the reel and land on the exact
                    // selected item. The phase is constructed so that at progress == 1
                    // it is an integer selectedIndex, making the last animated frame
                    // identical to the settled result.
                    val spins = (22f + power * 30f + reel * 7f).toInt().coerceAtLeast(1)
                    val startOffset = .35f + reel * .17f
                    val phase = if (animating) {
                        selectedIndex - spins * (1f - progress) - startOffset * (1f - progress)
                    } else {
                        selectedIndex.toFloat()
                    }
                    val index = floor(phase).toInt().mod(scene.choices.size)
                    val nextIndex = (index + 1) % scene.choices.size
                    val fraction = if (animating) phase - floor(phase) else 0f
                    Box(
                        modifier = Modifier.weight(1f).height(166.dp).clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(.10f), slotSurface, Color.Black.copy(.13f)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ReelItem(
                                scene.choices[index].name,
                                Color(scene.color),
                                1f - fraction * .55f,
                                Modifier.offset(y = (-fraction * 92f).dp),
                            )
                            ReelItem(
                                scene.choices[nextIndex].name,
                                Color(scene.color),
                                .45f + fraction * .55f,
                                Modifier.offset(y = ((1f - fraction) * 92f).dp),
                            )
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Brush.verticalGradient(listOf(slotBackground.copy(.75f), Color.Transparent, Color.Transparent, slotBackground.copy(.75f))))
                            drawLine(Color(scene.color).copy(.45f), Offset(0f, size.height * .3f), Offset(size.width, size.height * .3f), 1.dp.toPx())
                            drawLine(Color(scene.color).copy(.45f), Offset(0f, size.height * .7f), Offset(size.width, size.height * .7f), 1.dp.toPx())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelItem(label: String, color: Color, alpha: Float, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.alpha(alpha)) {
        Icon(Icons.Rounded.Casino, null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, textAlign = TextAlign.Center, maxLines = 2, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DiceStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, animating: Boolean, power: Float) {
    val total = scene.choices.map { it.weight }.reduce(::gcd).let { divisor -> scene.choices.sumOf { it.weight / divisor } }
    val count = com.playdice.pickone.domain.SelectionEngine.unitCountFor(total, 6)
    val values = outcome?.unitValues?.ifEmpty { null }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pluralStringResource(R.plurals.dice_count, count, count), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(count) { index ->
                val animated = ((progress * (18f + power * 36f)).toInt() + index * 2) % 6 + 1
                DiceFace3D(
                    value = if (animating || values == null) animated else values[index] + 1,
                    color = Color(scene.color),
                    rotation = if (animating) progress * (540f + power * 1_080f) + index * 30f else 0f,
                )
            }
        }
    }
}

@Composable
private fun DiceFace3D(value: Int, color: Color, rotation: Float) {
    Canvas(
        modifier = Modifier.size(116.dp).graphicsLayer {
            rotationZ = sin(Math.toRadians(rotation.toDouble())).toFloat() * 13f
            translationY = -abs(sin(Math.toRadians(rotation.toDouble())).toFloat()) * 18.dp.toPx()
            shadowElevation = 16.dp.toPx()
        },
    ) {
        val depth = 17.dp.toPx()
        val left = 10.dp.toPx()
        val top = 25.dp.toPx()
        val right = size.width - depth - 10.dp.toPx()
        val bottom = size.height - 10.dp.toPx()
        drawOval(
            Color.Black.copy(alpha = .24f),
            topLeft = Offset(left + 5.dp.toPx(), bottom - 3.dp.toPx()),
            size = Size(right - left + depth, 13.dp.toPx()),
        )
        val topFace = Path().apply {
            moveTo(left, top)
            lineTo(left + depth, top - depth)
            lineTo(right + depth, top - depth)
            lineTo(right, top)
            close()
        }
        val rightFace = Path().apply {
            moveTo(right, top)
            lineTo(right + depth, top - depth)
            lineTo(right + depth, bottom - depth)
            lineTo(right, bottom)
            close()
        }
        drawPath(
            topFace,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = .58f), color.copy(alpha = .95f)),
                start = Offset(left, top - depth),
                end = Offset(right, top),
            ),
        )
        drawPath(
            rightFace,
            Brush.linearGradient(
                listOf(color.copy(alpha = .86f), color.copy(red = color.red * .52f, green = color.green * .52f, blue = color.blue * .52f)),
                start = Offset(right, top),
                end = Offset(right + depth, bottom),
            ),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(color.copy(red = (color.red + .12f).coerceAtMost(1f), green = (color.green + .12f).coerceAtMost(1f), blue = (color.blue + .12f).coerceAtMost(1f)), color),
            ),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(18.dp.toPx()),
        )
        diceDotPositions(value).forEach { (x, y) ->
            val point = Offset(left + (right - left) * x, top + (bottom - top) * y)
            drawCircle(Color.Black.copy(alpha = .28f), radius = 6.5.dp.toPx(), center = point + Offset(1.dp.toPx(), 2.dp.toPx()))
            drawCircle(Color.White, radius = 5.7.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun CoinStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, animating: Boolean, power: Float) {
    val total = scene.choices.map { it.weight }.reduce(::gcd).let { divisor -> scene.choices.sumOf { it.weight / divisor } }
    val count = com.playdice.pickone.domain.SelectionEngine.unitCountFor(total, 2)
    val values = outcome?.unitValues?.ifEmpty { null }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pluralStringResource(R.plurals.coin_count, count, count), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(count) { index ->
                val animated = ((progress * 24).toInt() + index) % 2
                val value = if (animating || values == null) animated else values[index]
                val turns = if (animating) progress * (6f + power * 9f + index) else if (value == 0) 8f else 9f
                Coin3D(
                    turns = turns,
                    color = Color(scene.color),
                    frontLabel = stringResource(R.string.heads).take(1),
                    backLabel = stringResource(R.string.tails).take(1),
                )
            }
        }
    }
}

@Composable
private fun Coin3D(
    turns: Float,
    color: Color,
    frontLabel: String,
    backLabel: String,
) {
    val phase = turns * Math.PI
    val faceFactor = abs(cos(phase).toFloat()).coerceIn(0f, 1f)
    val front = cos(phase).toFloat() >= 0f
    Canvas(
        modifier = Modifier.size(96.dp).graphicsLayer {
            translationY = -abs(sin(phase).toFloat()) * 14.dp.toPx()
            rotationZ = sin(phase * .5).toFloat() * 7f
        },
    ) {
        val maxWidth = size.width * .78f
        val faceWidth = 8.dp.toPx() + (maxWidth - 8.dp.toPx()) * faceFactor
        val faceHeight = size.height * .78f
        val top = (size.height - faceHeight) / 2f
        val left = (size.width - faceWidth) / 2f
        val thickness = 8.dp.toPx() * (1f - faceFactor)

        drawOval(
            Color.Black.copy(alpha = .24f),
            topLeft = Offset(size.width * .14f, size.height * .82f),
            size = Size(size.width * .72f, 10.dp.toPx()),
        )
        repeat(7) { layer ->
            val offset = (layer - 3) * thickness / 6f
            drawOval(
                color = color.copy(
                    red = color.red * (.48f + layer * .035f),
                    green = color.green * (.48f + layer * .035f),
                    blue = color.blue * (.48f + layer * .035f),
                ),
                topLeft = Offset(left + offset, top),
                size = Size(faceWidth.coerceAtLeast(6.dp.toPx()), faceHeight),
            )
        }
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = .72f),
                    if (front) color else color.copy(red = color.red * .76f, green = color.green * .76f, blue = color.blue * .76f),
                    color.copy(red = color.red * .48f, green = color.green * .48f, blue = color.blue * .48f),
                ),
                center = Offset(size.width * .42f, size.height * .35f),
                radius = size.width * .55f,
            ),
            topLeft = Offset(left, top),
            size = Size(faceWidth.coerceAtLeast(6.dp.toPx()), faceHeight),
        )
        drawOval(
            Color.White.copy(alpha = .55f),
            topLeft = Offset(left + 2.dp.toPx(), top + 2.dp.toPx()),
            size = Size((faceWidth - 4.dp.toPx()).coerceAtLeast(2.dp.toPx()), faceHeight - 4.dp.toPx()),
            style = Stroke(2.dp.toPx()),
        )
        if (faceFactor > .28f) {
            // Fine radial machining marks and a raised inner rim give the coin a
            // physical, minted-metal surface instead of a flat color fill.
            drawOval(
                Color.White.copy(alpha = .24f),
                topLeft = Offset(left + 5.dp.toPx(), top + 5.dp.toPx()),
                size = Size((faceWidth - 10.dp.toPx()).coerceAtLeast(2.dp.toPx()), faceHeight - 10.dp.toPx()),
                style = Stroke(1.dp.toPx()),
            )
            repeat(28) { grain ->
                val angle = grain * (Math.PI * 2.0 / 28.0)
                val radius = .24f + ((grain * 17) % 11) / 100f
                val grainCenter = Offset(
                    size.width / 2f + cos(angle).toFloat() * faceWidth * radius,
                    size.height / 2f + sin(angle).toFloat() * faceHeight * radius,
                )
                drawCircle(
                    Color.White.copy(alpha = .08f + (grain % 3) * .025f),
                    radius = (.45f + (grain % 2) * .35f).dp.toPx(),
                    center = grainCenter,
                )
            }
            repeat(10) { ridge ->
                val sweep = 13f + ridge * 31f
                drawArc(
                    color = Color.White.copy(alpha = .12f),
                    startAngle = sweep,
                    sweepAngle = 18f,
                    useCenter = false,
                    topLeft = Offset(left + 7.dp.toPx(), top + 7.dp.toPx()),
                    size = Size((faceWidth - 14.dp.toPx()).coerceAtLeast(2.dp.toPx()), faceHeight - 14.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    this.color = Color.White.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 24.dp.toPx() * faceFactor.coerceAtLeast(.55f)
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setShadowLayer(3.dp.toPx(), 0f, 2.dp.toPx(), 0x66000000)
                }
                canvas.nativeCanvas.drawText(
                    if (front) frontLabel else backLabel,
                    size.width / 2f,
                    size.height / 2f - (paint.ascent() + paint.descent()) / 2f,
                    paint,
                )
            }
        }
    }
}

@Composable
private fun RouletteStage(scene: Scene, outcome: SelectionOutcome?, progress: Float, animating: Boolean, power: Float) {
    val accent = Color(scene.color)
    val selectedIndex = outcome?.let { result -> scene.choices.indexOfFirst { it.id == result.selected.id } }?.coerceAtLeast(0) ?: 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.18f),
            factory = { context -> Roulette3DView(context) },
            update = { view -> view.setState(progress, selectedIndex, accent.toArgb(), animating, power) },
        )
        Text(
            when {
                outcome == null -> stringResource(R.string.roll_now)
                !animating -> outcome.selected.name
                else -> scene.choices[(selectedIndex + (progress * scene.choices.size * (3f + power * 4f)).toInt()).mod(scene.choices.size)].name
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultCard(outcome: SelectionOutcome, colorValue: Long) {
    NeuCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(colorValue)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TouchApp, null, tint = Color.White)
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(stringResource(R.string.result_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(outcome.selected.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun wheelColor(base: Color, index: Int): Color {
    val factors = listOf(1f, .82f, 1.18f, .68f, 1.08f)
    val factor = factors[index % factors.size]
    return Color(
        red = (base.red * factor).coerceIn(0f, 1f),
        green = (base.green * factor).coerceIn(0f, 1f),
        blue = (base.blue * factor).coerceIn(0f, 1f),
    )
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

private fun diceDotPositions(value: Int): List<Pair<Float, Float>> {
    val topLeft = .23f to .23f
    val topRight = .77f to .23f
    val middleLeft = .23f to .5f
    val center = .5f to .5f
    val middleRight = .77f to .5f
    val bottomLeft = .23f to .77f
    val bottomRight = .77f to .77f
    return when (value) {
        1 -> listOf(center)
        2 -> listOf(topLeft, bottomRight)
        3 -> listOf(topLeft, center, bottomRight)
        4 -> listOf(topLeft, topRight, bottomLeft, bottomRight)
        5 -> listOf(topLeft, topRight, center, bottomLeft, bottomRight)
        else -> listOf(topLeft, topRight, middleLeft, middleRight, bottomLeft, bottomRight)
    }
}
