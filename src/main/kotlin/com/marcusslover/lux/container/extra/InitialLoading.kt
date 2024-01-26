package com.marcusslover.lux.container.extra

/**
 * Annotate your custom container class with this annotation to toggle
 * whether you want your container to load all the data upon plugin enabling process.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class InitialLoading(
    /**
     * Toggle whether this container's data should be loaded on start.
     *
     * @return True if it should autoload, false if it shouldn't load.
     */
    val value: Boolean = true
)

