package org.mitratul.plugin.beanexplore.handlers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mitratul.plugin.beanexplore.core.IOHelper;
import org.osgi.framework.BundleContext;

public class BeanExploreActivator extends AbstractUIPlugin {

	//* The plug-in ID
	public static final String PLUGIN_ID = "beanexplore"; //$NON-NLS-1$

	//* The shared instances
	private static BeanExploreActivator runtimeInstance;
	private static IOHelper ioHelper;
	//* TODO: refine the DS used
	private static HashMap<String, String> jarChecksumMap;
	private static HashMap<String, String> xmlChecksumMap;
	private static HashMap<String, String> beanIndexMap;
	
	//* the classpath listener
	private ClasspathChangedListener classpathChangeListener;

	public BeanExploreActivator() {
		super();
		System.out.println("Activator initiated");
	}

	public void start(BundleContext context) throws Exception {
		File stateLocation = null;
		try{
			stateLocation = this.getStateLocation().toFile();
		} catch (NullPointerException ex) {
			//* fix for helios NPE
			stateLocation = new File(ResourcesPlugin.getWorkspace().getRoot()
					.getLocation().toOSString() + "/.metadata/.plugins/", 
					"org.mitratul.plugin.beanexplore");
		}
		ioHelper = new IOHelper(stateLocation);
		classpathChangeListener = new ClasspathChangedListener();
		
		//* load the indices from already persisted data.
		try {
			loadFromFile();
		} catch (IOException ex) {
			System.err.println("Index load failed");
		}
		//* then start listening classpath changes
		addClasspathListener(); 
		
		//* TODO: then start updating the index 
		
		super.start(context);
		runtimeInstance = this;
		System.out.println("Activator started");
	}

	public void stop(BundleContext context) throws Exception {
		runtimeInstance = null;
		super.stop(context);
		
		//* stop listening to classpath changes
		removeClasspathListener();
		
		//* persist the indices.
		try {
			storeToFile();
		} catch (IOException ex) {
			System.err.println("Index save failed");
		}
		System.out.println("Activator stopped");
	}


	public static BeanExploreActivator getInstance() {
		return runtimeInstance;
	}
	
	public static IOHelper getIOHelper() {
		return ioHelper;
	}
	
	public static HashMap<String, String> getJarChecksumMap() {
		return jarChecksumMap;
	}

	public static HashMap<String, String> getXmlChecksumMap() {
		return xmlChecksumMap;
	}

	public static HashMap<String, String> getBeanIndexMap() {
		return beanIndexMap;
	}

	private void loadFromFile() throws IOException {
		jarChecksumMap = ioHelper.loadJarChecksum();
		xmlChecksumMap = ioHelper.loadXmlChecksum();
		beanIndexMap = ioHelper.loadBeanIndex();
	}

	private void storeToFile() throws IOException {
		//* TODO: call when the indexing is finished in the beginning, or when classpath changes.
		//*       maintain dirty bit if reqd.
		ioHelper.storeJarChecksum(jarChecksumMap);
		ioHelper.storeXmlChecksum(xmlChecksumMap);
		ioHelper.storeBeanIndex(beanIndexMap);
	}
	
	private void addClasspathListener() {
		//TODO: Implement listeners properly, and un-comment.
		//      Till then update index from project pop-up menu 
		//JavaCore.addElementChangedListener(classpathChangeListener, ElementChangedEvent.POST_CHANGE);
	}
	
	private void removeClasspathListener() {
		JavaCore.removeElementChangedListener(classpathChangeListener);
	}

}
