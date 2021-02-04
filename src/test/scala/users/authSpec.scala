package com.login.users

import java.util.concurrent.TimeUnit

import cats.effect.{ContextShift, IO}
import com.login.auth.{BCrypto, JwtTokens}
import com.login.{HashedPassword, Migrator, Password, User, UserId, UserNotFound, Username}
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.syntax._
import io.circe.parser.decode
import com.login.config.{JwtConfig, PostgresConfig}
import com.login.users.Users
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.effect.Log.Stdout
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest._
import pdi.jwt.JwtClaim

import scala.concurrent.duration.FiniteDuration

class UserAuthSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val log: Log[IO] = Stdout.instance[IO]

  private[this] val jdbcUrl = "jdbc:postgresql://localhost:5432/store"
  private[this] val pgUsername = "postgres"
  private[this] val pgPassword = "password"

  lazy val postgresConfig = PostgresConfig(jdbcUrl, pgUsername, pgPassword)

  private[this] var redis: RedisCommands[IO, String, String] = _
  private[this] var cleanup: IO[Unit] = _

  lazy val y = transactor.yolo
  lazy val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      jdbcUrl,
      pgUsername,
      pgPassword
    )

  override def beforeAll: Unit = {
    val (r, c) = Redis[IO].utf8(s"redis://localhost").allocated.unsafeRunSync()
    redis = r
    cleanup = c
    Migrator.migrate[IO](postgresConfig).unsafeRunSync()
  }

  override def afterAll: Unit = {
    cleanup.unsafeRunSync()
  }

  val secretKey = "secret-key"
  val jwtExp = FiniteDuration(30, TimeUnit.SECONDS)
  val jwtConfig = JwtConfig(secretKey, jwtExp)

  implicit val logger = Slf4jLogger.create[IO].unsafeRunSync()

  def doobie = PostgresUserRepository.build[IO](transactor)
  def crypto = BCrypto.build[IO]
  def userSvc = Users.build[IO](doobie, crypto)
  def tokens = JwtTokens.build[IO](jwtConfig)
  def userAuth = UserAuth.build[IO](userSvc, tokens, redis, jwtConfig.jwtExpiration)

  def randomHashPassword = HashedPassword(FUUID.randomFUUID[IO].unsafeRunSync().show)
  def randomUser = User(UserId(FUUID.randomFUUID[IO].unsafeRunSync()), Username(FUUID.randomFUUID[IO].unsafeRunSync().show))
  def randomPassword = Password(FUUID.randomFUUID[IO].unsafeRunSync().show)
  def randomUsername = Username(FUUID.randomFUUID[IO].unsafeRunSync().show)
  def randomToken = JwtToken(FUUID.randomFUUID[IO].unsafeRunSync().show)

  "User auth" should "successfully log in a user" in {
    val user = randomUser
    val pw = randomPassword
    val hashPw = crypto.getHashedPassword(pw).unsafeRunSync()

    doobie.createUser(user.userId.value, user.username, hashPw).unsafeRunSync()
    val jwt = userAuth.login(user.username, pw).unsafeRunSync()
    val retrievedToken = redis.get(user.userId.value.show).unsafeRunSync().map(JwtToken(_))
    val retrievedUserId = redis.get(jwt.value).unsafeRunSync()

    retrievedToken should be(Option(jwt))
    retrievedUserId should be(Option(user.userId.value.asJson.noSpaces))
  }

  it should "successfully verify a user" in {
    val user = randomUser
    val pw = randomPassword
    val hashPw = crypto.getHashedPassword(pw).unsafeRunSync()
    val token = randomToken

    val verifyUser = for {
      _    <- doobie.createUser(user.userId.value, user.username, hashPw)
      _    <- redis.setEx(user.userId.value.show, token.value, jwtExp)
      _    <- redis.setEx(token.value, user.userId.value.asJson.noSpaces, jwtExp)
      user <- userAuth.verify(token)(JwtClaim())
    } yield user

    verifyUser.unsafeRunSync() should be(Option(user.userId))
  }

  it should "successfully register a user" in {
    val user = randomUser
    val pw = randomPassword

    val (userId, jwt, retrievedUserId, retrievedToken) =
      (for {
        jwt            <- userAuth.register(user.username, pw)
        user           <- doobie.getUserWithUsername(user.username).flatMap(u => IO.fromOption(u)(UserNotFound(user.username)))
        userId         <- redis.get(jwt.value)
        retrievedUserId = userId.flatMap(u => decode[FUUID](u).toOption.map(UserId(_)))
        retrievedToken <- redis.get(user.userId.value.show)
      } yield (user.userId, jwt, retrievedUserId, retrievedToken)).unsafeRunSync()

    retrievedUserId should be(Option(userId))
    retrievedToken should be(Option(jwt.value))
  }

  it should "successfully log out a user" in {
    val user = randomUser
    val pw = randomPassword
    val hashPw = crypto.getHashedPassword(pw).unsafeRunSync()
    val token = randomToken

    val inserts = for {
      _ <- doobie.createUser(user.userId.value, user.username, hashPw)
      _ <- redis.setEx(user.userId.value.show, token.value, jwtExp)
      _ <- redis.setEx(token.value, user.userId.value.asJson.noSpaces, jwtExp)
    } yield ()

    inserts.unsafeRunSync()
    redis.get(token.value).unsafeRunSync() should be (defined)
    redis.get(user.userId.value.show).unsafeRunSync() should be (defined)

    userAuth.logout(token, user.userId).unsafeRunSync()
    val jwt = redis.get(user.userId.value.show).unsafeRunSync()
    val userId = redis.get(token.value).unsafeRunSync()

    jwt should not be (defined)
    userId should not be (defined)
  }

}
