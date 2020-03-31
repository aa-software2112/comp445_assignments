package SelectiveRepeat;

import java.net.InetAddress;
import java.lang.StringBuilder;

public class Packet {

  public enum PACKET_TYPE {
    DATA, ACK, SYN, SYN_ACK, FIN;
  }

  public static final Integer HEADER_SIZE = 11;
  public static final Integer MAX_PAYLOAD_SIZE = 1013;
  public static final Integer MAX_PACKET_SIZE = 1024;
  protected PACKET_TYPE packetType;
  protected Integer seqNumber;
  protected String peerAddress;
  protected Integer port;
  protected String payload;
  protected byte[] bytePacket;
  
  
  protected Packet(PACKET_TYPE t, Integer seq, String peerAddr, Integer port, String payload) {
    this.packetType = t;
    this.seqNumber = seq;
    this.peerAddress = peerAddr;
    this.port = port;
    this.payload = payload;
    this.generateBytePacket();
  }
  
  public Packet(byte[] packetData) {
    this.bytePacket = packetData;
    System.arraycopy(packetData, 0, this.bytePacket, 0, packetData.length); // Make deep copy
    this.unloadBytePacket();
  }

  private void unloadBytePacket() {
    this.packetType = this.unmarshalPacketType((int) this.bytePacket[0]);
    this.seqNumber = this.unmarshalEndian(this.bytePacket, 1, 4); // Unmarshal the sequence number
    this.peerAddress = String.format("%d.%d.%d.%d", this.bytePacket[5], this.bytePacket[6], this.bytePacket[7],
        this.bytePacket[8]);
    this.port = this.unmarshalEndian(this.bytePacket, 9, 2);
    this.payload = new String(this.bytePacket, 11, Packet.MAX_PAYLOAD_SIZE);
  }

  private PACKET_TYPE unmarshalPacketType(int pt) {
    PACKET_TYPE ret = PACKET_TYPE.DATA;

    switch (pt) {
      case 0:
        ret = PACKET_TYPE.DATA;
        break;
      case 1:
        ret = PACKET_TYPE.ACK;
        break;
      case 2:
        ret = PACKET_TYPE.SYN;
        break;
      case 3:
        ret = PACKET_TYPE.SYN_ACK;
        break;
      case 4:
        ret = PACKET_TYPE.FIN;
        break;
    }
    return ret;
  }

  private Integer unmarshalEndian(byte[] buff, int from, int length) {
    Integer value = 0;
    for (int i = 0; i < length; i++) {
      int nextValue = buff[from + i];
      nextValue = (nextValue < 0) ? Byte.MAX_VALUE + (Math.abs(Byte.MIN_VALUE) + nextValue) + 1 : nextValue;
      if (i == (length - 1)) {
        value = (value | nextValue);
      } else {
        value = (value | nextValue) << 8;
      }
    }
    return value;
  }

  private void generateBytePacket() {
    bytePacket = new byte[Packet.MAX_PACKET_SIZE];
    byte[] seq = this.toBigEndian(this.seqNumber, 4); // Takes up 4 bytes (at most)
    byte[] peerAddrBytes = byteAddress(this.peerAddress); // IP address in byte array
    byte[] peerPortBytes = this.toBigEndian(this.port, 2); // Port into byte array

    bytePacket[0] = (byte) this.packetType.ordinal(); // Converts enum to its value in listing
    this.copyBytes(seq, bytePacket, 1); // Adds 4-bytes SEQ # @ offset 1
    this.copyBytes(peerAddrBytes, bytePacket, 5); // Adds 4-bytes of peer address @ offset 5
    this.copyBytes(peerPortBytes, bytePacket, 9); // Adds 2-bytes of peer port @ offset 9
    this.copyStr(this.payload, bytePacket, 11); // Adds payload @ offset 11
  }

