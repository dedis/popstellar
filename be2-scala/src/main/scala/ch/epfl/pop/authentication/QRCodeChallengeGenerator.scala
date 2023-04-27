package ch.epfl.pop.authentication

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, ResponseEntity}
import com.google.zxing.aztec.encoder.Encoder.encode
import com.google.zxing.common.BitMatrix

import scala.reflect.io.File

object QRCodeChallengeGenerator {

  val templateFileName = "src/main/web/AuthenticationPageTemplate.html"
  val webTemplateQRCodeTemplate = "{{QRCODE_PATTERN}}"

  def generateChallengeContent(content: String): ResponseEntity = {
    val encodedContent = encode(content)
    val htmlQRCode = fromMatrixToHTML(encodedContent.getMatrix)

    val templateFile = File(templateFileName)
    val lines = for {
      line <- templateFile.lines()
      substitutedLine = line.replace(webTemplateQRCodeTemplate, htmlQRCode)
    } yield substitutedLine
    val challengePage = lines.mkString("\n")

    HttpEntity(ContentTypes.`text/html(UTF-8)`, challengePage)
  }

  def fromMatrixToHTML(matrix: BitMatrix): String = {
    matrix.toString
  }
}
