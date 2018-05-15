package csw.tools.mirroring.controller

import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.scheduler.MirrorSyncScheduler
import csw.tools.mirroring.service.{GitService, MirrorService}
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import org.scalatra._

import scala.util.Try

class MirrorApiController(mirrorService: MirrorService, mirrorSyncScheduler: MirrorSyncScheduler)
    extends ControllerBase
    with AccountService
    with OwnerAuthenticator
    with RepositoryService {

  delete("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly(repo => {
      val deletedMirror = mirrorService.deleteMirror(repo)
      deletedMirror.foreach(mirror => mirrorSyncScheduler.deleteMirrorSyncJob(Repo.toJsonString(repo)))
      deletedMirror.getOrElse(NotFound())
    })
  }

  get("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly(repo => mirrorService.findMirror(repo).getOrElse(NotFound()))
  }

  post("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repo =>
      Try {
        val mirror = parsedBody.extract[Mirror]
        if (mirror.syncIntervalInMinutes <= 0) {
          NotAcceptable("Please provide positive integer as auto sync interval")
        } else {
          mirrorService.upsert(repo, mirror)
          val location = s"${context.path}/api/v3/${repo.owner}/${repo.name}/mirror"
          mirrorSyncScheduler.upsertJobWithTrigger(repo, mirror)
          Created(mirror, Map("location" -> location))
        }
      }.getOrElse(BadRequest())
    }
  }

  put("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repo =>
      Try {
        val mirror = parsedBody.extract[Mirror]
        if (mirror.syncIntervalInMinutes <= 0) {
          NotAcceptable("Please provide positive integer as auto sync interval")
        } else {
          mirrorService.upsert(repo, mirror)
          mirrorSyncScheduler.upsertJobWithTrigger(repo, mirror)
          Ok(mirror)
        }
      }.getOrElse(NotFound())
    }
  }

  put("/api/v3/repos/:owner/:repository/mirror/status") {
    val maybeResult = for {
      repoName <- params.getAs[String]("repository")
      owner    <- params.getAs[String]("owner")
      repo = Repo(owner, repoName)
      mirror <- mirrorService.findMirror(repo)
      mirrorWithStatus = new GitService(repo, mirror).sync()
      _      <- mirrorService.upsert(repo, mirrorWithStatus)
      status <- mirrorWithStatus.status
    } yield {
      Ok(status)
    }
    maybeResult.getOrElse(NotFound())
  }
}
