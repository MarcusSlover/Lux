package com.marcusslover.lux.container.type

import com.marcusslover.lux.container.AbstractContainer
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Container that represents a single object.
 * The object is serialized to a single json file.
 * The file name is final and can be changed in the constructor.
 *
 * @param <V> Value type.
</V> */
abstract class SingleContainer<V> : AbstractContainer<V>() {
    private val valueType: Class<V>? = null
    private val fileName: String? = null
    protected var cache: V? = null

    /**
     * Creates a new instance of an object.
     *
     *
     * This function is called when the object does not exist and has to be created for the first time.
     *
     *
     * @return The object.
     */
    protected abstract fun emptyValue(): V

    /**
     * Called when a new object gets loaded.
     *
     *
     * This function is mainly called when [storeLocally] is called.
     *
     *
     * @param value The object that was most recently loaded.
     */
    protected fun onValueLoaded(value: V?) {
        // do some extra thing when the value is loaded
    }

    /**
     * Called when an object gets unloaded.
     *
     *
     * This function is mainly called when [cleanLocally]} is called.
     *
     *
     * @param value The object that was most recently unloaded.
     */
    protected fun onValueUnloaded(value: V?) {
        // do some extra thing when the value is unloaded
    }

    override fun update(value: V) {
        this.writeData(this.retrieveLocally())
    }

    fun update() {
        this.writeData(this.retrieveLocally())
    }

    /**
     * Loads an object from the file.
     *
     * @return The object.
     */
    fun loadData(): V {
        if (this.cache != null) {
            return cache!!
        }
        val data = this.readData()
        this.storeLocally(data)
        return data
    }

    override fun loadAllData() {
        val v = this.readData()
        this.storeLocally(v)
    }

    /**
     * Unloads the data from the cache and saves it to the file.
     */
    fun saveData() {
        if (this.cache == null) {
            return
        }
        val data: V? = this.retrieveLocally()
        this.writeData(data)
        this.cleanLocally()
    }


    /**
     * Cleans the local cache.
     *
     *
     * This function does not delete the file.
     *
     */
    fun cleanLocally() {
        this.onValueUnloaded(this.cache)
        this.cache = null
    }

    /**
     * Stores locally the data.
     *
     *
     * This function does not write the data to the file.
     *
     *
     * @param value The value to store.
     */
    fun storeLocally(value: V?) {
        this.cache = value
        this.onValueLoaded(value)
    }

    /**
     * Retrieves the object from the cache.
     *
     *
     * This function will not attempt to load the object from the file,
     * if it is not loaded -> Use [loadData] instead.
     *
     *
     * @return The object or null if it is not loaded.
     */
    fun retrieveLocally(): V? {
        return this.cache
    }

    /**
     * Reads the object from the file.
     *
     *
     * If the file doesn't exist, it will create a new one.
     * Default value created by [emptyValue] will be used.
     *
     *
     * @return The object.
     */
    fun readData(): V {
        var data: V
        val file = File(this.parentFolder, this.fileName + ".json")
        if (file.exists()) {
            try {
                FileReader(file).use { fileReader ->
                    data = gson.fromJson(fileReader, this.valueType)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            return data
        } else {
            return this.emptyValue()
        }
    }

    /**
     * Writes the data to the file.
     *
     *
     * This function does not unload anything from the cache.
     * The cache is not affected by this function.
     * If you want to delete the file, set the value to null!
     *
     *
     * @param value Object to write.
     */
    fun writeData(value: V?) {
        val file: File = File(this.parentFolder, this.fileName + ".json")
        if (value == null) {
            val delete = file.delete()
            if (!delete) {
                throw RuntimeException("Could not delete file: " + file.absolutePath)
            }
            return
        }
        try {
            FileWriter(file).use { fileWriter ->
                gson.toJson(value, fileWriter)
                fileWriter.flush()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}

