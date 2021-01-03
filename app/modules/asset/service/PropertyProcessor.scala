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
import modules.asset.service.AssetConstraintHelper.{ConstraintType, PropertyType}

/**
 * Trait which provides functionality for parsing, processing and validation Properties
 */
trait PropertyProcessor extends StringProcessor {

  /**
   * Extract all values from a constraint model which serve as Property keys.
   * Each key is associated with its expected datatype.
   *
   * @param constraints model
   * @return Property keys with their datatype [(key, type)]
   */
  def getAssetPropertyKeys(constraints: Seq[AssetConstraint]): Seq[(String, String)] = {
    constraints.filter(_.c == ConstraintType.HasProperty.short).map(c => (c.v1, c.v2))
  }

  /**
   * Extract all values from a constraint model which serve as Property keys and MUST be defined (not empty).
   * Each key is mapped to its default value (or 'hint').
   *
   * @param constraints model
   * @return obligatory Property keys with default value 'key -> default'
   */
  def getObligatoryPropertyKeys(constraints: Seq[AssetConstraint]): Map[String, String] = {
    constraints.filter(_.c == ConstraintType.MustBeDefined.short).map(c => (c.v1, c.v2)).toMap
  }

  /**
   * Derives a Property configuration from raw data and a Constraint model.<br />
   * It is expected, that the propData entries are in the same order as the received keys from
   * getAssetPropertyKeys() on the same model.<br />
   * <br />
   * The returned Properties have the parentId set to 0, because this id is only known, after the parent
   * Asset is successfully inserted to the db.
   *
   * @param constraints model
   * @param propData raw configuration data
   * @return valid Property configuration
   */
  def derivePropertiesFromRawData(constraints: Seq[AssetConstraint], propData: Seq[String]): Seq[AssetProperty] = {
    val propKeys = getAssetPropertyKeys(constraints)
    if(propKeys.length != propData.length) return Seq()
    var res = Seq[AssetProperty]()
    for(i <- propKeys.indices){
      res = res :+ AssetProperty(0, propKeys(i)._1, propData(i), 0)
    }
    res
  }

  /**
   * Checks if a Property value is of the given type.<br />
   * Does not check on empty values.
   *
   * @param value to check
   * @param typeName to match
   * @return result
   */
  def isOfType(value: String, typeName: String): Boolean = {
    typeName match {
      case PropertyType.StringType.name => true
      case PropertyType.NumericType.name => isNumericString(value)
      case _ => false
    }

  }

  /**
   * Maps the values of a new configuration candidate to an existing configuration.<br />
   * The configurations must have the same length and order.<br />
   * On error, an empty configuration will be returned (because this will surely fail the model check)
   *
   * @param oldConfiguration existing configuration
   * @param newConfiguration new raw configuration candidate
   * @return new configuration values mapped to existing properties
   */
  def mapConfigurations(oldConfiguration: Seq[AssetProperty], newConfiguration: Seq[String]): Seq[AssetProperty] = {
    if(oldConfiguration.length != newConfiguration.length) return Seq()
    oldConfiguration.zip(newConfiguration).map(c => {
      val (property, newValue) = c
      AssetProperty(property.id, property.key, newValue, property.parentId)
    })
  }

}
