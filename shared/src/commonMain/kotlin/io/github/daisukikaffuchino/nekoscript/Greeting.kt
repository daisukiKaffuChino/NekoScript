package io.github.daisukikaffuchino.nekoscript

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}