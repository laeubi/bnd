package bndtools.launch;

import static bndtools.launch.LaunchConstants.PLUGIN_ID;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunMode;
import org.bndtools.api.launch.LaunchConstants;
import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.service.EclipseJUnitTester;
import aQute.lib.io.IO;

@Component(scope = ServiceScope.PROTOTYPE, service = ILaunchConfigurationDelegate2.class)
public class OSGiJUnitLaunchDelegate extends AbstractOSGiLaunchDelegate {

	private static final BundleContext BC;

	static {
		BC = FrameworkUtil.getBundle(OSGiJUnitLaunchDelegate.class)
			.getBundleContext();
	}

	private static final ILogger	logger					= Logger.getLogger(OSGiJUnitLaunchDelegate.class);
	private final static String		JNAME_S					= "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
	private final static Pattern	FAILURES_P				= Pattern.compile("^(" + JNAME_S + ")   # method name\n"		//
		+ "\\("																												//
		+ "     (" + JNAME_S + "(?:\\." + JNAME_S + ")*)          # fqn class name\n"										//
		+ "\\)$                                                   # close\n",												//
		Pattern.UNIX_LINES | Pattern.MULTILINE | Pattern.COMMENTS);
	public static final String		ORG_BNDTOOLS_TESTNAMES	= "org.bndtools.testnames";
	private static final String		JDT_JUNIT_BSN			= "org.eclipse.jdt.junit";

	private int						junitPort;
	private ProjectTester			bndTester;
	private EclipseJUnitTester		bndEclipseTester;
	private boolean					rerunIDE;
	private boolean					keepAlive;

	@Override
	protected void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception {
		bndTester = model.getProjectTester();

		if (bndTester instanceof EclipseJUnitTester)
			bndEclipseTester = (EclipseJUnitTester) bndTester;
		junitPort = configureTester(configuration);
		bndTester.prepare();
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
		throws CoreException {
		boolean result = super.finalLaunchCheck(configuration, mode, monitor);

		// Trigger opening of the JUnit view
		Status junitStatus = new Status(IStatus.INFO, PLUGIN_ID, LaunchConstants.LAUNCH_STATUS_JUNIT, "", null);
		IStatusHandler handler = DebugPlugin.getDefault()
			.getStatusHandler(junitStatus);
		if (handler != null)
			handler.handleStatus(junitStatus, null);

		return result;
	}

	@Override
	protected RunMode getRunMode() {
		return RunMode.TEST;
	}

	// A couple of hacks to make sure the JUnit plugin is active and notices our
	// launch.
	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		// start the JUnit plugin
		try {
			Bundle jdtJUnitBundle = BundleUtils.findBundle(BC, JDT_JUNIT_BSN, null);
			if (jdtJUnitBundle == null)
				throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0,
					MessageFormat.format("Bundle \"{0}\" was not found. Cannot report JUnit results via the Workbench.",
						JDT_JUNIT_BSN),
					null));
			jdtJUnitBundle.start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			throw new CoreException(
				new Status(IStatus.ERROR, PLUGIN_ID, 0,
					MessageFormat.format(
						"Error starting bundle \"{0}\". Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN),
					null));
		}

		// JUnit plugin ignores the launch unless attribute
		// "org.eclipse.jdt.launching.PROJECT_ATTR" is set.
		ILaunchConfigurationWorkingCopy modifiedConfig = configuration.getWorkingCopy();

