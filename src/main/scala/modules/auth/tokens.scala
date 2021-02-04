package com.login.auth

import java.time.Clock

import cats.effect.Sync
import com.login.config.JwtConfig
import dev.profunktor.auth.jwt._
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import io.circe.syntax._
import cats.implicits._
import com.login.UserId

trait Tokens[F[_]] {
  def create(userId: UserId): F[JwtToken]
}

object JwtTokens {
  def build[F[_]](jwtConfig: JwtConfig)(implicit F: Sync[F]): Tokens[F] = {
    new Tokens[F] {
      implicit val c = Clock.systemUTC

      override def create(userId: UserId): F[JwtToken] = for {
        claim <- F.delay(JwtClaim(userId.asJson.noSpaces).issuedNow.expiresIn(jwtConfig.jwtExpiration.toMillis))
        secretKey = JwtSecretKey(jwtConfig.secretKey)
        jwt <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield jwt
    }
  }
}
