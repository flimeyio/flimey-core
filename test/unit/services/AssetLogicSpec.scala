/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package unit.services

import asset.service.AssetConstraintHelper.ConstraintType
import asset.model.AssetConstraint
import asset.service.AssetLogic
import org.scalatestplus.play.PlaySpec

class AssetLogicSpec extends PlaySpec {

  "An AssetConstraint model" must {
    "contain a 'Derives From' rule" in {
      val constraints = Seq[AssetConstraint]()
      val status = AssetLogic.isAssetConstraintModel(constraints)
      status.valid mustBe false
      status.msg.isDefined mustBe true
      status.msg.get mustBe "Asset Type must have a 'Derives From' constraint"
    }
  }

  "hasMatchingProperties" must {
    "return true if everything matches" in {
      val constraints = Seq[AssetConstraint](
        AssetConstraint(1, ConstraintType.HasProperty.short, "foo", "", 1),
        AssetConstraint(2, ConstraintType.MustBeDefined.short, "foo", "bar", 1),
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe true
    }
    "return false if something does not match" in {
      val constraints = Seq[AssetConstraint](
        AssetConstraint(1, ConstraintType.HasProperty.short, "foo", "", 1),
        AssetConstraint(2, ConstraintType.MustBeDefined.short, "foo", "bar", 1),
        AssetConstraint(3, ConstraintType.MustBeDefined.short, "something", "bar", 1)
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe false
    }
  }
}
