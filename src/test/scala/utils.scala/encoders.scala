package com.login.utils

import cats.Applicative
import com.login.LoginUser
import io.circe.Encoder
import org.http4s.EntityEncoder
import io.circe.generic.semiauto._
import org.http4s.circe.jsonEncoderOf

object encoders {
  implicit val loginUserEncoder: Encoder[LoginUser] = deriveEncoder[LoginUser]
  implicit def loginUserEntityEncoder[F[_]: Applicative]: EntityEncoder[F, LoginUser] =
    jsonEncoderOf[F, LoginUser]
}
