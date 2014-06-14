
package com.badlogicgames.partyroom.assets;

import java.io.File;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilderSpec;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/** Allows to statically serve assets from a specific directory.
 * @author mzechner */
public class StaticAssetsBundle implements ConfiguredBundle<StaticAssetsBundleConfiguration> {
	protected static final CacheBuilderSpec DEFAULT_CACHE_SPEC = CacheBuilderSpec.parse("maximumSize=100");
	private final CacheBuilderSpec cacheBuilderSpec;
	private final String uriPath;
	private final StaticAssetProcessor processor;

	public StaticAssetsBundle (String uriPath) {
		this(uriPath, StaticAssetProcessor.DEFAULT_PROCESSOR);
	}

	public StaticAssetsBundle (String uriPath, StaticAssetProcessor processor) {
		Preconditions.checkArgument(processor != null, "processor must not be null");
		this.cacheBuilderSpec = DEFAULT_CACHE_SPEC;
		this.uriPath = uriPath.endsWith("/") ? uriPath : (uriPath + '/');
		this.processor = processor;
	}

	@Override
	public void run (StaticAssetsBundleConfiguration config, Environment environment) throws Exception {
		CacheBuilderSpec spec = (config.getAssetsConfiguration().getCacheSpec() != null) ? CacheBuilderSpec.parse(config
			.getAssetsConfiguration().getCacheSpec()) : cacheBuilderSpec;
		environment.addServlet(new StaticAssetServlet(new File(config.getAssetsConfiguration().getResourcePath()), config.getAssetsConfiguration().getJarResourcePath(), spec, uriPath,
			"index.html", processor, config.getAssetsConfiguration().isDisableCache()), uriPath + "*");
	}

	@Override
	public void initialize (Bootstrap<?> bootstrap) {
	}
}
