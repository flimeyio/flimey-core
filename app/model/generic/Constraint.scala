package model.generic

/**
 * The Constraint data class.
 * A Constraint can be used as AssetConstraint or SubjectConstraint
 * @param id unique primary key (given by db interface)
 * @param c constraint rule key
 * @param v1 first rule argument
 * @param v2 second rule argument
 * @param typeId id of the associated AssetType or SubjectType
 */
case class Constraint (id: Long, c: String, v1: String, v2: String, typeId: Long)
