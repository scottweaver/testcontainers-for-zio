package io.github.scottweaver.models

final case class JdbcInfo(
  driverClassName: String,
  jdbcUrl: String,
  username: String,
  password: String
)
