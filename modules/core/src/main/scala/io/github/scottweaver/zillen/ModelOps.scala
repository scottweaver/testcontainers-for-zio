package io.github.scottweaver
package zillen

import models._
import zio._
import zio.prelude._

private[zillen] trait ModelOps { self: FailureOps =>

  def makeContainerName(name: String): Validation[String, ContainerName] =
    ContainerName.make(name)

  def makeContainerNameZIO(name: String): DockerIO[Any, ContainerName] =
    makeContainerName(name).toZIO
      .mapError(DockerContainerFailure.InvalidDockerConfiguration(_))

  def makeExposedTCPPorts(ports: Int*): ProtocolPort.Exposed =
    ProtocolPort.Exposed.make(ports.map(makeTCPPort): _*)

  def makeEnv(kvs: (String, String)*): Env = Env.make(kvs: _*)

  def makeHostConfig(portMap: PortMap): HostConfig =
    HostConfig(portMap)

  def makeImage(imageTag: String): Validation[String, Image] = Image.make(imageTag)

  def makeImageZIO(imageTag: String): DockerIO[Any, Image] =
    makeImage(imageTag).toZIO
      .mapError(DockerContainerFailure.InvalidDockerConfiguration(_))

  def mirrorExposedPorts(ports: ExposedPorts) = {
    val portMaps = ports.ports.map(pp => pp -> NonEmptyChunk(HostInterface.fromPortProtocol(pp)))
    PortMap.make(portMaps: _*)
  }

  def automapExposedPorts(exposed: ProtocolPort.Exposed): DockerIO[Network, PortMap] =
    PortMap
      .makeFromExposedPorts(exposed)
      .mapError(invalidConfig(s"Failed to create Docker PortMap."))

  def makeTCPPort(port: Int): ProtocolPort = ProtocolPort.makeTCPPort(port)

  def makeUDPPort(port: Int): ProtocolPort = ProtocolPort.makeUDPPort(port)

  def makeSCTPPort(port: Int): ProtocolPort = ProtocolPort.makeSCTPPort(port)

  object protocol {
    val SCTP = Protocol.SCTP
    val TCP  = Protocol.TCP
    val UDP  = Protocol.UDP
  }

  object status {
    import models.State._
    val Created    = Status.Created
    val Running    = Status.Running
    val Paused     = Status.Paused
    val Restarting = Status.Restarting
    val Exited     = Status.Exited
    val Dead       = Status.Dead
  }

}
