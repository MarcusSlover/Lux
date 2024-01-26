package com.marcusslover.lux.container

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Abstract container that represents a parent of files, similar to a folder.
 * Default implementations are [MapContainer] and [SingleContainer].
 *
 * @param <K> Value type.
</K> */
abstract class AbstractContainer<K> {
    lateinit var parentFolder: File
    protected val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    /**
     * Push the updated object to the cache and save it to the file.
     *
     * @param key Key of the object.
     */
    abstract fun update(key: K)

    /**
     * Attempts to load all the data from the container.
     * Look at [com.marcusslover.lux.container.extra.InitialLoading] for more information.
     */
    abstract fun loadAllData()
}

