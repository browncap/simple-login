package com.login.http

import cats.implicits._
import cats.effect.Sync
import com.login.{LoginUser, UsernameInUse}
import com.login.Encoders._
import com.login.users.UserAuth
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._

object RegistrationRoutes {
  def routes[F[_]](auth: UserAuth[F])(implicit F: Sync[F], L: Logger[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case req @ POST -> Root / "user" / "registration" =>
        (for {
          req <- req.as[LoginUser]
          jwt <- auth.register(req.username, req.password)
          resp <- Ok(jwt)
        } yield resp).recoverWith {
          case UsernameInUse(u) => L.error(s"Username ${u.value} already in use") *> NotFound()
        }

    }
  }
}
