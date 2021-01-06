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

import modules.asset.service.AssetLogic
import modules.core.model.{Constraint, ConstraintType}
import org.scalatestplus.play.PlaySpec

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
        Constraint(1, ConstraintType.HasProperty, "foo", "", 1),
        Constraint(2, ConstraintType.MustBeDefined, "foo", "bar", 1),
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe true
    }
    "return false if something does not match" in {
      val constraints = Seq[Constraint](
        Constraint(1, ConstraintType.HasProperty, "foo", "", 1),
        Constraint(2, ConstraintType.MustBeDefined, "foo", "bar", 1),
        Constraint(3, ConstraintType.MustBeDefined, "something", "bar", 1)
      )
      AssetLogic.hasMatchingProperties(constraints) mustBe false
    }
  }
}
