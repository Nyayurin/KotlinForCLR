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

package compiler.clr.frontend

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirAbstractSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.registerDefaultComponents
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import kotlin.reflect.KClass

@OptIn(SessionConfiguration::class)
object FirClrSessionFactory :
	FirAbstractSessionFactory<FirClrSessionFactory.LibraryContext, FirClrSessionFactory.SourceContext>() {

	fun createLibrarySession(
		mainModuleName: Name,
		sessionProvider: FirProjectSessionProvider,
		moduleDataProvider: ModuleDataProvider,
		projectEnvironment: AbstractProjectEnvironment,
		scope: AbstractProjectFileSearchScope,
		assemblies: Map<String, NodeAssembly>,
		languageVersionSettings: LanguageVersionSettings,
		extensionRegistrars: List<FirExtensionRegistrar> = emptyList(),
	) = createLibrarySession(
		mainModuleName,
		LibraryContext(projectEnvironment, assemblies),
		sessionProvider,
		moduleDataProvider,
		languageVersionSettings,
		extensionRegistrars,
		createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
			listOfNotNull(
				ClrAssemblyBasedSymbolProvider(
					session,
					builtinsModuleData,
					kotlinScopeProvider,
					assemblies.filterNot { it.key == "kotlin-stdlib" },
				),
				runUnless(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
					initializeBuiltinsProvider(
						session,
						builtinsModuleData,
						kotlinScopeProvider,
						assemblies.filter { it.key == "kotlin-stdlib" },
					)
				},
				FirBuiltinSyntheticFunctionInterfaceProvider(session, builtinsModuleData, kotlinScopeProvider),
				syntheticFunctionInterfaceProvider,
				FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
			)
		}
	)

	override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
		return FirKotlinScopeProvider { klass, declaredScope, useSiteSession, scopeSession, memberRequiredPhase ->
			wrapScopeWithClrMapped(
				klass,
				declaredScope,
				useSiteSession,
				scopeSession,
				memberRequiredPhase,
				false
			)
		}
	}

	override fun FirSession.registerLibrarySessionComponents(c: LibraryContext) {
		registerDefaultComponents()
		registerClrComponents(c.assemblies)
	}

	// ==================================== Platform session ====================================

	fun createModuleBasedSession(
		moduleData: FirModuleData,
		sessionProvider: FirProjectSessionProvider,
		csharpSourcesScope: AbstractProjectFileSearchScope,
		projectEnvironment: AbstractProjectEnvironment,
		extensionRegistrars: List<FirExtensionRegistrar>,
		languageVersionSettings: LanguageVersionSettings,
		assemblies: Map<String, NodeAssembly>,
		lookupTracker: LookupTracker?,
		enumWhenTracker: EnumWhenTracker?,
		importTracker: ImportTracker?,
		init: FirSessionConfigurator.() -> Unit,
	): FirSession {
		val context = SourceContext(assemblies, projectEnvironment)
		return createModuleBasedSession(
			moduleData,
			context = context,
			sessionProvider,
			extensionRegistrars,
			languageVersionSettings,
			lookupTracker,
			enumWhenTracker,
			importTracker,
			init,
			createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependencies ->
				listOfNotNull(
					ClrSymbolProvider(session, assemblies, session.moduleData),
					symbolProvider,
					generatedSymbolsProvider,
					initializeForStdlibIfNeeded(session, kotlinScopeProvider, dependencies, assemblies),
					*dependencies.toTypedArray(),
				)
			}
		)
	}

	override fun createKotlinScopeProviderForSourceSession(
		moduleData: FirModuleData,
		languageVersionSettings: LanguageVersionSettings,
	): FirKotlinScopeProvider {
		if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon) return FirKotlinScopeProvider()

		val filterOutClrPlatformDeclarations = !(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)
				&& /*languageVersionSettings.getFlag(AnalysisFlags.expectBuiltinsAsPartOfStdlib)*/ false)
		return FirKotlinScopeProvider { klass, declaredScope, useSiteSession, scopeSession, memberRequiredPhase ->
			val classIdForLog = klass.symbol.classId.asSingleFqName().asString()
			wrapScopeWithClrMapped(
				klass,
				declaredScope,
				useSiteSession,
				scopeSession,
				memberRequiredPhase,
				filterOutClrPlatformDeclarations = filterOutClrPlatformDeclarations
			)
		}
	}

	override fun FirSessionConfigurator.registerPlatformCheckers(c: SourceContext) {
		registerCommonCheckers() // CLR特定的检查器可以添加在这里
	}

	override fun FirSession.registerSourceSessionComponents(c: SourceContext) {
		registerDefaultComponents()
		registerClrComponents(c.assemblies)
		register(FirClrTargetProvider::class as KClass<out FirSessionComponent>, FirClrTargetProvider())
	}

	// ==================================== Common parts ====================================

	private fun FirSession.registerClrComponents(assemblies: Map<String, NodeAssembly>) {
		register(FirClrAssemblyProvider::class as KClass<out FirSessionComponent>, FirClrAssemblyProvider(assemblies))
	}

	// 为CLR平台创建适当的作用域包装器
	private fun wrapScopeWithClrMapped(
		klass: FirClass,
		declaredScope: FirContainingNamesAwareScope,
		useSiteSession: FirSession,
		scopeSession: ScopeSession,
		memberRequiredPhase: FirResolvePhase?,
		filterOutClrPlatformDeclarations: Boolean = false,
	): FirContainingNamesAwareScope {
		val classId = klass.symbol.classId
		val fqName = classId.asSingleFqName().asString()

		// 优先处理我们关心的 CLR 库类 (或者任何非 BuiltInsFallback 和非 Source 的 Library origin 类)
		if (klass.origin == FirDeclarationOrigin.Library) {
			return ClrClassMemberScope(klass, useSiteSession, scopeSession, declaredScope)
		}

		// 对于 Kotlin 的内建回退类
		if (klass.origin == FirDeclarationOrigin.BuiltInsFallback) {
			return declaredScope
		}

		// 对于源码中定义的类
		if (klass.origin == FirDeclarationOrigin.Source) {
			return declaredScope
		}

		// 其他情况（例如 Java 类、Enhancement 等，如果未来支持的话）
		// 或者如果一个 Library 类因为某些原因没有被上面的 if (klass.origin == FirDeclarationOrigin.Library) 捕获
		return declaredScope
	}

	// ==================================== Utilities ====================================

	class LibraryContext(
		val projectEnvironment: AbstractProjectEnvironment,
		val assemblies: Map<String, NodeAssembly>,
	)

	class SourceContext(
		val assemblies: Map<String, NodeAssembly>,
		val projectEnvironment: AbstractProjectEnvironment,
	)

	private fun initializeForStdlibIfNeeded(
		session: FirSession,
		kotlinScopeProvider: FirKotlinScopeProvider,
		dependencies: List<FirSymbolProvider>,
		assemblies: Map<String, NodeAssembly>,
	): FirSymbolProvider? {
		return runIf(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && !session.moduleData.isCommon) {
			val builtinsSymbolProvider = initializeBuiltinsProvider(
				session,
				session.moduleData,
				kotlinScopeProvider,
				assemblies.filter { it.key == "kotlin-stdlib" },
			)
			if (session.moduleData.dependsOnDependencies.isNotEmpty()) {
				runIf(!/*session.languageVersionSettings.getFlag(AnalysisFlags.expectBuiltinsAsPartOfStdlib)*/ false) {
					val refinedSourceSymbolProviders = dependencies.filter { it.session.kind == FirSession.Kind.Source }
					ClrActualizingBuiltinSymbolProvider(builtinsSymbolProvider, refinedSourceSymbolProviders)
				}
			} else {
				ClrBuiltinsSymbolProvider(
					session,
					session.moduleData,
					kotlinScopeProvider,
					assemblies
				)
			}
		}
	}

	private fun initializeBuiltinsProvider(
		session: FirSession,
		builtinsModuleData: FirModuleData,
		kotlinScopeProvider: FirKotlinScopeProvider,
		assemblies: Map<String, NodeAssembly>,
	): ClrBuiltinsSymbolProvider = ClrBuiltinsSymbolProvider(
		session,
		builtinsModuleData,
		kotlinScopeProvider,
		assemblies,
	)
}