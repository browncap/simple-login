package com.login.http

import cats.effect.Sync
import cats.implicits._
import com.login.auth.Authentication
import com.login.UserId
import dev.profunktor.auth.JwtAuthMiddleware
import io.chrisdavenport.log4cats.Logger
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware.CORS

trait RoutesApp[F[_]] {
  val app: HttpApp[F]
}

object RoutesApp {
  def build[F[_]](auth: Authentication[F])(implicit F: Sync[F], L: Logger[F]): RoutesApp[F] = {
    new RoutesApp[F] {
      private val jwtMiddleware = JwtAuthMiddleware[F, UserId](auth.jwtAuth.value, auth.userAuth.verify)

      private val registrationRoutes = RegistrationRoutes.routes[F](auth.userAuth)
      private val loginRoutes = LoginRoutes.routes[F](auth.userAuth)
      private val logoutRoutes = jwtMiddleware(LogoutRoutes.routes[F](auth.userAuth))
      private val routes: HttpRoutes[F] = registrationRoutes <+> loginRoutes <+> logoutRoutes

      override val app: HttpApp[F] = CORS(routes).orNotFound

    }
  }
}
