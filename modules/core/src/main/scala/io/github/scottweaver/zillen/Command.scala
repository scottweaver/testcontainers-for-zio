package io.github.scottweaver.zillen

sealed trait Command 

object Command {

  final case class Pull(image: Image) extends Command
}