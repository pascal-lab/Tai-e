/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.android.util;

import pxb.android.axml.AxmlVisitor;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.AbstractResourceParser;
import soot.jimple.infoflow.android.resources.IResourceHandler;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.android.resources.controls.LayoutControlFactory;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Revise the LayoutFileParser temporarily to avoid use soot
 */
public class LayoutFileParser extends AbstractResourceParser {

	protected final MultiMap<String, AndroidLayoutControl> userControls = new HashMultiMap<>();
	protected final MultiMap<String, String> callbackMethods = new HashMultiMap<>();
	protected final MultiMap<String, String> includeDependencies = new HashMultiMap<>();
	protected final MultiMap<String, String> fragments = new HashMultiMap<>();

    protected final MultiMap<String, String> views = new HashMultiMap<>();

    protected final MultiMap<String, String> fragmentsOrViews = new HashMultiMap<>();

    protected final String packageName;
	protected final ARSCFileParser resParser;

	private boolean loadOnlySensitiveControls = false;
	private SootClass scViewGroup = null;
	private SootClass scView = null;
	private SootClass scWebView = null;

	private LayoutControlFactory controlFactory = new LayoutControlFactory();

	public LayoutFileParser(String packageName, String apkPath, ARSCFileParser resParser) {
		this.packageName = packageName;
		this.resParser = resParser;
        this.parseLayoutFile(apkPath);
	}


	/**
	 * Adds a callback method found in an XML file to the result set
	 *
	 * @param layoutFile The XML file in which the callback has been found
	 * @param callback   The callback found in the given XML file
	 */
	private void addCallbackMethod(String layoutFile, String callback) {
		layoutFile = layoutFile.replace("/layout-large/", "/layout/");
		callbackMethods.put(layoutFile, callback);

		// Recursively process any dependencies we might have collected before
		// we have processed the target
		if (includeDependencies.containsKey(layoutFile))
			for (String target : includeDependencies.get(layoutFile))
				addCallbackMethod(target, callback);
	}

	/**
	 * Adds a fragment found in an XML file to the result set
	 *
	 * @param layoutFile The XML file in which the fragment has been found
	 * @param fragment   The fragment found in the given XML file
	 */
	private void addFragment(String layoutFile, String fragment) {
		// Do not add null fragments
		if (fragment == null)
			return;

		layoutFile = layoutFile.replace("/layout-large/", "/layout/");
		fragments.put(layoutFile, fragment);

		// Recursively process any dependencies we might have collected before
		// we have processed the target
		if (includeDependencies.containsKey(layoutFile))
			for (String target : includeDependencies.get(layoutFile))
				addFragment(target, fragment);
	}

    /**
     * Adds a view found in an XML file to the result set
     *
     * @param layoutFile The XML file in which the view has been found
     * @param view       The view found in the given XML file
     */
    private void addView(String layoutFile, String view) {
        // Do not add null views
        if (view == null)
            return;

        layoutFile = layoutFile.replace("/layout-large/", "/layout/");
        views.put(layoutFile, view);

        // Recursively process any dependencies we might have collected before
        // we have processed the target
        if (includeDependencies.containsKey(layoutFile))
            for (String target : includeDependencies.get(layoutFile))
                addFragment(target, view);
    }

    /**
     * Adds a fragmentsOrView found in an XML file to the result set
     *
     * @param layoutFile       The XML file in which the fragmentsOrView has been found
     * @param fragmentsOrView  The fragmentsOrView found in the given XML file
     */
    private void addFragmentsOrViews(String layoutFile, String fragmentsOrView) {
        // Do not add null others
        if (fragmentsOrView == null)
            return;

        layoutFile = layoutFile.replace("/layout-large/", "/layout/");
        fragmentsOrViews.put(layoutFile, fragmentsOrView);
    }


	/**
	 * Parses all layout XML files in the given APK file and loads the IDs of the
	 * user controls in it. This method only registers a Soot phase that is run when
	 * the Soot packs are next run
	 *
	 * @param fileName The APK file in which to look for user controls
	 */
	public void parseLayoutFile(final String fileName) {
        parseLayoutFileDirect(fileName);
	}

