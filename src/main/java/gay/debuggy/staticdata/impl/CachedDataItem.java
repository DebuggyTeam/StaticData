package gay.debuggy.staticdata.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import net.minecraft.util.Identifier;

public class CachedDataItem extends AbstractDataItem {
	private final byte[] data;
	
	public CachedDataItem(String modId, Identifier resourceId, byte[] data) {
		super(modId, resourceId);
		this.data = data;
	}

	@Override
	public InputStream getAsStream() throws IOException {
		return new ByteArrayInputStream(data);
	}
	
	@Override
	public byte[] getAsBytes() throws IOException {
		return Arrays.copyOf(data, data.length); //Defensive copy to keep the class immutable
	}
	
	@Override
	public String getAsString() throws IOException {
		return new String(data, StandardCharsets.UTF_8);
	}
	
	@Override
	public String toString() {
		return getModId()+":"+getResourceId().getNamespace()+":"+getResourceId().getPath()+" ("+data.length+" bytes)";
	}
	
}
