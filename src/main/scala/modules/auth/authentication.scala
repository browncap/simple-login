package com.login.auth

import cats.effect._
import com.login.UserJwtAuth
import com.login.config.JwtConfig
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.RedisCommands
import pdi.jwt._
import com.login.users.{UserAuth, UserRepository, Users}
import io.chrisdavenport.log4cats.Logger

final class Authentication[F[_]] private (
  val userAuth: UserAuth[F],
  val jwtAuth: UserJwtAuth,
  val crypto: Crypto[F]
)

object Authentication {
  def build[F[_]: Sync](jwtConfig: JwtConfig,
                        redis: RedisCommands[F, String, String],
                        userRepo: UserRepository[F])(implicit L: Logger[F]): Authentication[F] = {

    val jwtAuth: UserJwtAuth =
      UserJwtAuth(JwtAuth.hmac(jwtConfig.secretKey, JwtAlgorithm.HS256))

    val crypto = BCrypto.build[F]
    val users = Users.build[F](userRepo, crypto)
    val tokens = JwtTokens.build[F](jwtConfig)
    val userAuth = UserAuth.build[F](users, tokens, redis, jwtConfig.jwtExpiration)

    new Authentication[F](userAuth, jwtAuth, crypto)
  }
}