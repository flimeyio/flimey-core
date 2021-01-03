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

package modules.asset.service

import modules.asset.model.{AssetConstraint, AssetProperty}
import modules.asset.service.AssetConstraintHelper.ConstraintType
import modules.util.messages.{ERR, OK, Status}
import modules.user.service.ViewerProcessor

/**
 * The AssetLogic object provides static functionality to process, verify and validate
 * constraints of the AssetType model.
 */
object AssetLogic extends ConstraintProcessor with PropertyProcessor with StringProcessor with ViewerProcessor {

  /**
   * Checks a given AssetType constraint model for semantic correctness.
   * It is checked that:
   * 1) exactly one DerivesFrom rule exists
   * 2) every MustBeDefined rule has a corresponding HasProperty rule
   * 3) no rules are duplicates
   *
   * @param constraints model to check
   * @return Status with optional error message
   */
  def isAssetConstraintModel(constraints: Seq[AssetConstraint]): Status = {
    //exactly one derives rule
    val derivationCount = constraints.count(c => c.c == ConstraintType.DerivesFrom.short)
    if(derivationCount < 1){
      return ERR("Asset Type must have a 'Derives From' constraint")
    } else if (derivationCount > 1) {
      return ERR("Asset Type must have only one 'Derives From' constraint")
    }
    //must constrains only on existing properties
    if(!hasMatchingProperties(constraints)){
      return ERR("Every 'Must Be Defined' constraint needs a corresponding 'Has Property' constraint")
    }
    //no duplicates
    if(!hasNoDuplicates(constraints)){
      return ERR("Constraints must not have duplicates")
    }
    //everything ok
    OK()
  }

  /**
   * Validates, if a given sequence of raw property data is a correct model configuration.<br />
   * The passed property values must be in the same order as their corresponding keys from getAssetPropertyKeys().
   *
   * @param constraints model of the parent AssetType
   * @param rawProps raw configuration Properties
   * @return Status with optional error message
   */
  def isModelConfiguration(constraints: Seq[AssetConstraint], rawProps: Seq[AssetProperty]): Status = {
    val propKeys = getAssetPropertyKeys(constraints)
    val obligatoryKeys = getObligatoryPropertyKeys(constraints)

    for(i <- propKeys.indices){

      val (key, typeName) = propKeys(i)
      val property = rawProps.find(_.key == key)

      //check if the key has a property (basically true by architecture) just a defensive check
      if(property.isEmpty) return ERR("Property "+key+" is missing")

      //check if the property type is correct
      if(!isOfType(property.get.value, typeName)) return ERR("Property "+key+" is of a wrong type")

      //check if property has a non blank value, if it must be defined
      if(obligatoryKeys.contains(key) && property.get.value.isBlank) return ERR("Property "+key+" is not defined")
    }

    OK()
  }

}
