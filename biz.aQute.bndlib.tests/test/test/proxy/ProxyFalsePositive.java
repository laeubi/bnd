package test.proxy;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Test case to demonstrate a potential false positive in proxy detection.
 * This class creates a Class[] array for a purpose other than Proxy.newProxyInstance,
 * followed by a lambda (invokedynamic), and then uses newProxyInstance with
 * an array from a field. The bug is that TestInterface might get incorrectly
 * associated with the Runnable proxy.
 */
public class ProxyFalsePositive {

	// Array stored in a field - this won't be detected by the inline array pattern
	private static final Class<?>[] RUNNABLE_ARRAY = new Class<?>[] { Runnable.class };

	public static void main(String[] args) {
		// Create a Class[] array (this triggers anewarray, ldc, aastore pattern)
		// This sets inProxyArray=true and adds TestInterface to proxyInterfaces
		Class<?>[] unrelatedClasses = new Class<?>[] { TestInterface.class };
		
		// Use a lambda that gets compiled to invokedynamic
		// This should reset inProxyArray but currently doesn't
		Supplier<String> lambda = () -> "test";
		lambda.get();
		
		// Now create a proxy using an array from a field (getstatic)
		// The bug: if inProxyArray is still true and proxyInterfaces still has TestInterface,
		// it will incorrectly process TestInterface's methods even though the proxy is for Runnable
		Runnable proxy = (Runnable) Proxy.newProxyInstance(
			ProxyFalsePositive.class.getClassLoader(),
			RUNNABLE_ARRAY,  // From field - not detected by inline pattern
			(proxy1, method, args1) -> null
		);
		
		System.err.println(proxy);
		System.err.println(unrelatedClasses[0]);
	}
}
