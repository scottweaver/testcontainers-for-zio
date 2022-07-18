package io.github.scottweaver.zio.testcontainers.k3s

import com.coralogix.zio.k8s.client.apps.v1.deployments
import com.coralogix.zio.k8s.client.apps.v1.deployments.Deployments
import com.coralogix.zio.k8s.client.config.httpclient.k8sSttpClient
import com.coralogix.zio.k8s.client.config.{ k8sCluster, kubeconfigFromString }
import com.coralogix.zio.k8s.client.model.{ K8sNamespace, LabelSelector }
import com.coralogix.zio.k8s.client.v1.pods
import com.coralogix.zio.k8s.client.v1.pods.Pods
import org.testcontainers.k3s.K3sContainer
import zio.{ ZIO, ZLayer }
import zio.blocking.Blocking
import zio.system.System
import zio.clock.Clock
import zio.test._

import java.time.Duration

object ZK3sContainerSpec extends DefaultRunnableSpec {
  private val k8sConfig     = (for {
    k3sContainer <- ZIO.service[K3sContainer]
    config       <- kubeconfigFromString(k3sContainer.getKubeConfigYaml)
  } yield config).toLayer

  private val clusterConfig = (ZLayer.service[Blocking.Service] ++ (ZK3sContainer.default)) >>> k8sConfig
  private val sttpClient    =
    (ZLayer.service[Blocking.Service] ++ ZLayer.service[System.Service] ++ clusterConfig) >>> k8sSttpClient
  private val cluster       = (ZLayer.service[Blocking.Service] ++ clusterConfig) >>> k8sCluster

  def spec = suite("ZK3sContainerSpec")(
    testM("Should start container and provide access to kubernetes") {
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
  ).provideSomeLayerShared[Blocking with System](
    (cluster.orDie ++ sttpClient.orDie >>> (Pods.live ++ Deployments.live)) ++ Clock.live
  )
}
