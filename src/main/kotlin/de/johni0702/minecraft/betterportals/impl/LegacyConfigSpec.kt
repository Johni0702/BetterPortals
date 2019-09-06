package de.johni0702.minecraft.betterportals.impl
//#if MC>=11400
//$$
//$$ import net.minecraftforge.common.ForgeConfigSpec
//$$ import net.minecraftforge.fml.ModLoadingContext
//$$ import net.minecraftforge.fml.config.ModConfig
//$$ import java.lang.reflect.Field
//$$
//$$ annotation class Config(val modid: String) {
//$$     annotation class Name(val value: String)
//$$     annotation class Comment(val value: String)
//$$     @Suppress("DIVISION_BY_ZERO")
//$$     annotation class RangeDouble(
//$$             val min: Double = -1.0 / 0.0,
//$$             val max: Double = +1.0 / 0.0
//$$     )
//$$     annotation class RangeInt(
//$$             val min: Int = Int.MIN_VALUE,
//$$             val max: Int = Int.MAX_VALUE
//$$     )
//$$     annotation class RequiresMcRestart
//$$ }
//$$
//$$ class LegacyConfigSpec<T>(cls: Class<T>) {
//$$     val spec: ForgeConfigSpec
//$$     private val adapter: LegacyConfigAdapter<T>
//$$     init {
//$$         val builder = ForgeConfigSpec.Builder()
//$$         adapter = LegacyConfigAdapter(builder, cls, null)
//$$         spec = builder.build()
//$$         ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec)
//$$     }
//$$
//$$     fun load() {
//$$         adapter.load()
//$$     }
//$$
//$$     private class LegacyConfigAdapter<in T>(
//$$             builder: ForgeConfigSpec.Builder,
//$$             cls: Class<out T>,
//$$             private val instance: T?
//$$     ) {
//$$         private val fields = mutableMapOf<Field, ForgeConfigSpec.ConfigValue<*>>()
//$$         private val inner = mutableListOf<LegacyConfigAdapter<*>>()
//$$         init {
//$$             for (field in cls.declaredFields) { // intentionally not supporting inherited fields because forge didn't either
//$$                 val name = field.getAnnotation<Config.Name>()?.value ?: continue
//$$                 val value = field[instance]
//$$
//$$                 field.getAnnotation<Config.Comment>()?.let { builder.comment(it.value) }
//$$                 field.getAnnotation<Config.RequiresMcRestart>()?.let { builder.worldRestart() } // should really be mcRestart()
//$$
//$$                 if (field.type.isPrimitive) {
//$$                     val range = field.getAnnotation<Config.RangeInt>()
//$$                             ?: field.getAnnotation<Config.RangeDouble>()
//$$                     fields[field] = when (range) {
//$$                         is Config.RangeInt -> builder.defineInRange(name, value as Int, range.min, range.max)
//$$                         is Config.RangeDouble -> builder.defineInRange(name, value as Double, range.min, range.max)
//$$                         else -> builder.define(name, value)
//$$                     }
//$$                 } else {
//$$                     builder.push(name)
//$$                     inner.add(LegacyConfigAdapter<Any>(builder, field.type, value))
//$$                     builder.pop()
//$$                 }
//$$             }
//$$         }
//$$
//$$         fun load() {
//$$             fields.forEach { (field, configValue) ->
//$$                 field[instance] = configValue.get()
//$$             }
//$$             inner.forEach { it.load() }
//$$         }
//$$     }
//$$ }
//$$
//$$ private inline fun <reified T: Annotation> Field.getAnnotation(): T? = getAnnotation(T::class.java)
//#endif
