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

package compiler.clr.backend.mapping

import compiler.clr.backend.ClrBackendContext
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * CLR平台的IR类型映射器
 *
 * 负责将Kotlin IR类型映射到CLR平台类型
 */
class IrTypeMapper(val context: ClrBackendContext) {
	/**
	 * 映射IR类型到CLR类型字符串
	 */
	@OptIn(UnsafeDuringIrConstructionAPI::class)
	fun mapType(type: IrType): String {
		val classifier = type.classOrNull ?: return "global::System.Object"
		val fqName = classifier.owner.fqNameWhenAvailable ?: return "global::System.Object"

		return mapKotlinToCLRType(type, fqName)
	}

	/**
	 * 映射返回类型，特殊处理Unit类型为void
	 */
	fun mapReturnType(type: IrType): String {
		if (type.isUnit() || type.isNothing()) {
			return "void"
		}
		return mapType(type)
	}


	@OptIn(UnsafeDuringIrConstructionAPI::class)
	fun mapKotlinToCLRType(type: IrType, fqName: FqName) = when (fqName.asString()) {
		"kotlin.Annotation" -> "global::System.Attribute"
		"kotlin.Any" -> "global::System.Object"
		"kotlin.Array" -> when (val typeArgument = (type as IrSimpleType).arguments.single()) {
			is IrStarProjection -> "dynamic"
			is IrTypeProjection -> mapType(typeArgument.type) + "[]"
		}

		"kotlin.ByteArray" -> "global::System.SByte[]"
		"kotlin.CharArray" -> "global::System.Char[]"
		"kotlin.ShortArray" -> "global::System.Int16[]"
		"kotlin.IntArray" -> "global::System.Int32[]"
		"kotlin.LongArray" -> "global::System.Int64[]"
		"kotlin.FloatArray" -> "global::System.Single[]"
		"kotlin.DoubleArray" -> "global::System.Double[]"
		"kotlin.BooleanArray" -> "global::System.Boolean[]"
		"kotlin.Boolean" -> "global::System.Boolean"
		"kotlin.Char" -> "global::System.Char"
		"kotlin.CharSequence" -> "global::System.Collections.Generic.IEnumerable<global::System.Char>"

		"kotlin.Comparable" -> "global::System.IComparable"
		"kotlin.Enum" -> "global::System.Enum"
//			"kotlin.Nothing" -> "global::System.Void" // System.Void 不可用
		"kotlin.Number" -> "global::System.Numerics.INumber"
		"kotlin.Byte" -> "global::System.SByte"
		"kotlin.Short" -> "global::System.Int16"
		"kotlin.Int" -> "global::System.Int32"
		"kotlin.Long" -> "global::System.Int64"
		"kotlin.Float" -> "global::System.Single"
		"kotlin.Double" -> "global::System.Double"
		"kotlin.String" -> "global::System.String"
		"kotlin.Throwable" -> "global::System.Exception"

		else -> buildString {
			append(
				when (fqName.asString()) {
					"kotlin.collections.Iterable" -> "global::System.Collections.Generic.IEnumerable"
					"kotlin.collections.MutableIterable" -> "/* TODO: MutableIterable */"
					"kotlin.collections.Collection" -> "global::System.Collections.Generic.IReadOnlyCollection"
					"kotlin.collections.MutableCollection" -> "global::System.Collections.Generic.ICollection"
					"kotlin.collections.List" -> "global::System.Collections.Generic.IReadOnlyList"
					"kotlin.collections.MutableList" -> "global::System.Collections.Generic.IList"
					"kotlin.collections.Set" -> "global::System.Collections.Generic.IReadOnlySet"
					"kotlin.collections.MutableSet" -> "global::System.Collections.Generic.ISet"
					"kotlin.collections.Map" -> "global::System.Collections.Generic.IReadOnlyDictionary"
					"kotlin.collections.MutableMap" -> "global::System.Collections.Generic.IDictionary"
					"kotlin.collections.Iterator" -> "global::kotlin.collections.KotlinIterator"
					"kotlin.collections.MutableIterator" -> "/* TODO: MutableIterator */"
					"kotlin.collections.ListIterator" -> "/* TODO: ListIterator */"
					"kotlin.collections.MutableListIterator" -> "/* TODO: MutableListIterator */"
					else -> "global::" + fqName.asString()
				}
			)
			if (type is IrSimpleType) {
				type.arguments.ifNotEmpty {
					append("<")
					append(joinToString { mapType(it.typeOrFail) })
					append(">")
				}
			}
		}
	}
}