package unit.services

import model.asset.AssetConstraintHelper.ConstraintType
import model.generic.Constraint
import org.scalatestplus.play.PlaySpec
import services.asset.AssetLogic

class AssetLogicSpec extends PlaySpec {

  "An AssetConstraint model" must {
    "contain a 'Derives From' rule" in {
      val constraints = Seq[Constraint]()
      val status = AssetLogic.isAssetConstraintModel(constraints)
      status.valid mustBe false
      status.msg.isDefined mustBe true
      status.msg.get mustBe "Asset Type must have a 'Derives From' constraint"
    }
  }

  "hasMatchingProperties" must {
    "return true if everything matches" in {
      val constraints = Seq[Constraint](
        Constraint(1, ConstraintType.HasProperty.short, "foo", "", 1),
        Constraint(2, ConstraintType.MustBeDefined.short, "foo", "bar", 1),
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe true
    }
    "return false if something does not match" in {
      val constraints = Seq[Constraint](
        Constraint(1, ConstraintType.HasProperty.short, "foo", "", 1),
        Constraint(2, ConstraintType.MustBeDefined.short, "foo", "bar", 1),
        Constraint(3, ConstraintType.MustBeDefined.short, "something", "bar", 1)
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe false
    }
  }
}
