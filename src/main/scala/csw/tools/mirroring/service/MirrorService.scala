package csw.tools.mirroring.service

import csw.tools.mirroring.model.{Mirror, Repo}
import gitbucket.core.util.Directory
import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

class MirrorService {
  private implicit val formats: Formats = Serialization.formats(NoTypeHints)
  private val fileName                  = s"${Directory.GitBucketHome}/kv.mv.db"

  private lazy val store: MVStore                 = MVStore.open(fileName)
  private lazy val mirrors: MVMap[String, String] = store.openMap("mirrors")

  def close(): Unit = store.close()

  def findMirror(repo: Repo): Option[Mirror]   = read(mirrors.get(makeKey(repo)))
  def deleteMirror(repo: Repo): Option[Mirror] = read(mirrors.remove(makeKey(repo)))

  def upsert(repo: Repo, mirror: Mirror): Option[Mirror] = {
    findMirror(repo).fold(
      read(mirrors.put(makeKey(repo), Serialization.write(mirror)))
    ) { oldMirror =>
      val newMirror = if (mirror.status.isEmpty) { mirror.copy(status = oldMirror.status) } else mirror
      read(mirrors.put(makeKey(repo), Serialization.write(newMirror)))
    }
  }

  private def makeKey(repo: Repo) = s"${repo.owner}-${repo.name}"

  private def read(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])
}
