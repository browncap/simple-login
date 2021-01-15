package com.login

import dev.profunktor.auth.jwt.JwtToken
import io.circe._
import io.circe.generic.semiauto._

object Encoders {
  implicit val tokenEncoder: Encoder[JwtToken] = deriveEncoder[JwtToken]
}
