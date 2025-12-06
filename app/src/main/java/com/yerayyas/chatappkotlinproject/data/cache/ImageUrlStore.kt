package com.yerayyas.chatappkotlinproject.data.cache

/**
 * Singleton object for temporarily storing image URLs in memory.
 *
 * This store provides a simple in-memory cache using a thread-safe mutable map
 * to associate image identifiers with their corresponding URLs. It serves as a
 * lightweight caching mechanism for recently accessed images, reducing the need
 * for repeated network requests or database queries.
 *
 * Key characteristics:
 * - Thread-safe operations using concurrent collections
 * - In-memory storage for fast access
 * - Ephemeral storage (cleared when app process terminates)
 * - Simple key-value mapping for image URL retrieval
 * - Useful for image viewing components and galleries
 *
 * Use cases:
 * - Caching image URLs for full-screen image viewer
 * - Temporary storage during image upload/download processes
 * - Quick access to recently viewed images
 * - Reducing redundant network calls for image metadata
 *
 * Note: This storage is ephemeral and will be cleared if the app process is killed.
 * For persistent image caching, consider using a proper image loading library
 * like Glide or implementing a disk-based cache.
 */
object ImageUrlStore {

    private val imageUrls = mutableMapOf<String, String>()

    /**
     * Adds or updates an image URL associated with the given ID.
     *
     * This method provides a thread-safe way to store image URLs with their
     * corresponding identifiers. If an ID already exists, its URL will be updated.
     *
     * @param id Unique identifier for the image (should be non-blank)
     * @param url The URL corresponding to the image (should be non-blank)
     * @throws IllegalArgumentException if id or url are blank
     */
    @Synchronized
    fun addImageUrl(id: String, url: String) {
        require(id.isNotBlank()) { "Image ID cannot be blank" }
        require(url.isNotBlank()) { "Image URL cannot be blank" }

        imageUrls[id] = url
    }

    /**
     * Retrieves the image URL associated with the given ID.
     *
     * This method provides thread-safe access to cached image URLs.
     * Returns null if no URL is associated with the provided ID.
     *
     * @param id The identifier of the image to retrieve
     * @return The image URL if it exists, or null otherwise
     */
    @Synchronized
    fun getImageUrl(id: String): String? {
        return imageUrls[id]
    }

    /**
     * Removes the image URL associated with the given ID.
     *
     * This method provides thread-safe removal of cached image URLs.
     * No operation is performed if the ID doesn't exist in the cache.
     *
     * @param id The identifier of the image to remove
     * @return true if the image was removed, false if it didn't exist
     */
    @Synchronized
    fun removeImageUrl(id: String): Boolean {
        return imageUrls.remove(id) != null
    }

    /**
     * Clears all cached image URLs.
     *
     * This method removes all entries from the cache, effectively resetting
     * the store to its initial empty state. Useful for memory management
     * or when starting fresh sessions.
     */
    @Synchronized
    fun clearAll() {
        imageUrls.clear()
    }

    /**
     * Gets the current number of cached image URLs.
     *
     * @return The number of entries currently in the cache
     */
    @Synchronized
    fun size(): Int {
        return imageUrls.size
    }

    /**
     * Checks if the cache contains an entry for the given ID.
     *
     * @param id The identifier to check for
     * @return true if the ID exists in the cache, false otherwise
     */
    @Synchronized
    fun contains(id: String): Boolean {
        return imageUrls.containsKey(id)
    }
}
