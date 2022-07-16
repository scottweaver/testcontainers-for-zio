package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._

sealed trait Command

object Command {

  final case class Pull(image: Image) extends Command

  final case class CreateContainer(env: Env, exposedPorts: Port.Exposed, image: Image) extends Command

}
