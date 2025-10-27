package aQute.bnd.build.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Resource;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.Version;
import aQute.lib.utf8properties.UTF8Properties;

public final class EE implements Comparable<EE> {
	public static final EE OSGI_Minimum_1_0 = new EE(0, "OSGI_Minimum_1_0", "OSGi/Minimum-1.0", "OSGi/Minimum", "1.0", 0);

	public static final EE OSGI_Minimum_1_1 = new EE(1, "OSGI_Minimum_1_1", "OSGi/Minimum-1.1", "OSGi/Minimum", "1.1", 1, OSGI_Minimum_1_0);

	public static final EE OSGI_Minimum_1_2 = new EE(2, "OSGI_Minimum_1_2", "OSGi/Minimum-1.2", "OSGi/Minimum", "1.2", 2, OSGI_Minimum_1_1);

	public static final EE JRE_1_1 = new EE(3, "JRE_1_1", "JRE-1.1", "JRE", "1.1", 1);

	public static final EE J2SE_1_2 = new EE(4, "J2SE_1_2", "J2SE-1.2", "JavaSE", "1.2", 2, JRE_1_1);

	public static final EE J2SE_1_3 = new EE(5, "J2SE_1_3", "J2SE-1.3", "JavaSE", "1.3", 3, J2SE_1_2, OSGI_Minimum_1_1);

	public static final EE J2SE_1_4 = new EE(6, "J2SE_1_4", "J2SE-1.4", "JavaSE", "1.4", 4, J2SE_1_3, OSGI_Minimum_1_2);

	public static final EE J2SE_1_5 = new EE(7, "J2SE_1_5", "J2SE-1.5", "JavaSE", "1.5", 5, J2SE_1_4);

	public static final EE JavaSE_1_6 = new EE(8, "JavaSE_1_6", "JavaSE-1.6", "JavaSE", "1.6", 6, J2SE_1_5);

	public static final EE JavaSE_1_7 = new EE(9, "JavaSE_1_7", "JavaSE-1.7", "JavaSE", "1.7", 7, JavaSE_1_6);

	public static final EE JavaSE_compact1_1_8 = new EE(10, "JavaSE_compact1_1_8", "JavaSE/compact1-1.8", "JavaSE/compact1", "1.8", 8, OSGI_Minimum_1_2);

	public static final EE JavaSE_compact2_1_8 = new EE(11, "JavaSE_compact2_1_8", "JavaSE/compact2-1.8", "JavaSE/compact2", "1.8", 8, JavaSE_compact1_1_8);

	public static final EE JavaSE_compact3_1_8 = new EE(12, "JavaSE_compact3_1_8", "JavaSE/compact3-1.8", "JavaSE/compact3", "1.8", 8, JavaSE_compact2_1_8);

	public static final EE JavaSE_1_8 = new EE(13, "JavaSE_1_8", "JavaSE-1.8", "JavaSE", "1.8", 8, JavaSE_1_7, JavaSE_compact3_1_8);

	public static final EE JavaSE_9 = new EE(14, "JavaSE_9", 9);
	public static final EE JavaSE_10 = new EE(15, "JavaSE_10", 10);
	public static final EE JavaSE_11 = new EE(16, "JavaSE_11", 11);
	public static final EE JavaSE_12 = new EE(17, "JavaSE_12", 12);
	public static final EE JavaSE_13 = new EE(18, "JavaSE_13", 13);
	public static final EE JavaSE_14 = new EE(19, "JavaSE_14", 14);
	public static final EE JavaSE_15 = new EE(20, "JavaSE_15", 15);
	public static final EE JavaSE_16 = new EE(21, "JavaSE_16", 16);
	public static final EE JavaSE_17 = new EE(22, "JavaSE_17", 17);
	public static final EE JavaSE_18 = new EE(23, "JavaSE_18", 18);
	public static final EE JavaSE_19 = new EE(24, "JavaSE_19", 19);
	public static final EE JavaSE_20 = new EE(25, "JavaSE_20", 20);
	public static final EE JavaSE_21 = new EE(26, "JavaSE_21", 21);
	public static final EE JavaSE_22 = new EE(27, "JavaSE_22", 22);
	public static final EE JavaSE_23 = new EE(28, "JavaSE_23", 23);
	public static final EE JavaSE_24 = new EE(29, "JavaSE_24", 24);
	public static final EE JavaSE_25 = new EE(30, "JavaSE_25", 25);
	public static final EE JavaSE_26 = new EE(31, "JavaSE_26", 26);
	public static final EE JavaSE_27 = new EE(32, "JavaSE_27", 27);
	public static final EE JavaSE_28 = new EE(33, "JavaSE_28", 28);
	public static final EE JavaSE_29 = new EE(34, "JavaSE_29", 29);
	public static final EE JavaSE_30 = new EE(35, "JavaSE_30", 30);

