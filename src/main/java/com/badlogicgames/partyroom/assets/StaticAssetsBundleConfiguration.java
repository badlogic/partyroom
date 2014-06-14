
package com.badlogicgames.partyroom.assets;

import com.yammer.dropwizard.config.Configuration;

/**
 * Must be implemented by a service's {@link Configuration}
 * @author mzechner
 *
 */
public interface StaticAssetsBundleConfiguration {
	public StaticAssetsConfiguration getAssetsConfiguration ();
}
