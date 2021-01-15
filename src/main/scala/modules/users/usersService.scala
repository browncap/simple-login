package com.login.users

import cats.effect.Sync
import io.chrisdavenport.fuuid.FUUID
import cats.implicits._
import com.login.auth.Crypto
import com.login.{DoobieRepository, InvalidUsernameOrPassword, Password, User, UserId, UserNotFound, Username}

trait Users[F[_]] {
  def find(username: Username, password: Password): F[User]
  def find(username: Username): F[User]
  def create(username: Username, password: Password): F[UserId]
}

object Users {
  def build[F[_]](repo: DoobieRepository[F], crypto: Crypto[F])(implicit F: Sync[F]): Users[F] = {
    new Users[F] {
      override def find(username: Username, password: Password): F[User] = for {
        userAndHash <- repo.getUserAndHash(username).flatMap(F.fromOption(_, UserNotFound(username)))
        user <- crypto.checkHashedPassword(password, userAndHash.hashedPassword).flatMap {
                  case true => User(userAndHash.userId, username).pure[F]
                  case _    => F.raiseError[User](InvalidUsernameOrPassword(username))
                }
      } yield user

      override def find(username: Username): F[User] =
        repo.getUserWithUsername(username).flatMap(F.fromOption(_, UserNotFound(username)))

      override def create(username: Username, password: Password): F[UserId] = for {
        userId <- FUUID.randomFUUID
        hashPw <- crypto.getHashedPassword(password)
        _      <- repo.createUser(userId, username, hashPw)
      } yield UserId(userId)
    }
  }
}
