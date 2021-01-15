package com.login.auth

import java.util.concurrent.TimeUnit

import cats.effect.{ContextShift, IO}
import com.login.UserId
import io.chrisdavenport.fuuid.FUUID
import io.circe.parser.decode
import com.login.config.JwtConfig
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.effect.Log.Stdout
import dev.profunktor.auth.jwt
import pdi.jwt.JwtAlgorithm
import org.scalatest._

import scala.concurrent.duration.FiniteDuration

class TokensSpec extends FlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val log: Log[IO] = Stdout.instance[IO]

  val secretKey = "secret-key"
  val jwtExp = FiniteDuration(30, TimeUnit.SECONDS)
  val jwtConfig = JwtConfig(secretKey, jwtExp)

  val tokens = JwtTokens.build[IO](jwtConfig)
  val jwtAuth = JwtAuth.hmac(secretKey, JwtAlgorithm.HS256)

  "Tokens" should "successfully create a token that can be decoded to retrieve user information" in {
    val userId = UserId(FUUID.randomFUUID[IO].unsafeRunSync())

    val token = tokens.create(userId).unsafeRunSync()
    val claim = jwt.jwtDecode[IO](token, jwtAuth).unsafeRunSync()
    val decodeToken = decode[UserId](claim.content).toOption

    decodeToken should be(Option(userId))
  }

}
