package org.thordev.webapi;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author nuo.qin
 */
public final class Publisher extends TimerTask {
	private static class SrcInfo {
		public long lastModified;
		public String content;
		public String key;
		public SrcInfo(String key, String content, long lastModified) {
			this.key = key;
			this.lastModified = lastModified;
			this.content = content;
		}
	}
	
	private Timer timer = null;
	private static final Logger logger = Logger.getLogger(Publisher.class.getName());
	//private ExecutorService executorService = null;
	private File monitorPath = null;
	private File publishPath = null;
	private String contextPath = null;
	
	// key: path
	private final Map<String, SrcInfo> cache = new HashMap<>();
	// Means: the first(key) file was referenced by second(value) file.
	// So if key file changed then must update all valued
	private final Map<String, Set<String>> referencedBy = new HashMap<>();
	// If key file changed then must update all referencedBy mapping which pointed to the changed item
	private final Map<String, Set<String>> referencedTo = new HashMap<>();
	// Store alreay published key
	private final Set<String> published = new HashSet<>();
	
	public Publisher(String monitorPath, String publishPath, String contextPath) {
		this.monitorPath = new File(monitorPath).getAbsoluteFile();
		this.publishPath = new File(publishPath).getAbsoluteFile();
		if (contextPath == null)
			this.contextPath = "";
		else
			this.contextPath = contextPath;
		loadPublished();
	}
	
	private void loadPublished() {
		try {
			File f = new File(monitorPath + "/.published");
			if (f.exists()) {
				List<String> lines = Files.readAllLines(f.toPath(), Charset.forName("utf-8"));
				published.clear();
				for (String key: lines) {
					published.add(key);
				}
			}
		} catch (IOException ex) {
		}
	}
	
	private void savePublished() {
		try {
			File f = new File(monitorPath + "/.published");
			Files.write(f.toPath(), published, Charset.forName("utf-8"));
		} catch (IOException ex) {
		}
	}

