package SelectiveRepeat;

public class ReliablePacketHandler implements PacketHandler {

  public void handlePacket(ARQ ref, Packet p) {
    if (!p.isAck()) { // Let ACKS through no matter what
      if (ref.arqState == ARQ.TRANS_STATE.SYNCH && (!p.isSyn() && !p.isSynAck())) {
        System.out.println("\t\tIgnoring non-syn message");
        return;
      } else if (ref.arqState == ARQ.TRANS_STATE.DATA && !p.isData() && !p.isSyn() && !p.isSynAck()) {
        System.out.println("\t\tIgnoring non-data message");
        return;
      }
      if (ref.arqState == ARQ.TRANS_STATE.TEARDOWN && !p.isData() && !p.isFIN()) {
        System.out.println("\t\tIgnoring non-data/non-FIN message");
        return;
      }
    } else if (p.isAck() && ref.sendingSequenceBase == Integer.MIN_VALUE) {
      return;
    }

    if (p.isAck() || p.isSynAck()) { // The message is an acknowledgement
      int seq = p.getSeqNum();
      if (seq < ref.sendingSequenceBase) { // Already received this ACK! Drop the duplicate ACK
        if (p.isSynAck()) {
          // Must re-acknolwedge a syn-ack as it is also a message needing
          // acknolwedgement.
          System.out.println("Re-ack syn-ack");
          p.setSequenceNumber(Integer.parseInt(p.payload.trim()));
          ref.ackPacket(p);
        }
        System.out.println("Already received ACK for seq#: " + seq);
      } else { // Register the acknowledgement
        int idx = seq - ref.sendingSequenceBase;
        System.out.println("Index of sequence number: " + idx);
        ref.display();
        ref.windowList.get(idx).acknowledge();

        // If the index is 0, dequeue all sequential acknowledged packets
        if (idx == 0) {
          while (ref.windowList.size() > 0 && ref.windowList.get(0).isAcknowledged()) {
            ref.windowList.remove(0); // Remove from head
            ref.sendingSequenceBase++;
            synchronized (ref.ackNotifier) {
              ref.ackNotifier.notify();
            }
          }
        }
        ref.display();

        // A SYN-ACK requires an extra acknolwedgement
        if (p.isSynAck()) {
          Integer rsn = Integer.parseInt(p.payload.trim());
          ref.receivingSequenceNumber = rsn; // ackPacket will do the increment for us.
          p.setPayload("");
          p.setSequenceNumber(rsn);
          System.out.printf("[SYN-ACK.2] Receiving Sequence numbers set to [%d]\n", ref.receivingSequenceNumber);
          System.out.println("Acking syn-ack");
          ref.ackPacket(p);
          ref.synAckReceived = true;
          synchronized (ref.synAckMonitor) {
            ref.synAckMonitor.notify();
          }
        }
      }
    } else { // The message is not an ACK, so it needs to be sent an ACK!
      //
      //A SYN must be responded to via a SYN-ACK, which is both an ACK, and
      //a regular outbound message, and so much be added to both windowList,
      //and ackWindowList (done virtually, but not actually).
      //
      //Since a SYN represents the start of a handshake, store the incoming sequence
      //number as the receivingSequenceNumber, and the starting number as well
      //
      if (p.isSyn()) { // This message requires a SYN ACK, runs as repeat-send thread

        if (p.getSeqNum() + 1 == ref.receivingSequenceNumber) {
          System.out.println("Re-received SYN, doing nothing!");
          return;
        }
        // Set the next expected message from sending-host
        ref.receivingSequenceNumber = p.getSeqNum();
        System.out.printf("[SYN.1] Receiving Sequence number set to [%d]\n", ref.receivingSequenceNumber);

        synchronized (ref.synMonitorObject) {
          ref.synPacketReceived = p;
          ref.synReceived = true;
          ref.synMonitorObject.notify();
        }
      } else if (p.isFIN()) { // Send an ACK & a FIN
        System.out.println("************* RECEIVED FIN *****************");
        ref.ackPacket(p);
        synchronized (ref.finMonitor) { // Notify that a FIN was received!
          ref.finReceived = true;
          ref.finMonitor.notify();
        }
      } else { // All other messages require an ACK!
        ref.ackPacket(p);
      }
    }
  
  }

}