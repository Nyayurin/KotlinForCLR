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

package kotlin

import kotlin.internal.ActualizeByClrBuiltinProvider

@ActualizeByClrBuiltinProvider
class Boolean private constructor() : Comparable<Boolean> {
	external operator fun not(): Boolean
	external infix fun and(other: Boolean): Boolean
	external infix fun or(other: Boolean): Boolean
	external infix fun xor(other: Boolean): Boolean
	external override fun compareTo(other: Boolean): Int
	external override fun toString(): String
	external override fun equals(other: Any?): Boolean
	external override fun hashCode(): Int
	companion object
}