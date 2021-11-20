package io.github.scottweaver.jdbc

final case class JdbcInfo(
  driverClassName: String,
  jdbcUrl: String,
  username: String,
  password: String
)
