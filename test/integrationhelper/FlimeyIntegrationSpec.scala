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

package integrationhelper

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.db.DBApi
import play.api.db.evolutions.{Evolutions, SimpleEvolutionsReader}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting

/**
 * A helper superclass to write integration tests which need access to the database.
 * <p> This class does:
 * <p> 1. Connect to the flimey_data_test and flimey_session_test databases
 * <p> 2. Applies the defined evolutions (adds tables, system and public groups, system user)
 * <p> 3. provides an injector which can be used to create autowired instances
 *
 * <p> <strong> Before and after each test suite, the database will be reset automatically! </strong>
 */
abstract class FlimeyIntegrationSpec extends PlaySpec with ScalaFutures with BeforeAndAfterAll with GuiceOneAppPerSuite with Injecting {

  import play.api.db.evolutions.ThisClassLoaderEvolutionsReader.evolutions

  implicit override val patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Map(
      "slick.dbs.flimey_data.db.url" -> "jdbc:postgresql://localhost:5432/flimey_data_test",
      "slick.dbs.flimey_data.db.user" -> "developer",
      "slick.dbs.flimey_data.db.password" -> "developer",
      "slick.dbs.flimey_session.db.url" -> "jdbc:postgresql://localhost:5432/flimey_session_test",
      "slick.dbs.flimey_session.db.user" -> "developer",
      "slick.dbs.flimey_session.db.password" -> "developer"
    )).build()

  val injector: Injector = fakeApplication().injector

  override def beforeAll(): Unit = {
    super.beforeAll()

    val dbData = inject[DBApi].database("flimey_data")
    Evolutions.applyEvolutions(dbData, SimpleEvolutionsReader.forDefault(evolutions("flimey_data"): _*))

    val dbSession = inject[DBApi].database("flimey_session")
    Evolutions.applyEvolutions(dbSession, SimpleEvolutionsReader.forDefault(evolutions("flimey_session"): _*))
  }

  override def afterAll(): Unit = {
    super.afterAll()

    val dbData = inject[DBApi].database("flimey_data")
    Evolutions.cleanupEvolutions(dbData)

    val dbSession = inject[DBApi].database("flimey_session")
    Evolutions.cleanupEvolutions(dbSession)
  }

}