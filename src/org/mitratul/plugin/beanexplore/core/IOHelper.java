package org.mitratul.plugin.beanexplore.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mitratul.plugin.beanexplore.handlers.BeanExploreActivator;

public class IOHelper {

	//TODO: load from config file
	private static final String FILENAME_JAR_CHECKSUM = "jar.checksum";
	private static final String FILENAME_XML_CHECKSUM = "xml.checksum";
	private static final String FILENAME_BEAN_INDEX = "bean.index";
	private static final String SEPERATOR = ";";
	private static final String DIRNAME_BEANXML = ".bean-xmls";
	private static final String BEANXML_PREFIX = "extracted-bean-";
	private static final String BEANXML_SUFFIX = ".xml";
	
	private Set<String> tmpFilesToDelete;
	//TODO: delete the files in this list before exit
	
	private File indexDir;
	
	public IOHelper() {}
	
	public IOHelper(File pIndexDir) { 
		indexDir = pIndexDir;
		tmpFilesToDelete = new HashSet<String>();
	}
	
//	public HashSet<String> getBeansDummy(InputStream xmlIs) {
//		int s = 10000;
//		HashSet<String> ts = new HashSet<String>(s);
//		for (int i = 0; i < s; i++) {
//			ts.add("SampleBean_" + i);
//		}
//		
//		return ts;
//	}



	public boolean containsBean(JarFile jar, JarEntry jarElement) throws IOException {
		InputStream xmlIs = jar.getInputStream(jarElement);
		
		xmlIs.close();
		return true;
	}

	public List<String> getBeans(InputStream xmlIs) {
//		final String TAG_NAME_BEAN_ROOT = "beans";
		final String TAG_NAME_BEAN = "bean";
		final String ATTR_NAME_ID = "id";

		List<String> beanList = new ArrayList<String>();

		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader reader = factory.createXMLStreamReader(xmlIs);

			while(reader.hasNext()) {
			  int event = reader.next();

			  //* add the bean id to the list, not interested in the other contents.
			  switch(event){
			    case XMLStreamConstants.START_ELEMENT:
			      if (TAG_NAME_BEAN.equals(reader.getLocalName())){
			    	  beanList.add(reader.getAttributeValue(null, ATTR_NAME_ID));
			      }
			      break;
			    default:
			      break;
			  }
			}
			reader.close();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		return beanList;
	}

	public HashMap<String, String> loadJarChecksum() throws IOException {
		return loadMap(FILENAME_JAR_CHECKSUM);
	}

	public HashMap<String, String> loadXmlChecksum() throws IOException {
		return loadMap(FILENAME_XML_CHECKSUM);
	}

	public HashMap<String, String> loadBeanIndex() throws IOException {
		return loadMap(FILENAME_BEAN_INDEX);
	}

	public void storeJarChecksum(HashMap<String, String> jarChecksumMap) throws IOException {
		storeMap(jarChecksumMap, FILENAME_JAR_CHECKSUM);
	}

	public void storeXmlChecksum(HashMap<String, String> xmlChecksumMap) throws IOException {
		storeMap(xmlChecksumMap, FILENAME_XML_CHECKSUM);
	}

	public void storeBeanIndex(HashMap<String, String> beanIndex) throws IOException {
		storeMap(beanIndex, FILENAME_BEAN_INDEX);
	}

	private HashMap<String, String> loadMap(String pFileName) throws IOException {
		HashMap<String, String> map = new HashMap<String, String>();
		
		//* prepare the file to read
		File fMap = new File(indexDir, pFileName);
		fMap.createNewFile();
		BufferedReader brMap = new BufferedReader(
				new InputStreamReader(new FileInputStream(fMap)));
		
		//* read the content
		String line = null;
		String entry[] = new String[2];
		while(true) {
			line = brMap.readLine();
			
			if(line == null) {
				break;
			} else {
				entry = line.split(SEPERATOR);
				if(entry[0] == null || entry[1] == null) {
					System.err.println("Encountered corrupted entry: " + line);
					continue;
				}
				map.put(entry[0], entry[1]);
			}
		}
		
		//* close
		brMap.close();
		
		return map;
	}

	private void storeMap(HashMap<String, String> pMapToStore, String pFileName) throws IOException {
		//* prepare the file to write
		File fMap = new File(indexDir, pFileName);
		fMap.createNewFile();
		PrintWriter pwMap = new PrintWriter(new FileOutputStream(fMap));
		
		//* write the content
		for(String key : pMapToStore.keySet()) {
			pwMap.println(key + SEPERATOR + pMapToStore.get(key));
		}
		
		//* save and close
		pwMap.flush();
		pwMap.close();
	}

	public String extractXml(String jarPath, String xmlRelativePath) {
		String tmpXmlDir = BeanExploreActivator.getInstance().getStateLocation().toFile() 
				+ File.separator + DIRNAME_BEANXML;
		String tmpXmlPath = tmpXmlDir + File.separator 
				+ BEANXML_PREFIX + System.currentTimeMillis() + BEANXML_SUFFIX;
		
		JarFile jar = null;
		try {
			jar = new JarFile(jarPath);
			//* prepare the source
			BufferedReader jarReader = new BufferedReader(new InputStreamReader(
					jar.getInputStream(jar.getJarEntry(xmlRelativePath))));

			//* create the dir if required, and the file to write
			new File(tmpXmlDir).mkdirs();
			File tmpXmlFile = new File(tmpXmlPath);
			tmpXmlFile.createNewFile();
			//* add file to delete-list
			tmpFilesToDelete.add(tmpXmlPath);

			//* prepare the target
			PrintWriter tmpXmlWriter = new PrintWriter(new FileOutputStream(tmpXmlFile));

			//* write the content - first write the jar name, and XML name inside comment
			tmpXmlWriter.println("<!-- Jar name: " + jarPath + " -->");
			tmpXmlWriter.println("<!-- XML name: " + xmlRelativePath + " -->");
			tmpXmlWriter.println("\n\n");

			//* write the content from the xml inside jar to the temp file
			String line = null;
			while(true) {
				line = jarReader.readLine();
				if(line == null) {
					break;
				} else {
					tmpXmlWriter.println(line);
				}
			}

			//* save and close
			tmpXmlWriter.flush();
			tmpXmlWriter.close();
			jarReader.close();
			jar.close();
		} catch (IOException e) {
			tmpXmlPath = null;
		}

		return tmpXmlPath;
		
	}

}