	public static final EE UNKNOWN = new EE(36, "UNKNOWN", "<UNKNOWN>", "UNKNOWN", "0", 0);

	final public static int			MAX_SUPPORTED_RELEASE	= 24;
	
	private static final EE[] KNOWN_EES = {
		OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2, 
		JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, 
		JavaSE_1_6, JavaSE_1_7, 
		JavaSE_compact1_1_8, JavaSE_compact2_1_8, JavaSE_compact3_1_8, JavaSE_1_8,
		JavaSE_9, JavaSE_10, JavaSE_11, JavaSE_12, JavaSE_13, JavaSE_14, JavaSE_15, 
		JavaSE_16, JavaSE_17, JavaSE_18, JavaSE_19, JavaSE_20, JavaSE_21, JavaSE_22, 
		JavaSE_23, JavaSE_24, JavaSE_25, JavaSE_26, JavaSE_27, JavaSE_28, JavaSE_29, JavaSE_30,
		UNKNOWN
	};
	
	private static final Map<String, EE> BY_NAME = new HashMap<>();
	private static final Map<Integer, EE> BY_RELEASE = new HashMap<>();
	private final int				ordinal;
	private final String			name;
	private final String			eeName;
	private final String			capabilityName;
	private final String			versionLabel;
	private final Version			capabilityVersion;
	private final EE[]				compatible;
	private final int				release;
	private transient Set<EE>		compatibleSet;
	private transient Parameters	packages				= null;
	private transient Parameters	modules					= null;

	private Resource				resource;
	/**
	 * For use by JavaSE_9 and later.
	 */
	private EE(int ordinal, String name, int release) {
		this.ordinal = ordinal;
		this.name = name;
		int version = ordinal - 5;
		this.versionLabel = Integer.toString(version);
		this.eeName = "JavaSE-" + versionLabel;
		this.capabilityName = "JavaSE";
		this.capabilityVersion = new Version(version);
		this.compatible = null;
		this.release = release;
	}

	private EE(int ordinal, String name, String eeName, String capabilityName, String versionLabel, int release, EE... compatible) {
		this.ordinal = ordinal;
		this.name = name;
		this.eeName = eeName;
		this.capabilityName = capabilityName;
		this.versionLabel = versionLabel;
		this.capabilityVersion = new Version(versionLabel);
		this.compatible = compatible;
		this.release = release;
	}
	
	static {
		for (EE ee : KNOWN_EES) {
			BY_NAME.put(ee.eeName.toLowerCase(), ee);
			if (ee.release > 0) {
				BY_RELEASE.put(ee.release, ee);
			}
		}
	}
	
	/**
	 * Get or create an EE instance for the given release version.
	 * This allows dynamic support for future Java versions without code changes.
	 * 
	 * @param release the Java release version (e.g., 9, 11, 17, 21, etc.)
	 * @return the EE instance for the given release
	 */
	public static EE getOrCreate(int release) {
		EE existing = BY_RELEASE.get(release);
		if (existing != null) {
			return existing;
		}
		
		// Create a dynamic EE instance for newer Java versions
		// Find the highest known EE to use as a base for ordinal calculation
		int maxOrdinal = KNOWN_EES[KNOWN_EES.length - 2].ordinal; // -2 to skip UNKNOWN
		int newOrdinal = maxOrdinal + (release - JavaSE_30.release);
		String name = "JavaSE_" + release;
		EE newEE = new EE(newOrdinal, name, release);
		
		// Cache it for future use
		synchronized (BY_RELEASE) {
			BY_RELEASE.putIfAbsent(release, newEE);
			return BY_RELEASE.get(release);
		}
	}

