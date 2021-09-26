package com.example.memorygame.newmodels

import com.google.firebase.firestore.PropertyName

data class UserImageList (
   @PropertyName("images")val images: List<String>?= null
)

