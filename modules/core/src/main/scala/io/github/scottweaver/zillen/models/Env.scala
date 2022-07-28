package io.github.scottweaver
package zillen
package models

import zio.prelude._
import zio.json._

object Env extends Subtype[Map[String, String]] {

  def make(kvs: (String, String)*): Env = wrap(Map(kvs: _*))

  val empty: Env = wrap(Map.empty)

  implicit val EnvEncoder: JsonEncoder[Env] =
    JsonEncoder.seq[String].contramap[Env] { env =>
      (env.map { case (k, v) => s"${k}=${v}" }).toSeq
    }

  implicit class Syntax(val env: Env) extends AnyVal {

    def withOptionals(kvs: (String, Option[String])*): Env =
      wrap(env ++ (kvs.collect { case (k, Some(v)) => (k, v) }))

    def &&(rhs: Env): Env = wrap(env ++ rhs)
  }
}
