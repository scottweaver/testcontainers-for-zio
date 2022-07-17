package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.json._

object Env extends Subtype[Map[String, String]] {

  def make(kvs: (String, String)*): Env = wrap(Map(kvs: _*))

  val empty: Env = wrap(Map.empty)

  implicit val EnvEncoder: JsonEncoder[Env] =
    JsonEncoder.seq[String].contramap[Env] { env =>
      (env.map { case (k, v) => s"${k}=${v}" }).toSeq
    }
}
