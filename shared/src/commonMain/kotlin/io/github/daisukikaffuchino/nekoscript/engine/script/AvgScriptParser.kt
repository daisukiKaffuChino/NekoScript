package io.github.daisukikaffuchino.nekoscript.engine.script

import io.github.daisukikaffuchino.nekoscript.engine.character.CharacterPosition
import io.github.daisukikaffuchino.nekoscript.engine.choice.ChoiceItem
import io.github.daisukikaffuchino.nekoscript.engine.command.Command
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.variable.Variable
import io.github.daisukikaffuchino.nekoscript.engine.variable.ComparisonOperator
import io.github.daisukikaffuchino.nekoscript.engine.variable.Condition
import io.github.daisukikaffuchino.nekoscript.engine.effect.TransitionType

/**
 * Parser for the minimal, line-oriented NekoScript AVG syntax.
 *
 * Blank lines and lines beginning with `#` are ignored. Quoted arguments
 * support `\\n`, `\\r`, `\\t`, `\\"`, and `\\\\` escapes.
 */
class AvgScriptParser : ScriptParser {
    override fun parse(scriptId: String, source: String): Script {
        if (scriptId.isBlank()) {
            throw EngineException.ScriptParseError("script: $scriptId, line: 0: script id must not be blank")
        }

        val nodes = mutableListOf<ScriptNode>()
        val labels = mutableMapOf<String, Int>()

        val lines = source.lines()
        var index = 0
        while (index < lines.size) {
            val rawLine = lines[index]
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith('#')) {
                index++
                continue
            }
            if (rawLine.indentWidth(scriptId, lineNumber) != 0) {
                parseError(scriptId, lineNumber, "indentation", "top-level command must not be indented")
            }

            if (line == "choice:") {
                val parsedChoice = parseChoice(scriptId, lines, index)
                nodes += ScriptNode.CommandNode(parsedChoice.command)
                index = parsedChoice.nextLineIndex
                continue
            }
            if (line.startsWith("if ") && line.endsWith(':')) {
                val parsedIf = parseIf(scriptId, lines, index)
                nodes += ScriptNode.CommandNode(parsedIf.command)
                index = parsedIf.nextLineIndex
                continue
            }

            val tokens = tokenize(scriptId, lineNumber, line)
            val commandName = tokens.first().value
            when (commandName) {
                "label" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "label <name>")
                    val name = tokens[1].unquoted(scriptId, lineNumber, commandName, "label name")
                    if (name.isBlank()) parseError(scriptId, lineNumber, commandName, "label name must not be blank")
                    if (labels.containsKey(name)) {
                        parseError(scriptId, lineNumber, commandName, "duplicate label: $name")
                    }
                    labels[name] = nodes.size
                    nodes += ScriptNode.Label(name)
                }
                "say" -> {
                    if (tokens.size !in 2..3) {
                        parseError(scriptId, lineNumber, commandName, "expected: say [\"<speaker>\"] \"<text>\"")
                    }
                    val speaker = tokens.getOrNull(2)?.let {
                        tokens[1].quoted(scriptId, lineNumber, commandName, "speaker")
                    }
                    val textToken = if (speaker == null) tokens[1] else tokens[2]
                    nodes += ScriptNode.CommandNode(
                        Command.Say(
                            speaker = speaker,
                            text = textToken.quoted(scriptId, lineNumber, commandName, "text"),
                        ),
                    )
                }
                "background" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "background \"<id>\"")
                    nodes += ScriptNode.CommandNode(
                        Command.ChangeBackground(tokens[1].quoted(scriptId, lineNumber, commandName, "background id")),
                    )
                }
                "character" -> nodes += parseCharacter(scriptId, lineNumber, commandName, tokens)
                "hide" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "hide \"<character-id>\"")
                    nodes += ScriptNode.CommandNode(
                        Command.HideCharacter(tokens[1].quoted(scriptId, lineNumber, commandName, "character id")),
                    )
                }
                "jump" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "jump <label>")
                    nodes += ScriptNode.CommandNode(
                        Command.Jump(tokens[1].unquoted(scriptId, lineNumber, commandName, "label")),
                    )
                }
                "set" -> nodes += parseSet(scriptId, lineNumber, commandName, tokens)
                "play_bgm" -> {
                    if (tokens.size !in 2..3) {
                        parseError(scriptId, lineNumber, commandName, "expected: play_bgm \"<id>\" [loop|once]")
                    }
                    val mode = tokens.getOrNull(2)?.unquoted(
                        scriptId,
                        lineNumber,
                        commandName,
                        "playback mode",
                    )
                    val loop = when (mode) {
                        null, "loop" -> true
                        "once" -> false
                        else -> parseError(scriptId, lineNumber, commandName, "playback mode must be loop or once")
                    }
                    nodes += ScriptNode.CommandNode(
                        Command.PlayBgm(tokens[1].quoted(scriptId, lineNumber, commandName, "BGM id"), loop),
                    )
                }
                "stop_bgm" -> {
                    if (tokens.size !in 1..2) {
                        parseError(scriptId, lineNumber, commandName, "expected: stop_bgm [fade-millis]")
                    }
                    val fade = if (tokens.size == 1) {
                        0L
                    } else {
                        tokens[1].unquoted(scriptId, lineNumber, commandName, "fade duration").toLongOrNull()
                            ?: parseError(scriptId, lineNumber, commandName, "fade duration must be an integer")
                    }
                    if (fade < 0) parseError(scriptId, lineNumber, commandName, "fade duration must not be negative")
                    nodes += ScriptNode.CommandNode(Command.StopBgm(fade))
                }
                "play_se" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "play_se \"<id>\"")
                    nodes += ScriptNode.CommandNode(
                        Command.PlaySe(tokens[1].quoted(scriptId, lineNumber, commandName, "sound effect id")),
                    )
                }
                "play_voice" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "play_voice \"<id>\"")
                    nodes += ScriptNode.CommandNode(
                        Command.PlayVoice(tokens[1].quoted(scriptId, lineNumber, commandName, "voice id")),
                    )
                }
                "stop_voice" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 1, "stop_voice")
                    nodes += ScriptNode.CommandNode(Command.StopVoice)
                }
                "show_cg" -> {
                    if (tokens.size !in 2..4) {
                        parseError(scriptId, lineNumber, commandName, "expected: show_cg \"<id>\" [fade|crossfade|slide|flash] [duration]")
                    }
                    val transition = tokens.getOrNull(2)?.unquoted(
                        scriptId,
                        lineNumber,
                        commandName,
                        "transition",
                    )?.let { parseTransitionType(scriptId, lineNumber, commandName, it) }
                        ?: TransitionType.CrossFade
                    val duration = parseDuration(tokens.getOrNull(3), scriptId, lineNumber, commandName)
                    nodes += ScriptNode.CommandNode(
                        Command.ShowCg(tokens[1].quoted(scriptId, lineNumber, commandName, "CG id"), transition, duration),
                    )
                }
                "hide_cg" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 1, "hide_cg")
                    nodes += ScriptNode.CommandNode(Command.HideCg)
                }
                "transition" -> {
                    if (tokens.size !in 2..3) {
                        parseError(scriptId, lineNumber, commandName, "expected: transition <fade|crossfade|slide|flash> [duration]")
                    }
                    val type = parseTransitionType(
                        scriptId,
                        lineNumber,
                        commandName,
                        tokens[1].unquoted(scriptId, lineNumber, commandName, "transition"),
                    )
                    nodes += ScriptNode.CommandNode(
                        Command.Transition(type, parseDuration(tokens.getOrNull(2), scriptId, lineNumber, commandName)),
                    )
                }
                "shake" -> {
                    if (tokens.size !in 2..3) {
                        parseError(scriptId, lineNumber, commandName, "expected: shake <duration> [intensity]")
                    }
                    val duration = tokens[1].unquoted(scriptId, lineNumber, commandName, "duration").toLongOrNull()
                        ?: parseError(scriptId, lineNumber, commandName, "duration must be an integer")
                    val intensity = tokens.getOrNull(2)?.let {
                        it.unquoted(scriptId, lineNumber, commandName, "intensity").toFloatOrNull()
                            ?: parseError(scriptId, lineNumber, commandName, "intensity must be a number")
                    } ?: 1f
                    if (duration < 0) parseError(scriptId, lineNumber, commandName, "duration must not be negative")
                    if (intensity !in 0f..10f) parseError(scriptId, lineNumber, commandName, "intensity must be between 0 and 10")
                    nodes += ScriptNode.CommandNode(Command.Shake(duration, intensity))
                }
                "move_character" -> {
                    if (tokens.size !in 3..4) {
                        parseError(scriptId, lineNumber, commandName, "expected: move_character \"<id>\" <left|center|right> [duration]")
                    }
                    val characterId = tokens[1].quoted(scriptId, lineNumber, commandName, "character id")
                    val position = parsePosition(tokens[2], scriptId, lineNumber, commandName)
                    nodes += ScriptNode.CommandNode(
                        Command.MoveCharacter(
                            characterId,
                            position,
                            parseDuration(tokens.getOrNull(3), scriptId, lineNumber, commandName, 300L),
                        ),
                    )
                }
                "wait" -> {
                    expectShape(scriptId, lineNumber, commandName, tokens, 2, "wait <duration>")
                    val duration = tokens[1].unquoted(
                        scriptId,
                        lineNumber,
                        commandName,
                        "duration",
                    ).toLongOrNull() ?: parseError(
                        scriptId,
                        lineNumber,
                        commandName,
                        "duration must be an integer",
                    )
                    if (duration < 0) parseError(scriptId, lineNumber, commandName, "duration must not be negative")
                    nodes += ScriptNode.CommandNode(Command.Wait(duration))
                }
                else -> throw EngineException.UnknownCommand(
                    "script: $scriptId, line: $lineNumber, command: $commandName: unknown command",
                )
            }
            index++
        }

        return Script(scriptId, nodes, labels)
    }

    private fun parseChoice(scriptId: String, lines: List<String>, choiceLineIndex: Int): ParsedChoice {
        val items = mutableListOf<ChoiceItem>()
        var index = choiceLineIndex + 1
        while (index < lines.size) {
            val rawOption = lines[index]
            val optionLine = rawOption.trim()
            if (optionLine.isEmpty() || optionLine.startsWith('#')) {
                index++
                continue
            }
            val optionIndent = rawOption.indentWidth(scriptId, index + 1)
            if (optionIndent == 0) break
            if (!optionLine.endsWith(':')) {
                parseError(scriptId, index + 1, "choice", "expected quoted option followed by ':'")
            }
            val optionTokens = tokenize(scriptId, index + 1, optionLine.dropLast(1).trimEnd())
            if (optionTokens.size != 1) {
                parseError(scriptId, index + 1, "choice", "expected one quoted option before ':'")
            }
            val text = optionTokens.single().quoted(scriptId, index + 1, "choice", "option text")
            index++

            val commands = mutableListOf<Command>()
            while (index < lines.size) {
                val rawCommand = lines[index]
                val commandLine = rawCommand.trim()
                if (commandLine.isEmpty() || commandLine.startsWith('#')) {
                    index++
                    continue
                }
                val commandIndent = rawCommand.indentWidth(scriptId, index + 1)
                if (commandIndent <= optionIndent) break
                commands += parseBranchCommand(scriptId, index + 1, commandLine)
                index++
            }
            if (commands.isEmpty()) {
                parseError(scriptId, index.coerceAtMost(lines.lastIndex) + 1, "choice", "option '$text' has no commands")
            }
            items += ChoiceItem(text, commands)
        }
        if (items.isEmpty()) {
            parseError(scriptId, choiceLineIndex + 1, "choice", "choice must contain at least one option")
        }
        return ParsedChoice(Command.Choice(items), index)
    }

    private fun parseIf(scriptId: String, lines: List<String>, ifLineIndex: Int): ParsedIf {
        val headerLine = lines[ifLineIndex].trim().dropLast(1).trimEnd()
        val header = tokenize(scriptId, ifLineIndex + 1, headerLine)
        if (header.size != 4) {
            parseError(scriptId, ifLineIndex + 1, "if", "expected: if <variable> <operator> <value>:")
        }
        val name = header[1].unquoted(scriptId, ifLineIndex + 1, "if", "variable name")
        val operatorToken = header[2].unquoted(scriptId, ifLineIndex + 1, "if", "comparison operator")
        val operator = when (operatorToken) {
            "==" -> ComparisonOperator.Equal
            "!=" -> ComparisonOperator.NotEqual
            ">" -> ComparisonOperator.Greater
            ">=" -> ComparisonOperator.GreaterOrEqual
            "<" -> ComparisonOperator.Less
            "<=" -> ComparisonOperator.LessOrEqual
            else -> parseError(scriptId, ifLineIndex + 1, "if", "unsupported operator: $operatorToken")
        }
        val condition = Condition.VariableComparison(
            name = name,
            operator = operator,
            value = parseVariableLiteral(scriptId, ifLineIndex + 1, "if", header[3]),
        )

        val thenResult = parseCommandBranch(scriptId, lines, ifLineIndex + 1, "if")
        if (thenResult.commands.isEmpty()) {
            parseError(scriptId, ifLineIndex + 1, "if", "then branch must contain at least one command")
        }
        var index = thenResult.nextLineIndex
        var elseCommands = emptyList<Command>()
        if (index < lines.size && lines[index].trim() == "else:") {
            if (lines[index].indentWidth(scriptId, index + 1) != 0) {
                parseError(scriptId, index + 1, "else", "else must align with if")
            }
            val elseResult = parseCommandBranch(scriptId, lines, index + 1, "else")
            if (elseResult.commands.isEmpty()) {
                parseError(scriptId, index + 1, "else", "else branch must contain at least one command")
            }
            elseCommands = elseResult.commands
            index = elseResult.nextLineIndex
        }
        return ParsedIf(Command.If(condition, thenResult.commands, elseCommands), index)
    }

    private fun parseCommandBranch(
        scriptId: String,
        lines: List<String>,
        startIndex: Int,
        owner: String,
    ): ParsedBranch {
        val commands = mutableListOf<Command>()
        var index = startIndex
        while (index < lines.size) {
            val raw = lines[index]
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith('#')) {
                index++
                continue
            }
            if (raw.indentWidth(scriptId, index + 1) == 0) break
            commands += parseBranchCommand(scriptId, index + 1, line, owner)
            index++
        }
        return ParsedBranch(commands, index)
    }

    private fun parseBranchCommand(
        scriptId: String,
        lineNumber: Int,
        line: String,
        owner: String = "choice",
    ): Command {
        val tokens = tokenize(scriptId, lineNumber, line)
        return when (val commandName = tokens.first().value) {
            "set" -> parseSet(scriptId, lineNumber, commandName, tokens).command
            "jump" -> {
                expectShape(scriptId, lineNumber, commandName, tokens, 2, "jump <label>")
                Command.Jump(tokens[1].unquoted(scriptId, lineNumber, commandName, "label"))
            }
            else -> parseError(
                scriptId,
                lineNumber,
                commandName,
                "$owner branches currently support only set and jump",
            )
        }
    }

    private fun parseCharacter(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        tokens: List<Token>,
    ): ScriptNode.CommandNode {
        if (tokens.size !in 2..4) {
            parseError(
                scriptId,
                lineNumber,
                commandName,
                "expected: character \"<id>\" [\"<expression>\"] [left|center|right]",
            )
        }
        val characterId = tokens[1].quoted(scriptId, lineNumber, commandName, "character id")
        val expression = tokens.getOrNull(2)?.quoted(scriptId, lineNumber, commandName, "expression")
        val position = tokens.getOrNull(3)?.let {
            when (it.unquoted(scriptId, lineNumber, commandName, "position").lowercase()) {
                "left" -> CharacterPosition.Left
                "center" -> CharacterPosition.Center
                "right" -> CharacterPosition.Right
                else -> parseError(scriptId, lineNumber, commandName, "invalid character position: ${it.value}")
            }
        } ?: CharacterPosition.Center

        return ScriptNode.CommandNode(Command.ShowCharacter(characterId, expression, position))
    }

    private fun parseSet(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        tokens: List<Token>,
    ): ScriptNode.CommandNode {
        expectShape(scriptId, lineNumber, commandName, tokens, 4, "set <name> = <value>")
        val name = tokens[1].unquoted(scriptId, lineNumber, commandName, "variable name")
        if (tokens[2].quoted || tokens[2].value != "=") {
            parseError(scriptId, lineNumber, commandName, "expected '=' after variable name")
        }
        val value = parseVariableLiteral(scriptId, lineNumber, commandName, tokens[3])
        return ScriptNode.CommandNode(Command.SetVariable(name, value))
    }

    private fun parseVariableLiteral(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        token: Token,
    ): Variable = if (token.quoted) {
        Variable.StringValue(token.value)
    } else {
        parseUnquotedVariable(scriptId, lineNumber, commandName, token.value)
    }

    private fun parseTransitionType(scriptId: String, lineNumber: Int, commandName: String, value: String): TransitionType =
        when (value.lowercase()) {
            "fade" -> TransitionType.Fade
            "crossfade" -> TransitionType.CrossFade
            "slide" -> TransitionType.Slide
            "flash" -> TransitionType.Flash
            else -> parseError(scriptId, lineNumber, commandName, "invalid transition: $value")
        }

    private fun parsePosition(token: Token, scriptId: String, lineNumber: Int, commandName: String): CharacterPosition =
        when (token.unquoted(scriptId, lineNumber, commandName, "position").lowercase()) {
            "left" -> CharacterPosition.Left
            "center" -> CharacterPosition.Center
            "right" -> CharacterPosition.Right
            else -> parseError(scriptId, lineNumber, commandName, "invalid character position: ${token.value}")
        }

    private fun parseDuration(
        token: Token?,
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        default: Long = 400L,
    ): Long {
        val duration = token?.let {
            it.unquoted(scriptId, lineNumber, commandName, "duration").toLongOrNull()
                ?: parseError(scriptId, lineNumber, commandName, "duration must be an integer")
        } ?: default
        if (duration < 0) parseError(scriptId, lineNumber, commandName, "duration must not be negative")
        return duration
    }

    private fun parseUnquotedVariable(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        value: String,
    ): Variable = when (value) {
        "true" -> Variable.BooleanValue(true)
        "false" -> Variable.BooleanValue(false)
        else -> value.toIntOrNull()?.let(Variable::IntValue)
            ?: value.toDoubleOrNull()?.let(Variable::DoubleValue)
            ?: parseError(scriptId, lineNumber, commandName, "invalid variable literal: $value")
    }

    private fun expectShape(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        tokens: List<Token>,
        size: Int,
        expected: String,
    ) {
        if (tokens.size != size) parseError(scriptId, lineNumber, commandName, "expected: $expected")
    }

    private fun tokenize(scriptId: String, lineNumber: Int, line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < line.length) {
            while (index < line.length && line[index].isWhitespace()) index++
            if (index >= line.length) break

            if (line[index] == '"') {
                index++
                val value = StringBuilder()
                var closed = false
                while (index < line.length) {
                    val char = line[index++]
                    when {
                        char == '"' -> {
                            closed = true
                            break
                        }
                        char == '\\' -> {
                            if (index >= line.length) {
                                parseError(scriptId, lineNumber, "tokenizer", "unfinished escape sequence")
                            }
                            value.append(
                                when (val escaped = line[index++]) {
                                    'n' -> '\n'
                                    'r' -> '\r'
                                    't' -> '\t'
                                    '"' -> '"'
                                    '\\' -> '\\'
                                    else -> parseError(
                                        scriptId,
                                        lineNumber,
                                        "tokenizer",
                                        "unsupported escape sequence: \\$escaped",
                                    )
                                },
                            )
                        }
                        else -> value.append(char)
                    }
                }
                if (!closed) parseError(scriptId, lineNumber, "tokenizer", "unterminated quoted argument")
                if (index < line.length && !line[index].isWhitespace()) {
                    parseError(scriptId, lineNumber, "tokenizer", "expected whitespace after quoted argument")
                }
                tokens += Token(value.toString(), quoted = true)
            } else {
                val start = index
                while (index < line.length && !line[index].isWhitespace()) index++
                tokens += Token(line.substring(start, index), quoted = false)
            }
        }
        return tokens
    }

    private fun parseError(
        scriptId: String,
        lineNumber: Int,
        commandName: String,
        detail: String,
    ): Nothing = throw EngineException.ScriptParseError(
        "script: $scriptId, line: $lineNumber, command: $commandName: $detail",
    )

    private data class Token(val value: String, val quoted: Boolean) {
        fun quoted(scriptId: String, line: Int, command: String, role: String): String {
            if (!quoted) throw EngineException.ScriptParseError(
                "script: $scriptId, line: $line, command: $command: $role must be quoted",
            )
            return value
        }

        fun unquoted(scriptId: String, line: Int, command: String, role: String): String {
            if (quoted) throw EngineException.ScriptParseError(
                "script: $scriptId, line: $line, command: $command: $role must not be quoted",
            )
            return value
        }
    }

    private data class ParsedChoice(val command: Command.Choice, val nextLineIndex: Int)
    private data class ParsedIf(val command: Command.If, val nextLineIndex: Int)
    private data class ParsedBranch(val commands: List<Command>, val nextLineIndex: Int)

    private fun String.indentWidth(scriptId: String, lineNumber: Int): Int {
        var width = 0
        for (char in this) {
            when (char) {
                ' ' -> width++
                '\t' -> throw EngineException.ScriptParseError(
                    "script: $scriptId, line: $lineNumber, command: indentation: tabs are not supported",
                )
                else -> return width
            }
        }
        return width
    }
}
