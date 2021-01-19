/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

import modules.core.model.{Constraint, ConstraintType}
import org.scalatestplus.play.PlaySpec

class AssetLogicSpec extends PlaySpec {

  "hasMatchingProperties" must {
    "return true if everything matches" in {
      val constraints = Seq[Constraint](
        Constraint(1, ConstraintType.HasProperty, "foo", "", None, 1),
        Constraint(2, ConstraintType.MustBeDefined, "foo", "bar", None, 1),
      )
      //FIXME ist now private, see https://www.scalatest.org/user_guide/using_PrivateMethodTester
      //AssetLogic.hasMatchingProperties(constraints) mustBe true
    }
    "return false if something does not match" in {
      val constraints = Seq[Constraint](
        Constraint(1, ConstraintType.HasProperty, "foo", "", None, 1),
        Constraint(2, ConstraintType.MustBeDefined, "foo", "bar", None, 1),
        Constraint(3, ConstraintType.MustBeDefined, "something", "bar", None, 1)
      )
      //FIXME ist now private, see https://www.scalatest.org/user_guide/using_PrivateMethodTester
      //AssetLogic.hasMatchingProperties(constraints) mustBe false
    }
  }
}
