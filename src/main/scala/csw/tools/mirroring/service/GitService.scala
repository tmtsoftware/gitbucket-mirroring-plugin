package csw.tools.mirroring.service

import java.io._
import java.net.URI
import java.util.Date

import csw.tools.mirroring.model.{Mirror, MirrorStatus, Repo}
import gitbucket.core.util.Directory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.{RefSpec, URIish}
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.control.NonFatal

class GitService(repo: Repo, mirror: Mirror) {

  private val mirrorRefSpec      = new RefSpec("+refs/*:refs/*")
  private val fetchMirrorRefSpec = new RefSpec("refs/heads/*:refs/remotes/origin/*")
  private val logger             = LoggerFactory.getLogger(classOf[MirrorService])
  private val repositoryPath     = s"${Directory.GitBucketHome}/repositories/${repo.owner}/${repo.name}.git"

  import scala.language.postfixOps

  private val publishScriptPath = s"$repositoryPath/cloned/${repo.name}/publish.sh"
  private val publishScriptContent =
    s"""#!/bin/bash
       |echo 'running publish script'
       |cd $repositoryPath/cloned/${repo.name} && sbt publish""".stripMargin

  private val cleanUpScriptPath = s"$repositoryPath/cloned/${repo.name}/delete.sh"
  private val cleanUpScriptContent =
    s"""#!/bin/bash 
       |echo 'running clean-up script'
       |rm -rf $repositoryPath/cloned""".stripMargin

  def sync(): Mirror = {
    val mirrorStatus = try {
      fetchAndPublish()
      successStatus()
    } catch {
      case NonFatal(ex) => failureStatus(ex)
    }

    mirror.withStatus(mirrorStatus)
  }

  def fetchAndPublish(): Unit = {
    val remoteUrl     = URI.create(mirror.remoteUrl)
    val gitRepo       = git()
    val gitRepoRemote = gitRepo.remoteSetUrl()
    gitRepoRemote.setName("origin")
    gitRepoRemote.setUri(new URIish(remoteUrl.toString))
    gitRepoRemote.call()
    val initialCommitOpt = Option(gitRepo.getRepository.resolve("removeSC"))
    initialCommitOpt.fold {
      gitRepo.fetch().setRemote(remoteUrl.toString).setRefSpecs(mirrorRefSpec).call()
      publish()
    } { initialCommit =>
      gitRepo.fetch().setRemote(remoteUrl.toString).setRefSpecs(fetchMirrorRefSpec).call()
      gitRepo.fetch().setRemote(remoteUrl.toString).setRefSpecs(mirrorRefSpec).call()
      val newCommitOpt = Option(gitRepo.getRepository.resolve("removeSC"))
      newCommitOpt.foreach { newCommit =>
        if (newCommit.compareTo(initialCommit) != 0) {
          publish()
        } else {
          println("*" * 100)
          println("No change detected, script artifacts are already up to date !!")
          println("*" * 100)
        }
      }
    }
  }

  private def publish(): Unit = {
    println("*" * 100)
    println("Changes detected, publishing script artifacts !!")
    println("*" * 100)
    // Cloning bare git repo to get the content
    Git
      .cloneRepository()
      .setURI(repositoryPath)
      .setDirectory(new File(s"$repositoryPath/cloned/${repo.name}/"))
      .setBranch("refs/heads/removeSC")
      .call();
    // Creating script for publishing artifacts
    createScript(publishScriptPath, publishScriptContent)
    // Running publish script
    runScriptProcess(publishScriptPath)
    // Creating script for deleting cloned repo - Tear down for next sync
    createScript(cleanUpScriptPath, cleanUpScriptContent)
    // Running tear down
    runScriptProcess(cleanUpScriptPath)
  }

  private def createScript(path: String, content: String): Unit = {
    val file   = new File(path)
    val writer = new PrintWriter(file)
    writer.write(content)
    writer.close()
    file.setExecutable(true, false)
  }

  private def runScriptProcess(scriptPath: String): Unit = {
    val processBuilder: ProcessBuilder = new ProcessBuilder(scriptPath)
    try {
      val process = processBuilder.start();
      Source.fromInputStream(process.getInputStream).getLines().foreach(println)
    } catch {
      case NonFatal(ex: IOException) => logger.error("Something went wrong !!", ex)
    }
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
