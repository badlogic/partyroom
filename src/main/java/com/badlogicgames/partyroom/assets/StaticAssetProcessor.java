
package com.badlogicgames.partyroom.assets;

import java.io.File;

/**
 * Processes assets loaded by a {@link StaticAssetsBundle} and it's {@link StaticAssetServlet}
 * and returns a modified payload, e.g. after templating.
 * @author mzechner
 *
 */
public interface StaticAssetProcessor {
	/**
	 * Processes the assets at assetPath, with the binary payload input. Input
	 * can be returned as is or a new payload can be returned that is based on 
	 * input.
	 * @param rootPath the root path from where assets are served
	 * @param assetPath the path to the asset to process
	 * @param input the bytes making up the asset
	 * @return the processed asset payload
	 * @throws Exception
	 */
	public byte[] process (File rootPath, File assetPath, byte[] input) throws Exception;

	/** Default pass through processor, does nothing to the assets **/
	public static final StaticAssetProcessor DEFAULT_PROCESSOR = new StaticAssetProcessor() {
		@Override
		public byte[] process (File rootPath, File assetPath, byte[] input) {
			return input;
		}

		@Override
		public byte[] process(String jarPath, String assetPath, byte[] input) {
			return input;
		}
	};

	/**
	 * Processes the assets at assetPath, with the binary payload input. Input
	 * can be returned as is or a new payload can be returned that is based on 
	 * input. This method reads additionally required assets from the classpath/JAR
	 * @param rootPath the root path from where assets read in the JAR
	 * @param assetPath the path to the asset to process
	 * @param input the bytes making up the asset
	 * @return the processed asset payload
	 * @throws Exception
	 */
	public byte[] process(String jarPath, String assetPath, byte[] input) throws Exception;
}
