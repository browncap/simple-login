package com.login

import cats.Show
import dev.profunktor.auth.jwt.{JwtSymmetricAuth, JwtToken}
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder

import scala.util.control.NoStackTrace

final case class UserId(value: FUUID)
object UserId {
  implicit val decoder: Decoder[UserId] = deriveDecoder
  implicit val encoder: Encoder[UserId] = deriveEncoder
}
final case class User(userId: UserId, username: Username)
case class UserJwtAuth(value: JwtSymmetricAuth)

final case class LoginUser(username: Username, password: Password)
object LoginUser {
  implicit def decoder: Decoder[LoginUser] = deriveDecoder
}

final case class Username(value: String)
object Username {
  implicit val decoder: Decoder[Username] = deriveUnwrappedDecoder
  implicit val encoder: Encoder[Username] = deriveUnwrappedEncoder
  implicit val show: Show[Username] = Show.show(_.value)
}

final case class Password(value: String)
object Password {
  implicit val decoder: Decoder[Password] = deriveUnwrappedDecoder
  implicit val encoder: Encoder[Password] = deriveUnwrappedEncoder
}

final case class HashedPassword(value: String)

// Errors

final case class TokenHeaderNotFound(userId: UserId) extends NoStackTrace
final case class InvalidUsernameOrPassword(username: Username) extends NoStackTrace
final case class TokenNotFound(token: JwtToken) extends NoStackTrace
final case class UserNotFound(username: Username) extends NoStackTrace
final case class UsernameInUse(username: Username) extends NoStackTrace
