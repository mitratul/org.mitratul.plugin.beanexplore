package org.mitratul.plugin.beanexplore.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.mitratul.plugin.beanexplore.handlers.BeanExploreActivator;

public class BeanIndex {

	private static final String FILE_EXTENTION_XML = ".xml";
	private static final String JAR_XML_SEPERATOR = "!";

	private IOHelper ioHelper;

	private HashMap<String, String> jarChecksumMap;
	private HashMap<String, String> xmlChecksumMap;
	private HashMap<String, String> beanIndex;

	public BeanIndex() {
		ioHelper = BeanExploreActivator.getIOHelper();
		jarChecksumMap = BeanExploreActivator.getJarChecksumMap();
		xmlChecksumMap = BeanExploreActivator.getXmlChecksumMap();
		beanIndex = BeanExploreActivator.getBeanIndexMap();
	}

	public void indexJar (File jarFilePath) {
		long startTimeIndexJar = System.nanoTime();
		
		JarFile jar;
		final String CHECKSUM_KEY = jarFilePath.getAbsolutePath();
		
		try {
			jar = new JarFile(jarFilePath);
			Enumeration<JarEntry> entries = jar.entries();

			String oldJarChecksum = jarChecksumMap.get(CHECKSUM_KEY);
			//* Calculate the jar checksum
			InputStream jarIs = new FileInputStream(jarFilePath);
			String newJarChecksum = calculateChecksum(jarIs);
			jarIs.close();
			
			if( ! newJarChecksum.equals(oldJarChecksum) ) {
				System.out.println("* D * chksum * mismatch * " + jarFilePath);
				//* If jar checksum is changed, refresh the bean index with its content.
				while (entries.hasMoreElements()) {
					JarEntry jarElement = entries.nextElement();
					
					//* filter only XML files
					if (!jarElement.isDirectory() && 
							jarElement.getName().endsWith(FILE_EXTENTION_XML)) {
						System.out.println(jarElement.getName());
						
						//* chk if the xml has bean in it.
						if (ioHelper.containsBean(jar, jarElement)) {
							//* index beans from xml
							indexXml(encodeJarXmlPath(
									jarFilePath.getAbsolutePath(), jarElement.getName()),
									jar, jarElement);
						} //* nothing to do if there is no bean in it.
					} //* nothing to do for other files
				}
				
				//* Update the new checksum as well
				jarChecksumMap.put(CHECKSUM_KEY, newJarChecksum);
			} //* if the jar checksum is unchanged, nothing to do.
			
			jar.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long durationIndexJar = System.nanoTime() - startTimeIndexJar;
		System.out.printf(
				" ** ** indexJar() performance:: file: %20s - size: %9db - time: %9dns.\n", 
				jarFilePath.getName(), jarFilePath.length(), durationIndexJar);
	}

	
	private void indexXml(String absoluteXmlPath, JarFile jar, JarEntry xmlJarElement) 
			throws IOException {
		String oldXmlChecksum = xmlChecksumMap.get(absoluteXmlPath);
		
		InputStream xmlIs = jar.getInputStream(xmlJarElement);
		String newXmlChecksum = calculateChecksum(xmlIs);
		xmlIs.close();

		if( ! newXmlChecksum.equals(oldXmlChecksum) ) {
			System.out.println("* D * chksum * mismatch * " + absoluteXmlPath);
			//* If xml checksum is changed, refresh the bean index with its content.
			refreshIndex(absoluteXmlPath, jar, xmlJarElement);
			//* update the checksum
			xmlChecksumMap.put(absoluteXmlPath, newXmlChecksum);
		} //* if the xml checksum is unchanged, nothing to do.
	}


	private void refreshIndex(String xmlPath, JarFile jar, JarEntry xmlJarElement) 
			throws IOException {
		//* remove index entries from this xml, then re-index
		deleteFromIndex(xmlPath);
		addToIndex(xmlPath, jar, xmlJarElement);
		
		/*
		 * TODO: take care about cases where a jar is removed from classpath, or an xml 
		 *       is removed from jar.
		 */
	}

	
	private void addToIndex(String xmlPath, JarFile jar, JarEntry jarElement) 
			throws IOException {
		InputStream xmlIs = jar.getInputStream(jarElement);
		List<String> beans = ioHelper.getBeans(xmlIs);
		xmlIs.close();
//		long startTime = System.nanoTime();
		for (String beanId : beans) {
			synchronized (beanIndex) {
				beanIndex.put(beanId, xmlPath);
			}
//			System.out.println("putting in BEAN_INDEX: (" + beanId + ", " + xmlPath + ")");
		}
//		long endTime = System.nanoTime();
//		System.out.println("time taken to put in index: " + (endTime - startTime)); 
	}

	
	private void deleteFromIndex(String xmlPath) {
		//* TODO: remove from the index based on the value 
		
	}
	
	public String encodeJarXmlPath(String pJarPath, String pXmlRelativePath) {
		return pJarPath == null || pXmlRelativePath == null 
				? null 
				: pJarPath + JAR_XML_SEPERATOR + pXmlRelativePath;
	}
	
	public String[] decodeJarXmlPath(String pEncodedPath) {
		return pEncodedPath == null 
				? null 
				: pEncodedPath.split(JAR_XML_SEPERATOR);
	}

	
	private String calculateChecksum(InputStream pInputStream) {
		String chksum = null;

		long startTimeChecksum = System.nanoTime();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
//			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dataBytes = new byte[1024];

		    DigestInputStream digestInputStream  = new DigestInputStream(pInputStream, md);
			while (digestInputStream.read(dataBytes) != -1);

		    byte[] mdbytes = md.digest();

		    //convert the byte to hex format
		    StringBuffer sb = new StringBuffer("");
		    for (int i = 0; i < mdbytes.length; i++) {
		    	sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		    }
		    chksum = sb.toString();
		    System.out.println("Digest(in hex format):: " + chksum);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		long durationChecksum = System.nanoTime() - startTimeChecksum;
		System.out.printf(" ** ** ** calculateChecksum() performance:: time: %12dns.\n", durationChecksum);
		
		return chksum;
	}

	
	public String getXmlLocation(String pBeanId) {
		return beanIndex.get(pBeanId);
	}


}
