package com.login

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import com.login.config.PostgresConfig
import org.scalatest._

class DoobieRepositorySpec extends FlatSpec with Matchers with BeforeAndAfterAll  {

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

  def doobieRepo = PostgresRepository.build[IO](transactor)

  def randomHashPassword = HashedPassword(FUUID.randomFUUID[IO].unsafeRunSync().show)
  def randomUser = User(UserId(FUUID.randomFUUID[IO].unsafeRunSync()), Username(FUUID.randomFUUID[IO].unsafeRunSync().show))

  "Postgres repo" should "insert a user and successfully retrieve it w/ a username" in {
    val user = randomUser
    val hashPw = randomHashPassword

    doobieRepo.createUser(user.userId.value, user.username, hashPw).unsafeRunSync
    doobieRepo.getUserWithUsername(user.username).unsafeRunSync() should be(Option(user))
  }

  it should "insert a user and successfully retrieve a user_id + hashed_password w/ a username" in {
    val user = randomUser
    val hashPw = randomHashPassword
    val userAndHash = UserAndHash(user.userId, hashPw)

    doobieRepo.createUser(user.userId.value, user.username, hashPw).unsafeRunSync
    doobieRepo.getUserAndHash(user.username).unsafeRunSync() should be(Option(userAndHash))
  }

}
