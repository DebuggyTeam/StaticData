package gay.debuggy.staticdata.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.include.com.google.common.base.Preconditions;

import gay.debuggy.staticdata.StaticDataMod;
import gay.debuggy.staticdata.api.StaticDataItem;
import net.minecraft.util.Identifier;

public class StaticDataImpl {
	public static final boolean TRIM_BACKSLASHES = false;
	
	private static String fileName(Path p) {
		if (p == null) return "";
		if (p.getNameCount() == 0) return "";
		Path fileNamePath = p.getFileName();
		if (fileNamePath == null) return "";
		return fileNamePath.toString();
	}
	
	private static Identifier toIdentifier(@NotNull String namespace, @NotNull Path relativePath) {
		Preconditions.checkNotNull(namespace);
		Preconditions.checkNotNull(relativePath);
		
		StringBuilder equivalentPath = new StringBuilder();
		for(int i=0; i<relativePath.getNameCount(); i++) {
			if (i!=0) equivalentPath.append('/');
			equivalentPath.append(relativePath.getName(i).toString());
		}
		
		/*
		 * Even if they're real backslashes that belong as part of the namespace or path elements, we can't have them
		 * here. For the namespace, we're removing them as they're most likely to "leak" into the start or end. For the
		 * path, we'll flip them into forward slashes. In basically every case, these two lines should do *nothing* to
		 * the string.
		 */
		namespace = namespace.replace("\\", "");
		String path = equivalentPath.toString().replace('\\', '/');
		
		return Identifier.of(namespace, path);
	}
	
	public static void addExactData(String modId, Identifier resId, Path basePath, List<StaticDataItem> results) {
		//Any data found as a Path will have Identifier paths relative to this path
		Path relativePath = basePath.resolve(resId.getNamespace());
		//This is the Path actually evaluated / enumerated for data
		Path requestedPath = relativePath.resolve(resId.getPath());
		
		// List data inside packs first
		try {
			Files.list(basePath).forEachOrdered(subPath -> {
				if (fileName(subPath).endsWith(".zip")) {
					try {
						addExactZipData(modId, resId, subPath, results);
					} catch (IOException e) {
						// Unlikely to error out while iterating a directory, and should just quietly not add this data.
					}
				}
			});
			
		} catch (IOException e) {
			// Typically getting here means no staticdata folder exists, don't throw an error and don't return data.
		}
		
		// List standalone files
		if (Files.exists(requestedPath) && Files.isRegularFile(requestedPath)) {
			results.add(new PathDataItem(modId, resId, requestedPath));
		}
	}
	
	public static void addDirectoryData(String modId, Identifier resId, Path basePath, boolean recursive, List<StaticDataItem> results) {
		//Any data found as a Path will have Identifier paths relative to this path
		Path relativePath = basePath.resolve(resId.getNamespace());
		//This is the Path actually evaluated / enumerated for data
		Path requestedPath = relativePath.resolve(resId.getPath());
		
		// List data inside packs first
		if (!Files.exists(basePath)) return;
		try {
			Files.list(basePath).forEach(subPath -> {
				if (!Files.isRegularFile(subPath)) return;
				
				if (subPath.getFileName().toString().endsWith(".zip")) {
					try {
						addDirectoryZipData(modId, resId, subPath, recursive, results);
					} catch (IOException e) {
						StaticDataMod.LOGGER.error("Couldn't add zipped staticdata.", e);
					}
				}
			});
		} catch (IOException e) {
			StaticDataMod.LOGGER.error("There was an error enumerating data in a staticdata pack.", e);
		}
		
		// List standalone files
		if (!Files.exists(requestedPath)) return;
		
		if (Files.isDirectory(requestedPath)) {
			List<Path> fileList = listFiles(requestedPath, recursive);
			
			for(Path path : fileList) {
				
				if (!Files.isRegularFile(path)) return; //Don't list folders here
				
				Identifier resourceId = toIdentifier(modId, relativePath.relativize(path));
				
				PathDataItem item = new PathDataItem(modId, resourceId, path);
				results.add(item);
			}
		}
	}
	
	public static void addExactZipData(String modId, Identifier resId, Path zipPath, List<StaticDataItem> results) throws IOException {
		ZipInputStream in = new ZipInputStream(Files.newInputStream(zipPath, StandardOpenOption.READ));
		ZipEntry entry = in.getNextEntry();
		while (entry!=null) {
			if (!entry.isDirectory() && entry.getSize() <= Integer.MAX_VALUE) {
			
				String entryName = entry.getName();
				if (entryName.startsWith("/") || entryName.startsWith("\\")) { // Chop off any leading slashes. typically doesn't happen.
					entryName = entryName.substring(1);
				}
				
				if (entryName.startsWith("staticdata/") || entryName.startsWith("staticdata\\")) {
					entryName = entryName.substring("staticdata/".length());
					
					if (matchesExactFile(entryName, resId)) {
						byte[] data = in.readNBytes((int) entry.getSize());
						in.closeEntry();
						
						final String foundResourcePath = entryName.substring((resId.getNamespace()+"/").length());
						Identifier foundResourceId = Identifier.of(resId.getNamespace(), foundResourcePath);
						results.add(new CachedDataItem(modId, foundResourceId, data));
					}
				}
				
			}
			
			entry = in.getNextEntry();
		}
		
		in.close();
	}
	
