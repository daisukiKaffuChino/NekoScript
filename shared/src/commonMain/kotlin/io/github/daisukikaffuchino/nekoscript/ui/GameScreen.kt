package io.github.daisukikaffuchino.nekoscript.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.interaction.HotspotRegistry
import io.github.daisukikaffuchino.nekoscript.engine.runtime.BackgroundView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.CharacterView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameAction
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameViewState
import io.github.daisukikaffuchino.nekoscript.engine.runtime.VisualEffectView
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import io.github.daisukikaffuchino.nekoscript.engine.viewport.ContainerPoint
import io.github.daisukikaffuchino.nekoscript.engine.viewport.GameViewport
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint
import io.github.daisukikaffuchino.nekoscript.ui.asset.ImageAssetResolver
import io.github.daisukikaffuchino.nekoscript.ui.asset.ResolvedImageAsset
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Renders the default AVG presentation and forwards game commands as [GameAction].
 * [hotspotRegistry] receives unconsumed pointer presses inside the game content. Presses on
 * letterbox, pillarbox, or Compose controls do not activate scene hotspots.
 */
@Composable
fun GameScreen(
    state: GameViewState,
    imageAssets: ImageAssetResolver,
    imageLoader: ImageLoader,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
    hotspotRegistry: HotspotRegistry? = null,
) {
    val viewport = GameViewport.DEFAULT
    val currentHotspotRegistry = rememberUpdatedState(hotspotRegistry)
    val currentOnAction = rememberUpdatedState(onAction)
    val sceneProgress = remember { Animatable(1f) }
    val shakeOffset = remember { Animatable(0f) }
    val effect = state.visualEffect
    LaunchedEffect(effect?.sequence) {
        when (effect) {
            is VisualEffectView.Transition -> {
                sceneProgress.snapTo(0f)
                sceneProgress.animateTo(
                    1f,
                    tween(effect.durationMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                )
            }
            is VisualEffectView.Shake -> {
                val amplitude = effect.intensity * 7f
                val steps = (effect.durationMillis / 40L).coerceIn(1L, 30L).toInt()
                repeat(steps) { step ->
                    shakeOffset.animateTo(if (step % 2 == 0) amplitude else -amplitude, tween(20))
                }
                shakeOffset.animateTo(0f, tween(20))
            }
            is VisualEffectView.CharacterMove, null -> Unit
        }
        if (effect != null) {
            if (effect is VisualEffectView.CharacterMove) {
                delay(effect.durationMillis)
            }
            onAction(GameAction.CompleteVisualEffect(effect.sequence))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeContentPadding()
            .logicalPointerInput(
                viewport = viewport,
                enabled = !state.isBacklogOpen && hotspotRegistry != null,
                onLogicalPointerDown = { point ->
                    currentHotspotRegistry.value?.hitTest(point)?.let { hotspot ->
                        currentOnAction.value(GameAction.ActivateHotspot(hotspot.id))
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .aspectRatio(viewport.aspectRatio.toFloat())
                .clipToBounds()
                .background(Color(0xFF17201D)),
        ) {
            val transition = effect as? VisualEffectView.Transition
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = when (transition?.type) {
                        TransitionType.Fade, TransitionType.CrossFade -> sceneProgress.value
                        else -> 1f
                    }
                    translationX = shakeOffset.value + if (transition?.type == TransitionType.Slide) {
                        (1f - sceneProgress.value) * 96f
                    } else {
                        0f
                    }
                },
            ) {
                BackgroundLayer(state.background, imageAssets, imageLoader)
                CharacterLayer(state.characters, effect, imageAssets, imageLoader)
                CgLayer(state.cg?.assetId, imageAssets, imageLoader)
            }

            if (transition?.type == TransitionType.Flash && sceneProgress.value < 1f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - sceneProgress.value }
                        .background(Color.White),
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFE2B84B)))
                Text(
                    text = "NekoScript",
                    modifier = Modifier.padding(start = 9.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextButton(onClick = { onAction(GameAction.QuickSave) }) {
                    Text("Quick Save", color = Color.White, fontSize = 13.sp)
                }
                TextButton(onClick = { onAction(GameAction.QuickLoad) }) {
                    Text("Quick Load", color = Color.White, fontSize = 13.sp)
                }
            }

            UtilityPanel(
                state = state,
                onAction = onAction,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            )

            if (state.choices.isNotEmpty()) {
                ChoicePanel(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            state.dialogue?.let { dialogue ->
                var visibleCharacters by remember(dialogue.text, state.history.size) {
                    mutableIntStateOf(if (state.isSkipMode) dialogue.text.length else 0)
                }

                LaunchedEffect(dialogue.text, state.history.size, state.textSpeedMillis, state.isSkipMode) {
                    if (state.isSkipMode || state.textSpeedMillis == 0) {
                        visibleCharacters = dialogue.text.length
                    } else {
                        while (visibleCharacters < dialogue.text.length) {
                            delay(state.textSpeedMillis.toLong())
                            visibleCharacters++
                        }
                    }
                }

                LaunchedEffect(
                    dialogue.text,
                    state.history.size,
                    visibleCharacters,
                    state.isAutoMode,
                    state.isSkipMode,
                    state.choices.size,
                    state.isBacklogOpen,
                ) {
                    val isComplete = visibleCharacters >= dialogue.text.length
                    if (
                        isComplete &&
                        state.choices.isEmpty() &&
                        !state.isBacklogOpen &&
                        (state.isAutoMode || state.isSkipMode)
                    ) {
                        delay(if (state.isSkipMode) SKIP_DELAY_MILLIS else AUTO_DELAY_MILLIS)
                        onAction(GameAction.Next)
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clickable(enabled = state.choices.isEmpty()) {
                            if (visibleCharacters < dialogue.text.length) {
                                visibleCharacters = dialogue.text.length
                            } else {
                                onAction(GameAction.Next)
                            }
                        },
                    color = Color(0xED101513),
                    contentColor = Color.White,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
                    ) {
                        dialogue.speaker?.takeIf(String::isNotBlank)?.let { speaker ->
                            Text(
                                text = speaker,
                                color = Color(0xFFE2B84B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(7.dp))
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = dialogue.text.take(visibleCharacters),
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFF4F5F2),
                                fontSize = 19.sp,
                                lineHeight = 30.sp,
                            )
                            Text(
                                text = ">",
                                modifier = Modifier.padding(start = 20.dp),
                                color = Color(0xFFE2B84B),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (state.isBacklogOpen) {
                BacklogScreen(state, onAction)
            }
        }
    }
}

@Composable
private fun UtilityPanel(
    state: GameViewState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember(state.textSpeedMillis) {
        mutableFloatStateOf(state.textSpeedMillis.toFloat())
    }
    Surface(
        modifier = modifier.widthIn(min = 86.dp, max = 112.dp),
        color = Color(0xB5101513),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            TextButton(onClick = { onAction(GameAction.OpenBacklog) }, modifier = Modifier.fillMaxWidth()) {
                Text("Backlog", fontSize = 12.sp)
            }
            TextButton(onClick = { onAction(GameAction.ToggleAuto) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isAutoMode) "Auto: On" else "Auto", fontSize = 12.sp)
            }
            TextButton(onClick = { onAction(GameAction.Skip) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isSkipMode) "Skip: On" else "Skip", fontSize = 12.sp)
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onAction(GameAction.SetTextSpeed(sliderValue.roundToInt()))
                },
                valueRange = 0f..100f,
            )
        }
    }
}

@Composable
private fun BacklogScreen(
    state: GameViewState,
    onAction: (GameAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFA101513),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.safeContentPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Backlog", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onAction(GameAction.CloseBacklog) }) {
                    Text("Close")
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.history.forEach { entry ->
                    Column(Modifier.fillMaxWidth()) {
                        entry.speaker?.let {
                            Text(it, color = Color(0xFFE2B84B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(entry.text, color = Color(0xFFF4F5F2), fontSize = 17.sp, lineHeight = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundLayer(
    background: BackgroundView?,
    imageAssets: ImageAssetResolver,
    imageLoader: ImageLoader,
) {
    val image by produceState<ResolvedImageAsset?>(
        initialValue = null,
        key1 = background?.assetId,
        key2 = imageAssets,
    ) {
        value = background?.let { runCatching { imageAssets.resolveBackground(it.assetId) }.getOrNull() }
    }
    val color = if (background == null) Color(0xFF17201D) else Color(0xFF61756D)
    Box(Modifier.fillMaxSize().background(color)) {
        ResolvedAssetImage(
            image = image,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = background?.assetId?.let { "Background $it" },
        )
    }
}

@Composable
private fun CharacterLayer(
    characters: List<CharacterView>,
    effect: VisualEffectView?,
    imageAssets: ImageAssetResolver,
    imageLoader: ImageLoader,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        characters.forEach { character ->
            key(character.characterId) {
                CharacterSprite(
                    character = character,
                    effect = effect,
                    imageAssets = imageAssets,
                    imageLoader = imageLoader,
                    targetOffset = when (character.position) {
                        CharacterPosition.Left -> -maxWidth * 0.28f
                        CharacterPosition.Center -> 0.dp
                        CharacterPosition.Right -> maxWidth * 0.28f
                    },
                )
            }
        }
    }
}

@Composable
private fun CharacterSprite(
    character: CharacterView,
    effect: VisualEffectView?,
    imageAssets: ImageAssetResolver,
    imageLoader: ImageLoader,
    targetOffset: androidx.compose.ui.unit.Dp,
) {
    val image by produceState<ResolvedImageAsset?>(
        initialValue = null,
        key1 = character.characterId,
        key2 = character.expression,
        key3 = imageAssets,
    ) {
        value = runCatching {
            imageAssets.resolveCharacter(character.characterId, character.expression)
        }.getOrNull()
    }
    var imageLoaded by remember(image) { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageLoaded) 1f else 0f,
        animationSpec = tween(CHARACTER_FADE_IN_MILLIS),
        label = "character-image-alpha",
    )
    val move = effect as? VisualEffectView.CharacterMove
    val duration = if (move?.characterId == character.characterId) move.durationMillis else 0L
    val horizontalOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
        label = "character-position",
    )
    Box(
        modifier = Modifier.fillMaxSize().graphicsLayer { translationX = horizontalOffset.toPx() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        ResolvedAssetImage(
            image = image,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxWidth(CHARACTER_FRAME_WIDTH_FRACTION)
                .fillMaxHeight(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            contentDescription = "${character.characterId} ${character.expression.orEmpty()}".trim(),
            decodeOriginalSize = true,
            filterQuality = FilterQuality.High,
            alpha = imageAlpha,
            onSuccess = { imageLoaded = true },
        )
    }
}

@Composable
private fun CgLayer(
    assetId: String?,
    imageAssets: ImageAssetResolver,
    imageLoader: ImageLoader,
) {
    if (assetId == null) return
    val image by produceState<ResolvedImageAsset?>(
        initialValue = null,
        key1 = assetId,
        key2 = imageAssets,
    ) {
        value = runCatching { imageAssets.resolveCg(assetId) }.getOrNull()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF334C58)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = assetId.take(2).uppercase(),
            color = Color(0xFFE2B84B),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
        )
        ResolvedAssetImage(
            image = image,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = "CG $assetId",
        )
    }
}

@Composable
private fun ResolvedAssetImage(
    image: ResolvedImageAsset?,
    imageLoader: ImageLoader,
    modifier: Modifier,
    contentScale: ContentScale,
    contentDescription: String?,
    alignment: Alignment = Alignment.Center,
    decodeOriginalSize: Boolean = false,
    filterQuality: FilterQuality = FilterQuality.Low,
    alpha: Float = 1f,
    onSuccess: () -> Unit = {},
) {
    val platformContext = LocalPlatformContext.current
    val request = remember(image, platformContext, decodeOriginalSize) {
        image?.let {
            ImageRequest.Builder(platformContext)
                .data(it.data)
                .memoryCacheKey(it.cacheKey)
                .diskCachePolicy(CachePolicy.DISABLED)
                .apply {
                    if (decodeOriginalSize) size(Size.ORIGINAL)
                }
                .build()
        }
    }
    if (request != null) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
            filterQuality = filterQuality,
            alpha = alpha,
            onSuccess = { onSuccess() },
        )
    }
}

@Composable
private fun ChoicePanel(
    state: GameViewState,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(min = 280.dp, max = 520.dp).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.choices.forEach { choice ->
            Button(
                onClick = { onAction(GameAction.SelectChoice(choice.index)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = choice.isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xEDE8EAE4),
                    contentColor = Color(0xFF17201D),
                ),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = choice.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private const val AUTO_DELAY_MILLIS = 1_200L
private const val SKIP_DELAY_MILLIS = 60L
private const val CHARACTER_FADE_IN_MILLIS = 140
private const val CHARACTER_FRAME_WIDTH_FRACTION = 0.46f

private fun Modifier.logicalPointerInput(
    viewport: GameViewport,
    enabled: Boolean,
    onLogicalPointerDown: (LogicalPoint) -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(viewport) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val down = event.changes.firstOrNull { change ->
                    change.pressed && !change.previousPressed && !change.isConsumed
                } ?: continue
                val fit = viewport.fit(size.width.toDouble(), size.height.toDouble())
                fit.toLogicalOrNull(
                    ContainerPoint(
                        x = down.position.x.toDouble(),
                        y = down.position.y.toDouble(),
                    ),
                )?.let(onLogicalPointerDown)
            }
        }
    }
}