	/**
	 * Start a publish timer
	 * @param checkPeriod In milliseconds
	 */
	public void start(long checkPeriod) {
		try {
			if (timer != null)
				return;
			timer = new Timer();
			createAllDir(monitorPath);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		timer.schedule(this, 0, checkPeriod);
	}
	
	public void stop() {
		if (timer != null)
			timer.cancel();
	}
	
	private String keyPart(File file) {
		String absPath = file.getAbsolutePath();
		String key = absPath.replace(monitorPath.getAbsolutePath(), "");
		if (key.equals(absPath))
			return key.replace("\\", "/");
		else {
			key = key.replace("\\", "/");
			if (key.startsWith("/")) {
				return key.substring(1);
			} else
				return key;
		}
	}
	
	private String dirPart(File file) {
		String p;
		if (file.isDirectory())
			p = file.getAbsolutePath();
		else
			p = file.getParent();
		if (p == null)
			return "";
		else
			return p.replace("\\", "/");
	}
	
	private String absPath(File file) {
		return file.getAbsolutePath().replace("\\", "/");
	}
	
	private void addReference(String parentKey, String childKey) {
		Set<String> updateSet; 
		if (referencedTo.containsKey(parentKey)) {
			updateSet = referencedTo.get(parentKey);
		} else {
			updateSet = new HashSet<>();
			referencedTo.put(parentKey, updateSet);
		}
		updateSet.add(childKey);
	
		if (referencedBy.containsKey(childKey)) {
			updateSet = referencedBy.get(childKey);
		} else {
			updateSet = new HashSet<>();
			referencedBy.put(childKey, updateSet);
		}
		updateSet.add(parentKey);
	}
	
	private void deleteReference(String parentKey, String childKey) {
		Set<String> updateSet = referencedTo.get(parentKey); 
		if (updateSet != null) {
			updateSet.remove(childKey);
		} 
		updateSet = referencedBy.get(childKey);
		if (updateSet != null) {
			updateSet.remove(parentKey);
		} 
	}

	private void deleteCache(String key) {
		cache.remove(key);
		Set<String> parentSet = referencedBy.get(key); 
		if (parentSet != null) {
			String[] array = parentSet.toArray(new String[0]);
			for (String parentKey : array) {
				deleteCache(parentKey);
			}
		}
		Set<String> childSet = referencedTo.get(key); 
		if (childSet != null) {
			String[] arr = childSet.toArray(new String[0]);
			for (String childKey : arr) {
				deleteReference(key, childKey);
			}
		} 
		referencedTo.remove(key);
	}
	
	private void updateReference(String parentKey, Set<String> childKeySet) {
		Set<String> childSet = referencedTo.get(parentKey); 
		if (childSet != null) {
			String[] arr = childSet.toArray(new String[0]);
			for (String childKey : arr) {
				deleteReference(parentKey, childKey);
			}
		}
		for (String childKey : childKeySet) {
			addReference(parentKey, childKey);
		}
	}
	
	private boolean circularReferences(Stack<String> stack, String key) {
		for (String pushedKey : stack) {
			if (pushedKey.equals(key))
				return true;
		}
		return false;
	}
	
	private SrcInfo build(File file, Stack<String> stack) {
		String parentKey = keyPart(file);
		try {
			stack.push(parentKey);
			SrcInfo info = cache.get(parentKey);
			if (info != null && info.lastModified > file.lastModified()) {
				return info;
			}
			if (!file.exists())
				return null;
			String content = new String(Files.readAllBytes(file.toPath()),"utf-8");
			Pattern pattern = Pattern.compile(
					"<%\\s*(.+?)\\s*%>",
					Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(content);
			StringBuilder buffer = new StringBuilder();
			int scanPos = 0;
			Set<String> childKeySet = new HashSet<>();
			while (matcher.find()) {
				buffer.append(content.substring(scanPos, matcher.start()));
				scanPos = matcher.end();
				String cmd = matcher.group(1);
				if (cmd.toLowerCase().equals("context")) {
					buffer.append(this.contextPath);
				} else if (cmd.startsWith("@include")) {
					Pattern incPattern = Pattern.compile("@include\\s+file=\"(.+)\"", Pattern.MULTILINE);
					Matcher incMatcher = incPattern.matcher(cmd);
					if (incMatcher.find()) {
						String includePath = incMatcher.group(1);
						File includeFile;
						if (includePath.charAt(0) == '/' || includePath.charAt(0) == '\\') {
							includeFile = new File(dirPart(monitorPath) + includePath);
						} else {
							includeFile = new File(dirPart(file) + "/" + includePath);
						}
						includeFile = includeFile.getAbsoluteFile().getCanonicalFile();
						String childKey = keyPart(includeFile);
						if (!circularReferences(stack, childKey))
							build(includeFile, stack);
						else {
							StringBuilder msg = new StringBuilder();
							msg.append("ERROR: Circular references detected: \n");
							for (String path : stack) {
								msg.append("    '").append(path).append("'\n");
							}
							msg.append(" -> '").append(childKey).append("'\n");
							logger.log(Level.SEVERE, msg.toString());
						}
						if (cache.containsKey(childKey)) {
							buffer.append(cache.get(childKey).content);
						}
						childKeySet.add(childKey);
					}
				}
			}
			buffer.append(content.substring(scanPos, content.length()));
			info = new SrcInfo(parentKey, buffer.toString(), new Date().getTime());
			deleteCache(parentKey);
			cache.put(parentKey, info);
			updateReference(parentKey, childKeySet);
			return info;
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			return null;
		} finally {
			stack.pop();
		}
	}
	
	private void createAllDir(File destination) throws IOException {
		Files.createDirectories(destination.getParentFile().toPath());
	}
	
	private void publishDir(File path, Set<String> newSet) {
    	if (path == null)
    		return;
    	if (path.isFile()) {
    		if (path.getName().toLowerCase().endsWith(".shtml")) {
				SrcInfo srcInfo = build(path, new Stack<String>());
				if (srcInfo != null) {
					newSet.add(srcInfo.key);
					File destination = new File(absPath(publishPath) + "/" + srcInfo.key.substring(0, srcInfo.key.length()-5) + "html");
					if (!destination.exists() || destination.lastModified() < srcInfo.lastModified) {
						try {
							createAllDir(destination);
							Files.write(destination.toPath(), srcInfo.content.getBytes("utf-8"));
						} catch (UnsupportedEncodingException ex) {
							logger.log(Level.SEVERE, null, ex);
						} catch (IOException ex) {
							logger.log(Level.SEVERE, null, ex);
						}
					}
				}
    		} else if (path.getName().toLowerCase().endsWith(".ssi")) {
				build(path, new Stack<String>());
			}
    	} else if (path.isDirectory()) {
    		for ( File item : path.listFiles()) {
    			publishDir(item, newSet);
    		}
    	}
    }
	
	private void checkAdd(Set<String> deleteKeySet, String key) {
		Set<String> childSet = referencedTo.get(key);
		if (childSet == null)
			return;
		for (String childKey: childSet) {
			File child = new File(monitorPath.getAbsolutePath() + "/" + childKey);
			if (!cache.containsKey(childKey) && child.exists()) {
				deleteKeySet.add(childKey);
			}
			checkAdd(deleteKeySet, childKey);
		}
	}
	
	private void checkAdd(Set<String> deleteKeySet) {
		for (String key: referencedTo.keySet()) {
			checkAdd(deleteKeySet, key);
		}
	}
	
	private void checkModify() {
		Set<String> deleteKeySet = new HashSet<>();
		for (String key : cache.keySet()) {
			SrcInfo info = cache.get(key);
			File cachedFile = new File(absPath(monitorPath) + "/" + key);
			
			if (!cachedFile.exists() || cachedFile.lastModified() > info.lastModified)
				deleteKeySet.add(key);
		}
		
		checkAdd(deleteKeySet);
		
		for (String key : deleteKeySet) {
			deleteCache(key);
			File publishedFile = new File(absPath(publishPath) + "/" + key);
			if (publishedFile.exists()) {
				try {
					publishedFile.delete();
				} catch (Exception ex) {
					logger.log(Level.WARNING, 
							"Can not delete published file: " + key, ex);
				}
			}
		}
	}
	
	public void deleteEmptyFolder(File path) {
		File[] children = path.listFiles();
		for (File child : children) {
			if (child.isDirectory()) {
				deleteEmptyFolder(child);
				if (child.list().length == 0) {
					try {
						child.delete();
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	private void updatePublished(Set<String> newSet) {
		for (String key : published) {
			if (!newSet.contains(key)) {
				File destination = new File(absPath(publishPath) + "/" + key);
				if (destination.exists()) {
					try {
						destination.delete();
					} catch (Exception e) {
					}
				}
			}
		}
		published.clear();
		for (String key : newSet) {
			published.add(key);
		}
	}
	/*
	private void printInfo() {
		System.out.println("\nReference to: --------------------------");
		for (String key: referencedTo.keySet()) {
			System.out.println("parent: " + key);
			Set<String> children = referencedTo.get(key);
			for (String child: children) {
				System.out.println("  child: " + child);
			}
		}
		
		System.out.println("\nReference by: --------------------------");
		for (String key: referencedBy.keySet()) {
			System.out.println("child: " + key);
			Set<String> parents = referencedBy.get(key);
			for (String parent: parents) {
				System.out.println("  parent: " + parent);
			}
		}
		
		System.out.println("\nCached: --------------------------");
		for (String key: cache.keySet()) {
			System.out.println("cached: " + key);
		}
	}
	*/
	
	/**
	 * Do a fully publishing operation
	 */
	public void publish() {
		try {
			createAllDir(monitorPath);
			createAllDir(publishPath);
			checkModify();
			Set<String> newSet = new HashSet<>();
			publishDir(monitorPath, newSet);
			updatePublished(newSet);
			deleteEmptyFolder(publishPath);
			// printInfo();
			savePublished();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public void run() {
		publish();
	}
	

//	public static void main(String arg[]) throws Exception {
//		Publisher publisher = new Publisher(
//				"C:\\Users\\nuo.qin\\Workspace\\Current\\Supplier\\src\\main\\resources\\ssi",
//				"C:\\Users\\nuo.qin\\Workspace\\Current\\Supplier\\src\\main\\webapp",
//				"/Supplier");
//		publisher.publish();
//	}

}
