package org.mitratul.plugin.beanexplore.handlers;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;

public class ClasspathChangedListener implements IElementChangedListener {

	@Override
	public void elementChanged(ElementChangedEvent arg0) {
		//* TODO: Add proper filtering for classpath changes, 
		//        and change of elements in classpath. 
		System.out.println("* D * elemChanged * Element changed:: " + arg0.getSource());
		System.out.println("* D * elemChanged * Kind:: " + arg0.getDelta().getKind());
	}

}
