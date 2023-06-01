package gay.debuggy.staticdata.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import net.minecraft.util.Identifier;

public class PathDataItem extends AbstractDataItem {
	private final Path path;
	
	public PathDataItem(String modId, Identifier resourceId, Path path) {
		super(modId, resourceId);
		this.path = path;
	}
	
	@Override
	public InputStream getAsStream() throws IOException {
		return Files.newInputStream(path, StandardOpenOption.READ);
	}
	
	@Override
	public byte[] getAsBytes() throws IOException {
		return Files.readAllBytes(path);
	}
	
	@Override
	public List<String> getAsLines() throws IOException {
		return Files.readAllLines(path, StandardCharsets.UTF_8);
	}
	
	@Override
	public String getAsString() throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8);
	}
	
	@Override
	public String toString() {
		return getModId()+":"+getResourceId().getNamespace()+":"+getResourceId().getPath()+" > "+path.toString();
	}
}
