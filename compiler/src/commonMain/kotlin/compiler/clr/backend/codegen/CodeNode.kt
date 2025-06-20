/*
   Copyright 2025 Nyayurin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package compiler.clr.backend.codegen

sealed class CodeNode {
	data object None : CodeNode()
	data class SingleLineList(val nodes: List<CodeNode>) : CodeNode()
	data class MultiLineList(val nodes: List<CodeNode>) : CodeNode()
	data class SingleLine(val nodes: List<CodeNode>) : CodeNode()
	data class MultiLine(val nodes: List<CodeNode>) : CodeNode()
	data class StringConcatenation(val nodes: List<CodeNode>) : CodeNode()
}

sealed class PlainNode : CodeNode() {
	data class Plain(val text: String) : PlainNode()
	data class SingleLine(val nodes: List<Plain>) : PlainNode()
	data class MultiLine(val nodes: List<Plain>) : PlainNode()
}

sealed class PaddingNode : CodeNode() {
	data class If(
		val condition: CodeNode,
		val content: CodeNode,
		val elseContent: CodeNode,
	) : PaddingNode()

	data class IfExp(
		val condition: CodeNode,
		val content: Pair<CodeNode, String>,
		val elseContent: Pair<CodeNode, String>,
	) : PaddingNode()

	data class Block(
		val nodes: List<CodeNode>,
	) : PaddingNode()

	data class BlockList(
		val nodes: List<CodeNode>,
	) : PaddingNode()
}

val noneCode = CodeNode.None

fun singleLineListCode(vararg nodes: CodeNode) = singleLineListCode(nodes.toList())
fun singleLineListCode(nodes: List<CodeNode>) = CodeNode.SingleLineList(nodes)

fun multiLineListCode(vararg nodes: CodeNode) = multiLineListCode(nodes.toList())
fun multiLineListCode(nodes: List<CodeNode>) = CodeNode.MultiLineList(nodes)

fun singleLineCode(vararg nodes: CodeNode) = singleLineCode(nodes.toList())
fun singleLineCode(nodes: List<CodeNode>) = CodeNode.SingleLine(nodes)

fun multiLineCode(vararg nodes: CodeNode) = multiLineCode(nodes.toList())
fun multiLineCode(nodes: List<CodeNode>) = CodeNode.MultiLine(nodes)

fun stringConcatenationCode(nodes: List<CodeNode>) = CodeNode.StringConcatenation(nodes)

fun plainPlain(text: String) = PlainNode.Plain(text)

fun singleLinePlain(vararg nodes: String) = singleLinePlain(nodes.toList())
fun singleLinePlain(nodes: List<String>) = PlainNode.SingleLine(nodes.map { plainPlain(it) })

fun multiLinePlain(vararg nodes: String) = multiLinePlain(nodes.toList())
fun multiLinePlain(nodes: List<String>) = PlainNode.MultiLine(nodes.map { plainPlain(it) })

fun ifPadding(
	condition: CodeNode,
	content: CodeNode,
	elseContent: CodeNode,
) = PaddingNode.If(condition, content, elseContent)

fun ifExpPadding(
	condition: CodeNode,
	content: Pair<CodeNode, String>,
	elseContent: Pair<CodeNode, String>,
) = PaddingNode.IfExp(condition, content, elseContent)

fun blockPadding(vararg nodes: CodeNode) = blockPadding(nodes.toList())
fun blockPadding(nodes: List<CodeNode>) = PaddingNode.Block(nodes)

fun blockListPadding(vararg nodes: CodeNode) = blockListPadding(nodes.toList())
fun blockListPadding(nodes: List<CodeNode>) = PaddingNode.BlockList(nodes)

fun CodeNode.appendSingleLine(vararg appends: CodeNode) = when (this) {
	is CodeNode.SingleLine -> singleLineCode(*nodes.toTypedArray(), *appends)
	else -> singleLineCode(this, *appends)
}

fun CodeNode.pushSingleLine(vararg appends: CodeNode) = when (this) {
	is CodeNode.SingleLine -> singleLineCode(*appends, *nodes.toTypedArray())
	else -> singleLineCode(*appends, this)
}