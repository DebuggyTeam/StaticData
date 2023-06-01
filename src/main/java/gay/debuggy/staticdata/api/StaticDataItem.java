package gay.debuggy.staticdata.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.util.Identifier;

/**
 * Represents an identified piece of static data, and methods that can be used to load the data in.
 * 
 * <p>You may make subclasses of this class, but for thread-safety reasons the object, and the data it contains, MUST be
 * immutable, and MUST NOT change across resource / data reloads. If this is challenging in your circumstance, consider
 * using {@link gay.debuggy.staticdata.impl.CachedDataItem CachedDataItem} instead of subclassing as it checks all those
 * boxes itself.
 */
public interface StaticDataItem {
	
	/**
	 * Gets the modId of the mod which supplied the data, or the special value "file" if the data comes from a file
	 * source instead of a mod source.
	 * @return the modId of the mod which supplied the data.
	 */
	public String getModId();
	
	/**
	 * Gets a value which identifies where the file is in its staticdata container.
	 * @return an Identifier with the namespace and path/filename of the resource.
	 */
	public Identifier getResourceId();
	
	/**
	 * Gets this static data item as a raw InputStream
	 * @return an InputStream at the start of this static data
	 * @throws IOException if there was an error opening the stream
	 */
	public InputStream getAsStream() throws IOException;
	
	/**
	 * Gets this static data item as a byte array
	 * @return a byte array containing the static data
	 * @throws IOException if there was an error reading in the data
	 */
	public default byte[] getAsBytes() throws IOException {
		return getAsStream().readAllBytes();
	};
	
	/**
	 * Gets this static data item as UTF-8 character data, as a List of lines
	 * @return the List of lines of static data
	 * @throws IOException if there was an error reading in the data
	 */
	public default List<String> getAsLines() throws IOException {
		return new BufferedReader(new InputStreamReader(getAsStream(), StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.toList());
	}
	
	/**
	 * Gets this static data item as UTF-8 character data
	 * @return the data interpreted as UTF-8 characters
	 * @throws IOException if there was an error reading in the data
	 */
	public default String getAsString() throws IOException {
		return new String(getAsStream().readAllBytes(), StandardCharsets.UTF_8);
	};
}
