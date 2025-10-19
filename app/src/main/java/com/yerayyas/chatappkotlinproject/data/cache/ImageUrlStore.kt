package com.yerayyas.chatappkotlinproject.data.cache

/**
 * Singleton object for temporarily storing image URLs in memory.
 *
 * This store provides a simple in-memory cache using a mutable map
 * to associate image identifiers with their corresponding URLs.
 * It is useful for quick access to recently used image URLs
 * without repeatedly fetching them from the network or database.
 *
 * Note: This storage is ephemeral and will be cleared if the app process is killed.
 */
object ImageUrlStore {

    private val imageUrls = mutableMapOf<String, String>()

    /**
     * Adds or updates an image URL associated with the given ID.
     *
     * @param id Unique identifier for the image.
     * @param url The URL corresponding to the image.
     */
    fun addImageUrl(id: String, url: String) {
        imageUrls[id] = url
    }

    /**
     * Retrieves the image URL associated with the given ID.
     *
     * @param id The identifier of the image.
     * @return The image URL if it exists, or null otherwise.
     */
    fun getImageUrl(id: String): String? {
        return imageUrls[id]
    }

    /**
     * Removes the image URL associated with the given ID.
     *
     * @param id The identifier of the image to remove.
     */
    fun removeImageUrl(id: String) {
        imageUrls.remove(id)
    }
}
