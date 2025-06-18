public sealed class KFC : global::System.Object
{
    public static global::KFC INSTANCE { get; } = new global::KFC();

    private KFC() : base()
    {

    }
}

[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void main()
    {
        global::MainKt.vivo(global::KFC.INSTANCE, 50);
    }

    [global::kotlin.clr.KotlinExtension]
    public static void vivo(global::KFC receiver, global::System.Int32 value)
    {

    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}