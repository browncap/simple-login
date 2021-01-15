package com.login.http

import cats.effect.IO
import com.login.{MockUserAuth, UserId}
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.{Method, Request, Uri}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.http4s.implicits._
import org.http4s._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import org.http4s.Status._

class LogoutRoutesSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {

  "return expected json response for /logout" in {

    val userId = UserId(FUUID.randomFUUID[IO].unsafeRunSync())

    val mockUserAuth = new MockUserAuth {
      override def logout(token: JwtToken, userId: UserId): IO[Unit] = IO.unit
      override def verify(token: JwtToken)(claim: JwtClaim): IO[Option[UserId]] = IO(Option(userId))
    }

    implicit val logger = Slf4jLogger.create[IO].unsafeRunSync()
    val jwtAuth = JwtAuth.hmac("secret-key", JwtAlgorithm.HS256)
    val authMiddleware = JwtAuthMiddleware[IO, UserId](jwtAuth, mockUserAuth.verify)
    val jwt = Jwt.encode(JwtClaim("token"), jwtAuth.secretKey.value, jwtAuth.jwtAlgorithms.head)

    val routes = authMiddleware(LogoutRoutes.routes(mockUserAuth)).orNotFound
    val req = Request[IO](method = Method.POST, uri = Uri.unsafeFromString("/user/logout")).withHeaders(Header("Authorization", s"Bearer $jwt"))
    val routeResult = routes.run(req).unsafeRunSync

    routeResult.status should be(NoContent)

  }

}