	public static void addDirectoryZipData(String modId, Identifier resId, Path zipPath, boolean recursive, List<StaticDataItem> results) throws IOException {
		ZipInputStream in = new ZipInputStream(Files.newInputStream(zipPath, StandardOpenOption.READ));
		ZipEntry entry = in.getNextEntry();
		while (entry!=null) {
			if (!entry.isDirectory() && entry.getSize() <= Integer.MAX_VALUE) {
				String entryName = entry.getName();
				if (entryName.startsWith("/") || entryName.startsWith("\\")) { // Chop off any leading slashes. typically doesn't happen.
					entryName = entryName.substring(1);
				}
				
				if (entryName.startsWith("staticdata/") || entryName.startsWith("staticdata\\")) {
					entryName = entryName.substring("staticdata/".length());
					
					if (matchesDirectoryContents(entryName, resId, recursive)) {
						byte[] data = in.readNBytes((int) entry.getSize());
						in.closeEntry();
						
						final String foundResourcePath = entryName.substring((resId.getNamespace()+"/").length());
						Identifier foundResourceId = Identifier.of(resId.getNamespace(), foundResourcePath);
						results.add(new CachedDataItem(modId, foundResourceId, data));
					}
				}
				
			}
			
			entry = in.getNextEntry();
		}
		
		in.close();
	}
	
	/**
	 * Performs a depth-first search of all directories visible from base, including base, and returns a list of Paths
	 * to all files encountered along the way.
	 * @param base The folder to find all files in
	 * @param recursive if false, only the base directory will be searched. If true, all subdirectories will be searched.
	 * @return A list of all files in the directory
	 */
	public static List<Path> listFiles(Path base, boolean recursive) {
		ArrayList<Path> result = new ArrayList<>();
		
		if (Files.isDirectory(base)) {
			ArrayDeque<Path> stack = new ArrayDeque<>();
			stack.push(base);
			
			while(!stack.isEmpty()) {
				Path curDir = stack.pop();
				try (DirectoryStream<Path> ls = Files.newDirectoryStream(curDir)) {
					for(Path entry : ls) {
						if (Files.isDirectory(entry)) {
							if (recursive) stack.push(entry);
						} else if (Files.isRegularFile(entry)) {
							result.add(entry);
						}
					}
				} catch (IOException e) {
					//We can't list the directory - either it doesn't exist or we don't have permission.
				}
			}
		} else {
			result.add(base);
		}
		return result;
	}
	
	public static boolean matchesExactFile(String partialPath, Identifier resourceId) {
		if (partialPath.startsWith("/")) partialPath = partialPath.substring(1);
		String domainPart = resourceId.getNamespace()+"/";
		String basePath = resourceId.getPath();
		if (basePath.startsWith("/")) basePath = basePath.substring(1);
		
		String prefix = domainPart+basePath;
		return (partialPath.equals(prefix));
	}
	
	/**
	 * Where partialPath is a path relative to the staticdata root, and resourceId is an Identifier pointing to a base
	 * directory, returns whether partialPath satisfies a folder search.
	 * @param partialPath
	 * @param resourceId
	 * @param recursive
	 * @return
	 */
	public static boolean matchesDirectoryContents(String partialPath, Identifier resourceId, boolean recursive) {
		if (partialPath.startsWith("/")) partialPath = partialPath.substring(1);
		String domainPart = resourceId.getNamespace()+"/";
		String basePath = resourceId.getPath();
		
		//Chop off both leading and trailing slashes
		if (basePath.startsWith("/")) basePath = basePath.substring(1);
		if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length()-1);
		
		String prefix = domainPart+basePath;
		if (resourceId.getPath().equals("")) prefix = prefix.substring(0, prefix.length()-1); //chop the trailing slash off on root searches
		if (partialPath.equals(prefix)) return false; //Exact matches are wrong in this case
		if (!partialPath.startsWith(prefix)) return false; //Non-matches get rejected

		String relativePath = partialPath.substring(prefix.length());
		if (relativePath.startsWith("/")) {
			//This path is actually *within* the prefix
			
			if (recursive) return true; //if we don't care about the nesting level, we can just stop here.
			
			relativePath = relativePath.substring(1); //Chop off that starting slash so we can get to work
			return !relativePath.contains("/"); // It's inside this directory and not inside a subdirectory.
			
		} else {
			/* e.g.:
			 *   resourceId  = foo:bar
			 *   partialPath = foo/barrista/baz.json
			 *   prefix      = foo/bar
			 * 
			 * which results in relativePath being "rista/baz.json".
			 * because this doesn't start with a slash, it's not actually inside the prefix, and we want to reject
			 * the file.
			 */
			
			return false;
		}
		
	}
}
