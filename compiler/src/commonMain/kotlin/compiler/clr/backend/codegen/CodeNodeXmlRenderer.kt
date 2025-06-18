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

fun CodeNode.render() = Renderer().run { render(0) }

private class Renderer {
	fun CodeNode.render(padding: Int): String = when (this) {
		is CodeNode.None -> render(padding)
		is CodeNode.SingleLineList -> render(padding)
		is CodeNode.MultiLineList -> render(padding)
		is CodeNode.SingleLine -> render(padding)
		is CodeNode.MultiLine -> render(padding)
		is CodeNode.StringConcatenation -> render(padding)
		is PlainNode.Plain -> render(padding)
		is PlainNode.SingleLine -> render(padding)
		is PlainNode.MultiLine -> render(padding)
		is PaddingNode.If -> render(padding)
		is PaddingNode.IfExp -> render(padding)
		is PaddingNode.Block -> render(padding)
	}

	private fun CodeNode.None.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append("<CodeNode.None />")
	}

	private fun CodeNode.SingleLineList.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<CodeNode.CodeList>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</CodeNode.CodeList>")
	}

	private fun CodeNode.MultiLineList.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<CodeNode.MultiLineList>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</CodeNode.MultiLineList>")
	}

	private fun CodeNode.SingleLine.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<CodeNode.SingleLine>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</CodeNode.SingleLine>")
	}

	private fun CodeNode.MultiLine.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<CodeNode.MultiLine>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</CodeNode.MultiLine>")
	}

	private fun CodeNode.StringConcatenation.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<CodeNode.StringConcatenation>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</CodeNode.StringConcatenation>")
	}

	private fun PlainNode.Plain.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		append("<PlainNode.Plain>")
		append(text.replace("<", "&lt;").replace(">", "&gt;"))
		append("</PlainNode.Plain>")
	}

	private fun PlainNode.SingleLine.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<PlainNode.SingleLine>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</PlainNode.SingleLine>")
	}

	private fun PlainNode.MultiLine.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<PlainNode.MultiLine>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</PlainNode.MultiLine>")
	}

	private fun PaddingNode.If.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<PaddingNode.If>")

		repeat(padding + 1) { append("    ") }
		appendLine("<condition>")

		appendLine(condition.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</condition>")

		repeat(padding + 1) { append("    ") }
		appendLine("<content>")

		appendLine(content.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</content>")

		repeat(padding + 1) { append("    ") }
		appendLine("<else>")

		appendLine(elseContent.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</else>")

		repeat(padding) { append("    ") }
		append("</PaddingNode.If>")
	}

	private fun PaddingNode.IfExp.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<PaddingNode.IfExp>")

		repeat(padding + 1) { append("    ") }
		appendLine("<condition>")

		appendLine(condition.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</condition>")

		repeat(padding + 1) { append("    ") }
		appendLine("<content type=\"${content.second}\">")

		appendLine(content.first.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</content>")

		repeat(padding + 1) { append("    ") }
		appendLine("<else type=\"${content.second}\">")

		appendLine(elseContent.first.render(padding + 2))

		repeat(padding + 1) { append("    ") }
		appendLine("</else>")

		repeat(padding) { append("    ") }
		append("</PaddingNode.IfExp>")
	}

	private fun PaddingNode.Block.render(padding: Int) = buildString {
		repeat(padding) { append("    ") }
		appendLine("<PaddingNode.Block>")

		appendLine(nodes.joinToString("\n") { it.render(padding + 1) })

		repeat(padding) { append("    ") }
		append("</PaddingNode.Block>")
	}
}