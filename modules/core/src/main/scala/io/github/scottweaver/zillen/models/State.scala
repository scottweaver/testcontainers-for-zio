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
