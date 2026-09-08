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
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadIssue
import io.github.daisukikaffuchino.nekoscript.engine.interaction.HotspotRegistry
import io.github.daisukikaffuchino.nekoscript.engine.runtime.BackgroundView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.CharacterView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.DebugAssetView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameAction
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameDebugView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameViewState
import io.github.daisukikaffuchino.nekoscript.engine.runtime.SaveMenuMode
import io.github.daisukikaffuchino.nekoscript.engine.runtime.SaveMenuView
import io.github.daisukikaffuchino.nekoscript.engine.runtime.VisualEffectView
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType
import io.github.daisukikaffuchino.nekoscript.engine.viewport.ContainerPoint
import io.github.daisukikaffuchino.nekoscript.engine.viewport.GameViewport
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalPoint
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveSlotSummary
import io.github.daisukikaffuchino.nekoscript.ui.asset.ImageAssetResolver
import io.github.daisukikaffuchino.nekoscript.ui.asset.ResolvedImageAsset
import kotlinx.coroutines.CancellationException
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
    assetLoadIssues: List<AssetLoadIssue> = emptyList(),
    renderBackendInfo: RenderBackendInfo = platformRenderBackendInfo(),
) {
    val viewport = state.viewport
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
                enabled = !state.isBacklogOpen && state.saveMenu == null && hotspotRegistry != null,
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
                TextButton(onClick = { onAction(GameAction.OpenSaveMenu) }) {
                    Text("Save", color = Color.White, fontSize = 13.sp)
                }
                TextButton(onClick = { onAction(GameAction.OpenLoadMenu) }) {
                    Text("Load", color = Color.White, fontSize = 13.sp)
                }
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

            state.debug?.let { debug ->
                DebugPanel(
                    debug = debug,
                    assetLoadIssues = assetLoadIssues,
                    renderBackendInfo = renderBackendInfo,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 48.dp),
                )
            }

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

            state.saveMenu?.let { saveMenu ->
                SaveMenuScreen(
                    saveMenu = saveMenu,
                    onAction = onAction,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun DebugPanel(
    debug: GameDebugView,
    assetLoadIssues: List<AssetLoadIssue>,
    renderBackendInfo: RenderBackendInfo,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(true) }
    Surface(
        modifier = modifier.widthIn(min = 300.dp, max = 460.dp),
        color = Color(0xD9101513),
        contentColor = Color(0xFFE9ECE8),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("DEBUG", color = Color(0xFFE2B84B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(if (isExpanded) "^" else "v", color = Color(0xFFE2B84B), fontSize = 15.sp)
                }
            }
            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    DebugLine("Render", renderBackendInfo.displayName)
                    DebugLine("SCRIPT", "${debug.scriptId} -> ${debug.nextNodeIndex}")
                    debug.textId?.let { DebugLine("TEXT", it) }
                    debug.background?.let { DebugAssetLine("BG", it, assetLoadIssues) }
                    debug.characters.forEach { DebugAssetLine("CHAR", it, assetLoadIssues) }
                    debug.cg?.let { DebugAssetLine("CG", it, assetLoadIssues) }
                    debug.bgm?.let { DebugAssetLine("BGM", it, assetLoadIssues) }
                    debug.voice?.let { DebugAssetLine("VOICE", it, assetLoadIssues) }
                    debug.effect?.let { DebugLine("EFFECT", it) }
                    val currentAssets = buildList {
                        debug.background?.let(::add)
                        addAll(debug.characters)
                        debug.cg?.let(::add)
                        debug.bgm?.let(::add)
                        debug.voice?.let(::add)
                    }
                    assetLoadIssues
                        .filterNot { issue -> currentAssets.any { it.matches(issue) } }
                        .forEach { issue ->
                            DebugLine(
                                label = "MISSING ${issue.assetType.uppercase()}",
                                value = "${issue.assetId} | ${issue.location}",
                                isError = true,
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun DebugAssetLine(
    label: String,
    asset: DebugAssetView,
    issues: List<AssetLoadIssue>,
) {
    DebugLine(
        label = label,
        value = "${asset.id} | ${asset.location}",
        isError = issues.any(asset::matches),
    )
}

private fun DebugAssetView.matches(issue: AssetLoadIssue): Boolean =
    id == issue.assetId && location == issue.location

@Composable
private fun DebugLine(
    label: String,
    value: String,
    isError: Boolean = false,
) {
    Text(
        text = "$label  $value",
        color = if (isError) Color(0xFFFF6B6B) else Color(0xFFE9ECE8),
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )
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
        modifier = Modifier.fillMaxSize().clickable(onClick = {}),
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
private fun SaveMenuScreen(
    saveMenu: SaveMenuView,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeLabel = when (saveMenu.mode) {
        SaveMenuMode.Save -> "Save"
        SaveMenuMode.Load -> "Load"
    }
    Surface(
        modifier = modifier.fillMaxSize().clickable(onClick = {}),
        color = Color(0xF2101513),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(modeLabel, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (saveMenu.mode == SaveMenuMode.Save) "Choose a slot to overwrite." else "Choose a slot to load.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
                TextButton(onClick = { onAction(GameAction.CloseSaveMenu) }) {
                    Text("Close")
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                saveMenu.slots.forEach { slot ->
                    SaveSlotCard(
                        slot = slot,
                        mode = saveMenu.mode,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveSlotCard(
    slot: SaveSlotSummary,
    mode: SaveMenuMode,
    onAction: (GameAction) -> Unit,
) {
    val isLoadable = !slot.isEmpty && !slot.isBroken
    val primaryLabel = when (mode) {
        SaveMenuMode.Save -> if (slot.isEmpty) "Save" else "Overwrite"
        SaveMenuMode.Load -> if (isLoadable) "Load" else "Unavailable"
    }
    val primaryEnabled = when (mode) {
        SaveMenuMode.Save -> true
        SaveMenuMode.Load -> isLoadable
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when {
            slot.isBroken -> Color(0xFF4D2323)
            slot.isEmpty -> Color(0xB41D2420)
            else -> Color(0xD7222B27)
        },
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = primaryEnabled) {
                    when (mode) {
                        SaveMenuMode.Save -> onAction(GameAction.SaveToSlot(slot.slotId))
                        SaveMenuMode.Load -> onAction(GameAction.LoadFromSlot(slot.slotId))
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.displayName,
                    color = when {
                        slot.isBroken -> Color(0xFFFF8C8C)
                        slot.isEmpty -> Color(0xFFE2B84B).copy(alpha = 0.86f)
                        else -> Color(0xFFE2B84B)
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        slot.isBroken -> slot.errorMessage ?: "Broken save"
                        slot.isEmpty -> "Empty slot"
                        else -> buildString {
                            slot.speaker?.takeIf(String::isNotBlank)?.let {
                                append(it)
                                append(" · ")
                            }
                            append(slot.previewText ?: "No preview")
                        }
                    },
                    color = when {
                        slot.isBroken -> Color(0xFFFFB3B3)
                        slot.isEmpty -> Color.White.copy(alpha = 0.55f)
                        else -> Color.White.copy(alpha = 0.92f)
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                slot.contextLabel?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = primaryLabel,
                    color = if (primaryEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                slot.timestamp?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "ts: $it",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.height(7.dp))
                TextButton(
                    onClick = { onAction(GameAction.DeleteSaveSlot(slot.slotId)) },
                    enabled = !slot.isEmpty,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text("Delete", fontSize = 11.sp)
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
    val loadState by produceState(
        initialValue = ImageLoadState(),
        key1 = background?.assetId,
        key2 = imageAssets,
    ) {
        value = loadImage { background?.let { imageAssets.resolveBackground(it.assetId) } }
    }
    val color = if (background == null) Color(0xFF17201D) else Color(0xFF61756D)
    Box(Modifier.fillMaxSize().background(color)) {
        ResolvedAssetImage(
            image = loadState.image,
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
    val loadState by produceState(
        initialValue = ImageLoadState(),
        key1 = character.characterId,
        key2 = character.expression,
        key3 = imageAssets,
    ) {
        value = loadImage { imageAssets.resolveCharacter(character.characterId, character.expression) }
    }
    var imageLoaded by remember(loadState.image) { mutableStateOf(false) }
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
            image = loadState.image,
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
    val loadState by produceState(
        initialValue = ImageLoadState(),
        key1 = assetId,
        key2 = imageAssets,
    ) {
        value = loadImage { imageAssets.resolveCg(assetId) }
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
            image = loadState.image,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = "CG $assetId",
        )
    }
}

private data class ImageLoadState(
    val image: ResolvedImageAsset? = null,
)

private suspend fun loadImage(resolve: suspend () -> ResolvedImageAsset?): ImageLoadState = try {
    ImageLoadState(image = resolve())
} catch (error: Throwable) {
    if (error is CancellationException) throw error
    ImageLoadState()
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
