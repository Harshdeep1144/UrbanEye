package com.example.urbaneye.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val rating: Double = 5.0
)
