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

package modules.asset.service

import modules.asset.model.AssetConstraintSpec
import modules.core.model.{Constraint, ConstraintType}
import modules.core.util.ConstraintProcessor
import modules.util.messages.{ERR, Status}

/**
 * Trait which provides functionality for parsing and processing [[modules.core.model.Constraint Constraints]] of
 * [[modules.asset.model.Asset Assets]].
 */
trait AssetConstraintProcessor extends ConstraintProcessor {

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct Constraint of an AssetType.
   * No semantic analysis is done!
   *
   * @param constraint to check
   * @return Status with optional error message
   */
  override def isValidConstraint(constraint: Constraint): Status = {
    constraint.c match {
      case ConstraintType.HasProperty => isHasPropertyConstraint(constraint.v1, constraint.v2, AssetConstraintSpec.hasPropertyTypes)
      case ConstraintType.MustBeDefined => isMustBeDefinedConstraint(constraint.v1, constraint.v2)
      case _ => ERR("Invalid Asset Constraint Rule")
    }
  }

  override def isConstraintModel(constraints: Seq[Constraint]): Status = super.isConstraintModel(constraints)

  override def applyConstraint(newConstraint: Constraint): Seq[Constraint] = Seq(newConstraint)

  override def removeConstraint(removedConstraint: Constraint, constraints: Seq[Constraint]): Seq[Constraint] =
    Seq(removedConstraint)
}
