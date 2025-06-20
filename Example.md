# KFC vivo 50

```kotlin
fun main() {
    KFC vivo 50
}

object KFC

infix fun KFC.vivo(value: Int) {}
```

```c#
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
```

# [Read from the standard input](https://kotlinlang.org/docs/basic-syntax.html#read-from-the-standard-input)

```kotlin
fun main() {
    // Prints a message to request input
    println("Enter any word: ")

    // Reads and stores the user input. For example: Happiness
    val yourWord = readln()

    // Prints a message with the input
    print("You entered the word: ")
    print(yourWord)
    // You entered the word: Happiness
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void main()
    {
        global::kotlin.io.ConsoleKt.println("Enter any word: ");
        global::System.String yourWord = global::kotlin.io.ConsoleKt.readln();
        global::kotlin.io.ConsoleKt.print("You entered the word: ");
        global::kotlin.io.ConsoleKt.print(yourWord);
    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}
```

# [Functions](https://kotlinlang.org/docs/basic-syntax.html#functions)

```kotlin
fun main() {
    println("sum(1, 2) = ${sum(1, 2)}")
}

fun sum(a: Int, b: Int): Int {
    return a + b
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void main()
    {
        global::kotlin.io.ConsoleKt.println($"{("sum(1, 2) = ")}{(global::MainKt.sum(1, 2))}");
    }

    public static global::System.Int32 sum(global::System.Int32 a, global::System.Int32 b)
    {
        return (a) + (b);
    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}
```

```kotlin
fun printSum(a: Int, b: Int): Unit {
    println("sum of $a and $b is ${a + b}")
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void printSum(global::System.Int32 a, global::System.Int32 b)
    {
        global::kotlin.io.ConsoleKt.println($"{("sum of ")}{(a)}{(" and ")}{(b)}{(" is ")}{((a) + (b))}");
    }
}
```

# [Variables](https://kotlinlang.org/docs/basic-syntax.html#variables)

```kotlin
val PI = 3.14
var x = 0

fun incrementX() {
    x += 1
}

fun main() {
    println("x = $x, PI = $PI")
    incrementX()
    println("x = $x, PI = $PI")
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static global::System.Double PI
    {
        get;
    }

    public static global::System.Int32 x
    {
        get;
        set;
    }

    public static void incrementX()
    {
        global::MainKt.x = (global::MainKt.x) + (1);
    }

    public static void main()
    {
        global::kotlin.io.ConsoleKt.println($"{("x = ")}{(global::MainKt.x)}{(", PI = ")}{(global::MainKt.PI)}");
        global::MainKt.incrementX();
        global::kotlin.io.ConsoleKt.println($"{("x = ")}{(global::MainKt.x)}{(", PI = ")}{(global::MainKt.PI)}");
    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}
```

# [Creating classes and instances](https://kotlinlang.org/docs/basic-syntax.html#creating-classes-and-instances)

```kotlin
open class Shape

class Rectangle(val height: Double, val length: Double): Shape() {
    val perimeter = (height + length) * 2
}
```

```c#
public class Shape : global::System.Object
{
    public Shape() : base()
    {

    }
}

public sealed class Rectangle : global::Shape
{
    public Rectangle(global::System.Double height, global::System.Double length) : base()
    {
        this.height = height;
        this.length = length;
        this.perimeter = ((this.height) + (this.length)) * (2);
    }

    public global::System.Double height
    {
        get;
    }

    public global::System.Double length
    {
        get;
    }

    public global::System.Double perimeter
    {
        get;
    }
}
```

# [String templates](https://kotlinlang.org/docs/basic-syntax.html#string-templates)

```kotlin
fun main() {
    var a = 1
    // simple name in template:
    val s1 = "a is $a"

    a = 2
    // arbitrary expression in template:
    val s2 = "${s1.replace("is", "was")}, but now is $a"
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static void main()
    {
        global::System.Int32 a = 1;
        global::System.String s1 = $"{("a is ")}{(a)}";
        a = 2;
        global::System.String s2 = $"{(global::kotlin.text.TextH.replace(s1, "is", "was"))}{(", but now is ")}{(a)}";
    }

    public static void Main(global::System.String[] args)
    {
        global::MainKt.main();
    }
}
```

# [Conditional expressions](https://kotlinlang.org/docs/basic-syntax.html#conditional-expressions)

```kotlin
fun maxOf(a: Int, b: Int): Int {
    if (a > b) {
        return a
    } else {
        return b
    }
}
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static global::System.Int32 maxOf(global::System.Int32 a, global::System.Int32 b)
    {
        if ((a) > (b))
        {
            return a;
        }
        else
        {
            return b;
        }
    }
}
```

```kotlin
fun maxOf(a: Int, b: Int) = if (a > b) a else b
```

```c#
[global::kotlin.clr.KotlinFileClass]
public static class MainKt
{
    public static global::System.Int32 maxOf(global::System.Int32 a, global::System.Int32 b)
    {
        return ((a) > (b))
            ? (a)
            : (b);
    }
}
```

# [for loop](https://kotlinlang.org/docs/basic-syntax.html#for-loop)

```kotlin
fun main() {
    val items = listOf("apple", "banana", "kiwifruit")
    for (item in items) {
        println(item)
    }
}
```

```c#
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
```