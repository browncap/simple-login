package com.login.users

import cats.implicits._
import cats.effect.{Bracket, Sync}
import com.login.{HashedPassword, User, UserId, Username, UsernameInUse}
import doobie.free.connection.ConnectionIO
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.postgres.sqlstate
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.update.Update0
import doobie.postgres._
import io.chrisdavenport.fuuid.doobie.implicits._
import io.chrisdavenport.fuuid.FUUID

final case class UserAndHash(userId: UserId, hashedPassword: HashedPassword)

trait UserRepository[F[_]]{
  def getUserAndHash(username: Username): F[Option[UserAndHash]]
  def getUserWithUsername(username: Username): F[Option[User]]
  def createUser(userId: FUUID, username: Username, password: HashedPassword): F[Unit]
}

object PostgresUserRepository {
  def build[F[_]](transactor: Transactor[F])(implicit F: Sync[F], ev: Bracket[F, Throwable]): UserRepository[F] = new UserRepository[F] {
    import Queries._

    def getUserAndHash(username: Username): F[Option[UserAndHash]] = {
      selectUserAndHash(username).option.transact(transactor)
    }

    def getUserWithUsername(username: Username): F[Option[User]] = {
      selectUserWithUsername(username).option.transact(transactor)
    }

    def createUser(userId: FUUID, username: Username, hashedPassword: HashedPassword): F[Unit] = {
      safeInsertUsernamePassword(userId, username, hashedPassword).transact(transactor).flatMap(F.fromEither(_))
    }
  }
}

object Queries {
  def selectUserAndHash(username: Username): Query0[UserAndHash] =
    sql"SELECT user_id, hashed_password from users where username = ${username.value}".query[UserAndHash]

  def selectUserWithUsername(username: Username): Query0[User] =
    sql"SELECT user_id, username from users where username = ${username.value}".query[User]

  def safeInsertUsernamePassword(userId: FUUID, username: Username, hashedPassword: HashedPassword): ConnectionIO[Either[UsernameInUse, Unit]] =
    sql"INSERT INTO users values ($userId, ${username.value}, ${hashedPassword.value})".update.run.void.attemptSomeSqlState {
      case sqlstate.class23.UNIQUE_VIOLATION => UsernameInUse(username)
    }
}
