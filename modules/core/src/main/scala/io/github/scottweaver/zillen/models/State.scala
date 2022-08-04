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

import zio.json._

final case class State(@jsonField("Status") status: State.Status)

object State {

  implicit val StateDecoder: JsonDecoder[State] = DeriveJsonDecoder.gen

  sealed trait Status

  object Status {

    case object Created extends Status

    case object Dead extends Status

    case object Exited extends Status

    case object Paused extends Status

    case object Removing extends Status

    case object Restarting extends Status

    case object Running extends Status

    implicit val statusDecoder: JsonDecoder[Status] =
      JsonDecoder.string.mapOrFail {
        case "created"    => Right(Created)
        case "dead"       => Right(Dead)
        case "exited"     => Right(Exited)
        case "paused"     => Right(Paused)
        case "removing"   => Right(Removing)
        case "restarting" => Right(Restarting)
        case "running"    => Right(Running)
        case unrecognized =>
          Left(
            s"Docker reported an unrecognized container status, '$unrecognized'.  Expected one of 'created', 'dead', 'exited', 'paused', 'removing', 'restarting', 'running'."
          )
      }
  }
}
