package csw.tools.mirroring.service

import java.io.File
import java.net.URI
import java.util.Date

import csw.tools.mirroring.model.{Mirror, MirrorStatus, Repo}
import gitbucket.core.util.Directory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.{FetchResult, RefSpec}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

class GitService(repo: Repo, mirror: Mirror) {

  private val mirrorRefSpec  = new RefSpec("+refs/*:refs/*")
  private val logger         = LoggerFactory.getLogger(classOf[MirrorService])
  private val repositoryPath = s"${Directory.GitBucketHome}/repositories/${repo.owner}/${repo.name}.git"

  def sync(): Mirror = {
    val mirrorStatus = try {
      fetch()
      successStatus()
    } catch {
      case NonFatal(ex) => failureStatus(ex)
    }

    mirror.withStatus(mirrorStatus)
  }

  private def fetch(): FetchResult = {
    val remoteUrl    = URI.create(mirror.remoteUrl)
    val gitRepo      = git()
    val fetchCommand = gitRepo.fetch().setRemote(remoteUrl.toString).setRefSpecs(mirrorRefSpec)
    val result       = fetchCommand.call()
    gitRepo.close()
    result
  }

  private def git(): Git = new Git(
    new FileRepositoryBuilder()
      .setGitDir(new File(repositoryPath))
      .readEnvironment()
      .findGitDir()
      .build()
  )

  private def successStatus(): MirrorStatus = {
    logger.info(s"Mirror status has been successfully executed for repository ${repo.owner}/${repo.name}.")
    MirrorStatus(new Date(System.currentTimeMillis()), successful = true, None)
  }

  private def failureStatus(throwable: Throwable): MirrorStatus = {
    val repositoryName = s"${repo.owner}/${repo.name}"
    val message        = s"Error while executing mirror status for repository $repositoryName: ${throwable.getMessage}"
    logger.error(message, throwable)
    MirrorStatus(new Date(System.currentTimeMillis()), successful = false, Some(throwable.getMessage))
  }
}
