package de.johni0702.minecraft.betterportals.impl
//#if MC>=11400
//$$
//$$ import java.lang.reflect.Field
//$$
//#if FABRIC>=1
//$$ import me.zeroeightsix.fiber.JanksonSettings
//$$ import me.zeroeightsix.fiber.builder.ConfigValueBuilder
//$$ import me.zeroeightsix.fiber.builder.constraint.ConstraintsBuilder
//$$ import me.zeroeightsix.fiber.tree.ConfigNode
//$$ import me.zeroeightsix.fiber.tree.ConfigValue
//$$ import net.fabricmc.loader.api.FabricLoader
//$$ import org.apache.logging.log4j.LogManager
//$$ import java.io.FileNotFoundException
//#else
//$$ import net.minecraftforge.common.ForgeConfigSpec
//$$ import net.minecraftforge.fml.ModLoadingContext
//$$ import net.minecraftforge.fml.config.ModConfig
//#endif
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
//$$     annotation class Ignore
//$$ }
//$$
//$$ class LegacyConfigSpec<T>(cls: Class<T>) {
    //#if FABRIC>=1
    //$$ private val LOGGER = LogManager.getLogger("betterportals")
    //$$ private val configPath = FabricLoader.getInstance().configDirectory.toPath().resolve("betterportals.json5")
    //$$ private val rootNode = ConfigNode()
    //$$ private val adapter: LegacyConfigAdapter<T>
    //$$ init {
    //$$     adapter = LegacyConfigAdapter(rootNode, cls, null)
    //$$ }
    //$$
    //$$ fun load() {
    //$$     try {
    //$$         configPath.toFile().inputStream().use {
    //$$             JanksonSettings().deserialize(rootNode, it)
    //$$         }
    //$$     } catch (e: FileNotFoundException) {
    //$$         LOGGER.warn("BetterPortals config file was missing: ", e)
    //$$         saveDefault()
    //$$     }
    //$$     adapter.load()
    //$$ }
    //$$
    //$$ private fun saveDefault() {
    //$$     LOGGER.info("Creating default config file.")
    //$$     configPath.toFile().outputStream().use {
    //$$         JanksonSettings().serialize(rootNode, it, false)
    //$$     }
    //$$ }
    //$$
    //$$ private class LegacyConfigAdapter<in T>(
    //$$         node: ConfigNode,
    //$$         cls: Class<out T>,
    //$$         private val instance: T?
    //$$ ) {
    //$$     private val fields = mutableMapOf<Field, ConfigValue<*>>()
    //$$     private val inner = mutableListOf<LegacyConfigAdapter<*>>()
    //$$     init {
    //$$         for (field in cls.declaredFields) { // intentionally not supporting inherited fields because forge didn't either
    //$$             if (field.getAnnotation<Config.Ignore>() != null) continue
    //$$             val type = field.type
    //$$             val name = field.getAnnotation<Config.Name>()?.value ?: continue
    //$$             val comment = field.getAnnotation<Config.Comment>()?.value
    //$$             val value = field[instance]
    //$$             if (!type.isPrimitive) {
    //$$                 val child = ConfigNode(name, comment)
    //$$                 node.add(child)
    //$$                 inner.add(LegacyConfigAdapter(child, type, value))
    //$$                 continue
    //$$             }
    //$$
    //$$             fields[field] = ConfigValue.builder(type).apply {
    //$$                 withParent(node)
    //$$                 withName(name)
    //$$                 @Suppress("UNCHECKED_CAST")
    //$$                 (this as ConfigValueBuilder<Any>).withDefaultValue(value)
    //$$                 comment?.let { withComment(it) }
    //$$                 with(constraints()) {
    //$$                     field.getAnnotation<Config.RangeInt>()?.let {
    //$$                         @Suppress("UNCHECKED_CAST")
    //$$                         this as ConstraintsBuilder<Int>
    //$$                         minNumerical(it.min)
    //$$                         maxNumerical(it.max)
    //$$                     }
    //$$                     field.getAnnotation<Config.RangeDouble>()?.let {
    //$$                         @Suppress("UNCHECKED_CAST")
    //$$                         this as ConstraintsBuilder<Double>
    //$$                         minNumerical(it.min)
    //$$                         maxNumerical(it.max)
    //$$                     }
    //$$                 }
    //$$             }.build()
    //$$         }
    //$$     }
    //$$
    //$$     fun load() {
    //$$         fields.forEach { (field, configValue) ->
    //$$             field[instance] = configValue.value
    //$$         }
    //$$         inner.forEach { it.load() }
    //$$     }
    //$$ }
    //#else
    //$$ val spec: ForgeConfigSpec
    //$$ private val adapter: LegacyConfigAdapter<T>
    //$$ init {
    //$$     val builder = ForgeConfigSpec.Builder()
    //$$     adapter = LegacyConfigAdapter(builder, cls, null)
    //$$     spec = builder.build()
    //$$     ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec)
    //$$ }
    //$$
    //$$ fun load() {
    //$$     adapter.load()
    //$$ }
    //$$
    //$$ private class LegacyConfigAdapter<in T>(
    //$$         builder: ForgeConfigSpec.Builder,
    //$$         cls: Class<out T>,
    //$$         private val instance: T?
    //$$ ) {
    //$$     private val fields = mutableMapOf<Field, ForgeConfigSpec.ConfigValue<*>>()
    //$$     private val inner = mutableListOf<LegacyConfigAdapter<*>>()
    //$$     init {
    //$$         for (field in cls.declaredFields) { // intentionally not supporting inherited fields because forge didn't either
    //$$             val name = field.getAnnotation<Config.Name>()?.value ?: continue
    //$$             val value = field[instance]
    //$$
    //$$             field.getAnnotation<Config.Comment>()?.let { builder.comment(it.value) }
    //$$             field.getAnnotation<Config.RequiresMcRestart>()?.let { builder.worldRestart() } // should really be mcRestart()
    //$$
    //$$             if (field.type.isPrimitive) {
    //$$                 val range = field.getAnnotation<Config.RangeInt>()
    //$$                         ?: field.getAnnotation<Config.RangeDouble>()
    //$$                 fields[field] = when (range) {
    //$$                     is Config.RangeInt -> builder.defineInRange(name, value as Int, range.min, range.max)
    //$$                     is Config.RangeDouble -> builder.defineInRange(name, value as Double, range.min, range.max)
    //$$                     else -> builder.define(name, value)
    //$$                 }
    //$$             } else {
    //$$                 builder.push(name)
    //$$                 inner.add(LegacyConfigAdapter<Any>(builder, field.type, value))
    //$$                 builder.pop()
    //$$             }
    //$$         }
    //$$     }
    //$$
    //$$     fun load() {
    //$$         fields.forEach { (field, configValue) ->
    //$$             field[instance] = configValue.get()
    //$$         }
    //$$         inner.forEach { it.load() }
    //$$     }
    //$$ }
    //#endif
//$$ }
//$$
//$$ private inline fun <reified T: Annotation> Field.getAnnotation(): T? = getAnnotation(T::class.java)
//#endif
