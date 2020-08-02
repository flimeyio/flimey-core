package model.generic

/**
 * The Property data class.
 * A Property can be used as AssetProperty or SubjectProperty.
 *
 * @param id unique identifier
 * @param key of the property
 * @param value of the property
 * @param parentId id of the parent Asset/Subject
 */
case class Property(id: Long, key: String, value: String, parentId: Long)
