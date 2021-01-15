package com.login.config

import scala.concurrent.duration.FiniteDuration

final case class ServiceConfig(
  http: HttpConfig,
  postgres: PostgresConfig,
  jwt: JwtConfig,
  redis: RedisConfig
)

final case class HttpConfig(
  port: Int,
  host: String
)

case class PostgresConfig(
  jdbcUrl: String,
  username: String,
  password: String
)

case class JwtConfig(
  secretKey: String,
  jwtExpiration: FiniteDuration
)

case class RedisConfig(
  uri: String
)
