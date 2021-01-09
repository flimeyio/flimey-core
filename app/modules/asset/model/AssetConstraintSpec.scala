package modules.asset.model

import modules.core.model.{ConstraintType, PropertyType}

/**
 * Object with static helper functionality for Constraints used by Assets.
 */
object AssetConstraintSpec {

  val ASSET: String = "asset"
  /**
   * Sequence of possible parent types.
   */
  val canDeriveFrom: Seq[String] = Seq[String](ASSET)

  /**
   * Sequence of possible property data types.
   */
  val hasPropertyTypes: Seq[String] = PropertyType.values.map(_.name).toSeq

  /**
   * Sequence of allowed constraint types of an asset
   */
  val allowedConstraintTypes: Seq[ConstraintType.Type] = Seq(
    ConstraintType.MustBeDefined,
    ConstraintType.HasProperty
  )
}
