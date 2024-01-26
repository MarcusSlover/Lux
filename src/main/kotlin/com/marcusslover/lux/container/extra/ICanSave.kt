package com.marcusslover.lux.container.extra

/**
 * Indicates that this object can be written to files without having to call
 * its handler or manager class aka the container.
 *
 * @param <T> Type of the object.
</T> */
interface ICanSave<T> {
    /**
     * Saves the object.
     */
    fun save()
}

