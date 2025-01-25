import scala.io.Source
import java.net.URLEncoder
import java.io.PrintWriter
import scala.util.{Try, Using}
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.math.Ordering.Implicits.infixOrderingOps

case class Movie(id: Int, title: String)

object Scalix extends App {
  implicit val formats: DefaultFormats.type = DefaultFormats
  // insert your TMDB api key here
  private val apiKey = ""
  private val baseUrl = "https://api.themoviedb.org/3"
  private val cacheDir = new java.io.File("data")
  private val primaryCache = scala.collection.mutable.Map[String, String]()

  if (!cacheDir.exists()) {
    cacheDir.mkdir()
  }

  private def fetchFromApi(url: String): Option[String] = {
    Try(Using(Source.fromURL(url))(_.mkString).toOption).getOrElse(None)
  }

  private def useCache(key: String)(fetch: => Option[String]): Option[String] = {
    primaryCache.get(key).orElse {
      val filePath = s"$cacheDir/$key.json"
      val file = new java.io.File(filePath)

      if (file.exists()) {
        val contents = Try(Using(Source.fromFile(file))(_.mkString).toOption).getOrElse(None)
        contents.foreach(content => primaryCache.put(key, content))
        contents
      } else {
        fetch.map { result =>
          new PrintWriter(filePath) {
            write(result)
            close()
          }
          primaryCache.put(key, result)
          result
        }
      }
    }
  }

  def findActorId(firstname: String, lastname: String): Option[Int] = {
    val query = URLEncoder.encode(s"$firstname $lastname", "UTF-8")
    val url = s"$baseUrl/search/person?api_key=$apiKey&query=$query"
    val key = s"actor-${firstname}_$lastname"
    val response = useCache(key)(fetchFromApi(url))
    response.flatMap { json =>
      val parsedJson = parse(json)
      (parsedJson \ "results").extract[List[JValue]].headOption.flatMap { person =>
        (person \ "id").extractOpt[Int]
      }
    }
  }

  def findActorMovies(actorId: Int): Set[(Int, String)] = {
    val url = s"$baseUrl/person/$actorId/movie_credits?api_key=$apiKey"
    val response = useCache(s"actor$actorId")(fetchFromApi(url))
    response.map { json =>
      val parsedJson = parse(json)
      (parsedJson \ "cast").extract[List[Movie]].map(movie => (movie.id, movie.title)).toSet
    }.getOrElse(Set.empty)
  }

  def findMovieDirector(movieId: Int): Option[(Int, String)] = {
    val url = s"$baseUrl/movie/$movieId/credits?api_key=$apiKey"
    val response = useCache(s"movie$movieId")(fetchFromApi(url))

    response.flatMap { json =>
      val parsedJson = parse(json)
      (parsedJson \ "crew").extract[List[JValue]].find { crew =>
        (crew \ "job").extractOpt[String].contains("Director")
      }.flatMap { director =>
        val id = (director \ "id").extractOpt[Int]
        val name = (director \ "name").extractOpt[String]
        id.flatMap(i => name.map(n => (i, n)))
      }
    }
  }

  def collaboration(actor1: (String, String), actor2: (String, String)): Set[(String, String)] = {

    val movies1 = findActorId(actor1._1, actor1._2).map(findActorMovies).getOrElse(Set.empty)
    val movies2 = findActorId(actor2._1, actor2._2).map(findActorMovies).getOrElse(Set.empty)

    val commonMovies = movies1.intersect(movies2)
    commonMovies.flatMap { case (movieId, movieTitle) =>
      findMovieDirector(movieId).map { case (_, directorName) =>
        (directorName, movieTitle)
      }
    }
  }

  def mostFrequentActorPairs(actors: Seq[(String, String)]): Seq[((String, String), (String, String), Int)] = {
    val actorPairsWithMovies = for {
      actor1 <- actors
      actor2 <- actors
      if actor1 < actor2
      commonMovies = collaboration(actor1, actor2)
      if commonMovies.nonEmpty
    } yield (actor1, actor2, commonMovies.size)

    actorPairsWithMovies
      .groupBy { case (actor1, actor2, _) => (actor1, actor2) }
      .view.mapValues(_.map(_._3).sum)
      .toSeq
      .sortBy(-_._2)
      .map { case ((actor1, actor2), count) => (actor1, actor2, count) }
  }
}
