
package com.badlogicgames.partyroom.assets;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Resources;

/** Simple processor for HTML files. Recognized include tags and will insert the respective asset file.
 * @author gschulze (especially process()) */
public class TemplateProcessor implements StaticAssetProcessor {
	private final Set<String> extensions;
	private final Pattern includePattern = Pattern.compile("<include>(.+?)</include>");

	/** @param extensions the extensions to process, or empty to process any resource */
	public TemplateProcessor (String... extensions) {
		this.extensions = new HashSet<String>();
		for(String extension : extensions)
			this.extensions.add(extension.toLowerCase());
	}

	@Override
	public byte[] process(File rootPath, File resourcePath, byte[] input) throws Exception {
		// scan extensions
		if(extensions.size() > 0) {
			String lower = resourcePath.getName().toLowerCase();
			boolean found = false;
			for(String extension : extensions) {
				if(lower.endsWith(extension)) {
					found = true;
					break;
				}
			}
			if(!found) return input;
		}

		String text = new String(input, "UTF-8");
		List<Tag> foundTags = new ArrayList<Tag>();
		Matcher matcher = includePattern.matcher(text);
		while(matcher.find()) {
			Tag tag = new Tag(matcher.group(0), matcher.group(1));
			@SuppressWarnings("deprecation")
			String value = Resources
				.toString(new File(resourcePath.getParentFile(), tag.value).toURL(), Charset.forName("UTF-8"));
			tag.value = value;
			foundTags.add(tag);
		}

		for(Tag tag : foundTags) {
			text = text.replace(tag.full, tag.value);
		}
		return text.getBytes("UTF-8");
	}

	private static class Tag {
		public String full;
		public String value;

		public Tag (String full, String value) {
			this.full = full;
			this.value = value;
		}
	}

	@Override
	public byte[] process(String jarPath, String assetPath, byte[] input) throws Exception {
		// scan extensions
		if(extensions.size() > 0) {
			String lower = assetPath.toLowerCase();
			boolean found = false;
			for(String extension : extensions) {
				if(lower.endsWith(extension)) {
					found = true;
					break;
				}
			}
			if(!found) return input;
		}

		String text = new String(input, "UTF-8");
		List<Tag> foundTags = new ArrayList<Tag>();
		Matcher matcher = includePattern.matcher(text);
		while(matcher.find()) {
			Tag tag = new Tag(matcher.group(0), matcher.group(1));
			String parent = new File(assetPath).getParent();
			if(parent == null) parent = "";
			InputStream in = TemplateProcessor.class.getResourceAsStream(jarPath + parent + tag.value);
			String value = IOUtils.toString(in);		
			tag.value = value;
			foundTags.add(tag);
		}

		for(Tag tag : foundTags) {
			text = text.replace(tag.full, tag.value);
		}
		return text.getBytes("UTF-8");
	}
}
