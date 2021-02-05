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

package modules.subject.model

import modules.core.model.Property

/**
 * The CollectibleHeader class wraps a [[modules.subject.model.Collectible Collectible]] together with with all its objectified
 * [[modules.core.model.Property Properties]].
 *
 * @param collectible Collectible (contains only id and type reference)
 * @param properties  all Properties of the Collectible
 */
case class CollectibleHeader(collectible: Collectible, properties: Seq[Property])
