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

import compiler.clr.backend.ClrBackendContext
import compiler.clr.backend.mapping.IrTypeMapper
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Modality.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.javac.resolve.classId

inline fun <T> List<T>.join(separator: T): List<T> = when {
	isEmpty() -> this
	else -> zipWithNext { node, _ -> listOf(node, separator) }.flatten() + last()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
class ClassCodegen(val context: ClrBackendContext) {
	val typeMapper = IrTypeMapper(context)

	fun IrFile.visit(): CodeNode {
		val `package` = packageFqName

		val content = multiLineCode(
			*declarations
				.map { declaration ->
					when (declaration) {
						is IrClass -> declaration.visit()
						else -> multiLinePlain(
							"/*",
							"Unsupported declaration: ${declaration::class.java.simpleName}",
							"at IrFile.visit: $this",
							"*/",
						)
					}
				}
				.join(noneCode)
				.toTypedArray()
		).let {
			when (it.nodes.size == 1) {
				true -> it.nodes.single()
				else -> it
			}
		}

		return when (!`package`.isRoot) {
			true -> multiLineCode(
				singleLinePlain("namespace $`package`"),
				blockPadding(content),
			)

			else -> content
		}
	}

	fun IrClass.visit() = when {
		isFileClass -> visitFileClass()
		kind == CLASS -> visitClass()
		kind == INTERFACE -> visitInterface()
		kind == ENUM_CLASS -> visitEnumClass()
		kind == ENUM_ENTRY -> multiLinePlain(
			"/*",
			"TODO enum entry",
			"at IrClass.visit: $this",
			"*/",
		)

		kind == ANNOTATION_CLASS -> visitAnnotationClass()
		kind == OBJECT -> visitObject()
		else -> multiLinePlain(
			"/*",
			"Unknown class type",
			"at IrClass.visit: $this",
			"*/",
		)
	}

	fun IrClass.visitFileClass() = multiLineCode(
		singleLinePlain(
			"[global::kotlin.clr.KotlinFileClass]"
		),
		singleLineCode(
			visibility.delegate.visit(),
			plainPlain("static "),
			plainPlain("class "),
			plainPlain(name.asString())
		),
		blockPadding(
			declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
		),
	)

	fun IrClass.visitClass() = multiLineCode(
		singleLineCode(
			visibility.delegate.visit(),
			modality.visit(),
			plainPlain("class "),
			plainPlain(name.asString()),
			plainPlain(" : "),
			plainPlain(superTypes.joinToString(", ") { typeMapper.mapType(it) }),
		),
		blockPadding(
			declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
		),
	)

	fun IrClass.visitInterface() = multiLineCode(
		singleLineCode(
			buildList {
				add(visibility.delegate.visit())
				add(modality.visit())
				add(plainPlain("interface "))
				add(plainPlain(name.asString()))
				if (superTypes.isNotEmpty()) {
					add(plainPlain(" : "))
					add(plainPlain(superTypes.joinToString(", ") { typeMapper.mapType(it) }))
				}
			}
		),
		blockPadding(
			declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
		),
	)

	fun IrClass.visitEnumClass() = multiLineCode(
		singleLineCode(
			visibility.delegate.visit(),
			modality.visit(),
			plainPlain("enum "),
			plainPlain(name.asString()),
		),
		blockPadding(
			declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
		),
	)

	fun IrClass.visitAnnotationClass() = multiLineCode(
		singleLineCode(
			visibility.delegate.visit(),
			modality.visit(),
			plainPlain("class "),
			plainPlain(name.asString()),
			plainPlain(" : global::System.Attribute"),
		),
		blockPadding(
			declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
		),
	)

	fun IrClass.visitObject() = multiLineCode(
		singleLineCode(
			visibility.delegate.visit(),
			modality.visit(),
			plainPlain("class "),
			plainPlain(name.asString()),
			plainPlain(" : "),
			plainPlain(superTypes.joinToString(", ") { typeMapper.mapType(it) }),
		),
		blockPadding(
			singleLinePlain(
				"public static ",
				typeMapper.mapType(defaultType),
				" INSTANCE { get; } = new ",
				typeMapper.mapType(defaultType),
				"();",
			),
			noneCode,
			*declarations
				.mapNotNull { it.visit() }
				.join(noneCode)
				.toTypedArray(),
		),
	)

	fun IrDeclaration.visit(): CodeNode? {
		if (isFakeOverride) return null
		return when (this) {
			is IrClass -> visit()
			is IrFunction -> visit()
			is IrProperty -> visit()
			else -> multiLinePlain(
				"/*",
				"Unsupported declaration: ${this::class.java.simpleName}",
				"at IrDeclaration.visit",
				"*/"
			)
		}
	}

	fun IrFunction.visit() = when (this) {
		is IrConstructor -> visit()
		else -> multiLineCode(
			buildList {
				if (returnType.isNothing()) {
					add(singleLineCode(plainPlain("[global::System.Diagnostics.CodeAnalysis.DoesNotReturnAttribute]")))
				}
				if (extensionReceiverParameter != null) {
					add(singleLineCode(plainPlain("[global::kotlin.clr.KotlinExtension]")))
				}
				add(
					singleLineCode(
						buildList {
							val isStatic = when {
								parent is IrFile -> true
								isStatic -> true
								else -> false
							}

							val returnType = typeMapper.mapReturnType(returnType)

							val parameters = valueParameters.map {
								typeMapper.mapType(it.type) to it.name.asString()
							}

							add(visibility.delegate.visit())
							if (isStatic) {
								add(plainPlain("static "))
							}
							add(plainPlain("$returnType "))
							add(plainPlain("${name.asString()}("))
							extensionReceiverParameter?.let {
								add(plainPlain("${typeMapper.mapType(it.type)} receiver, "))
							}
							add(plainPlain(parameters.joinToString(", ") { "${it.first} ${it.second}" }))
							add(plainPlain(")"))
						}
					)
				)
				add(body?.visit() ?: blockPadding())
			}
		)
	}

	fun IrConstructor.visit() = multiLineCode(
		singleLineCode(
			buildList {
				val className = (parent as? IrClass)?.name?.asString()!!

				val parameters = valueParameters.map {
					typeMapper.mapType(it.type) to it.name.asString()
				}

				add(visibility.delegate.visit())
				add(plainPlain("$className("))
				add(plainPlain(parameters.joinToString(", ") { "${it.first} ${it.second}" }))
				add(plainPlain(")"))
				if (body?.statements?.get(0) is IrDelegatingConstructorCall) {
					add(plainPlain(" : "))
					add((body?.statements?.get(0) as IrDelegatingConstructorCall).visit())
				}
			}
		),
		blockPadding(
			buildList {
				body?.visit()?.let {
					add(it)
				}
				addAll(
					parentAsClass.declarations
						.filterIsInstance<IrProperty>()
						.map { it to it.backingField?.initializer?.expression }
						.filter { it.second != null }
						.map { it.first to it.second!! }
						.map { (property, initializer) ->
							singleLineCode(
								plainPlain("this.${property.name.asString()} = "),
								initializer.visitUsing(),
								plainPlain(";")
							)
						}
				)
			}
		),
	)

	fun IrProperty.visit() = multiLineCode(
		singleLineCode(
			buildList {
				val isStatic = when {
					parent is IrFile -> true
					(backingField?.isStatic ?: getter?.isStatic ?: setter?.isStatic) == true -> true
					else -> false
				}
				add(visibility.delegate.visit())
				if (isStatic) {
					add(plainPlain("static "))
				}
				when (modality) {
					FINAL -> {}
					SEALED -> add(
						multiLinePlain(
							"/*",
							"TODO sealed",
							"at IrProperty.visit: ${this@visit}",
							"*/"
						)
					)

					OPEN -> add(plainPlain("virtual "))
					ABSTRACT -> add(plainPlain("abstract "))
				}

				val type = getter?.returnType
					?: setter?.returnType
					?: error("Property must have either getter or setter: $this")
				add(plainPlain(typeMapper.mapType(type)))
				add(plainPlain(" "))

				add(plainPlain(name.asString()))
			}
		),
		blockPadding(
			multiLinePlain(
				buildList {
					if (getter != null) {
						add("get;")
					}
					if (setter != null) {
						add("set;")
					}
				}
			)
		),
	)

	fun IrBody.visit() = when (this) {
		is IrBlockBody -> statements
			.filterNot { it is IrDelegatingConstructorCall }
			.filterNot { it is IrInstanceInitializerCall }
			.map {
				when (it) {
					is IrWhen -> it.visit()
					is IrExpression -> singleLineCode(it.visit(), plainPlain(";"))
					is IrVariable -> it.visit().appendSingleLine(plainPlain(";"))
					else -> multiLinePlain(
						"/*",
						"Unsupported statement: ${it::class.java.simpleName}",
						"at IrBody.visit: $this",
						"is IrBlockBody",
						"*/",
					)
				}
			}
			.let {
				when (it.isEmpty()) {
					true -> null
					else -> blockPadding(it)
				}
			}

		else -> statements
			.filterNot { it is IrDelegatingConstructorCall }
			.filterNot { it is IrInstanceInitializerCall }
			.map {
				when (it) {
					is IrWhen -> it.visit()
					is IrExpression -> singleLineCode(it.visit(), plainPlain(";"))
					is IrVariable -> it.visit().appendSingleLine(plainPlain(";"))
					else -> multiLinePlain(
						"/*",
						"Unsupported statement: ${it::class.java.simpleName}",
						"at IrBody.visit: $this",
						"*/",
					)
				}
			}
			.let {
				when (it.isEmpty()) {
					true -> null
					else -> multiLineCode(it)
				}
			}
	}

	fun IrDelegatingConstructorCall.visit() = singleLineListCode(
		plainPlain("base("),
		*valueArguments
			.filterNotNull()
			.map { it.visitUsing() }
			.join(plainPlain(", "))
			.toTypedArray(),
		plainPlain(")"),
	)

	fun IrGetObjectValue.visit() = singleLineListCode(
		plainPlain(typeMapper.mapType(symbol.owner.defaultType)),
		plainPlain(".INSTANCE"),
	)

	fun IrConstructorCall.visit(): CodeNode = singleLineListCode(
		buildList {
			val constructedClass = symbol.owner.parent as IrClass
			val packageFragment = constructedClass.getPackageFragment()

			add(plainPlain("new global::"))
			if (!packageFragment.packageFqName.isRoot) {
				add(plainPlain(packageFragment.packageFqName.asString()))
				add(plainPlain("."))
			}
			add(plainPlain(constructedClass.name.asString()))
			add(plainPlain("("))
			valueArguments
				.filterNotNull()
				.map { it.visitUsing() }
				.join(plainPlain(", "))
				.forEach { add(it) }
			add(plainPlain(")"))
		}
	)

	fun IrCall.visitClassParent(): CodeNode {
		val function = symbol.owner
		val parent = function.parent as IrClass

		val isStatic = function.isStatic
		val isCompanionStatic = (function as? Fir2IrLazySimpleFunction)?.fir?.annotations?.any {
			it.annotationTypeRef.coneType.classId == classId("kotlin.clr", "ClrStatic")
		} == true

		return when {
			isStatic -> singleLineListCode(
				buildList {
					add(plainPlain(typeMapper.mapType(parent.defaultType)))
					add(plainPlain("."))
					when (function.name.isSpecial) {
						true -> {
							val name = function.name.asString()

							when {
								name.startsWith("<set-") -> {
									add(plainPlain(name.substring("<set-".length, name.length - 1)))
									add(plainPlain(" = "))
									add(valueArguments[0]!!.visitUsing())
								}

								name.startsWith("<get-") -> {
									add(plainPlain(name.substring("<get-".length, name.length - 1)))
								}

								else -> add(
									multiLinePlain(
										"/*",
										"Unsupported name: $name",
										"at IrCall.visitClassParent: ${this@visitClassParent}",
										"is static",
										"is special",
										"*/",
									)
								)
							}
						}

						else -> {
							add(plainPlain(function.name.asString()))
							add(plainPlain("("))
							receiverAndArgs()
								.map { it.visitUsing() }
								.join(plainPlain(", "))
								.forEach { add(it) }
							add(plainPlain(")"))
						}
					}
				}
			)

			isCompanionStatic -> singleLineListCode(
				buildList {
					val outer = parent.parent as? IrClass ?: return multiLinePlain(
						"/*",
						"Expected IrClass but got ${parent::class.java.simpleName}",
						"at IrCall.visitClassParent: ${this@visitClassParent}",
						"is companion static",
						"*/",
					)
					add(plainPlain(typeMapper.mapType(outer.defaultType)))
					add(plainPlain("."))
					when (function.name.isSpecial) {
						true -> {
							val name = function.name.asString()
							when {
								name.startsWith("<set-") -> {
									add(plainPlain(name.substring("<set-".length, name.length - 1)))
									add(plainPlain(" = "))
									add(valueArguments[0]!!.visitUsing())
								}

								name.startsWith("<get-") -> {
									add(plainPlain(name.substring("<get-".length, name.length - 1)))
								}

								else -> add(
									multiLinePlain(
										"/*",
										"Unsupported name: $name",
										"at IrCall.visitClassParent: ${this@visitClassParent}",
										"is companion static",
										"is special",
										"*/",
									)
								)
							}
						}

						else -> {
							add(plainPlain(function.name.asString()))
							add(plainPlain("("))
							receiverAndArgs()
								.map { it.visitUsing() }
								.join(plainPlain(", "))
								.forEach { add(it) }
							add(plainPlain(")"))
						}
					}
				}
			)

			else -> when {
				function.isOperator -> when (function.name.asString()) {
					"plus" -> singleLineListCode(
						plainPlain("("),
						arguments[0]!!.visitUsing(),
						plainPlain(")"),
						plainPlain(" + "),
						plainPlain("("),
						arguments[1]!!.visitUsing(),
						plainPlain(")"),
					)

					"times" -> singleLineListCode(
						plainPlain("("),
						arguments[0]!!.visitUsing(),
						plainPlain(")"),
						plainPlain(" * "),
						plainPlain("("),
						arguments[1]!!.visitUsing(),
						plainPlain(")"),
					)

					else -> multiLinePlain(
						"/*",
						"Unsupported name: ${function.name.asString()}",
						"at IrCall.visitClassParent: $this",
						"is operator",
						"*/",
					)
				}

				else -> singleLineListCode(
					buildList {
						add(dispatchReceiver!!.visit())
						add(plainPlain("."))
						when (function.name.isSpecial) {
							true -> {
								val name = function.name.asString()
								when {
									name.startsWith("<set-") -> {
										add(plainPlain(name.substring("<set-".length, name.length - 1)))
										add(plainPlain(" = "))
										add(valueArguments[0]!!.visitUsing())
									}

									name.startsWith("<get-") -> {
										add(plainPlain(name.substring("<get-".length, name.length - 1)))
									}

									else -> add(
										multiLinePlain(
											"/*",
											"Unsupported name: $name",
											"at IrCall.visitClassParent: ${this@visitClassParent}",
											"is special",
											"*/",
										)
									)
								}
							}

							else -> {
								add(plainPlain(function.name.asString()))
								add(plainPlain("("))
								receiverAndArgs()
									.map { it.visitUsing() }
									.join(plainPlain(", "))
									.forEach { add(it) }
								add(plainPlain(")"))
							}
						}
					}
				)
			}
		}
	}

	fun IrCall.visitExternalPackageFragmentParent(): CodeNode {
		val function = symbol.owner
		val parent = function.parent as IrExternalPackageFragment

		return when (parent.packageFqName.asString()) {
			"kotlin.internal.ir" -> when (function.name.asString()) {
				"greater" -> singleLineListCode(
					plainPlain("("),
					valueArguments[0]!!.visitUsing(),
					plainPlain(")"),
					plainPlain(" > "),
					plainPlain("("),
					valueArguments[1]!!.visitUsing(),
					plainPlain(")"),
				)

				else -> multiLinePlain(
					"/*",
					"Unsupported function in kotlin.internal.ir: ${function.name}",
					"at IrCall.visitExternalPackageFragmentParent: $this",
					"*/",
				)
			}

			else -> multiLinePlain(
				"/*",
				"Unexpected external package fragment: ${parent.packageFqName}",
				"at IrCall.visitExternalPackageFragmentParent: $this",
				"*/",
			)
		}
	}

	fun IrCall.visit(): CodeNode {
		val function = symbol.owner
		val parent = function.parent

		return when (parent) {
			is IrClass -> visitClassParent()
			is IrExternalPackageFragment -> visitExternalPackageFragmentParent()
			else -> multiLinePlain(
				"/*",
				"Unexpected parent declaration: ${parent::class.java}: ${parent.render()}",
				"at IrCall.visit: $this",
				"*/",
			)
		}
	}

	fun IrReturn.visit(): CodeNode = singleLineListCode(
		plainPlain("return "),
		value.visitUsing(),
	)

	fun IrVariable.visit(): CodeNode = singleLineCode(
		buildList {
			add(plainPlain(typeMapper.mapType(type)))
			add(plainPlain(" "))
			add(plainPlain(name.asString()))
			initializer?.let {
				add(plainPlain(" = "))
				add(it.visitUsing())
			}
		},
	)

	fun IrSetValue.visit(): CodeNode = singleLineListCode(
		symbol.visit(),
		plainPlain(" = "),
		value.visitUsing(),
	)

	fun IrWhen.visit() = branches.visit()

	fun IrWhen.visitUsing() = branches.visitUsing(typeMapper.mapType(type))

	fun List<IrBranch>.visit(): CodeNode {
		val car = first()
		val cdr = drop(1)
		return ifPadding(
			condition = car.condition.visit(),
			content = car.result.visit(),
			elseContent = when (cdr.size) {
				1 -> cdr.single().result.visit()
				else -> cdr.visit()
			}
		)
	}

	fun List<IrBranch>.visitUsing(type: String): CodeNode {
		val car = first()
		val cdr = drop(1)
		return ifExpPadding(
			condition = car.condition.visitUsing(),
			content = when (val content = car.result) {
				is IrBlock -> content.visitUsing().let { node ->
					node as PaddingNode.Block
					blockPadding(
						*node.nodes.dropLast(1).toTypedArray(),
						node.nodes.last().pushSingleLine(plainPlain("return ")),
					)
				}

				else -> content.visitUsing()
			} to type,
			elseContent = when (cdr.size) {
				1 -> when (val content = cdr.single().result) {
					is IrBlock -> content.visitUsing().let { node ->
						node as PaddingNode.Block
						blockPadding(
							*node.nodes.dropLast(1).toTypedArray(),
							node.nodes.last().pushSingleLine(plainPlain("return ")),
						)
					}

					else -> content.visitUsing()
				} to type

				else -> cdr.visitUsing(type) to type
			}
		)
	}

	fun IrBlock.visit() = blockPadding(statements.mapNotNull { it.visit() })

	fun IrVararg.visit() = singleLineListCode(elements.map { it.visit() }.join(plainPlain(", ")))

	fun IrVarargElement.visit(): CodeNode = when (this) {
		is IrExpression -> visit()
		else -> multiLinePlain(
			"/*",
			"Unsupported vararg element: ${this::class.java.simpleName}",
			"at IrVarargElement.visit",
			"*/",
		)
	}

	fun IrStatement.visit(): CodeNode? = when (this) {
		is IrWhen -> visit()
		is IrExpression -> singleLineCode(visit(), plainPlain(";"))
		is IrDeclaration -> visit()?.appendSingleLine(plainPlain(";"))
		else -> multiLinePlain(
			"/*",
			"Unsupported statement: ${this::class.java.simpleName}",
			"at IrStatement.visit",
			"*/",
		)
	}

	fun IrExpression.visit() = when (this) {
		is IrConst -> visit()
		is IrCall -> visit()
		is IrStringConcatenation -> visit()
		is IrGetValue -> visit()
		is IrConstructorCall -> visit()
		is IrGetObjectValue -> visit()
		is IrReturn -> visit()
		is IrSetValue -> visit()
		is IrWhen -> visit()
		is IrBlock -> visit()
		is IrVararg -> visit()
		else -> multiLinePlain(
			"/*",
			"Unsupported expression: ${this::class.java.simpleName}",
			"at IrExpression.visit",
			"*/",
		)
	}

	// Using return type
	fun IrExpression.visitUsing() = when (this) {
		is IrWhen -> visitUsing()
		else -> visit()
	}

	fun IrConst.visit() = when (value) {
		is String -> plainPlain("\"$value\"")
		is Number -> plainPlain(value.toString())
		is Boolean -> plainPlain(value.toString())
		is Char -> plainPlain("'$value'")
		null -> plainPlain("null")
		else -> multiLinePlain(
			"/*",
			"Unsupported constant type: ${(value!!)::class.java.simpleName}",
			"at IrConst.visit: $this",
			"*/",
		)
	}

	fun IrStringConcatenation.visit(): CodeNode = stringConcatenationCode(arguments.map { it.visitUsing() })

	fun IrGetValue.visit() = symbol.visit()

	fun IrSymbol.visit() = when (this) {
		is IrVariableSymbol,
		is IrValueSymbol,
			-> {
			val name = owner.name
			when (name.isSpecial) {
				true -> when (name.asString()) {
					"<this>" -> plainPlain("this")
					else -> multiLinePlain(
						"/*",
						"Unsupported special name: ${name.asString()}",
						"at IrSymbol.visit: $this",
						"is IrValueSymbol",
						"is special",
						"*/",
					)
				}

				else -> plainPlain(owner.name.asString())
			}
		}

		else -> multiLinePlain(
			"/*",
			"Unsupported symbol: ${this::class.java.simpleName}",
			"at IrSymbol.visit",
			"*/",
		)
	}

	fun Visibility.visit() = when (this) {
		is Visibilities.Private -> plainPlain("private ")
		is Visibilities.Protected -> plainPlain("protected ")
		is Visibilities.Internal -> plainPlain("internal ")
		is Visibilities.Public -> plainPlain("public ")
		else -> multiLinePlain(
			"/*",
			"Unsupported visibility: $this",
			"at Visibility.visit",
			"*/",
		)
	}

	fun Modality.visit() = when (this) {
		FINAL -> plainPlain("sealed ")
		ABSTRACT -> plainPlain("abstract ")
		SEALED -> multiLinePlain(
			"/*",
			"TODO sealed modality",
			"at Modality.visit",
			"*/",
		)

		OPEN -> plainPlain("")
	}
}