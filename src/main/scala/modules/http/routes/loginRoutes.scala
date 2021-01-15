package com.login.http

import cats.implicits._
import cats.effect.Sync
import com.login.auth.UserAuth
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.login.Encoders._
import com.login.{InvalidUsernameOrPassword, LoginUser, UserNotFound}
import io.chrisdavenport.log4cats.Logger
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._

object LoginRoutes {
  def routes[F[_]](auth: UserAuth[F])(implicit F: Sync[F], L: Logger[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "user" / "login" =>
        (for {
          req  <- req.as[LoginUser]
          jwt  <- auth.login(req.username, req.password)
          resp <- Ok(jwt)
        } yield resp).recoverWith {
          case UserNotFound(u) => L.error(s"User not found for username: ${u.value}") *> NotFound()
          case InvalidUsernameOrPassword(u) => L.error(s"Invalid username or password for username: ${u.value}") *> BadRequest()
        }

    }
  }
}
