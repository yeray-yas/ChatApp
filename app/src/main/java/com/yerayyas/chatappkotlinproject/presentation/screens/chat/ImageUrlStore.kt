package com.yerayyas.chatappkotlinproject.presentation.screens.chat

object ImageUrlStore {
    private val imageUrls = mutableMapOf<String, String>()

    fun addImageUrl(id: String, url: String) {
        imageUrls[id] = url
    }

    fun getImageUrl(id: String): String? {
        return imageUrls[id]
    }

    fun removeImageUrl(id: String) {
        imageUrls.remove(id)
    }
} 