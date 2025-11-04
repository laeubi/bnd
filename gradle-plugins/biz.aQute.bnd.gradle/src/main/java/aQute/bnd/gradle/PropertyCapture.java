package aQute.bnd.gradle;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * A serializable wrapper for captured project/task properties that can be
 * introspected by BeanProperties without holding references to Gradle
 * Project or Task objects.
 * <p>
 * This class allows bnd macros like ${project.name} and ${task.name} to work
 * while being compatible with Gradle's configuration cache.
 * <p>
 * BeanProperties will call getter methods like getName() when processing
 * macros like ${project.name}.
 */
class PropertyCapture implements Serializable {
	private static final long serialVersionUID = 1L;
	private final Map<String, Object> properties;

	PropertyCapture(Map<String, Object> properties) {
		this.properties = properties;
	}

	/**
	 * Get a property value by name.
	 * BeanProperties will call this method via reflection when processing
	 * macros. For example, ${project.name} will call getName().
	 */
	public Object get(String key) {
		return properties.get(key);
	}

	// Getters for commonly used properties.
	// These are called by BeanProperties via reflection.
	
	public String getName() {
		return (String) properties.get("name");
	}

	public Object getGroup() {
		return properties.get("group");
	}

	public Object getVersion() {
		return properties.get("version");
	}

	public String getDescription() {
		return (String) properties.get("description");
	}

	public File getDir() {
		return (File) properties.get("dir");
	}

	public File getBuildDir() {
		return (File) properties.get("buildDir");
	}

	// For task properties
	public String getArchiveBaseName() {
		return (String) properties.get("archiveBaseName");
	}

	public String getArchiveClassifier() {
		return (String) properties.get("archiveClassifier");
	}

	public String getArchiveVersion() {
		return (String) properties.get("archiveVersion");
	}

	public String getArchiveFileName() {
		return (String) properties.get("archiveFileName");
	}

	// Allow access to any custom property via ${project.propertyName}
	// or ${task.propertyName} by supporting dynamic property access.
	// BeanProperties will call get<PropertyName>() methods.
}
