/*
  PathFileLocator.java

  (c) 2011-2015 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.files;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.files.MD5FilterAlgorithms.FileSegmentFilter;
import v9t9.common.files.MD5FilterAlgorithms.FullContentFilter;
import v9t9.common.memory.MemoryEntryInfo;
import ejs.base.properties.IProperty;
import ejs.base.properties.IPropertyListener;
import ejs.base.utils.FileUtils;
import ejs.base.utils.ListenerList;
import ejs.base.utils.Pair;

/**
 * This class assists in locating files along paths (which are {@link IProperty} instances
 * that contain lists of Strings).  This allows e.g. user configuration files
 * to reference bare filenames -- assuming these are unique no matter where they
 * live -- without performing path lookups themselves. 
 * @author Ed
 *
 */
public class PathFileLocator implements IPathFileLocator {

	private static final Logger logger = Logger.getLogger(PathFileLocator.class);
	
	/**
	 * Files beyond this size are not queried for MD5
	 */
	private static final int MAX_FILE_SIZE = 16 * 1024 * 1024;

	private List<IProperty> roPathProperties = new ArrayList<IProperty>();
	private IProperty rwPathProperty = null;
	private IPropertyListener pathListChangedListener;

	private Set<String> potentiallyWriteableFiles = new HashSet<String>(2);
	
	private int lastCachedHash;
	private Set<URI> cachedSlowURIs = new HashSet<URI>();
	private URI[] cachedURIs = new URI[0];
	private URI cachedWriteURI = null;
	
	private Map<URI, Map<String, FileInfo>> cachedListings = new HashMap<URI, Map<String, FileInfo>>();
	private Map<URI, Long> cachedListingTime = new HashMap<URI, Long>();
	private Map<URI, Long> cachedListingModifiedTime = new HashMap<URI, Long>();
	
//	private Map<URL, Map<String, Collection<String>>> cachedJarListings = new HashMap<URL, Map<String,Collection<String>>>();
	
	private Map<URI, Map<String, String>> cachedUriToMD5Hashes = new HashMap<URI, Map<String,String>>();
	private Map<String, URI> cachedMD5HashesToURIs = new LinkedHashMap<String, URI>();
	
	private int timeoutMs = 30 * 1000;

	private ListenerList<IPathChangeListener> listeners = new ListenerList<IPathFileLocator.IPathChangeListener>();

	private static Set<String> sReportedMissing = new HashSet<String>();
	
