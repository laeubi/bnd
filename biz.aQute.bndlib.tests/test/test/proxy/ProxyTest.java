package test.proxy;

import java.lang.reflect.Proxy;

public class ProxyTest {

	public static void main(String[] args) {
		// TestInterface has methods that reference types from java.nio.file and java.util
		TestInterface proxy = (TestInterface) Proxy.newProxyInstance(
			ProxyTest.class.getClassLoader(),
			new Class<?>[] { TestInterface.class },
			(proxy1, method, args1) -> null
		);
		System.err.println(proxy);
	}
}
