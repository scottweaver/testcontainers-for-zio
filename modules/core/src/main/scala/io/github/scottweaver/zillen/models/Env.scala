/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
