package csw.tools.mirroring.model

import gitbucket.core.service.RepositoryService.RepositoryInfo

import scala.language.implicitConversions

case class Repo(owner: String, name: String)

object Repo {
  implicit def fromRepositoryInfo(repositoryInfo: RepositoryInfo): Repo = Repo(repositoryInfo.owner, repositoryInfo.name)
}
