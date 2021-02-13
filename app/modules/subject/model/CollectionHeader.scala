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

import modules.core.model.{EntityType, Property}
import modules.user.model.ViewerCombinator

/**
 * The CollectionHeader extends the [[modules.subject.model.Collection Collection]] class how it is commonly used with
 * all of its objectified [[modules.core.model.Property Properties]] and some additional child data.
 *
 * @param collection   Collection head (contains only id and type reference)
 * @param collectibles child [[modules.subject.model.CollectibleHeader CollectibleHeaders]]
 * @param properties   all Properties of the Collection
 * @param viewers      [[modules.user.model.ViewerCombinator ViewerCombinator]] with Viewer rights
 */
case class CollectionHeader(collection: Collection,
                            collectibles: Seq[CollectibleHeader],
                            properties: Seq[Property],
                            viewers: ViewerCombinator,
                            entityType: EntityType)