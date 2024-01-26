package com.marcusslover.lux.container

import com.marcusslover.lux.container.extra.InitialLoading
import com.marcusslover.lux.container.type.MapContainer
import com.marcusslover.lux.container.type.SingleContainer
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.util.*

/**
 * Manages all containers of the server.
 */
class ContainerManager {
    private val containerMap: MutableMap<String, AbstractContainer<*>> = HashMap()

    /**
     * Registers a new container.
     *
     * @param parent    Parent folder name.
     * @param container Instance of the container.
     */
    fun register(parent: String, container: AbstractContainer<*>) {
        containerMap[parent] = container
    }

    /**
     * Initializes all containers.
     *
     * @param modId Mod id.
     */
    fun init(modId: String) {
        val gameDir = FabricLoader.getInstance().gameDir
        val toFile = gameDir.toFile()
        val dataFolder = File(toFile, modId)
        if (!dataFolder.exists()) {
            val mkdirs = dataFolder.mkdirs()
            check(mkdirs) { "Could not create data folder." }
        }

        /*Creates directories for all containers*/
        for (parent in containerMap.keys) {
            val containerFolder = File(dataFolder, parent)

            if (!containerFolder.exists()) {
                val mkdirs = containerFolder.mkdirs()
                check(mkdirs) { "Could not create container folder." }
            }

            val container = containerMap[parent]!!
            container.parentFolder = containerFolder

            /*Extra data settings*/
            val initialLoading: InitialLoading = this.getInitialLoadingAnnotation(container) ?: continue
            if (!initialLoading.value) {
                continue
            }

            try { // Safe loading.
                if (container is MapContainer<*, *>) {
                    container.loadAllData()
                } else if (container is SingleContainer<*>) {
                    container.loadAllData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getInitialLoadingAnnotation(container: AbstractContainer<*>): InitialLoading? {
        val klass: Class<out AbstractContainer<*>> = container.javaClass
        val annotationsByType: Array<InitialLoading> = klass.getAnnotationsByType(InitialLoading::class.java)
        if (annotationsByType.isNotEmpty()) return annotationsByType[0]
        return null
    }

    /**
     * You may want to save all containers before the plugin is disabled.
     * It's not a mandatory method, but it's recommended.
     * Additionally, it clears the container map after saving.
     */
    fun shutdown() {
        for (container in containerMap.values) {
            if (container is SingleContainer<*>) {
                try { // Safe saving.
                    container.saveData()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (container is MapContainer<*, *>) {
                try { // Safe saving.
                    container.saveData() // Saves all the data.
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        containerMap.clear() // Clears the map.
    }

    /**
     * Finds a container by its type.
     *
     * @param type Type of the container.
     * @return Optional of the container.
     */
    fun <T : AbstractContainer<*>?> getByType(type: Class<T>): Optional<T> {
        return containerMap.values.stream().filter { x: AbstractContainer<*> -> x.javaClass == type }
            .findFirst().map { x: AbstractContainer<*> -> x as T }
    }
}

