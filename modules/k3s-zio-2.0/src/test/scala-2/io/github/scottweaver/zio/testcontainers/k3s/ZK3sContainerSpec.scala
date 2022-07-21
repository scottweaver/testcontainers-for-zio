package io.github.scottweaver.zio.testcontainers.k3s

import com.coralogix.zio.k8s.client.apps.v1.deployments
import com.coralogix.zio.k8s.client.apps.v1.deployments.Deployments
import com.coralogix.zio.k8s.client.config.httpclient.k8sSttpClient
import com.coralogix.zio.k8s.client.config.{ k8sCluster, kubeconfigFromString }
import com.coralogix.zio.k8s.client.model.{ K8sNamespace, LabelSelector }
import com.coralogix.zio.k8s.client.v1.pods
import com.coralogix.zio.k8s.client.v1.pods.Pods
import org.testcontainers.k3s.K3sContainer
import zio.{ &, Scope, ZIO, ZLayer }
import zio.test._
import TestAspect._

import java.time.Duration

object ZK3sContainerSpec extends ZIOSpecDefault {
  private val k8sConfig = ZLayer.fromZIO(for {
    k3sContainer <- ZIO.service[K3sContainer]
    config       <- kubeconfigFromString(k3sContainer.getKubeConfigYaml)
  } yield config)

  def spec = suite("ZK3sContainerSpec")(
    test("Should start container and provide access to kubernetes") {
      for {
        _     <- deployments.create(Data.deployment, K8sNamespace.default)
        count <- pods
                   .getAll(
                     labelSelector = Some(LabelSelector.LabelEquals(Data.app, Data.name)),
                     namespace = Some(K8sNamespace.default)
                   )
                   .runCount
                   .delay(Duration.ofSeconds(1))
                   .repeatWhile(_ != Data.replicas.toLong)
                   .timeout(Duration.ofSeconds(10))
      } yield assertTrue(count.contains(Data.replicas.toLong))
    }
  ).provideSomeShared[Scope & Annotations](
    ZK3sContainer.default,
    k8sConfig.orDie,
    k8sCluster.orDie,
    k8sSttpClient.orDie,
    Pods.live,
    Deployments.live
  ) @@ withLiveClock
}
