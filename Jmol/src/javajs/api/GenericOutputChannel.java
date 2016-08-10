package javajs.api;

public interface GenericOutputChannel {

  boolean isBigEndian();

  void writeByteAsInt(int b);

  void write(byte[] b, int off, int n);

  void reset();

  String closeChannel();


}
