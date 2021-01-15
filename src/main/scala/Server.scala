package com.login

import cats.implicits._
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, ExitCode, Resource, Timer}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import com.login.auth.Authentication
import com.login.config.{ConfigService, PostgresConfig}
import com.login.http.RoutesApp
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Server {

  def serve[F[_]](implicit F: ConcurrentEffect[F], CS: ContextShift[F], T: Timer[F]): Resource[F, ExitCode] = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    for {
      implicit0(l: SelfAwareStructuredLogger[F]) <- Resource.liftF(Slf4jLogger.create[F])
      cfg <- Resource.liftF(ConfigService.getConfig)
      trans <- buildTransactor[F](cfg.postgres)
      redis <- Redis[F].utf8(cfg.redis.uri)
      repo = PostgresRepository.build(trans)
      auth = Authentication.build(cfg.jwt, redis, repo)
      svc = RoutesApp.build[F](auth).app
      server <- Resource.liftF(buildServer(ec)(svc))
    } yield server
  }

  private def buildServer[F[_]: ConcurrentEffect : Timer](ec: ExecutionContext)(services: HttpApp[F]): F[ExitCode] =
    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(services)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  private def buildTransactor[F[_]: ContextShift : Async](postgresConfig: PostgresConfig): Resource[F, Transactor[F]] = {
    for {
      ce             <- ExecutionContexts.fixedThreadPool[F](10)
      ec             <- ExecutionContexts.cachedThreadPool
      transactor     <-
        HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          postgresConfig.jdbcUrl,
          postgresConfig.username,
          postgresConfig.password,
          ce,
          Blocker.liftExecutionContext(ec))
    } yield transactor
  }

}
