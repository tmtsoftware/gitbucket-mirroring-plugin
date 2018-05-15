import csw.tools.mirroring.controller.{MirrorApiController, MirrorController}
import csw.tools.mirroring.scheduler.MirrorSyncScheduler
import csw.tools.mirroring.service.MirrorService
import gitbucket.core.controller.Context
import gitbucket.core.plugin.{Link, PluginRegistry}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service.SystemSettingsService
import io.github.gitbucket.solidbase.model.Version
import javax.servlet.ServletContext
import org.apache.http.impl.client.HttpClients

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String    = "mirroring"
  override val pluginName: String  = "Mirroring Plugin"
  override val description: String = "A Gitbucket plugin for pull based repository mirroring"

  private val mirrorService       = new MirrorService
  private val httpClient          = HttpClients.createDefault()
  private val mirrorSyncScheduler = new MirrorSyncScheduler(mirrorService, httpClient)

  override val versions: List[Version] = List(
    new Version("1.0.0")
  )

  override val assetsMappings = Seq("/mirror" -> "/gitbucket/mirror/assets")

  override val controllers = Seq(
    "/*"      -> new MirrorController(mirrorService),
    "/api/v3" -> new MirrorApiController(mirrorService, mirrorSyncScheduler)
  )

  mirrorSyncScheduler.start()
  mirrorSyncScheduler.scheduleAll()

  override val repositoryMenus = Seq(
    (repository: RepositoryInfo, context: Context) => Some(Link("mirror", "Mirror", "/mirror", Some("mirror")))
  )

  override def shutdown(registry: PluginRegistry, context: ServletContext, settings: SystemSettingsService.SystemSettings): Unit = {
    mirrorService.close()
    mirrorSyncScheduler.shutdown()
    httpClient.close()
    super.shutdown(registry, context, settings)
  }
}
