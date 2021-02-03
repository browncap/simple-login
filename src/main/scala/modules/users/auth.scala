package com.login.users

import cats.implicits._
import cats.effect.Sync
import com.login.auth.Tokens
import com.login.{Password, TokenNotFound, UserId, Username}
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.chrisdavenport.log4cats.Logger
import io.circe.syntax._
import io.circe.parser.decode
import pdi.jwt.JwtClaim

import scala.concurrent.duration.FiniteDuration

trait UserAuth[F[_]] {
  def register(username: Username, password: Password): F[JwtToken]
  def login(username: Username, password: Password): F[JwtToken]
  def logout(token: JwtToken, userId: UserId): F[Unit]
  def verify(token: JwtToken)(claim: JwtClaim): F[Option[UserId]]
}

object UserAuth {
  def build[F[_]](users: Users[F],
                  tokens: Tokens[F],
                  redis: RedisCommands[F, String, String],
                  jwtExpiration: FiniteDuration)(implicit F: Sync[F], L: Logger[F]): UserAuth[F] = {

    new UserAuth[F]{
      override def register(username: Username, password: Password): F[JwtToken] = for {
        _       <- L.info(s"Attemping to register username: ${username.value}")
        userId  <- users.create(username, password)
        jwt     <- tokens.create(userId)
        _       <- redis.setEx(userId.value.show, jwt.value, jwtExpiration)
        _       <- redis.setEx(jwt.value, userId.value.asJson.noSpaces, jwtExpiration)
      } yield jwt

      override def login(username: Username, password: Password): F[JwtToken] = for {
        _       <- L.info(s"Attemping login for username: ${username.value}")
        user    <- users.find(username, password)
        jwt     <- redis.get(user.userId.value.show).flatMap {
                     case Some(t) => JwtToken(t).pure[F]
                     case None => tokens.create(user.userId)
                   }
        _       <- redis.setEx(user.userId.value.show, jwt.value, jwtExpiration)
        _       <- redis.setEx(jwt.value, user.userId.value.asJson.noSpaces, jwtExpiration)
      } yield jwt

      override def logout(token: JwtToken, userId: UserId): F[Unit] =
        redis.del(token.value) *> redis.del(userId.value.show) *> F.unit

      override def verify(token: JwtToken)(claim: JwtClaim): F[Option[UserId]] =
        for {
          t <- redis.get(token.value)
          o <- F.fromOption(t, TokenNotFound(token))
          u <- F.delay(decode[FUUID](o).toOption.map(UserId(_)))
        } yield u
    }
  }
}
