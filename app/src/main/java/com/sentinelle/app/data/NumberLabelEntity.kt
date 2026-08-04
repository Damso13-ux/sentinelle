package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "number_labels",
    indices = [Index(value = ["phoneNumber"], unique = true)],
)
data class NumberLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: Long,
    val category: String,
    val note: String?,
    val dateAdded: Long,
) {
    companion object {
        const val CATEGORY_DEMARCHAGE = "demarchage"
        const val CATEGORY_ARNAQUE = "arnaque"
        const val CATEGORY_ROBOT = "robot"
        const val CATEGORY_LIVRAISON = "livraison"
        const val CATEGORY_BANQUE_ASSURANCE = "banque_assurance"
        const val CATEGORY_AUTRE = "autre"

        fun displayName(category: String): String =
            when (category) {
                CATEGORY_DEMARCHAGE -> "Démarchage"
                CATEGORY_ARNAQUE -> "Arnaque"
                CATEGORY_ROBOT -> "Robot / automatisé"
                CATEGORY_LIVRAISON -> "Livraison"
                CATEGORY_BANQUE_ASSURANCE -> "Banque / assurance"
                else -> "Autre"
            }
    }
}
