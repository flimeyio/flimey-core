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

package modules.core.model

/**
 * The Property data class is used to store objectified attributes of a [[FlimeyEntity]]. The combination of all
 * Properties (basically a multi-bridge pattern) of a FlimeyEntity is called "configuration".
 * <p> What valid Properties values are is not given by the scala or sql model! This must always be validated by the
 * application logic!
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 * @param key of the property
 * @param value of the property
 * @param parentId id of the parent Asset
 */
case class Property(id: Long, key: String, value: String, parentId: Long)
