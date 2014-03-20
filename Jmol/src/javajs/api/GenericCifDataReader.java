package javajs.api;

import java.io.BufferedReader;
import java.util.Map;

import javajs.util.CifDataReader;
import javajs.util.GenericLineReader;

public interface GenericCifDataReader {

  static final int NONE = -1;

  String fullTrim(String str);

  Map<String, Object> getAllCifData();

  boolean getData() throws Exception;

  String getFileHeader();

  String getNextDataToken() throws Exception;

  String getNextToken() throws Exception;

  String getTokenPeeked();

  String peekToken() throws Exception;

  String readLine();

  CifDataReader set(GenericLineReader reader, BufferedReader br);

  String toUnicode(String data);

  String getLoopData(int i);

  int parseLoopParameters(String[] fields, int[] fieldOf, int[] propertyOf) throws Exception;

  int getFieldCount();

  String getField(int i);

}
