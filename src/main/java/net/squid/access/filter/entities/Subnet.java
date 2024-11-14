package net.squid.access.filter.entities;

public class Subnet {	
	
	private long addrMin = 0;
	private long addrMax = 0;
	private long addr = -1;
	private int mask = 0;
	private boolean valid = false;
	private String errorMessage = null;
	
	private long addrFromBytes(byte[] arr) {
		return (arr[0] << 24 | arr[1] << 16 | arr[2] | arr[3]);
	}
		
	public Subnet(String address) {
		try {
			String[] parts = address.split("\\/");
			mask = Integer.valueOf(parts[1]);
			byte[] octets = new byte[4];
			int i=0;
			for (String octet : parts[0].split("\\.")) {
				octets[i]=(byte)(Integer.valueOf(octet) & 0xff);
				i++;
			}
			if (octets[3]==0) {
				octets[0]=1;
			}
			addrMin = addrFromBytes(octets);
			addrMax = addrMin;
			for (i=0; i < 32-mask; i++) {
				addrMax |= 1 << i;
			}
			valid = true;
		}
		catch (Exception e) {
			errorMessage = e.getMessage();
		}
	}
	
	public String next() {
		if (addr==-1) {
			addr = addrMin-1;
		}
		addr++;
		int lastOctet = (int)(addr & 0x000000ff);
		if (lastOctet==0 || lastOctet==255)
			addr++;
		if (addr >= addrMax)
			return null;
		int[] octets = new int[4];
		octets[0] = (int)(addr & 0xff000000) >> 24;
		octets[1] = (int)(addr & 0x00ff0000) >> 16;
		octets[2] = (int)(addr & 0x0000ff00) >> 8;
		octets[3] = (int)(addr & 0x000000ff);
		return octets[0]+"."+octets[1]+"."+octets[2]+"."+octets[3];
	}
	
	public int getMask() {
		return mask;
	}
	
	public void reset() {
		addr=-1;
	}

	public boolean isValid() {
		return valid;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
