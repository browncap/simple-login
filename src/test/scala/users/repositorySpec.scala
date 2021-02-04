package com.login.users

import cats.effect.{ContextShift, IO}
import com.login.{HashedPassword, Migrator, User, UserId, Username}
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import com.login.config.PostgresConfig
import com.login.users.{PostgresUserRepository, UserAndHash}
import org.scalatest._

class UserRepositorySpec extends FlatSpec with Matchers with BeforeAndAfterAll  {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

  private[this] val jdbcUrl = "jdbc:postgresql://localhost:5432/store"
  private[this] val pgUsername = "postgres"
  private[this] val pgPassword = "password"

  lazy val postgresConfig = PostgresConfig(jdbcUrl, pgUsername, pgPassword)
  lazy val y = transactor.yolo
  lazy val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      jdbcUrl,
      pgUsername,
      pgPassword
    )

  override def beforeAll: Unit = {
    Migrator.migrate[IO](postgresConfig).unsafeRunSync()
  }

  override def afterAll: Unit = {}

  def userRepo = PostgresUserRepository.build[IO](transactor)

  def randomHashPassword = HashedPassword(FUUID.randomFUUID[IO].unsafeRunSync().show)
  def randomUser = User(UserId(FUUID.randomFUUID[IO].unsafeRunSync()), Username(FUUID.randomFUUID[IO].unsafeRunSync().show))

  "Postgres repo" should "insert a user and successfully retrieve it w/ a username" in {
    val user = randomUser
    val hashPw = randomHashPassword

    userRepo.createUser(user.userId.value, user.username, hashPw).unsafeRunSync
    userRepo.getUserWithUsername(user.username).unsafeRunSync() should be(Option(user))
  }

  it should "insert a user and successfully retrieve a user_id + hashed_password w/ a username" in {
    val user = randomUser
    val hashPw = randomHashPassword
    val userAndHash = UserAndHash(user.userId, hashPw)

    userRepo.createUser(user.userId.value, user.username, hashPw).unsafeRunSync
    userRepo.getUserAndHash(user.username).unsafeRunSync() should be(Option(userAndHash))
  }

}
