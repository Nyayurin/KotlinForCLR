[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void main()
    {
        global::System.Collections.Generic.IReadOnlyList<global::System.String> items = global::kotlin.collections.CollectionsKt.listOf("apple", "banana", "kiwifruit");
        {
            global::kotlin.collections.KotlinIterator<global::System.String> iterator = new global::kotlin.collections.KotlinIterator<global::System.String>(items.GetEnumerator());
            while (iterator.hasNext())
            {
                global::System.String item = iterator.next();
                {
                    global::kotlin.io.ConsoleKt.println(item);
                };
            };
        };
    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}