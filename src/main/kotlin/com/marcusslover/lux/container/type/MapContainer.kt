package com.marcusslover.lux.container.type

import com.marcusslover.lux.container.AbstractContainer
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.function.Function

/**
 * Container that represents a map of objects.
 * Key is the name of the file. Files are dynamic.
 * Value is the object. Each value is serialized to its own json file.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * */
abstract class MapContainer<K, V> : AbstractContainer<K>() {
    /*Container data*/
    protected val cache: MutableMap<K, V> = HashMap()

    /*File naming solution*/
    private val keyTransformer: Function<K, String>? = null
    private val keyComposer: Function<String, K>? = null
    private val valueType: Class<V>? = null

    /**
     * Creates a new instance of an object.
     *
     *
     * This function is called when the object with the given key
     * does not exist and has to be created for the first time.
     *
     *
     * @param key The key of the object.
     * @return The object.
     */
    protected abstract fun emptyValue(key: K): V

    /**
     * Called when a new object gets loaded.
     *
     *
     * This function is mainly called when [storeLocally] is called.
     *
     *
     * @param value The object that was most recently loaded.
     */
    protected fun onValueLoaded(value: V) {
        // do some extra thing when the value is loaded
    }

    /**
     * Called when an object gets unloaded.
     *
     *
     * This function is mainly called when [cleanLocally] is called.
     *
     *
     * @param value The object that was most recently unloaded.
     */
    protected fun onValueUnloaded(value: V) {
        // do some extra thing when the value is unloaded
    }

    override fun update(key: K) {
        this.writeData(key, this.retrieveLocally(key))
    }

    /**
     * Loads an object from the file.
     * The key is the name of the file.
     *
     * @param key Key to the object.
     * @return The object.
     */
    fun loadData(key: K): V {
        if (cache.containsKey(key)) {
            return this.retrieveLocally(key)!! // No null
        }
        val data = this.readData(key)
        this.storeLocally(key, data)
        return data
    }

    /**
     * Loads all the objects from the files.
     * Called during start of the plugin, only when the container
     * is annotated with [com.marcusslover.lux.container.extra.InitialLoading].
     */
    override fun loadAllData() {
        val files: Array<File> = this.parentFolder.listFiles() ?: return
        for (file in files) {
            try {
                val fileName = file.name
                val apply = keyComposer!!.apply(fileName.replaceFirst("(\\.json)".toRegex(), ""))
                val read: V? = this.read(fileName)
                if (this.containsKeyLocally(apply)) {
                    continue
                }
                this.storeLocally(apply, read)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Unloads an object from the cache and saves it to the file.
     *
     * @param key Key to the object.
     */
    fun saveData(key: K) {
        if (!cache.containsKey(key)) {
            return
        }
        val data: V? = this.retrieveLocally(key)
        this.writeData(key, data)
        this.cleanLocally(key)
    }

    /**
     * Saves all the objects from the cache to the file.
     * Should be called when the plugin is disabled.
     */
    fun saveData() {
        for (key in ArrayList(cache.keys)) { // use keys to prevent concurrent modification
            this.saveData(key) // save data individually
        }
    }

    /**
     * Checks if object with the given key is loaded in the cache.
     *
     * @param key Key to the object.
     * @return True if the object is loaded, false otherwise.
     */
    fun containsKeyLocally(key: K): Boolean {
        return cache.containsKey(key)
    }

    /**
     * Checks if the object is loaded in the cache.
     *
     *
     * The check does not include any files, only the cache!
     *
     *
     * @param value Object to check.
     * @return True if the object is loaded, false otherwise.
     */
    fun containsValueLocally(value: V): Boolean {
        return cache.containsValue(value)
    }

    /**
     * Cleans the object from the cache.
     *
     *
     * This function does not write the object to the file.
     *
     *
     * @param key Key to the object.
     */
    fun cleanLocally(key: K) {
        val value = this.retrieveLocally(key)
        if (value != null) {
            this.onValueUnloaded(value)
        }
        cache.remove(key)
    }

    /**
     * Puts the object in the cache.
     *
     *
     * This function only puts the object in the cache.
     * It doesn't save the object to the file -> Use [update] for that
     * or [writeData].
     *
     *
     * @param key   Key to the object.
     * @param value Object to put.
     */
    fun storeLocally(key: K, value: V?) {
        if (value == null) {
            this.cleanLocally(key)
        } else {
            this.onValueLoaded(value)
            cache[key] = value
        }
    }

    /**
     * Retrieves the object from the cache.
     *
     *
     * This function will not attempt to load the object from the file,
     * if it is not loaded -> Use [loadData] instead.
     *
     *
     * @param key Key to the object.
     * @return The object or null if it is not loaded.
     */
    fun retrieveLocally(key: K): V? {
        return cache.getOrDefault(key, null)
    }

    /**
     * Reads the object from the file.
     *
     *
     * If the file doesn't exist, it will create a new one.
     * Default value created by [.emptyValue] will be used.
     *
     *
     * @param key Key to the object.
     * @return The object.
     */
    fun readData(key: K): V {
        val fileName = keyTransformer!!.apply(key)
        val read: V? = this.read("$fileName.json")
        return Objects.requireNonNullElseGet(read) { this.emptyValue(key) }
    }

    /**
     * Reads the object from the file.
     *
     *
     * This is an internal (raw) function -> Use [.readData] instead.
     *
     *
     * @param fileName Name of the file.
     * @return The object.
     */
    fun read(fileName: String): V? {
        var data: V
        val file = File(this.parentFolder, fileName)
        if (file.exists()) {
            try {
                FileReader(file).use { fileReader ->
                    data = this.gson.fromJson(fileReader, this.valueType)
                }
            } catch (e: IOException) {

                throw RuntimeException(e)
            }
            return data
        }
        return null
    }

    /**
     * Writes the object to the file.
     *
     *
     * This function does not unload anything from the cache.
     * The cache is not affected by this function.
     * If you want to delete the file, set the value to null!
     *
     *
     * @param key   Key to the object.
     * @param value Object to write.
     */
    fun writeData(key: K, value: V?) {
        val fileName = keyTransformer!!.apply(key)
        val file = File(this.parentFolder, "$fileName.json")
        if (value == null) {
            val delete = file.delete()
            if (!delete) {
                throw RuntimeException("Failed to delete file: " + file.absolutePath)
            }
            return
        }
        try {
            FileWriter(file).use { fileWriter ->
                this.gson.toJson(value, fileWriter)
                fileWriter.flush()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    val values: Collection<V>
        /**
         * Gets all the loaded objects from the cache.
         *
         * @return All the loaded objects.
         */
        get() = cache.values

    /**
     * Gets all the loaded keys from the cache.
     * @return All the loaded keys and their objects.
     */
    fun getCache(): Map<K, V> {
        return this.cache
    }

    companion object {
        /*Default string-to-string solution for file naming*/
        protected val TRANSFORMER: Function<String, String?> = (Function { x: String? -> x })
        protected val COMPOSER: Function<String, String?> = (Function { x: String? -> x })
    }
}

