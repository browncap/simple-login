package com.login.http

import cats.syntax.all._
import cats.effect.Sync
import com.login.{TokenHeaderNotFound, UserId}
import com.login.auth.UserAuth
import dev.profunktor.auth.AuthHeaders
import io.chrisdavenport.log4cats.Logger
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

object LogoutRoutes {
  def routes[F[_]](auth: UserAuth[F])(implicit F: Sync[F], L: Logger[F]): AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case authReq @ POST -> Root / "user" / "logout" as user =>
        (for {
          t    <- F.fromOption(AuthHeaders.getBearerToken(authReq.req), TokenHeaderNotFound(user))
          _    <- auth.logout(t, user)
          resp <- NoContent()
        } yield resp).recoverWith {
          case TokenHeaderNotFound(u) => L.error(s"Token header not found for userId: ${u.value.show}") *> Forbidden()
        }

    }
  }
}