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

package pascal.taie.analysis.pta.plugin.spring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public final class XmlConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(XmlConfiguration.class);

    private static final String XML_EXT = ".xml";

    private static final DocumentBuilder builder;

    static {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newDefaultInstance();
            builderFactory.setNamespaceAware(false);
            builderFactory.setValidating(false);
            // do not load external DTDs from the Internet
            builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private final MultiMap<String, Pair<String, Node>> tag2FilenameNodes
            = Maps.newMultiMap();

    private final List<String> classPaths;

    public XmlConfiguration(List<String> classPaths) {
        this.classPaths = classPaths;
    }

    public void initialize() {
        for (String classPath : classPaths) {
            if (classPath.endsWith(DirectoryTraverser.JAR_EXT)) {
                DirectoryTraverser.walkJarFile(classPath,
                        filename -> filename.endsWith(XML_EXT), this::read);
            } else {
                DirectoryTraverser.walkDirectory(classPath, false,
                        filename -> filename.endsWith(XML_EXT), this::read);
            }
        }
    }

    private void read(String filename, InputStream inputStream) {
        try {
            Document doc = builder.parse(inputStream);
            traverseNodes(filename, doc.getDocumentElement());
        } catch (IOException | SAXException e) {
            logger.error("", e);
            throw new RuntimeException("Failed to read xml configuration file", e);
        }
    }

    /**
     * Recursively traverses the XML tree and records all element nodes.
     */
    private void traverseNodes(String filename, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String tagName = node.getNodeName();
            tag2FilenameNodes.put(tagName, new Pair<>(filename, node));
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            traverseNodes(filename, children.item(i));
        }
    }

    @Nonnull
    public Collection<Node> getNodesByTag(String tagName) {
        return tag2FilenameNodes.get(tagName)
                .stream()
                .map(Pair::second)
                .toList();
    }

}
