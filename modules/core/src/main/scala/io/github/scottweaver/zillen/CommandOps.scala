package io.github.scottweaver
package zillen

private[zillen] trait CommandOps {

  def createContainer(
    env: Env,
    exposedPorts: ExposedPorts,
    hostConfig: HostConfig,
    image: Image,
    containerName: Option[ContainerName]
  ) = Command.CreateContainer(
    env,
    exposedPorts,
    hostConfig,
    image,
    containerName
  )

}
