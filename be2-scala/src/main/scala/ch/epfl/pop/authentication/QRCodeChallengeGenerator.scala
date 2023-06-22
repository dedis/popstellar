package ch.epfl.pop.authentication

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, ResponseEntity}
import ch.epfl.pop.config.RuntimeEnvironment
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.{ByteMatrix, Encoder}

import scala.io.Source

/** Generates an http response that holds a web page with any desired challenge content in the form of a displayed qrcode The html/svg representation of the qrcode is inspired from https://github.com/svg/svgo
  */
object QRCodeChallengeGenerator {

  private val templateFileName = "src/main/web/AuthenticationPageTemplate.html"

  private val webTemplateQRCodePlaceholder = "{{.SVGImage}}"
  private val webTemplateWebSocketAddressPlaceholder = "{{.WebSocketAddr}}"

  private val qrcodeTotalSize = 650
  private val qrcodeMargin = 25

  /** Generates an html response representing a web page with a qrcode holding the content provided
    * @param content
    *   data to insert in the qrcode
    * @return
    *   a web page in the form of an http-html response
    */
  def generateChallengeContent(content: String, laoId: String, clientId: String, nonce: String): ResponseEntity = {
    val encodedContent = Encoder.encode(content, ErrorCorrectionLevel.H)
    val htmlQRCode = fromMatrixToHTML(encodedContent.getMatrix)
    val webSocketAddress = RuntimeEnvironment.ownAuthWSAddress + s"/$laoId/$clientId/$nonce"

    val templateFile = Source.fromFile(templateFileName)
    val lines = for {
      line <- templateFile.getLines()
      substitutedLine = line
        .replace(webTemplateQRCodePlaceholder, htmlQRCode)
        .replace(webTemplateWebSocketAddressPlaceholder, webSocketAddress)
    } yield substitutedLine

    val challengePage = lines.mkString("\n")
    templateFile.close()

    HttpEntity(ContentTypes.`text/html(UTF-8)`, challengePage)
  }

  private def fromMatrixToHTML(matrix: ByteMatrix): String = {
    if (matrix.getWidth != matrix.getHeight)
      return "invalid qrcode matrix"

    val svgHtmlOpenTag =
      s"<svg width=\"$qrcodeTotalSize\" height=\"$qrcodeTotalSize\"\n" +
        s"xmlns=\"http://www.w3.org/2000/svg\"\n" +
        s"xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
    val svgHtmlCloseTag = "</svg>"

    val matrixSize = matrix.getWidth
    val squareSize = (qrcodeTotalSize - 2 * qrcodeMargin) / matrixSize
    val centeredMarginStart = (qrcodeTotalSize - squareSize * matrixSize) / 2

    val allSquares = for {
      y <- 0 until matrixSize
      x <- 0 until matrixSize
      square = generateSvgSquare(
        x,
        y,
        squareSize,
        centeredMarginStart,
        matrix.get(x, y)
      )
    } yield square

    allSquares.mkString(svgHtmlOpenTag, "\n", svgHtmlCloseTag)
  }

  private def generateSvgSquare(x: Int, y: Int, size: Int, margin: Int, value: Byte): String = {
    val filled = (value & 0x1) != 0 // 1st bits indicates whether it is filled or not (but other bits may be filled)
    val color = if (filled) "black" else "white"
    val xPos = x * size + margin
    val yPos = y * size + margin
    s"<rect x=\"$xPos\" y=\"$yPos\" width=\"$size\" height=\"$size\" style=\"fill:$color;stroke:none\" />"
  }
}
