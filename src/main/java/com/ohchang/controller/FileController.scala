package com.ohchang.controller

import java.io.File
import javax.servlet.http.HttpServletResponse

import com.fasterxml.jackson.databind.ObjectMapper
import com.ohchang.service.CsvFileService
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.http.{HttpHeaders, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.multipart.MultipartFile

import scala.beans.BeanProperty
import scala.util.parsing.json.JSONObject

/**
  * @author OhChang Kwon(ohchang.kwon@navercorp.com)
  */
@Controller
@RequestMapping(path = Array("/file"))
class FileController {
  @Autowired
  @BeanProperty
  var csvFileService: CsvFileService = null

  val objectMapper: ObjectMapper = new ObjectMapper()

  @RequestMapping(path = Array("/upload"), method = Array(RequestMethod.POST))
  @ResponseBody
  def upload(@RequestParam col: Integer,
            @RequestParam sourceFile: MultipartFile): ResponseEntity[String] = {

    if (sourceFile != null && sourceFile.isEmpty()) {
      return ResponseEntity
        .badRequest()
        .body(null)
    }

    val tempFile = File.createTempFile("temp-csv-", ".tmp");
    sourceFile.transferTo(tempFile);

    val resource = csvFileService.convertCsvFileWithGivenCol(col, tempFile)
    tempFile.delete();

    return ResponseEntity
      .ok()
      .body(new JSONObject(Map("url" -> resource.getAbsolutePath)).toString)
  }

  @RequestMapping(path = Array("/download"), method = Array(RequestMethod.GET))
  @ResponseBody
  def download(@RequestParam filepath: String, response: HttpServletResponse): Unit = {
    val resource = new FileSystemResource(new File(filepath))

    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+resource.getFilename+"\"")
    response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString)

    val out = response.getOutputStream
    val in = resource.getInputStream

    try {
      IOUtils.copy(in, out)
    } finally {
      out.close()
      in.close()
    }

    resource.getFile().delete()
  }
}