		IResource launchResource = LaunchUtils.getTargetResource(configuration);
		if (launchResource != null) {
			String launchProjectName = LaunchUtils.getLaunchProjectName(launchResource);

			IProject launchProject = ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProject(launchProjectName);

			if (!launchProject.exists()) {
				launchProjectName = launchResource.getProject()
					.getName();
			}

			modifiedConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, launchProjectName);
		}

		return super.getLaunch(modifiedConfig.doSave(), mode);
	}

	private static class TestRunSessionAndPort extends TestRunSession {

		volatile boolean	keepAlive;
		volatile boolean	isReusedLaunch	= false;

		public TestRunSessionAndPort(ILaunch launch, IJavaProject project, int port, boolean keepAlive) {
			super(launch, project, port);
			this.keepAlive = keepAlive;
			// Try and honor the "maxCount" setting.
			// Subtract 1 because we need room to add the new session into the
			// list.
			final int maxCount = Platform.getPreferencesService()
				.getInt(JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.MAX_TEST_RUNS, 10, null) - 1;

			List<TestRunSession> runningSessions = JUnitCorePlugin.getModel()
				.getTestRunSessions();
			final int size = runningSessions.size();

			// TODO: This code interacts in a complicated manner with the
			// Eclipse core. It could do with some decent regression tests..
			if (maxCount < size) {
				Set<ILaunch> runningLaunches = new HashSet<>();
				runningLaunches.add(launch);
				for (TestRunSession trs : runningSessions.subList(0, maxCount)) {
					if (trs instanceof TestRunSessionAndPort && !runningLaunches.add(trs.getLaunch())) {
						((TestRunSessionAndPort) trs).isReusedLaunch = true;
					}
				}
				for (TestRunSession trs : runningSessions.subList(maxCount, size)) {
					if (trs instanceof TestRunSessionAndPort && !runningLaunches.add(trs.getLaunch())) {
						TestRunSessionAndPort trsp = (TestRunSessionAndPort) trs;
						// If the launch already has a session, then allow
						// this session to be cleaned up by JUnit plugin
						// core by clearing its keepalive flag.
						trsp.keepAlive = false;
						trsp.isReusedLaunch = true;
					}
				}
			}

			JUnitCorePlugin.getModel()
				.addTestRunSession(this);

			for (TestRunListener listener : JUnitCorePlugin.getDefault()
				.getNewTestRunListeners()) {
				listener.sessionLaunched(this);
			}
		}

		// Setting keepAlive signals JUnit core to enable the
		// "rerun" menu items. Also prevents the session from
		// being cleaned up.
		@Override
		public boolean isKeptAlive() {
			return keepAlive;
		}

		// The Eclipse JUnit interface assumes that there is a one-to-one
		// relationship between a TestRunSession and an ILaunch. We violate
		// that assumption in continuous testing mode because one launch can
		// generated multiple TestRunSessions in its lifetime.
		//
		// Because of the above assumption, Eclipse JUnit core attempts to
		// remove the TestRunSession's corresponding ILaunch from the
		// LaunchManager when the session is removed.
		//
		// We use the "isReused" flag to indicate that a launch has been
		// reused by another test session. If it has been reused, then we
		// return "null" which signals to the rest of the code that the
		// launch is being managed "externally". So when the session is cleaned
		// up, the launch will not be removed. Ref bndtools issue #4716
		@Override
		public ILaunch getLaunch() {
			return isReusedLaunch ? null : super.getLaunch();
		}
	}

	private static class ControlThread implements Runnable, AutoCloseable {

		final ServerSocket				listener;
		final ILaunch					launch;
		final IJavaProject				project;
		volatile Socket					socket;
		final boolean					keepAlive;
		volatile TestRunSessionAndPort	testRunSession;
		volatile DataOutputStream		outStr;

		public ControlThread(ILaunch launch, IJavaProject project, int port, boolean keepAlive) throws IOException {
			this.launch = launch;
			this.project = project;
			listener = new ServerSocket(port);
			this.keepAlive = keepAlive;
		}

		@Override
		public void run() {
			try {
				// Note that this loop will terminate when accept() throws a
				// SocketException. This ordinarily happens when the
				// ServerSocket is closed in ControlThread.close(), which is in
				// turn called by the registered TerminationListener (configured
				// by launch()) at process termination time.
				while (true) {
					socket = listener.accept();
					outStr = new DataOutputStream(socket.getOutputStream());
					while (socket.getInputStream()
						.read() != -1) {
						int port = SocketUtil.findFreePort();
						testRunSession = new TestRunSessionAndPort(launch, project, port, keepAlive);
						outStr.writeInt(port);
					}
				}
			} catch (SocketException se) {
				logger.logInfo("Connection to tester terminated", se);
			} catch (IOException e) {
				logger.logError("Error communicating to the tester", e);
			}
			// This call is defensive; close() should have already been called
			// by the time we get here.
			close();
		}

		@Override
		public void close() {
			IO.close(listener);
			IO.close(outStr);
			IO.close(socket);
			synchronized (this) {
				if (testRunSession != null) {
					testRunSession.stopTestRun();
					testRunSession = null;
				}
			}
		}
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
		throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);

		final IJavaProject javaProject = JUnitLaunchConfigurationConstants.getJavaProject(configuration);
		if (javaProject == null)
			throw new CoreException(
				new Status(IStatus.ERROR, PLUGIN_ID, 0, "Error obtaining OSGi project tester.", null));

		if (rerunIDE) {
			ControlThread controlThread;
			try {
				controlThread = new ControlThread(launch, javaProject, junitPort, keepAlive);
			} catch (Exception e) {
				throw new CoreException(
					new Status(IStatus.ERROR, PLUGIN_ID, 0, "Couldn't start control thread for tester.", e));
			}
			DebugPlugin.getDefault()
				.addDebugEventListener(new TerminationListener(launch, () -> controlThread.close()));

			new Thread(controlThread).start();
		} else {

			TestRunSessionAndPort testRunSessionAndPort = new TestRunSessionAndPort(launch, javaProject, junitPort,
				keepAlive);
			DebugPlugin.getDefault()
				.addDebugEventListener(new TerminationListener(launch, () -> testRunSessionAndPort.stopTestRun()));
		}

		super.launch(configuration, mode, launch, progress.split(1, SubMonitor.SUPPRESS_NONE));
	}

	private int configureTester(ILaunchConfiguration configuration) throws CoreException, IOException {
		assertBndEclipseTester();

		// Find free socket for JUnit protocol
		int port = SocketUtil.findFreePort();

		keepAlive = enableKeepAlive(configuration);
		rerunIDE = keepAlive
			&& configuration.getAttribute(LaunchConstants.ATTR_RERUN_IDE, LaunchConstants.DEFAULT_RERUN_IDE);
		if (rerunIDE) {
			bndEclipseTester.setControlPort(port);
		} else {
			bndEclipseTester.setPort(port);
		}

		// Set up launcher properties from FrameworkTabPiece (keep storage,
		// enable tracing)
		configureLauncher(configuration);

		// Keep alive?
		bndTester.setContinuous(keepAlive);

		//
		// The JUnit runner can set a file with names of failed tests
		// that are requested to rerun
		//

		String failuresFileName = configuration.getAttribute("org.eclipse.jdt.junit.FAILURENAMES", (String) null);
		if (failuresFileName != null) {
			File failuresFile = new File(failuresFileName);
			if (failuresFile.isFile()) {
				String failures = IO.collect(failuresFile);
				Matcher m = FAILURES_P.matcher(failures);
				while (m.find()) {
					bndTester.addTest(m.group(2) + ":" + m.group(1));
				}
			}
		}

		//
		// Check if we were asked to re-run a specific test class/method
		//

		String testClass = configuration.getAttribute("org.eclipse.jdt.launching.MAIN_TYPE", (String) null);
		String testMethod = configuration.getAttribute("org.eclipse.jdt.junit.TESTNAME", (String) null);

		if (testClass != null) {
			String testName = testClass;
			if (testMethod != null)
				testName += ":" + testMethod;
			bndTester.addTest(testName);
		} else {
			// We're not being asked to run a specific class and/or method, so
			// use
			String tests = configuration.getAttribute(ORG_BNDTOOLS_TESTNAMES, (String) null);
			if (tests != null && !tests.trim()
				.isEmpty()) {
				for (String test : tests.trim()
					.split("\\s+")) {
					bndTester.addTest(test);
				}
			}
		}

		// if (bndTester.getTests().isEmpty()) {
		// if (!bndTester.getContinuous() ||
		// bndTester.getProject().getProperty(Constants.TESTCASES) == null) {
		// throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
		// 0, "No tests are selected. " //
		// + "This starts the tester who will then wait " //
		// + "for bundles with the Test-Cases header listing the test cases. To
		// enable this, set " +
		// Constants.TESTCONTINUOUS + " to true", null));
		// }
		// }

		return port;
	}

	private void assertBndEclipseTester() throws CoreException {
		if (bndEclipseTester == null)
			throw new CoreException(
				new Status(IStatus.ERROR, PLUGIN_ID, 0, "Bnd/Eclipse tester was not initialised.", null));
	}

	@SuppressWarnings("deprecation")
	private static boolean enableKeepAlive(ILaunchConfiguration configuration) throws CoreException {
		boolean keepAlive = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE,
			LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
		if (keepAlive == LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE) {
			keepAlive = configuration.getAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE,
				LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
		}
		return keepAlive;
	}

	@Override
	protected ProjectLauncher getProjectLauncher() throws CoreException {
		if (bndTester == null)
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Bnd tester was not initialised.", null));
		return bndTester.getProjectLauncher();
	}
}
