package com.guidewire.certificationtracker;


import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;


public class CSVUtil { // SUPPRESS CHECKSTYLE CSV is a common abbreviation
  protected CSVUtil() {
    throw new UnsupportedOperationException();
  }

  public static List<String[]> readCsvAsList(String path, boolean skipHead) throws Exception {
    return readCsvAsListFromStream(new BufferedReader(new FileReader(new File(path))), skipHead);
  }

  public static List<String[]> readCsvAsListFromLine(String line) throws Exception {
    return readCsvAsListFromStream(new BufferedReader(new StringReader(line)), false);
  }

  public static List<String[]> readCsvAsListFromStream(Reader ireader, boolean skipHead) throws Exception {
    // We use ',' as the separator and '"' as the quote character.
    // We use '/0' as the escape character instead of the default '\' because we want to keep the chars like "\n".
    final CSVReader reader = new CSVReader(ireader,
      CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER);
    if (skipHead) {
      reader.readNext();
    }
    return reader.readAll();
  }
}