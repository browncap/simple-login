package com.login.http

import cats.effect.IO
import com.login.{LoginUser, MockUserAuth, Password, Username}
import com.login.utils.arbitraries._
import com.login.utils.encoders._
import dev.profunktor.auth.jwt.JwtToken
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import io.circe.Json
import org.http4s.{Method, Request, Uri}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.implicits._

class RegistrationRoutesSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {

  "return expected json response for /registration" in {
    forAll { (l: LoginUser, tokenValue: String) =>

      val result = Json.obj(
        "value" := tokenValue
      )

      val mockUserAuth = new MockUserAuth {
        override def register(username: Username, password: Password): IO[JwtToken] = IO.pure(JwtToken(tokenValue))
      }

      implicit val logger = Slf4jLogger.create[IO].unsafeRunSync()
      val routes = RegistrationRoutes.routes(mockUserAuth).orNotFound
      val req = Request[IO](method = Method.POST, uri = Uri.unsafeFromString("/user/registration")).withEntity(l)
      val routeResult = routes.run(req).unsafeRunSync
      val jwtToken = routeResult.as[Json].unsafeRunSync

      routeResult.status.isSuccess should be(true)
      jwtToken should be(result)
    }
  }

}
