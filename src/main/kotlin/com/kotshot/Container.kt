package com.kotshot

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

interface IContainer {
    operator fun <T : Any> get(klass: KClass<T>): T
    fun hasRegistration(klass: KClass<*>): Boolean
}

sealed class Producer<out T> {
    abstract operator fun invoke(): T
}

class SingletonProducer<T>(block: ()->T) : Producer<T>() {
    constructor(instance: T): this({ instance })
    private val instance by lazy(block)
    override operator fun invoke() = instance
}

class FunctionProducer<T>(private val producerFn: () -> T) : Producer<T>() {
    override operator fun invoke() = producerFn()
}

class PrimaryConstructorProducer<T>(private val ctor: KFunction<T>, private val container: IContainer) : Producer<T>() {
    override operator fun invoke() = ctor.call(*ctor.parameters.map {
        container[it.type.classifier as KClass<*>]
    }.toTypedArray())
}

class JavaConstructorProducer<T>(private val type: Class<T>, private val container: IContainer) : Producer<T>() {
    override operator fun invoke(): T {
        val ctor = type.constructors.first() {
            it.parameterTypes.all {
                container.hasRegistration(it.kotlin)
            }
        }
        return ctor.newInstance(ctor.parameterTypes.map { container[it.kotlin] }) as T
    }
}

@Suppress("FunctionName")
fun <T: Any> ConstructorProducer(klass: KClass<T>, container: IContainer): Producer<T>{
    val primary = klass.primaryConstructor
    return if (primary != null) {
        PrimaryConstructorProducer(primary, container)
    } else {
        JavaConstructorProducer(klass.java, container)
    }
}

// todo - extend exceptions to tell us what requires the not found registration

class Container internal constructor() : IContainer {

    private val map = mutableMapOf<KClass<*>, Producer<*>>()

    operator fun <T : Any> set(i: KClass<T>, producer: Producer<T>) {
        map[i] = producer
    }

    override fun hasRegistration(klass: KClass<*>): Boolean {
        return map.keys.contains(klass)
    }

    override operator fun <T : Any> get(i: KClass<T>): T =
        map[i]?.invoke() as T ?: throw Exception("No registration for type ${i.simpleName}") // todo - unfail this

    fun verify() =
        map.keys.forEach { k ->
            try {
                get(k)
            } catch (e: Exception) {
                throw Exception("${k.simpleName}: Failed", e)
            }
        }

    inline operator fun <reified I : Any> invoke(): I = get(I::class)

}


fun container(block: Container.() -> Unit = {}): Container = Container().apply(block)

fun <T: Any, U: T> Container.register(pair: Pair<KClass<T>, KClass<U>>) {
    this[pair.first] = ConstructorProducer(pair.second, this)
}
inline fun <reified T: Any, reified U: T> Container.set() = this.register(T::class to U::class)


inline fun <reified T: Any, reified U: T> Container.singleton(instance: U) {
    this[T::class] = SingletonProducer(instance)
}
inline fun <reified T: Any,  reified U: T> Container.singleton(pair: Pair<KClass<T>, KClass<U>>) {
    this[T::class] =   SingletonProducer(ConstructorProducer(pair.second, this)::invoke)
}
inline fun <reified T: Any,  reified U: T> Container.singleton() = singleton( T::class to U::class )

inline fun <reified T: Any> Container.register(noinline block: ()->T){
    this[T::class] = FunctionProducer(block)
}
