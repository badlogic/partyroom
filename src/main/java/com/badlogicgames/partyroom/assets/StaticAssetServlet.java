
package com.badlogicgames.partyroom.assets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

/** Servlet responsible for serving assets to the caller. This is basically completely stolen from
 * {@link com.yammer.dropwizard.assets.AssetServlet} with the exception of allowing for override options.
 * 
 * @see com.yammer.dropwizard.assets.AssetServlet */
class StaticAssetServlet extends HttpServlet {
	private static final long serialVersionUID = 6393345594784987908L;
	private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;

	private final transient File rootPath;
	private final transient String jarPath;
	private final transient MimeTypes mimeTypes = new MimeTypes();
	private final transient LoadingCache<String, Asset> cache;
	private final transient StaticAssetProcessor processor;
	private final transient boolean disableCache;

	private Charset defaultCharset = Charsets.UTF_8;

	/** Creates a new {@code AssetServlet} that serves static assets loaded from {@code resourceURL} (typically a file: or jar:
	 * URL). The assets are served at URIs rooted at {@code uriPath}. For example, given a {@code resourceURL} of
	 * {@code "file:/data/assets"} and a {@code uriPath} of {@code "/js"}, an {@code AssetServlet} would serve the contents of
	 * {@code /data/assets/example.js} in response to a request for {@code /js/example.js}. If a directory is requested and
	 * {@code indexFile} is defined, then {@code AssetServlet} will attempt to serve a file with that name in that directory. If a
	 * directory is requested and {@code indexFile} is null, it will serve a 404.
	 * 
	 * @param resourcePath the base URL from which assets are loaded
	 * @param spec specification for the underlying cache
	 * @param uriPath the URI path fragment in which all requests are rooted
	 * @param indexFile the filename to use when directories are requested, or null to serve no indexes
	 * @param overrides the path overrides
	 * @see CacheBuilderSpec */
	public StaticAssetServlet (File rootPath, String jarPath, CacheBuilderSpec spec, String uriPath, String indexFile,
		StaticAssetProcessor processor, boolean disableCache) {
		this.rootPath = rootPath;
		if(!jarPath.endsWith("/")) jarPath += "/";
		this.jarPath = jarPath;
		AssetLoader loader = new AssetLoader(uriPath, indexFile);
		this.cache = CacheBuilder.from(spec).weigher(new AssetSizeWeigher()).build(loader);
		this.processor = processor;
		this.disableCache = disableCache;
	}

	public void setDefaultCharset (Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	public Charset getDefaultCharset () {
		return this.defaultCharset;
	}

	@Override
	protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			final StringBuilder builder = new StringBuilder(req.getServletPath());
			if (req.getPathInfo() != null) {
				builder.append(req.getPathInfo());
			}
			Asset asset = cache.getUnchecked(builder.toString());
			if (asset == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// Check the etag...
			if (asset.getETag().equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))) {
				resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}

			// Check the last modified time...
			if (asset.getLastModifiedTime() <= req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)) {
				resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}

			resp.setDateHeader(HttpHeaders.LAST_MODIFIED, asset.getLastModifiedTime());
			resp.setHeader(HttpHeaders.ETAG, asset.getETag());

			MediaType mediaType = DEFAULT_MEDIA_TYPE;

			Buffer mimeType = mimeTypes.getMimeByExtension(req.getRequestURI());

			if (mimeType != null) {
				try {
					mediaType = MediaType.parse(mimeType.toString());
					if (defaultCharset != null && mediaType.is(MediaType.ANY_TEXT_TYPE)) {
						mediaType = mediaType.withCharset(defaultCharset);
					}
				} catch (IllegalArgumentException ignore) {
				}
			}

			resp.setContentType(mediaType.type() + "/" + mediaType.subtype());

			if (mediaType.charset().isPresent()) {
				resp.setCharacterEncoding(mediaType.charset().get().toString());
			}

			final ServletOutputStream output = resp.getOutputStream();
			try {
				output.write(asset.getResource());
			} finally {
				output.close();
			}
		} catch (RuntimeException ignored) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private class AssetLoader extends CacheLoader<String, Asset> {
		private final String uriPath;
		private final String indexFilename;

		private AssetLoader (String uriPath, String indexFilename) {
			final String trimmedUri = CharMatcher.is('/').trimTrailingFrom(uriPath);
			this.uriPath = trimmedUri.length() == 0 ? "/" : trimmedUri;
			this.indexFilename = indexFilename;
		}

		@Override
		public Asset load (String key) throws Exception {
			Preconditions.checkArgument(key.startsWith(uriPath));
			key = key.substring(uriPath.length());
			File file = new File(rootPath, key);
			if (!file.exists()) {
				return new ResourceAsset(key);
			}

			if (file.isDirectory()) {
				file = new File(file, indexFilename);
			}

			if (file.exists()) {
				return new FileSystemAsset(file);
			}

			return null;
		}
	}

	private static interface Asset {
		byte[] getResource ();

		String getETag ();

		long getLastModifiedTime ();
	}

	private class ResourceAsset implements Asset {
		byte[] bytes = null;
		String eTag;
		
		public ResourceAsset(String filename) throws Exception {
			InputStream in = null;
			
			try {
				in = StaticAssetServlet.class.getResourceAsStream(jarPath + filename);
				bytes = IOUtils.toByteArray(in);
				byte[] newBytes = processor.process(jarPath, filename, bytes);
				String newETag = Hashing.murmur3_128().hashBytes(newBytes).toString();

				bytes = newBytes;
				eTag = '"' + newETag + '"';
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
		
		@Override
		public byte[] getResource() {
			return bytes;
		}

		@Override
		public String getETag() {
			return eTag;
		}

		@Override
		public long getLastModifiedTime() {
			return System.nanoTime();
		}		
	}
	
	/** Weigh an asset according to the number of bytes it contains. */
	private static final class AssetSizeWeigher implements Weigher<String, Asset> {
		@Override
		public int weigh (String key, Asset asset) {
			return asset.getResource().length;
		}
	}

	/** An asset implementation backed by the file-system. If the backing file changes on disk, then this asset will automatically
	 * reload its contents from disk. */
	private class FileSystemAsset implements Asset {
		private final File file;
		private byte[] bytes;
		private String eTag;
		private long lastModifiedTime;

		public FileSystemAsset (File file) {
			this.file = file;
			refresh();
		}

		@Override
		public byte[] getResource () {
			maybeRefresh();
			return bytes;
		}

		@Override
		public String getETag () {
			maybeRefresh();
			return eTag;
		}

		@Override
		public long getLastModifiedTime () {
			maybeRefresh();
			return (lastModifiedTime / 1000) * 1000;
		}

		private synchronized void maybeRefresh () {
			if (disableCache || (lastModifiedTime != file.lastModified())) {
				refresh();
			}
		}

		private synchronized void refresh () {
			try {
				byte[] newBytes = Files.toByteArray(file);
				newBytes = processor.process(rootPath, file, newBytes);
				String newETag = Hashing.murmur3_128().hashBytes(newBytes).toString();

				bytes = newBytes;
				eTag = '"' + newETag + '"';
				lastModifiedTime = file.lastModified();
			} catch (Exception e) {
				e.printStackTrace();
				// Ignored, don't update anything
			}
		}
	}
}
