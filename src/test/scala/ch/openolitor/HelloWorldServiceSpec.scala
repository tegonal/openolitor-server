/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       OpenOlitor                          *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://openolitor.ch                *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                            */
package ch.openolitor

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import spray.json._
import OpenOlitorJsonProtocol._

class HelloWorldServiceSpec extends Specification with Specs2RouteTest with HelloWorldService {
  def actorRefFactory = system

  "HelloWorldService" should {

    "return a greeting for GET requests to the root path as xml" in {
      Get("/hello/xml") ~> myRoute ~> check {
        responseAs[String] must contain("<h1>Hello World</h1>")
      }

      "return a greeting for GET requests to the root path as json" in {
        Get("/hello/json") ~> myRoute ~> check {
          responseAs[String].parseJson.convertTo[HelloWorld] must beEqualTo(HelloWorld("Hello World!"))
        }
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put("/hello/xml") ~> sealRoute(myRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}
