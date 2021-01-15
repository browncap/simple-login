package com.login.auth

import cats.effect.Sync
import com.login.{HashedPassword, Password}
import org.mindrot.jbcrypt.BCrypt

trait Crypto[F[_]] {
  def getHashedPassword(password: Password): F[HashedPassword]
  def checkHashedPassword(password: Password, hashedPassword: HashedPassword): F[Boolean]
}

object BCrypto {
  def build[F[_]](implicit F: Sync[F]): Crypto[F] = new Crypto[F] {
    override def getHashedPassword(password: Password): F[HashedPassword] =
      F.delay(HashedPassword(BCrypt.hashpw(password.value, BCrypt.gensalt())))

    override def checkHashedPassword(password: Password, hashedPassword: HashedPassword): F[Boolean] =
      F.delay(BCrypt.checkpw(password.value, hashedPassword.value))
  }
}
