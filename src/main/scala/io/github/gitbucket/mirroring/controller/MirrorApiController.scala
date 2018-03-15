package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.model.Mirror
import io.github.gitbucket.mirroring.service.{GitService, MirrorService}
import org.scalatra.{Ok, _}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class MirrorApiController extends ControllerBase
  with AccountService
  with MirrorService
  with GitService
  with OwnerAuthenticator
  with RepositoryService {

  delete("/api/v3/repos/:owner/:repository/mirror") (ownerOnly { _ =>
    (for {
      owner <- params.getAs[String]("owner")
      repository <- params.getAs[String]("repository")
      if Await.result(deleteMirrorByRepository(owner, repository), 60.seconds)
    } yield {
      NoContent()
    }).getOrElse(NotFound())
  })

  get("/api/v3/repos/:owner/:repository/mirror") (ownerOnly { repository =>
    Await.result(findMirrorByRepository(repository.owner, repository.name), 60.seconds).getOrElse(NotFound())
  })

  post("/api/v3/repos/:owner/:repository/mirror") (ownerOnly { repository =>
    Try(parsedBody.extract[Mirror])
      .map { body =>
        val mirror = Await.result(insertMirror(body), 60.seconds)
        val location = s"${context.path}/api/v3/${repository.owner}/${repository.name}/mirror"
        Created(mirror, Map("location" -> location))
      }
      .getOrElse(BadRequest())

  })

  put("/api/v3/repos/:owner/:repository/mirror") (ownerOnly { repository =>
    val result = for {
      owner <- params.getAs[String]("owner").toRight(NotFound())
      repositoryName <- params.getAs[String]("repository").toRight(NotFound())
      body <- Try(parsedBody.extract[Mirror]).fold[Either[ActionResult, Mirror]](_ => Left(BadRequest()), Right(_))
      mirror <- Await.result(updateMirror(body.copy(userName = owner, repositoryName = repositoryName)), 60.seconds).toRight(NotFound())
    } yield Ok(mirror)

    result.merge
  })

  put("/api/v3/repos/:owner/:repository/mirror/status") (ownerOnly { repository =>
    val status = for {
      owner <- params.getAs[String]("owner")
      repositoryName <- params.getAs[String]("repository")
      mirror <- Await.result(findMirrorByRepository(owner, repositoryName), 60.seconds)
      mirrorStatus = sync(mirror)
    } yield Await.result(insertOrUpdateMirrorUpdate(mirrorStatus), 60.seconds)

    status
      .map(Ok(_))
      .getOrElse(NotFound())
  })

}