	/**
	 * 
	 */
	public PathFileLocator() {
		pathListChangedListener = new IPropertyListener() {
			
			@Override
			public void propertyChanged(IProperty property) {
				synchronized (PathFileLocator.this) {
					if (!listeners.isEmpty()) {
						cachePaths();
						for (IPathChangeListener listener : listeners) {
							listener.pathsChanged();
						}
					}
				}
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#addReadOnlyPathProperty(ejs.base.properties.IProperty)
	 */
	@Override
	public synchronized void addReadOnlyPathProperty(IProperty property) {
		roPathProperties.add(property);
		
		property.addListenerAndFire(pathListChangedListener);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#setReadWritePathProperty(ejs.base.properties.IProperty)
	 */
	@Override
	public synchronized void setReadWritePathProperty(IProperty property) {
		if (rwPathProperty == property)
			return;
		
		if (property.getValue() instanceof Collection)
			throw new IllegalArgumentException("read/write path must be singular");
		
		if (rwPathProperty != null)
			rwPathProperty.removeListener(pathListChangedListener);
			
		rwPathProperty = property;
		property.addListenerAndFire(pathListChangedListener);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#resetPathProperties()
	 */
	@Override
	public synchronized void resetPathProperties() {
		if (rwPathProperty != null)
			rwPathProperty.removeListener(pathListChangedListener);
		
		rwPathProperty = null;
		
		for (IProperty prop : roPathProperties) {
			prop.removeListener(pathListChangedListener);
		}
		roPathProperties.clear();
		
	}
	
	interface IPathIterator {
		void handle(IProperty property, String path);
	}
	
	protected void cachePaths() {
		final Set<URI> uris = new LinkedHashSet<URI>();
		
		lastCachedHash = 0;
		cachedSlowURIs.clear();
		cachedListings.clear();
		cachedListingTime.clear();
		cachedListingModifiedTime.clear();
		cachedUriToMD5Hashes.clear();
		cachedMD5HashesToURIs.clear();
		
		iteratePaths(new IPathIterator() {

			@Override
			public void handle(IProperty property, String path) {
				try {
					if (path == null || path.length() == 0)
						return;
					URI uri = createURI(path);
					if (!uri.isAbsolute())
						return;
					logger.info("Adding URI " + uri);
					uris.add(uri);
					
					try {
						getDirectoryListing(uri);
					} catch (IOException e) {
						logger.error("could not cache directory for " + uri, e);
					}
				} catch (URISyntaxException e) {
					logger.error("URI syntax on " + path, e);
					e.printStackTrace();
				}
			}
			
		});
		
		cachedURIs = uris.toArray(new URI[uris.size()]);
		
		lastCachedHash = calculateCachedHash();
		
		cachedWriteURI = null;
		if (rwPathProperty != null) {
			try {
				String path = rwPathProperty.getValue().toString();
				cachedWriteURI = createURI(path);
				if (!cachedWriteURI.isAbsolute()) {
					cachedWriteURI = null;
				}
			} catch (URISyntaxException e) {
				logger.error("URI syntax on " + rwPathProperty, e);
				e.printStackTrace();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#createURI(java.lang.String)
	 */
	@Override
	public URI createURI(String path) throws URISyntaxException {
		return URIUtils.createURI(path);
	}

	/**
	 * @param iPathIterator
	 */
	private void iteratePaths(IPathIterator iterator) {

		if (rwPathProperty != null) {
			Object val = rwPathProperty.getValue();
			if (val != null)
				iterator.handle(rwPathProperty, val.toString());
		}
		
		for (IProperty prop : roPathProperties) {
			List<Object> propPaths = prop.getList();
			
			for (Object propPath : propPaths) {
				if (propPath != null)
					iterator.handle(prop, propPath.toString());
			}
		}
				
	}
	
	/**
	 * Cache the contents of properties (and/or their lists)
	 * to quickly detect if they have changed.  This augments
	 * the {@link IPropertyListener} already attached to the
	 * properties and serves to detect changes happening inside
	 * list properties.
	 * @return new hash code for current paths
	 */
	protected int calculateCachedHash() {
		final int[] code = { 0 };

		iteratePaths(new IPathIterator() {

			@Override
			public void handle(IProperty property, String path) {
				code[0] ^= path.hashCode();
				code[0] += 1000;
			}
			
		});
		
		return code[0];
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getSearchURIs()
	 */
	@Override
	public synchronized URI[] getSearchURIs() {
		// lists do not maintain inner listeners
		if (calculateCachedHash() != lastCachedHash) {
			cachePaths();
		}
		return cachedURIs;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#setConnectionTimeout(int)
	 */
	@Override
	public void setConnectionTimeout(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getTimeoutMs()
	 */
	@Override
	public int getTimeoutMs() {
		return timeoutMs;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#findFile(java.lang.String)
	 */
	@Override
	public synchronized URI findFile(String file) {
		if (file == null)
			return null;
		
		File localFile = new File(file);
		if (localFile.isAbsolute())
			return localFile.toURI();

		if (file.contains("/")) {
			try {
				URI resolved = URI.create(file);
				if (!"jar".equals(resolved.getScheme()) && 
						(resolved.isOpaque() || resolved.isAbsolute()))
					return resolved;
			} catch (IllegalArgumentException e) {
				
			}
		}

		URI[] searchURIs = getSearchURIs();
		
		URI uri = null;
		
		logger.info("searching for " + file);
		for (URI baseUri : searchURIs) {
			logger.debug("searching " + file + " on " + baseUri);
			if (cachedSlowURIs.contains(baseUri))
				continue;
			
			uri = resolveInsideURI(baseUri, file);
			if (uri == null) {
				logger.error("failed to get listing from " + baseUri + " for " + file + "; got " + uri);
				return null;
			}
			
			Pair<URI, String> dirAndName = splitFileName(uri);
			String baseFile = dirAndName.second; 
			//logger.debug("\t" +uri + " base = " + baseFile);
		
			URI dirURI = dirAndName.first;
			Collection<String> listing;
			try {
				listing = getDirectoryListing(dirURI).keySet();
				if (listing.contains(baseFile)) {
					logger.info("\tfound in " + dirURI + "; listing: " + listing.size() + " entries");
					return uri;
				}
			} catch (IOException e) {
				if (sReportedMissing.add(dirURI.toString())) {
					logger.error("failed to get listing from " + dirURI, e);
				} else {
					logger.debug("failed to get listing from " + dirURI);
				}
				if (false == e instanceof FileNotFoundException)
					e.printStackTrace();
			}
		}
		
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getDirectoryListing(java.net.URI)
	 */
	@Override
	public Map<String, FileInfo> getDirectoryListing(URI uri) throws IOException {
		
		// read from directory, not file
		URI directory = resolveInsideURI(uri, ".");
		if (directory == null)
			throw new IOException("cannot read directory in " + uri);
		
		Map<String, FileInfo> cachedListing;
		synchronized (this) {
			cachedListing = cachedListings.get(directory);
		}
		
		if (cachedListing != null && !directory.equals(rwPathProperty)) {
			return cachedListing;
//			// never update JAR cache
//			if ("jar".equals(directory.getScheme())) 
//				return cachedListing;
//			
//			// don't update cache more than twice a minute
//			Long listingTime = cachedListingTime.get(directory);
//			if (listingTime != null && listingTime + 1 * 30*1000 > System.currentTimeMillis()) {
//				return cachedListing;
//			}
		}
		
		Pair<Long, Map<String, FileInfo>> info = fetchDirectoryListing(directory);
		long time = info.first;
		cachedListing = info.second;
		synchronized (this) {
			cachedListings.put(directory, cachedListing);
			cachedListingModifiedTime.put(directory, time);
			cachedListingTime.put(directory, System.currentTimeMillis());
			
			for (Map.Entry<String, FileInfo> ent : info.second.entrySet()) {
				if (ent.getKey().toLowerCase().endsWith(".bin")) {
					// cache reverse mapping too
					FileInfo finfo = ent.getValue();
					registerURIForMd5Key(finfo.uri,
							0, -1,
							finfo.md5, 
							new MD5FilterAlgorithms.FileSegmentFilter(0, (int) finfo.length));
					registerURIForMd5Key(finfo.uri, 
							0, -1,
							finfo.md5, MD5FilterAlgorithms.FullContentFilter.INSTANCE);
				}
			}
		}		
		
		return cachedListing;
	}

	/**
	 * @param uri
	 * @param mdKey
	 */
	protected void registerURIForMd5Key(URI uri, int offset, int size, String md5, IMD5SumFilter filter) {
		Map<String, String> md5Map = cachedUriToMD5Hashes.get(uri);
		if (md5Map == null) {
			md5Map = new LinkedHashMap<String, String>();
			cachedUriToMD5Hashes.put(uri, md5Map);
		}
		String uriKey = getURIKey(uri, offset, size, filter);
		md5Map.put(uriKey, md5);
		
		String mdKey = getMd5Key(offset, size, md5, filter);
		cachedMD5HashesToURIs.put(mdKey, uri);
	}

	/**
	 * @param uri
	 * @param directory
	 * @param cachedListing
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws SocketTimeoutException
	 */
	protected Pair<Long, Map<String, FileInfo>> fetchDirectoryListing(URI directory) throws IOException,
			MalformedURLException, SocketTimeoutException {
		
		logger.info("\tfetching listing for " + directory);
		Map<String, FileInfo> cachedListing = null;

		// skip JarFile access which just leaks files
		if (directory.getScheme().equals("jar")) {
			String ssp = directory.getSchemeSpecificPart();
			if (ssp == null)
				ssp = directory.getPath();
			int dirIdx = ssp.lastIndexOf('!');
			if (dirIdx < 0)
				throw new IOException("Malformed URI: no '!' in spec: " + ssp);
			
			String zipPath = ssp.substring(0, dirIdx);
			String entPrefix = ssp.substring(dirIdx + 2);	// skip '!' and '/'
			ZipFile zf;
			try {
				logger.info("reading zip file " + directory);
				File file = new File(URI.create(zipPath));
				zf = new ZipFile(file);
				try {
					cachedListing = new LinkedHashMap<String, IPathFileLocator.FileInfo>();
					for (Enumeration<? extends ZipEntry> en = zf.entries(); en.hasMoreElements(); ) {
						ZipEntry entry = en.nextElement();
						
						String entPath = entry.getName();
						// get only entries under the desired directory...
						if (entPath.length() > entPrefix.length() && entPath.startsWith(entPrefix)) {
							String entSuffix = entPath.substring(entPrefix.length());
							int slashIdx = entSuffix.indexOf('/');
							String name;
							if (slashIdx < 0) {
								// and not in a deeper directory
								name = entSuffix;
							} else if (slashIdx == entSuffix.length() - 1) {
								// directory entry
								name = entSuffix.substring(0, slashIdx - 1);
							} else {
								continue;
							}
							
							if (entry.isDirectory() && !name.endsWith("/"))
								name += "/";
								
							URI fileURI;
							fileURI = resolveInsideURI(directory, name);
							String md5 = "";
							
							if (!entry.isDirectory() && entry.getSize() <= MAX_FILE_SIZE) {
								InputStream is = null;
								try {
									is = zf.getInputStream(entry);
									byte[] content = FileUtils.readInputStreamContentsAndClose(is, (int) entry.getSize()); 
									md5 = FileUtils.getMD5Hash(content);
								} catch (EOFException e) {
									md5 = "";
								} finally {
									if (is != null) { try { is.close(); } catch (IOException e) { } }
								}
							}
							
							FileInfo info = new FileInfo(fileURI, 
									entry.getTime(), 
									md5);
							cachedListing.put(name, info);
						}
					}
					
//					synchronized (this) {
//						cachedListings.put(directory, cachedListing);
//						cachedListingModifiedTime.put(directory, file.lastModified());
//						cachedListingTime.put(directory, System.currentTimeMillis());
//					}
					
					logger.info("\tlisting: " + cachedListing.size() + " entries");
					return new Pair<Long, Map<String,FileInfo>>(file.lastModified(), cachedListing);
				} finally {
					zf.close();
				}
				
			} catch (IllegalArgumentException e) {
				// ok, need to try harder below
				logger.error("\tfailed:", e);
				throw new IOException("URI listing failed: " + directory, e);
			}
		}
		
		File dir = null;
		if (directory.getScheme() == null)
			dir = new File(directory.getPath());
		else if (directory.getScheme().equals("file"))
			dir = new File(directory.getSchemeSpecificPart());
		else
			throw new IOException("URI listing not supported: " + directory);
		
		File[] files = dir.listFiles();
		cachedListing = new HashMap<String, IPathFileLocator.FileInfo>();
		if (files != null) {
			for (File file : files) {
				String md5 = "";
				long length = 0;
				if (file.isFile()) {
					length = file.length();
					if (length <= MAX_FILE_SIZE) {
						InputStream is = null;
						try {
							is = new BufferedInputStream(new FileInputStream(file));
							byte[] content = FileUtils.readInputStreamContentsAndClose(is, (int) length); 
							md5 = FileUtils.getMD5Hash(content);
						} catch (IOException e) {
							md5 = "";
						} finally {
							if (is != null) { try { is.close(); } catch (IOException e) { } }
						}
					}
				}
				URI fileURI;
				fileURI = file.toURI();
				cachedListing.put(file.getName(), new FileInfo(
						fileURI,
						length,
						md5));
			}
		}
		
		return new Pair<Long, Map<String,FileInfo>>(dir.lastModified(), cachedListing);
		
	}
	
	/**
	 * @param uri
	 * @param string
	 * @return URI or <code>null</code>
	 */
	public URI resolveInsideURI(URI uri, String string) {
		return URIUtils.resolveInsideURI(uri, string);
		
	}
	
	protected URLConnection connect(URI uri) throws IOException,
			MalformedURLException {
		return connect(uri, true);
	}

	protected URLConnection connect(URI uri, boolean useCache) throws IOException,
		MalformedURLException {
		URLConnection connection = uri.toURL().openConnection();
		configureConnection(connection, useCache);
		try {
			connection.connect();
		} catch (NullPointerException e) {
			// too-many files open bug...
			throw new IOException(e);
		}
		return connection;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#createInputStream(java.net.URI)
	 */
	@Override
	public InputStream createInputStream(URI uri) throws IOException {

		try {
			return new BufferedInputStream(new FileInputStream(new File(uri)));
		} catch (IllegalArgumentException e) 
		{
			InputStream is = null; 
			URLConnection connection = connect(uri, false);
			is = connection.getInputStream();
			
			if (is == null)
				throw new FileNotFoundException("failed to connect to " + uri);
			return is;
		}
	}

	

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getContentLength(java.net.URI)
	 */
	@Override
	public int getContentLength(URI uri) throws IOException {
		try {
			File file = new File(uri);
			return (int) Math.min(Integer.MAX_VALUE, file.length());
		} catch (IllegalArgumentException e) {
			URLConnection connection = connect(uri, false);
			return connection.getContentLength();
		}
	}
	
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getLastModified(java.net.URI)
	 */
	@Override
	public long getLastModified(URI uri) throws IOException {
		try {
			File file = new File(uri);
			return file.lastModified();
		} catch (IllegalArgumentException e) {
			URLConnection connection = connect(uri, false);
			return connection.getLastModified();
		}
	}
	
	
	/**
	 * @param connection
	 * @param useCache 
	 */
	private void configureConnection(URLConnection connection, boolean useCache) {
		connection.setConnectTimeout(timeoutMs);
		connection.setUseCaches(useCache);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#openConnection(java.net.URI)
	 */
	@Override
	public URLConnection openConnection(URI uri) {
		try {
			return uri.toURL().openConnection();
		} catch (MalformedURLException e) {
			logger.error("malformed URL from " + uri, e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("ignored IOException from " + uri, e);
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getWriteURI(java.lang.String)
	 */
	@Override
	public URI getWriteURI(String file) {
		if (cachedWriteURI == null)
			return null;

		// assume the user will create it
		potentiallyWriteableFiles.add(file);
		
		return resolveInsideURI(cachedWriteURI, file);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#createOutputStream(java.net.URI)
	 */
	@Override
	public synchronized OutputStream createOutputStream(URI uri) throws IOException {
		URLConnection conn = uri.toURL().openConnection();
		OutputStream os;
		try {
			os = conn.getOutputStream();
		} catch (UnknownServiceException e) {
			// blah
			if (!uri.getScheme().equals("file")) 
				throw e;
			
			File file = new File(uri);
			os = new FileOutputStream(file);
		}
		
		cachedListings.remove(uri.resolve("."));
		return os;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#exists(java.net.URI)
	 */
	@Override
	public boolean exists(URI uri) {
		InputStream is = null;
		try {
			logger.debug("exists? " + uri);
			try {
				// cheaper path for files
				File file = new File(uri);
				return file.exists();
			} catch (IllegalArgumentException e) {
				is = createInputStream(uri);
				return true;
			}
		} catch (IllegalArgumentException e) {
			logger.error("illegal URI: " + uri, e);
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getSearchPathProperties()
	 */
	@Override
	public IProperty[] getSearchPathProperties() {
		List<IProperty> props = new ArrayList<IProperty>();
		if (rwPathProperty != null)
			props.add(rwPathProperty);
		props.addAll(roPathProperties);
		return (IProperty[]) props.toArray(new IProperty[props.size()]);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getContentMD5(java.net.URI)
	 */
	@Override
	public String getContentMD5(URI uri, IMD5SumFilter filter) throws IOException {
		return getContentMD5(uri, 0, -1, filter);
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getContentMD5(java.net.URI)
	 */
	@Override
	public String getContentMD5(URI uri, int offset, int size, IMD5SumFilter filter) throws IOException {
		URI directory = resolveInsideURI(uri, ".");
		Map<String, String> md5Dir = cachedUriToMD5Hashes.get(directory);
		if (md5Dir == null) {
			md5Dir = new LinkedHashMap<String, String>(); 
			cachedUriToMD5Hashes.put(directory, md5Dir);
		}
		String uriKey = getURIKey(uri, offset, size, filter);
		String md5 = md5Dir.get(uriKey);
		if (md5 == null) {
			md5 = fetchMD5(uri, offset, size, filter);
			md5Dir.put(uriKey, md5);

			// store reverse mapping too
			String mdKey = getMd5Key(offset, size, md5, filter);
			cachedMD5HashesToURIs.put(mdKey, uri);
		}
		return md5;
	}

	protected String getURIKey(URI uri, int offset, int size, IMD5SumFilter filter) {
		String key;
		if (offset == 0 && size < 0)
			key = uri + (filter != null ? ":" + filter.getId() : "");
		else
			key = uri + (filter != null ? ":" + filter.getId() : "") + ":" + offset + ":" + size;
		return key;
	}

	/**
	 * @param uri
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	protected String fetchMD5(URI uri, int offset, int size, IMD5SumFilter filter)
			throws IOException {
		
		String md5;
		if (uri == null) {
			throw new IOException("null URI for md5");
		}

		try {
			if (uri.toString().endsWith("/")) {
				// ignore directories
				md5 = "";
				return md5;
			}

			int fullSize = getContentLength(uri);
			if (fullSize < 0 || (fullSize > MAX_FILE_SIZE && size < 0)) {
				// ignore huge files -- probably bogus
				md5 = "";
			}
			else {
				InputStream is = createInputStream(uri);
				try {
					FileUtils.skipFully(is, offset);
					md5 = MD5SumEngine.createMD5(filter, is, size > 0 ? size : fullSize);
				} catch (EOFException e) {
					md5 = "";
				}
			}
		} catch (NullPointerException e) {
			// too many files open...
			md5 = "";
			logger.error("can't fetch MD5 for " + uri, e);
		} catch (IOException e) {
			// this can happen when invalid filenames are located and the
			// URI was not properly de/en-coded -- TODO
			// ... or when the file actually doesn't exist...
			// ... or for other mysterious reasons ("bad file descriptor")... wtf?
			md5 = "";
			System.err.println("can't fetch MD5 for " + uri);
			logger.error("can't fetch MD5 for " + uri, e);
		}
		return md5;
	}

	protected String getMd5Key(int offset, int size, String md5, IMD5SumFilter filter) {
		String mdKey;
		if (offset == 0 && size < 0)
			mdKey = md5 + ":" + filter.getId();
		else
			mdKey = md5 + ":" + filter.getId() + ":" + offset + ":" + size;
		return mdKey;
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#getContentMD5(java.net.URI)
	 */
	@Override
	public String getContentMD5(URI uri) throws IOException {
		return getContentMD5(uri, MD5FilterAlgorithms.FullContentFilter.INSTANCE);
	}
	
	@Override
	public URI findFileByMD5(String md5, IMD5SumFilter filter) {
		return findFileByMD5(md5, filter, 0, -1);
	}
	
	@Override
	public URI findFileByMD5(String md5, IMD5SumFilter filter, int offset, int size) {
		String md5Key;
		md5Key = getMd5Key(offset, size, md5, filter);
		if (cachedMD5HashesToURIs.containsKey(md5Key)) {
			return cachedMD5HashesToURIs.get(md5Key);	// if null, we already tried
		}
		if (filter instanceof FileSegmentFilter && 
				((FileSegmentFilter) filter).getOffset() == 0) {
			// quickly see if we have a match where the "segment" 
			// covers the entire file
			md5Key = getMd5Key(offset, size, md5, FullContentFilter.INSTANCE);
			if (cachedMD5HashesToURIs.containsKey(md5Key)) {
				URI uri = cachedMD5HashesToURIs.get(md5Key);
				if (uri == null)
					return null;	// already tried
				try {
					int length = size > 0 ? size : getContentLength(uri);
					if (length == ((FileSegmentFilter) filter).getLength()) {
						return uri;
					}
				} catch (IOException e) {
					// nope
					cachedMD5HashesToURIs.put(md5Key, null);
				}
			}
		}

		// cache loss, need to sum according to the filter
		if (filter != FullContentFilter.INSTANCE) {
			for (URI directory : getSearchURIs()) {
				URI uri = findFileByMD5(directory, offset, size, md5, filter);
				if (uri != null)
					return uri;
			}
		}
		
		// nope... 
		return null;
	}
	

	/**
	 * @param directory
	 * @param md5
	 * @param limit
	 */
	protected URI findFileByMD5(URI directory, int offset, int size, String md5, IMD5SumFilter filter) {
		try {
//			System.out.println("searching " + directory + " for " + md5);
			Map<String, FileInfo> direct = getDirectoryListing(directory);
			for (Map.Entry<String, FileInfo> ent : direct.entrySet()) {
				try {
					FileInfo info = ent.getValue();
					URI uri = info.uri;
					String entMd5;
					if (filter == FullContentFilter.INSTANCE
							|| (filter instanceof FileSegmentFilter
									&& ((FileSegmentFilter) filter).getOffset() == 0 
									&& ((FileSegmentFilter) filter).getLength() == info.length)) {
						entMd5 = info.md5;
					} else {
						entMd5 = getContentMD5(uri, offset, size, filter);
					}
					if (entMd5.equalsIgnoreCase(md5)) {
						//System.out.println("\t" + entMd5 + " = " + uri);
						return uri; 
					}
					
					// try inside archive itself
					if (uri.getScheme().equals("file") && ent.getKey().toLowerCase().matches(".*\\.(rpk|zip)")) {
						URI zipURI = URI.create("jar:" + uri + "!/");
						uri = findFileByMD5(
								zipURI,
								offset, size,
								md5, 
								filter);
						if (uri != null) {
							//System.out.println("\t" + entMd5 + " = " + uri);
							return uri;
						}
					}
					
				} catch (Throwable e) {
					logger.error(ent + ": " + e.toString(), e);
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			if (sReportedMissing.add(e.toString())) {
				logger.error("file not found", e);
			}
		} catch (IOException e) {
			logger.error("MD5 search error", e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#splitFileName(java.net.URI)
	 */
	@Override
	public Pair<URI, String> splitFileName(URI uri) {
		return URIUtils.splitFileName(uri);
	}
	
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#findFile(v9t9.common.memory.MemoryEntryInfo, v9t9.common.memory.StoredMemoryEntryInfo)
	 */
	@Override
	public URI findFile(ISettingsHandler settings, MemoryEntryInfo info) {
		// note: if stored, this finds the user's copy first or the original template
		URI uri = null;

		boolean searchByContent = info.getFileMD5() != null;
			
		String theFilename = info.getResolvedFilename(settings);
		
		if (uri == null && theFilename != null) {
			uri = findFile(theFilename);
		}
		
		if (uri == null && searchByContent) {
			uri = findFileByMD5(info.getFileMD5(), 
					MD5FilterAlgorithms.create(info.getEffectiveFileMD5Algorithm()));
			if (uri == null) {
				// maybe the MD5 sum is from a portion of the file
				uri = findFileByMD5(info.getFileMD5(), 
						MD5FilterAlgorithms.create(info.getEffectiveFileMD5Algorithm()),
						info.getOffset(), -1);
			}

			if (uri != null) {
				logger.info("*** Found matching entry by MD5: " + uri);
				theFilename = splitFileName(uri).second;
			}
		}

			
		if (uri == null && theFilename != null) {
			if (info.isStored()) {
				uri = getWriteURI(theFilename);
			}
		}
		return uri;
	}
	

	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#addListener(v9t9.common.files.IPathFileLocator.IPathChangeListener)
	 */
	@Override
	public void addListener(IPathChangeListener listener) {
		listeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see v9t9.common.files.IPathFileLocator#removeListener(v9t9.common.files.IPathFileLocator.IPathChangeListener)
	 */
	@Override
	public void removeListener(IPathChangeListener listener) {
		
		listeners.remove(listener);
	}
}
