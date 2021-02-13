/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
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

package modules.subject.service

import modules.core.model.{Constraint, ConstraintType}
import modules.core.util.ConstraintProcessor
import modules.subject.model.CollectibleConstraintSpec
import modules.util.messages.{ERR, OK, Status}

/**
 * Trait which provides functionality for parsing and processing constraints
 */
trait CollectibleConstraintProcessor extends ConstraintProcessor {

  /**
   * Checks if a given [[modules.core.model.Constraint Constraint]] is a syntactically correct Constraint of an
   * [[modules.subject.model.Collectible Collectible]] [[modules.core.model.EntityType EntityType]]
   * No semantic analysis is done!
   *
   * @param constraint to check
   * @return Status with optional error message
   */
  override def isValidConstraint(constraint: Constraint): Status = {
    constraint.c match {
      case ConstraintType.HasProperty => isHasPropertyConstraint(constraint.v1, constraint.v2, CollectibleConstraintSpec.hasPropertyTypes)
      case ConstraintType.MustBeDefined => isMustBeDefinedConstraint(constraint.v1, constraint.v2)
      case ConstraintType.UsesPlugin => isUsesPluginConstraint(constraint.v1, constraint.v2)
      case ConstraintType.CanContain => isCanContainConstraint(constraint.v1, constraint.v2)
      case _ => ERR("Invalid Constraint Rule")
    }
  }

  /**
   * Checks if a [[modules.core.model.Constraint Constraint]] model is valid to model a [[modules.subject.model.Collectible Collectible]].
   * <p> 1. Checks if HasProperty(s) and MustBeDefined(s) are matching
   * <p> 2. Checks if no duplicate Constraints are present
   * <p> 3. Checks if the configuration serves its UsesPlugin(s)
   * <p> 4. Checks NOT for invalid Constraints without effect, use [[isValidConstraint]]
   *
   * @param constraints model to check
   * @return Status with optional error message
   */
  override def isConstraintModel(constraints: Seq[Constraint]): Status = {
    //check HasProperty, MustBeDefined Constraints and duplicates.
    val checkBaseModelStatus = super.isConstraintModel(constraints)

    if (!checkBaseModelStatus.valid) return checkBaseModelStatus
    if (!hasCompletePlugins(constraints)) return ERR("Model requires properties to serve its plugins")
    OK()
  }

}
