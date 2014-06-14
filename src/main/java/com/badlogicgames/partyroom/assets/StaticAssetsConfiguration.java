
package com.badlogicgames.partyroom.assets;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;

/**
 * Specifies the resource path from which to serve assets as well as the cache behaviour, see
 * {@link CacheBuilderSpec} and {@link CacheBuilder}.
 * @author mzechner
 *
 */
public class StaticAssetsConfiguration {
	@NotNull @JsonProperty private String cacheSpec = StaticAssetsBundle.DEFAULT_CACHE_SPEC.toParsableString();

	@NotNull @JsonProperty private String resourcePath = "src/main/resources/assets/";
	
	@NotNull @JsonProperty private String jarResourcePath = "/assets/";
	
	@NotNull @JsonProperty private boolean disableCache = false;
	
	/** The caching specification for how to memoize assets. */
	public String getCacheSpec () {
		return cacheSpec;
	}

	/** path to the statically served resources **/
	public String getResourcePath () {
		return resourcePath;
	}

	public void setCacheSpec (String cacheSpec) {
		this.cacheSpec = cacheSpec;
	}

	public void setResourcePath (String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public boolean isDisableCache () {
		return disableCache;
	}

	public void setDisableCache (boolean disableCache) {
		this.disableCache = disableCache;
	}

	public String getJarResourcePath() {
		return jarResourcePath;
	}

	public void setJarResourcePath(String jarResourcePath) {
		this.jarResourcePath = jarResourcePath;
	}
}
