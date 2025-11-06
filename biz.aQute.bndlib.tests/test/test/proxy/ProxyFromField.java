package test.proxy;

import java.lang.reflect.Proxy;

/**
 * Test that we don't incorrectly detect proxy interfaces when the Class[] array
 * comes from a static field rather than being created inline.
 */
public class ProxyFromField {

	private static final Class<?>[] INTERFACES = new Class<?>[] { TestInterface.class };

	public static void main(String[] args) {
		TestInterface proxy = (TestInterface) Proxy.newProxyInstance(
			ProxyFromField.class.getClassLoader(),
			INTERFACES,  // Array from field - we cannot detect interfaces reliably
			(proxy1, method, args1) -> null
		);
		System.err.println(proxy);
	}
}
