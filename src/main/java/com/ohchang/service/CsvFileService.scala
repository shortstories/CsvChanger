package com.ohchang.service

import java.io._

import com.ohchang.commons.BufferedCsvReader
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
  * @author OhChang Kwon(ohchang.kwon@navercorp.com)
  */
@Service
class CsvFileService {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def convertCsvFileWithGivenCol(col: Integer, sourceFile: File) : File = {
    // sourceFile 언제나 1 x n 으로 가정
    val convertedFile = File.createTempFile("converted-", ".tmp");
    val output = new FileOutputStream(convertedFile)
    val input = new BufferedCsvReader(sourceFile.toPath)
    var completeFlag = false
    var count = 0

    try {
      while (completeFlag == false) {
        var str = input.readCol()
        if (str == null) {
          completeFlag = true
        } else {
          count += 1
          if (count % col == 0) {
            str = str.replaceFirst(",", "\n")
          }
          output.write(str.getBytes)
        }
      }
    } finally {
      output.close()
      input.close();
    }

    return convertedFile;
  }
}