	/**
	 * Parses all layout XML files in the given APK file and loads the IDs of the
	 * user controls in it. This method directly executes the analyses witout
	 * registering any Soot phases.
	 *
	 * @param fileName The APK file in which to look for user controls
	 */
	public void parseLayoutFileDirect(final String fileName) {
		handleAndroidResourceFiles(fileName, /* classes, */ null, new IResourceHandler() {

			@Override
			public void handleResourceFile(final String fileName, Set<String> fileNameFilter, InputStream stream) {
				// We only process valid layout XML files
				if (!fileName.startsWith("res/layout") && !fileName.startsWith("res/navigation"))
					return;
				if (!fileName.endsWith(".xml")) {
					logger.warn(String.format("Skipping file %s in layout folder...", fileName));
					return;
				}

				// Initialize the Soot classes
				scViewGroup = Scene.v().getSootClassUnsafe("android.view.ViewGroup");
				scView = Scene.v().getSootClassUnsafe("android.view.View");
				scWebView = Scene.v().getSootClassUnsafe("android.webkit.WebView");

				// Get the fully-qualified class name
				String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
				if (!packageName.isEmpty())
					entryClass = packageName + "." + entryClass;

				// We are dealing with resource files
				if (fileNameFilter != null) {
					boolean found = false;
					for (String s : fileNameFilter)
						if (s.equalsIgnoreCase(entryClass)) {
							found = true;
							break;
						}
					if (!found)
						return;
				}

				try {
					AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
					parseLayoutNode(fileName, handler.getDocument().getRootNode());
				} catch (Exception ex) {
					logger.error("Could not read binary XML file: " + ex.getMessage(), ex);
				}
			}
		});
	}

	/**
	 * Parses the layout file with the given root node
	 *
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode   The root node from where to start parsing
	 */
	private void parseLayoutNode(String layoutFile, AXmlNode rootNode) {
		if (rootNode.getTag() == null || rootNode.getTag().isEmpty()) {
			logger.warn("Encountered a null or empty node name in file %s, skipping node...", layoutFile);
			return;
		}

		String tname = rootNode.getTag().trim();
		if (tname.equals("dummy")) {
			// dummy root node, ignore it
		}
		// Check for inclusions
		else if (tname.equals("include")) {
			parseIncludeAttributes(layoutFile, rootNode);
		}
		// The "merge" tag merges the next hierarchy level into the current
		// one for flattening hierarchies.
		else if (tname.equals("merge")) {
			// do not consider any attributes of this elements, just
			// continue with the children
		} else if (tname.equals("fragment")) {
			AXmlAttribute<?> attr = rootNode.getAttribute("name");
			// final AXmlAttribute<?> attrID = rootNode.getAttribute("id");
			if (attr == null)
				logger.warn("Fragment without class name or id detected");
			else if (rootNode.getAttribute("navGraph") != null)
				parseIncludeAttributes(layoutFile, rootNode);
			else {
				addFragment(layoutFile, attr.getValue().toString());
				if (attr.getType() != AxmlVisitor.TYPE_STRING)
					logger.warn("Invalid target resource " + attr.getValue() + "for fragment class value");
			}
		} else if (tname.equals("view")){
            AXmlAttribute<?> attr = rootNode.getAttribute("name");
            if (attr != null) {
                addView(layoutFile, attr.getValue().toString());
            }
        } else if (tname.equals("Button")){
            AXmlAttribute<?> attr = rootNode.getAttribute("onClick");
            if (attr != null) {
                addCallbackMethod(layoutFile, attr.getValue().toString());
            }
        } else {
            addFragmentsOrViews(layoutFile, tname);
		}

		// Parse the child nodes
		for (AXmlNode childNode : rootNode.getChildren())
			parseLayoutNode(layoutFile, childNode);
	}

