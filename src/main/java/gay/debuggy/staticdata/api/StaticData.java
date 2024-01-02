package gay.debuggy.staticdata.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gay.debuggy.staticdata.impl.StaticDataImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

/**
 * This class accesses a non-mojang type of data called "static data". This kind of data differs from resource and data
 * for several reasons:
 * 
 * <ul>
 *   <li> It's available much earlier in the loading process - by the time a ModInitializer is called, all static data
 *        is available.
 *   <li> It's insensitive to mod load order.
 *   <li> It's not allowed to change between respack and datapack reloads.
 * </ul>
 * 
 * These properties make static data ideal for "passive", loader-agnostic mod integrations, especially ones which result
 * in new block registrations.
 * 
 * <p>Note: If block IDs are specified in static data, they may not be available yet, as the mod that registers them may
 * not have run its ModInitializer yet. It is the responsibility of the mod requesting block IDs from static data to
 * detect unregistered block IDs and defer their resolution until the data is available. This generally takes the form
 * of a block registry listener to determine when the block ID in question becomes available.
 * 
 * <p>Note: Jarmods cannot add static data. Nor can the JVM. If static data is found in a sensitive location such as
 * these, it will be ignored.
 */
public class StaticData {
	private static final Set<String> FORBIDDEN_CONTAINERS = Set.of( "java", "minecraft" );
	
	/**
	 * I'm not aware of any *specific* thread safety issues with any of the code here, but by synchronizing all the
	 * public methods (except getStaticDataDir), and holding no mutable state, we can be sure of its safety when
	 * accessing it from both client and server, or from additional unknown worker threads.
	 */
	private static final Object GLOBAL_MUTEX = new Object();
	
	/**
	 * Gets a Path to the folder in the game directory which will be searched for additional static data files provided
	 * by the modpack.
	 * @return The staticdata folder path
	 */
	public static Path getStaticDataDir() {
		return FabricLoader.getInstance().getGameDir().resolve("staticdata");
	}
	
	/**
	 * Acquires static data exactly matching the given Identifier. The identifier must be a file name, such as
	 * "examplemod:configs/config.json". If a directory is specified, an empty list will be returned.
	 * @param resourceId The Identifier for the file you wish to retrieve
	 * @return A list of StaticDataItems that represent the file requested.
	 */
	public static List<StaticDataItem> getExactData(Identifier resourceId) {
		synchronized(GLOBAL_MUTEX) {
			List<StaticDataItem> result = new ArrayList<>();
			
			for(ModContainer container : FabricLoader.getInstance().getAllMods()) {
				if (FORBIDDEN_CONTAINERS.contains(container.getMetadata().getId())) continue;
				
				for(Path p : container.getRootPaths()) {
					StaticDataImpl.addExactData(
						container.getMetadata().getId(),
						resourceId,
						p.resolve("staticdata"),
						result
					);
				}
			}
			
			Path saticDataDir = getStaticDataDir();
			boolean searchFiles = true;
			if (!Files.exists(saticDataDir)) {
				searchFiles = false;
				try {
					Files.createDirectory(saticDataDir);
				} catch (IOException e) {
					// Creating the directory is a nice-to-have but we don't really care if it fails.
				}
			}
			
			if (searchFiles) {
				StaticDataImpl.addExactData(
						"file",
						resourceId,
						getStaticDataDir(),
						result
						);
			}
			
			return List.copyOf(result);
		}
	}
	
	/**
	 * Acquires static data contained within the directory specified by the Identifier. The Identifier must point to a
	 * folder, such as "examplemod:configs" or {@code new Identifier("examplemod", "")} - even if a filename is
	 * specified, only items within a folder of that name will be returned.
	 * @param resourceId The Identifier of the folder containing data you wish to retrieve
	 * @param recursive true if subfolders within this folder should also be searched
	 * @return A list of StaticDataItems that match the criteria specified
	 */
	public static List<StaticDataItem> getDataInDirectory(Identifier resourceId, boolean recursive) {
		synchronized(GLOBAL_MUTEX) {
			List<StaticDataItem> result = new ArrayList<>();
			
			for(ModContainer container : FabricLoader.getInstance().getAllMods()) {
				if (FORBIDDEN_CONTAINERS.contains(container.getMetadata().getId())) continue;
				
				for(Path p : container.getRootPaths()) {
					StaticDataImpl.addDirectoryData(
							container.getMetadata().getId(),
							resourceId,
							p.resolve("staticdata"),
							recursive,
							result
							);
				}
			}
			
			Path saticDataDir = getStaticDataDir();
			boolean searchFiles = true;
			if (!Files.exists(saticDataDir)) {
				searchFiles = false;
				try {
					Files.createDirectory(saticDataDir);
				} catch (IOException e) {
					// Creating the directory is a nice-to-have but we don't really care if it fails.
				}
			}
			
			if (searchFiles) {
				StaticDataImpl.addDirectoryData(
						"file",
						resourceId,
						getStaticDataDir(),
						recursive,
						result
						);
			}
			
			return List.copyOf(result);
		}
	}
}