	public String getEEName() {
		return eeName;
	}
	
	/**
	 * Returns the symbolic name of this EE, equivalent to the enum constant name.
	 * Used for loading properties files.
	 * 
	 * @return the symbolic name
	 */
	public String name() {
		return name;
	}
	
	/**
	 * Returns the ordinal of this EE, equivalent to the enum ordinal.
	 * 
	 * @return the ordinal
	 */
	public int ordinal() {
		return ordinal;
	}

	/**
	 * @return An array of EEs that this EE implicitly offers, through backwards
	 *         compatibility.
	 */
	public EE[] getCompatible() {
		Set<EE> set = getCompatibleSet();
		return set.toArray(new EE[0]);
	}

	private static final EE[] values = KNOWN_EES;

	private Optional<EE> previous() {
		int prevOrdinal = ordinal - 1;
		if (prevOrdinal >= 0 && prevOrdinal < values.length) {
			return Optional.of(values[prevOrdinal]);
		}
		return Optional.empty();
	}

	private Set<EE> getCompatibleSet() {
		if (compatibleSet != null) {
			return compatibleSet;
		}
		Set<EE> set = new LinkedHashSet<>();
		if (compatible != null) {
			for (EE ee : compatible) {
				set.add(ee);
				set.addAll(ee.getCompatibleSet());
			}
		} else {
			Optional<EE> previous = previous();
			previous.ifPresent(ee -> {
				set.add(ee);
				set.addAll(ee.getCompatibleSet());
			});
		}
		return compatibleSet = set;
	}

