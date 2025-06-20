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

fun CodeNode.visit() = Visitor().run { visit(0) }

private class Visitor {
	fun CodeNode.visit(padding: Int): String = when (this) {
		is CodeNode.None -> ""
		is CodeNode.SingleLineList -> visit(padding)
		is CodeNode.MultiLineList -> visit(padding)
		is CodeNode.SingleLine -> visit(padding)
		is CodeNode.MultiLine -> visit(padding)
		is CodeNode.StringConcatenation -> visit(padding)
		is PlainNode.Plain -> visit(padding)
		is PlainNode.SingleLine -> visit(padding)
		is PlainNode.MultiLine -> visit(padding)
		is PaddingNode.If -> visit(padding)
		is PaddingNode.IfExp -> visit(padding)
		is PaddingNode.Block -> visit(padding)
		is PaddingNode.BlockList -> visit(padding)
	}

	private fun CodeNode.SingleLineList.visit(padding: Int) = buildString {
		append(nodes.joinToString("") { it.visit(padding) })
	}

	private fun CodeNode.MultiLineList.visit(padding: Int) = buildString {
		append(nodes.joinToString("\n") { it.visit(padding) })
	}

	private fun CodeNode.SingleLine.visit(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append(nodes.joinToString("") { it.visit(padding) })
	}

	private fun CodeNode.MultiLine.visit(padding: Int) = nodes.joinToString("\n") {
		buildString {
			when (it) {
				is CodeNode.None,
				is CodeNode.SingleLine,
				is PaddingNode.Block,
					-> {
				}

				else -> repeat(padding) { append("    ") }
			}
			append(it.visit(padding))
		}
	}

	private fun CodeNode.StringConcatenation.visit(padding: Int) = nodes.joinToString("", "$\"", "\"") {
		"{(${it.visit(padding)})}"
	}

	private fun PlainNode.Plain.visit(padding: Int) = text

	private fun PlainNode.SingleLine.visit(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append(nodes.joinToString("") { it.visit(padding) })
	}

	private fun PlainNode.MultiLine.visit(padding: Int) = nodes.joinToString("\n") {
		buildString {
			repeat(padding) { append("    ") }
			append(it.visit(padding))
		}
	}

	private fun PaddingNode.If.visit(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append("if (")
		append(condition.visit(padding))
		appendLine(")")
		append(content.visit(padding))
		if (elseContent != noneCode) {
			appendLine()
			repeat(padding) { append("    ") }
			appendLine("else")
			append(elseContent.visit(padding))
		}
	}

	private fun PaddingNode.IfExp.visit(padding: Int) = buildString {
		append("(")
		append(condition.visit(padding))
		appendLine(")")
		repeat(padding + 1) { append("    ") }
		append("? ")
		when (content.first is PaddingNode.Block) {
			true -> {
				append("(")
				append("(global::System.Func<${content.second}>)")
				append("(")
				appendLine("() =>")
				append(content.first.visit(padding + 1))
				append(")")
				append(")")
				append("()")
			}
			else -> {
				append("(")
				append(content.first.visit(padding + 1))
				append(")")
			}
		}

		appendLine()
		repeat(padding + 1) { append("    ") }
		append(": ")
		when (elseContent.first is PaddingNode.Block) {
			true -> {
				append("(")
				append("(global::System.Func<${elseContent.second}>)")
				append("(")
				appendLine("() =>")
				append(elseContent.first.visit(padding + 1))
				append(")")
				append(")")
				append("()")
			}
			else -> {
				append("(")
				append(elseContent.first.visit(padding + 1))
				append(")")
			}
		}
	}

	private fun PaddingNode.Block.visit(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append("{")
		appendLine()
		append(nodes.joinToString("\n") { it.visit(padding + 1) })
		appendLine()
		repeat(padding) { append("    ") }
		append("}")
	}

	private fun PaddingNode.BlockList.visit(padding: Int) = buildString {
		append("{")
		appendLine()
		append(nodes.joinToString("\n") { it.visit(padding + 1) })
		appendLine()
		repeat(padding) { append("    ") }
		append("}")
	}
}