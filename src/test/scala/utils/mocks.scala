package com.login.utils

import cats.effect.IO
import com.login.{HashedPassword, Password, User, UserId, Username}
import com.login.users.{UserAndHash, UserAuth, UserRepository}
import dev.profunktor.auth.jwt.JwtToken
import io.chrisdavenport.fuuid.FUUID
import pdi.jwt.JwtClaim

class MockDoobieRepository extends UserRepository[IO]{
  def getUserAndHash(username: Username): IO[Option[UserAndHash]] = ???
  def getUserWithUsername(username: Username): IO[Option[User]] = ???
  def createUser(userId: FUUID, username: Username, hashedPassword: HashedPassword): IO[Unit] = ???
}

class MockUserAuth extends UserAuth[IO] {
  def register(username: Username, password: Password): IO[JwtToken] = ???
  def login(username: Username, password: Password): IO[JwtToken] = ???
  def logout(token: JwtToken, userId: UserId): IO[Unit] = ???
  def verify(token: JwtToken)(claim: JwtClaim): IO[Option[UserId]] = ???
}
