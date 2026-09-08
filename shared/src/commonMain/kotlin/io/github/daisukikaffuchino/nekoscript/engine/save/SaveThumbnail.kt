package io.github.daisukikaffuchino.nekoscript.engine.save

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.runtime.GameState
import kotlinx.serialization.Serializable

/** Compact save-slot thumbnail used by the presentation layer. */
@Serializable
data class SaveThumbnail(
    val imageBytes: ByteArray? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val backgroundId: String? = null,
    val cgId: String? = null,
    val characters: List<SaveThumbnailCharacter> = emptyList(),
) {
    init {
        require(imageWidth == null || imageWidth > 0) { "Save thumbnail width must be positive." }
        require(imageHeight == null || imageHeight > 0) { "Save thumbnail height must be positive." }
        require(imageBytes == null || imageBytes.isNotEmpty()) { "Save thumbnail bytes must not be empty." }
        require(
            imageBytes != null ||
                characters.isNotEmpty() ||
                backgroundId != null ||
                cgId != null,
        ) {
            "Save thumbnail must contain image bytes or logical preview data."
        }
    }
}

@Serializable
data class SaveThumbnailCharacter(
    val characterId: String,
    val expression: String? = null,
    val position: CharacterPosition = CharacterPosition.Center,
)

fun GameState.toSaveThumbnail(): SaveThumbnail? {
    val thumbnailCharacters = characters.map {
        SaveThumbnailCharacter(
            characterId = it.characterId,
            expression = it.expression,
            position = it.position,
        )
    }
    return if (background != null || cgId != null || thumbnailCharacters.isNotEmpty()) {
        SaveThumbnail(
            backgroundId = background?.backgroundId,
            cgId = cgId,
            characters = thumbnailCharacters,
        )
    } else {
        null
    }
}

fun RawFrame.toSaveThumbnail(logicalPreview: SaveThumbnail? = null): SaveThumbnail? {
    val downsampled = downsampleForThumbnail()
    val encoded = encodeThumbnailFrameToBytes(downsampled)
    return if (encoded != null) {
        SaveThumbnail(
            imageBytes = encoded,
            imageWidth = downsampled.width,
            imageHeight = downsampled.height,
            backgroundId = logicalPreview?.backgroundId,
            cgId = logicalPreview?.cgId,
            characters = logicalPreview?.characters ?: emptyList(),
        )
    } else {
        logicalPreview
    }
}

fun SaveThumbnail.mergeLogicalPreview(logicalPreview: SaveThumbnail?): SaveThumbnail {
    if (logicalPreview == null) return this
    return copy(
        backgroundId = backgroundId ?: logicalPreview.backgroundId,
        cgId = cgId ?: logicalPreview.cgId,
        characters = if (characters.isNotEmpty()) characters else logicalPreview.characters,
    )
}

data class RawFrame(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(width > 0) { "Raw frame width must be positive." }
        require(height > 0) { "Raw frame height must be positive." }
        require(pixels.size >= width * height) { "Raw frame pixels must cover the full frame." }
    }
}

private fun RawFrame.downsampleForThumbnail(maxWidth: Int = 360): RawFrame {
    if (width <= maxWidth) return this
    val scale = maxWidth.toDouble() / width.toDouble()
    val targetWidth = maxWidth
    val targetHeight = maxOf(1, (height * scale).toInt())
    val downsampled = IntArray(targetWidth * targetHeight)
    for (y in 0 until targetHeight) {
        val srcY = ((y + 0.5) / targetHeight * height - 0.5).coerceIn(0.0, (height - 1).toDouble())
        val y0 = srcY.toInt()
        val y1 = minOf(y0 + 1, height - 1)
        val yWeight = srcY - y0
        for (x in 0 until targetWidth) {
            val srcX = ((x + 0.5) / targetWidth * width - 0.5).coerceIn(0.0, (width - 1).toDouble())
            val x0 = srcX.toInt()
            val x1 = minOf(x0 + 1, width - 1)
            val xWeight = srcX - x0
            downsampled[y * targetWidth + x] = bilinearSample(
                pixels[y0 * width + x0],
                pixels[y0 * width + x1],
                pixels[y1 * width + x0],
                pixels[y1 * width + x1],
                xWeight,
                yWeight,
            )
        }
    }
    return RawFrame(targetWidth, targetHeight, downsampled)
}

private fun bilinearSample(
    topLeft: Int,
    topRight: Int,
    bottomLeft: Int,
    bottomRight: Int,
    xWeight: Double,
    yWeight: Double,
): Int {
    fun channel(color: Int, shift: Int): Int = (color ushr shift) and 0xFF
    fun mix(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    fun mixChannel(left: Int, right: Int, t: Double): Double = mix(left.toDouble(), right.toDouble(), t)
    val aTop = mixChannel(channel(topLeft, 24), channel(topRight, 24), xWeight)
    val rTop = mixChannel(channel(topLeft, 16), channel(topRight, 16), xWeight)
    val gTop = mixChannel(channel(topLeft, 8), channel(topRight, 8), xWeight)
    val bTop = mixChannel(channel(topLeft, 0), channel(topRight, 0), xWeight)
    val aBottom = mixChannel(channel(bottomLeft, 24), channel(bottomRight, 24), xWeight)
    val rBottom = mixChannel(channel(bottomLeft, 16), channel(bottomRight, 16), xWeight)
    val gBottom = mixChannel(channel(bottomLeft, 8), channel(bottomRight, 8), xWeight)
    val bBottom = mixChannel(channel(bottomLeft, 0), channel(bottomRight, 0), xWeight)
    val a = mix(aTop, aBottom, yWeight).toInt().coerceIn(0, 255)
    val r = mix(rTop, rBottom, yWeight).toInt().coerceIn(0, 255)
    val g = mix(gTop, gBottom, yWeight).toInt().coerceIn(0, 255)
    val b = mix(bTop, bBottom, yWeight).toInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

expect fun encodeThumbnailFrameToBytes(frame: RawFrame): ByteArray?