	public String getCapabilityName() {
		return capabilityName;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public Version getCapabilityVersion() {
		return capabilityVersion;
	}

	/**
	 * @return the java release target corresponding to this EE
	 */
	public OptionalInt getReleaseTarget() {
		Version version = getCapabilityVersion();
		int major = version.getMajor();
		if (major > 1) {
			return OptionalInt.of(major);
		}
		if (major == 1 && version.getMinor() > 5) {
			return OptionalInt.of(version.getMinor());
		}
		return OptionalInt.empty();
	}

	public static Optional<EE> highestFromTargetVersion(String targetVersion) {
		Version version = Optional.of(targetVersion)
			.map(Analyzer::cleanupVersion)
			.map(Version::new)
			.map(v -> {
				int major = v.getMajor();
				int minor = v.getMinor();
				// maps 8 to 1.8
				if ((major > 1) && (major < 9)) {
					minor = major;
					major = 1;
				}
				// drop the MICRO version since EEs don't have them
				return new Version(major, minor, 0);
			})
			// practically unreachable since NPE and invalid syntax are caught
			// earlier
			.orElseThrow(() -> new IllegalArgumentException(
				"Argument could not be recognized as a version string: " + targetVersion));
		return Arrays.stream(values)
			.filter(ee -> ee.capabilityVersion.compareTo(version) == 0)
			.sorted(Collections.reverseOrder())
			.findFirst();
	}

	public static EE parse(String str) {
		if (str == null) {
			return null;
		}
		return BY_NAME.get(str.toLowerCase());
	}

	/**
	 * Return the list of packages
	 *
	 * @throws IOException (Unchecked via {@link Exceptions})
	 */
	@SuppressWarnings("javadoc")
	public Parameters getPackages() {
		if (packages == null) {
			init();
		}
		return packages;
	}

	/**
	 * Return the list of modules
	 *
	 * @throws IOException (Unchecked via {@link Exceptions})
	 */
	@SuppressWarnings("javadoc")
	public Parameters getModules() {
		if (modules == null) {
			init();
		}
		return modules;
	}

	private void init() {
		try (InputStream stream = EE.class.getResourceAsStream(name() + ".properties")) {
			if (stream == null) {
				Optional<EE> previous = previous();
				packages = previous.map(EE::getPackages)
					.orElseGet(Parameters::new);
				modules = previous.map(EE::getModules)
					.orElseGet(Parameters::new);
				return;
			}
			UTF8Properties props = new UTF8Properties();
			props.load(stream);
			String packagesProp = props.getProperty("org.osgi.framework.system.packages");
			packages = new Parameters(packagesProp);
			String modulesProp = props.getProperty("jpms.modules");
			modules = new Parameters(modulesProp);
		} catch (IOException ioe) {
			throw Exceptions.duck(ioe);
		}
	}

	public int getRelease() {
		return release;
	}

	public int getMajorVersion() {
		return release + 44;
	}

	/**
	 * From a Require-Capability header, extract the Execution Environment
	 * capability and match against EEs. This is generally used with one
	 * requirement. If you add multiple, the EE's are or'ed.
	 *
	 * @param requirement the Require-Capability header
	 * @return A sorted set of EEs that match one of the given requirements
	 */
	public static SortedSet<EE> getEEsFromRequirement(String requirement) {
		Parameters reqs = new Parameters(requirement);
		SortedSet<EE> result = new TreeSet<>();
		FilterParser fp = new FilterParser();

		for (Map.Entry<String, Attrs> e : reqs.entrySet()) {
			Attrs attrs = reqs.get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			if (attrs != null) {
				String filter = attrs.get("filter:");
				Expression expr = fp.parse(filter);
				Map<String, Object> map = new HashMap<>();
				for (EE v : EE.values) {
					map.put(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, v.getCapabilityName());
					map.put(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, v.getCapabilityVersion());
					if (expr.eval(map)) {
						result.add(v);
					}
				}
			}
		}
		return result;
	}

	final static EE[] classFileVersionsMinus44 = {
		UNKNOWN, JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, JavaSE_1_6, JavaSE_1_7, JavaSE_1_8, JavaSE_9,
		JavaSE_10, JavaSE_11, JavaSE_12, JavaSE_13, JavaSE_14, JavaSE_15, JavaSE_16, JavaSE_17, JavaSE_18, JavaSE_19,
		JavaSE_20, JavaSE_21, JavaSE_22, JavaSE_23, JavaSE_24, JavaSE_25, JavaSE_26, JavaSE_27, JavaSE_28, JavaSE_29,
		JavaSE_30
	};

	/**
	 * Return the EE associated with the given class file version
	 *
	 * @param majorVersion the class file major version
	 * @return the EE or empty
	 */
	public static EE getEEFromClassVersion(int majorVersion) {
		majorVersion -= 44;
		if (majorVersion < 0 || majorVersion > classFileVersionsMinus44.length)
			return UNKNOWN;
		return classFileVersionsMinus44[majorVersion];
	}

	/**
	 * Return the EE related to the release version
	 */

	public static EE getEEFromReleaseVersion(int releaseVersion) {
		EE ee = BY_RELEASE.get(releaseVersion);
		if (ee != null) {
			return ee;
		}
		// For unknown release versions, return UNKNOWN
		return UNKNOWN;
	}

	/**
	 * Returns an array of all known EE instances.
	 * 
	 * @return array of all known EE instances
	 */
	public static EE[] values() {
		return values.clone();
	}

	static SortedSet<EE> all;
	static {
		var a = new TreeSet<>(Arrays.asList(values));
		a.remove(UNKNOWN);
		all = Collections.unmodifiableSortedSet(a);
	}

	public static SortedSet<EE> all() {
		return all;
	}

	/**
	 * Return a list of capabilities associated with this EE
	 */
	public Resource getResource() {
		if (resource != null)
			return resource;

		ResourceBuilder rb = new ResourceBuilder();
		getPackages()
			.forEach((k, v) -> {
				String name = Processor.removeDuplicateMarker(k);
				rb.addExportPackage(name, v);
			});

		rb.addEE(this);
		return resource = rb.build();
	}
	
	@Override
	public int compareTo(EE other) {
		if (other == null) {
			return 1;
		}
		return Integer.compare(this.ordinal, other.ordinal);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof EE)) {
			return false;
		}
		EE other = (EE) obj;
		return this.ordinal == other.ordinal;
	}
	
	@Override
	public int hashCode() {
		return Integer.hashCode(ordinal);
	}
	
	@Override
	public String toString() {
		return name;
	}
}