  private byte[] toBigEndian(Integer value, Integer length) {
    if (length < 0) {
      return null;
    }

    byte[] bigEndian = new byte[length];
    while (length > 0) {
      bigEndian[length - 1] = (byte) (value & 0xFF);
      length--;
      value = value >> 8;
    }
    return bigEndian;
  }

  private void copyBytes(byte[] from, byte[] to, int offset) {
    if (!verify(from.length, offset)) {
      System.out.println("Message too big in copyBytes");
      return;
    }
    int bytesWritten = 0;
    while (bytesWritten != from.length) {
      to[offset + bytesWritten] = from[bytesWritten];
      bytesWritten++;
    }
  }

  private void copyStr(String from, byte[] to, int offset) {
    if (!verify(from.length(), offset)) {
      System.out.println("Message too big in copyStr");
      return;
    }
    this.copyBytes(from.getBytes(), to, offset);
  }

  private boolean verify(int msgSize, int offset) {
    return (msgSize + offset) <= Packet.MAX_PACKET_SIZE;
  }

  private static byte[] byteAddress(String address) {
    try {
      return InetAddress.getByName(address).getAddress();
    } catch (Exception e) {
      return new byte[1];
    }
  }
  
  private String printBuff(byte[] buff, int offset, int length) {
    StringBuilder b = new StringBuilder();
    int cnt = 0;
    while (length != cnt) {
      if (cnt % 16 == 0 && cnt > 0) {
        b.append("\n");
      }
      b.append(String.format("%03d ", buff[(offset + cnt++)]));
    }
    return b.toString();
  }
  
  public byte[] getBytes() {
    return this.bytePacket;
  }

  public String getPayload() {
    return this.payload;
  }

  public String toString() {
    System.out.println("***** STRING  VERSION *****");
    System.out.println("PTYPE: " + this.packetType.name());
    System.out.println("SEQ: " + this.seqNumber);
    System.out.println("PEER_ADDR: " + this.peerAddress);
    System.out.println("PEER_PORT: " + this.port);
    System.out.println("PAYLOAD: " + this.payload.trim());

    System.out.println("***** BYTE VERSION *****");
    System.out.println("PTYPE: " + printBuff(this.bytePacket, 0, 1));
    System.out.println("SEQ: " + printBuff(this.bytePacket, 1, 4));
    System.out.println("PEER_ADDR: " + printBuff(this.bytePacket, 5, 4));
    System.out.println("PEER_PORT: " + printBuff(this.bytePacket, 9, 2));
    System.out.println("PAYLOAD:\n" + printBuff(this.bytePacket, 11, this.payload.length()));
    
    return "";
  }

  public Packet generateAck() {
    return this.generateAck();
  }
  
  public boolean isAck() {
    return this.packetType == PACKET_TYPE.ACK;
  }

  public boolean isSyn() {
    return this.packetType == PACKET_TYPE.SYN;
  }

  public boolean isSynAck() {
    return this.packetType == PACKET_TYPE.SYN_ACK;
  }

  public boolean isFIN() {
    return this.packetType == PACKET_TYPE.FIN;
  }

  public boolean isData() {
    return this.packetType == PACKET_TYPE.DATA;
  }
  public PACKET_TYPE getType() {
    return this.packetType;
  }

  public int getSeqNum() {
    return this.seqNumber;
  }

  public int getPort() {
  return this.port;
  }

  public void setPayload(String payload) {
    if (payload.length() > Packet.MAX_PAYLOAD_SIZE) {
      throw new RuntimeException(String.format("Payload %s too large! [Len = %d]", payload, payload.length()));
    }
    this.copyStr(payload, this.bytePacket, 11); // Adds payload @ offset 11
    this.payload = payload;
  }

  public void setSequenceNumber(Integer seqNum) {
    byte[] seq = this.toBigEndian(seqNum, 4); // Takes up 4 bytes (at most)
    this.copyBytes(seq, bytePacket, 1); // Adds 4-bytes SEQ # @ offset 1
    this.seqNumber = seqNum;
  }

  public String getAddress() {
    return this.peerAddress;
  }

}