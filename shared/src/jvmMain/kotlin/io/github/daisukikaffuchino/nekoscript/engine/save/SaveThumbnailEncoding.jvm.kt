package io.github.daisukikaffuchino.nekoscript.engine.save

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun encodeThumbnailFrameToBytes(frame: RawFrame): ByteArray? {
    val image = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB)
    image.setRGB(0, 0, frame.width, frame.height, frame.pixels, 0, frame.width)
    return ByteArrayOutputStream().use { stream ->
        if (ImageIO.write(image, "png", stream)) {
            stream.toByteArray()
        } else {
            null
        }
    }
}
