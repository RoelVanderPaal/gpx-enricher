import java.io.{File, PrintWriter}
import java.time.{Duration, Instant}

import com.github.tototoshi.csv.CSVWriter
import spray.json._

import scala.Console.GREEN
import scala.util.Properties
import scala.xml.{Node, XML}

case class Location(lon: Double, lat: Double)

case class Point(location: Location, elevation: Double, timestamp: Instant, heartRate: Option[Int], source: String)

case class EnrichedPoint(
                          location: Location,
                          elevation: Double,
                          timestamp: Instant,
                          heartRate: Option[Int],
                          source: String,
                          distance: Double,
                          timeDelta: Long,
                          time: Long
                        ) {
  def this(p: Point, distance: Double = 0, timeDelta: Long = 0, time: Long = 0) = this(p.location,
    p.elevation, p.timestamp, p.heartRate, p.source, distance, timeDelta, time)
}


object Main extends App {
  val folder = args(0)
  Console.println(s"${GREEN}Reading folder $folder")
  val results = new File(folder)
    .listFiles().flatMap(handleFile)
  toJson("log/gpx.json", results)
  toCsv("data.csv", results)

  private def toCsv(fileName: String, points: Seq[EnrichedPoint]) = {
    val writer = CSVWriter.open(new File(fileName))
    writer.writeRow("source,lon,lat,elevation,timestamp,heartRate,distance,timeDelta,time".split(","))
    points
      .map(p => List(p.source, p.location.lon, p.location.lat, p.elevation, p.timestamp, p.heartRate.map(_.toString).getOrElse(""), p.distance, p.timeDelta, p.time))
      .foreach(writer.writeRow)
    writer.close()
  }

  private def toJson(fileName: String, points: Seq[EnrichedPoint]) = {
    object MyJsonProtocol extends DefaultJsonProtocol {

      implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
        override def read(json: JsValue): Instant = json match {
          case JsString(s) => Instant.parse(s)
          case _ => deserializationError("String expected")
        }

        override def write(obj: Instant): JsValue = JsString(obj.toString)
      }

      implicit val locationFormat = jsonFormat2(Location)
      implicit val enrichedPointFormat = jsonFormat8(EnrichedPoint)

    }

    import MyJsonProtocol._
    val writer = new PrintWriter(new File("log/gpx.json"))
    points
      .map(_.toJson.compactPrint)
      .map(_ + Properties.lineSeparator)
      .foreach(writer.write)
    writer.close()
  }

  private def handleFile(file: File) = {
    def xmlToPoint(source: String)(p: Node) = Point(
      Location(
        (p \ "@lon").text.toDouble,
        (p \ "@lat").text.toDouble
      ),
      (p \ "ele").text.toDouble,
      Instant.parse((p \ "time").text),
      Some((p \ "extensions" \ "TrackPointExtension" \ "hr").text.toInt),
      source
    )

    def enrich(e: EnrichedPoint, p2: Point) = {
      val l1 = e.location
      val l2 = p2.location
      val distance = Haversine.haversine(l1.lat, l1.lon, l2.lat, l2.lon)
      val timeDelta = Duration.between(e.timestamp, p2.timestamp).getSeconds
      new EnrichedPoint(p2, distance, timeDelta, e.time + timeDelta)
    }

    Console.println(s"Handling $file")
    val gpx = XML.loadFile(file)
    val points = (gpx \ "trk" \ "trkseg" \ "trkpt").map(xmlToPoint(file.toString))
    val first = new EnrichedPoint(points.head)
    points.tail.scanLeft(first)(enrich)
  }


}
