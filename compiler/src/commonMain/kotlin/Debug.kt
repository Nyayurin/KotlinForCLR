import compiler.clr.CLRCompiler
import compiler.clr.CLRCompilerArguments

fun main() {
	val arguments = CLRCompilerArguments().apply {
		freeArgs += "../kotlin/src"
		destination = "..\\csharp\\KotlinCLR\\gen"
		kotlinHome = "../home"
		dotnetHome = "C:\\Program Files\\dotnet"
		dotnetVersion = "9.0.6"
	}
	val compiler = CLRCompiler()
	val exitCode = compiler.exec(System.err, arguments)
	println("exit, code: $exitCode")
}