	/**
	 * Parses the attributes required for a layout file inclusion
	 *
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode   The AXml node containing the attributes
	 */
	private void parseIncludeAttributes(String layoutFile, AXmlNode rootNode) {
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			String attrName = entry.getKey();
			if (attrName == null || attrName.isEmpty())
				continue;
			attrName = attrName.trim();
			AXmlAttribute<?> attr = entry.getValue();

			if (attrName.equals("layout") || attrName.equals("navGraph")) {
				if ((attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)
						&& attr.getValue() instanceof Integer) {
					// We need to get the target XML file from the binary
					// manifest
					AbstractResource targetRes = resParser.findResource((Integer) attr.getValue());
					if (targetRes == null) {
						logger.warn("Target resource " + attr.getValue() + " for layout include not found");
						return;
					}
					if (!(targetRes instanceof StringResource)) {
						logger.warn(String.format("Invalid target node for include tag in layout XML, was %s",
								targetRes.getClass().getName()));
						return;
					}
					String targetFile = ((StringResource) targetRes).getValue();

					// If we have already processed the target file, we can
					// simply copy the callbacks we have found there
					if (callbackMethods.containsKey(targetFile))
						for (String callback : callbackMethods.get(targetFile))
							addCallbackMethod(layoutFile, callback);
					else {
						// We need to record a dependency to resolve later
						includeDependencies.put(targetFile, layoutFile);
					}
				}
			}
		}
	}

	/**
	 * Parses the layout attributes in the given AXml node
	 *
	 * @param layoutFile  The full path and file name of the file being parsed
	 * @param layoutClass The class for the attributes are parsed
	 * @param rootNode    The AXml node containing the attributes
	 */
	private void parseLayoutAttributes(String layoutFile, SootClass layoutClass, AXmlNode rootNode) {
		// Create the new user control
		AndroidLayoutControl lc = controlFactory.createLayoutControl(layoutFile, layoutClass, rootNode);

		// Check for a button click listener
		if (lc.getClickListener() != null)
			addCallbackMethod(layoutFile, lc.getClickListener());

		// Register the user control
		if (!loadOnlySensitiveControls || lc.isSensitive())
			this.userControls.put(layoutFile, lc);
	}

	/**
	 * Gets the user controls found in the layout XML file. The result is a mapping
	 * from the id to the respective layout control.
	 *
	 * @return The layout controls found in the XML file.
	 */
	public Map<Integer, AndroidLayoutControl> getUserControlsByID() {
		Map<Integer, AndroidLayoutControl> res = new HashMap<>();
		for (AndroidLayoutControl lc : this.userControls.values()) {
			if (lc.getID() != -1)
				res.put(lc.getID(), lc);
		}
		return res;
	}

	/**
	 * Gets the user controls found in the layout XML file. The result is a mapping
	 * from the file name in which the control was found to the respective layout
	 * control.
	 *
	 * @return The layout controls found in the XML file.
	 */
	public MultiMap<String, AndroidLayoutControl> getUserControls() {
		return this.userControls;
	}

	/**
	 * Gets the callback methods found in the layout XML file. The result is a
	 * mapping from the file name to the set of found callback methods.
	 *
	 * @return The callback methods found in the XML file.
	 */
	public MultiMap<String, String> getCallbackMethods() {
		return this.callbackMethods;
	}

	/**
	 * Gets the fragments found in the layout XML file. The result is a mapping from
	 * the activity class to the set of found fragments ids.
	 *
	 * @return The fragments found in the XML file.
	 */
	public MultiMap<String, String> getFragments() {
		return this.fragments;
	}

    /**
     * Gets the views found in the layout XML file. The result is a mapping from
     * the activity class to the set of found views ids.
     *
     * @return The views found in the XML file.
     */
    public MultiMap<String, String> getViews() {
        return this.views;
    }

    /**
     * Gets the views or fragments found in the layout XML file. The result is a mapping from
     * the activity class to the set of found views or fragments ids.
     *
     * @return The views or fragments found in the XML file.
     */
    public MultiMap<String, String> getFragmentsOrViews() {
        return this.fragmentsOrViews;
    }

	/**
	 * Gets whether this analysis shall only collect sensitive controls such as
	 * password fields
	 *
	 * @return True if this analysis shall only collect sensitive controls,
	 *         otherwise false
	 */
	public boolean getLoadOnlySensitiveControls() {
		return this.loadOnlySensitiveControls;
	}

	/**
	 * Sets the layout control factory to use for creating new layout controls
	 *
	 * @param controlFactory The layout control factory
	 */
	public void setControlFactory(LayoutControlFactory controlFactory) {
		this.controlFactory = controlFactory;
	}

}
