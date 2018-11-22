# KotShot
A Kotlin dependency injection container

## Why
The aim is to provide a lightweight dependency injection container, without
relying on any anti-patterns 

### What about {other DI library}
Several other libraries have been looked at. There are problems with each, including

- Reliance on annotations for wired up objects - this creates a global dependency on the container used.
- Encouragement of "Service locator antipattern" (i.e. passing the container to everythign in the project or marking it as global). This too creates a global dependency on the container.
- No automatic wireup - Dependency injection should make things easier, we shouldn't have to rewrite the wire up code every time a constructor parameter changes
- Java-based - kotlin has plenty of features that make this easier than using a Java based DI framework, we should make use of them
- Heavyweight/part of a larger framework that cannot be extracted.

In order to provide this, KotShot *does* depend on reflection to wire up the components.

## Example

```kotlin
// A simple dependency graph

interface IFoo{
    fun foo(): String
}

interface IBar{
    fun bar(): Int
}

class Baz: IBar{
    override fun bar() = 42
}

class Foz(private val baz: IBar): Foo {
    override fun foo() = "${baz.bar()}"
}

fun main(args : Array<String>){
    // create a container
    val di = container {
        // register the providers of the interfaces
        register<IFoo, Foz>()
        register<IBar, Baz>
    }
    
    // create and use an instance
    println(di.get<IFoo>().foo())
}
```

prints `42`

Demonstrated above - no need to specify constructor parameters - the container works them out. simple registration syntax. No dependency from the objects on the container.