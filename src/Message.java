public class Message {

    private String version = Client.version;
    private String senderId = "";
    private String fileId = "";
    private String chunkNo = "";
    private String replicationDeg = "";
    private String messageType = "";
    private String body = "";

    private static String CRLFCRLF = "0xD0xA0xD0xA";

    private boolean isValid = false;

    public Message() {
        version = Client.version;
        senderId = String.valueOf(Peer.peer_id);
    }

    public Message(String string) {

        version = Client.version;
        senderId = String.valueOf(Peer.peer_id);

        String[] lines = string.split(CRLFCRLF);
        String[] fields = lines[0].split("\\s+");

        if (!fields[1].equals(Client.version)) {
            System.out.println("Version does not match client.");
            return;
        }

        switch (fields[0]) {
            case "PUTCHUNK":
                messageType = "PUTCHUNK";
                if (fields.length != 6) {
                    System.out.println("Number of fields not expected for a PUTCHUNK message");
                    return;
                }
                if (lines.length != 2) {
                    System.out.println("Body is missing for PUTCHUNK message");
                    return;
                }
                chunkNo = fields[4];
                replicationDeg = fields[5];
                break;
            case "STORED":
                messageType = "STORED";
                if (fields.length != 5) {
                    System.out.println("Number of fields not expected for a STORED message");
                    return;
                }
                chunkNo = fields[4];
                break;
            case "GETCHUNK":
                messageType = "GETCHUNK";
                if (fields.length != 5) {
                    System.out.println("Number of fields not expected for a GETCHUNK message");
                    return;
                }
                chunkNo = fields[4];
                break;
            case "CHUNK":
                messageType = "CHUNK";
                if (fields.length != 5) {
                    System.out.println("Number of fields not expected for a CHUNK message");
                    return;
                }
                if (lines.length != 2) {
                    System.out.println("Body is missing for CHUNK message");
                    return;
                }
                chunkNo = fields[4];
                break;
            case "DELETE":
                messageType = "DELETE";
                if (fields.length != 4) {
                    System.out.println("Number of fields not expected for a DELETE message");
                    return;
                }
                break;
            case "REMOVED":
                messageType = "REMOVED";
                if (fields.length != 5) {
                    System.out.println("Number of fields not expected for a REMOVED message");
                    return;
                }
                chunkNo = fields[4];
                break;
            default:
                System.out.println("Not a valid message type");
                return;
        }

        isValid = true;

        senderId = fields[2];
        fileId = fields[3];
    }

    public boolean isValid() {
        return isValid;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(String chunkNo) {
        this.chunkNo = chunkNo;
    }

    public String getReplicationDeg() {
        return replicationDeg;
    }

    public void setReplicationDeg(int replicationDeg) {
        this.replicationDeg = String.valueOf(replicationDeg);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        switch (messageType) {
            case "PUTCHUNK":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + replicationDeg + " " + CRLFCRLF + body;
            case "STORED":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLFCRLF;
            case "GETCHUNK":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLFCRLF;
            case "CHUNK":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLFCRLF + body;
            case "DELETE":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + CRLFCRLF;
            case "REMOVED":
                return messageType + " " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + CRLFCRLF;
        }
        return "";
    }
}
