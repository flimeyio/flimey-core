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

package modules.subject.repository

import scala.concurrent.Future

class CollectionRepository {

  def deleteCollectionType(id: Long): Future[Unit] = {
    //val sub = assets.filter(_.typeId === id).map(_.entityId)
    //db.run((for {
    //  _ <- properties.filter(_.parentId in sub).delete
    //  _ <- viewers.filter(_.targetId in sub).delete
    //  _ <- assets.filter(_.typeId === id).delete
    //  _ <- flimeyEntities.filter(_.id in sub).delete
    //  _ <- constraints.filter(_.typeId === id).delete
    //  _ <- types.filter(_.id === id).delete
    //} yield ()).transactionally)
    Future.successful()
  }

}
