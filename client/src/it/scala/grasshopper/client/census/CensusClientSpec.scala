package grasshopper.client.census

import grasshopper.model.census.ParsedInputAddress
import org.scalatest.{FlatSpec, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class CensusClientSpec extends FlatSpec with MustMatchers {

  val timeout = 5.seconds

  "A 'status' request" must "return an 'OK' response" in {
    val maybeStatus = Await.result(CensusClient.status, timeout)
    maybeStatus match {
      case Right(s) =>
        s.status mustBe "OK"
      case Left(b) =>
        b.desc mustBe "503 Service Unavailable"
        fail("SERVICE_UNAVAILABLE")
    }
  }


  "A 'geocode' request" must "geocode an address string" in {
    val parsedAddress = ParsedInputAddress("3146", "M St NW", "20007", "DC")
    val maybeAddress = Await.result(CensusClient.geocode(parsedAddress), timeout)
    maybeAddress match {
      case Right(result) =>
        result.status mustBe "OK"
        val features = result.features
        features.size mustBe 1
        val f = features(0)
        val address = f.values.getOrElse("FULLNAME", "")
        address mustBe "M St NW"
      case Left(b) =>
        b.desc mustBe "503 Service Unavailable"
        fail("SERVICE_UNAVAILABLE")
    }
  }

  it should "respond with address with synonym in State definition" in {
    val parsedAddress = ParsedInputAddress("456", "Central Ave", "11516", "New York")
    val maybeAddress = Await.result(CensusClient.geocode(parsedAddress), timeout)
    maybeAddress match {
      case Right(result) =>
        result.status mustBe "OK"
        val features = result.features
        features.size mustBe 1
        val f = features(0)
        val address = f.values.getOrElse("FULLNAME", "")
        address mustBe "Central Ave"
      case Left(b) =>
        b.desc mustBe "503 Service Unavailable"
        fail("SERVICE_UNAVAILABLE")
    }
  }

}
