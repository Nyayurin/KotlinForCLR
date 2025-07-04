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

package kotlin.ranges

internal class CharProgressionIterator(first: Char, last: Char, val step: Int) : CharIterator() {
	private val finalElement: Int = last.code
	private var hasNext: Boolean = if (step > 0) first <= last else first >= last
	private var next: Int = if (hasNext) first.code else finalElement

	override fun hasNext(): Boolean = hasNext
	external override fun nextChar(): Char
}

internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator() {
	private val finalElement: Int = last
	private var hasNext: Boolean = if (step > 0) first <= last else first >= last
	private var next: Int = if (hasNext) first else finalElement

	override fun hasNext(): Boolean = hasNext
	external override fun nextInt(): Int
}

internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator() {
	private val finalElement: Long = last
	private var hasNext: Boolean = if (step > 0) first <= last else first >= last
	private var next: Long = if (hasNext) first else finalElement

	override fun hasNext(): Boolean = hasNext
	external override fun nextLong(): Long
}