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

package compiler.clr.backend

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

interface IrGenerationExtension {
	companion object :
		ProjectExtensionDescriptor<IrGenerationExtension>(
			"org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java
		)

	fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

	fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension? = null

	// In K1 mode, returns true if this extension should also be applied in the KAPT stub generation mode in Kotlin/JVM. This mode uses
	// light analysis in the compiler frontend to produce an "API-only" class file which is then converted to a .java stub. Because of the
	// light analysis, the resulting IR does not have function bodies and can contain references to error types. If this method returns
	// true, the extension should be ready to handle such incomplete IR. Any modifications to the IR applied during the KAPT stub generation
	// mode will only have effect on the .java stub, not the resulting .class files. Compilation to the .class files is done in a separate
	// step after stub generation.
	//
	// IR generation extensions are not applied in K2 KAPT, so this property has no effect when K2 is enabled.
	@FirIncompatiblePluginAPI
	val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean get() = false
}

/**
 * This interface for common IR is empty because intrinsics are done in a platform-specific way (because of inliner).
 * Currently, only JVM intrinsics are supported via JvmIrIntrinsicExtension interface.
 */
interface IrIntrinsicExtension
