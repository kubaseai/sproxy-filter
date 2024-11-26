package net.squid.access.filter.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HttpConnectHeader {

    private String method;
    private String host;
    private int port;
    private boolean tls;
    private boolean complete;
    private ByteBuf byteBuf = Unpooled.buffer();

    private final StringBuilder lineBuf = new StringBuilder();

    public void digest(ByteBuf in) {
        while (in.isReadable()) {
            if (complete) {
                throw new IllegalStateException("Buffer reading already complete");
            }
            String line = readLine(in);
            if (line == null) {
                return;
            }
            if (method == null) {
                method = line.split(" ")[0]; // the first word is http method name
                tls = method.equalsIgnoreCase("CONNECT"); // method CONNECT means https
            }
            if (line.startsWith("Host: ")) {
                String[] arr = line.split(":");
                host = arr[1].trim();
                if (arr.length == 3) {
                    port = Integer.parseInt(arr[2]);
                } else if (tls) {
                    port = 443; // https
                } else {
                    port = 80; // http
                }
            }
            if (line.isEmpty()) {
                if (host == null || port == 0) {
                    throw new IllegalStateException("Cannot find header \'Host\'");
                }
                byteBuf = byteBuf.asReadOnly();
                complete = true;
                break;
            }
        }
    }

    private String readLine(ByteBuf in) {
        while (in.isReadable()) {
            byte b = in.readByte();
            byteBuf.writeByte(b);
            lineBuf.append((char) b);
            int len = lineBuf.length();
            if (len >= 2 && lineBuf.substring(len - 2).equals("\r\n")) {
                String line = lineBuf.substring(0, len - 2);
                lineBuf.delete(0, len);
                return line;
            }
        }
        return null;
    }

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isTls() {
		return tls;
	}

	public void setTls(boolean tls) {
		this.tls = tls;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public ByteBuf getByteBuf() {
		return byteBuf;
	}

	public void setByteBuf(ByteBuf byteBuf) {
		this.byteBuf = byteBuf;
	}

	public StringBuilder getLineBuf() {
		return lineBuf;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HttpProxyClientHeader [method=").append(method).append(", host=").append(host).append(", port=")
			.append(port).append(", https=").append(tls).append(", complete=").append(complete).append("]");
		return builder.toString();
	}	
}