/*
 * Copyright 2012 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.nrw.hbz.regal.sync.ingest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.nrw.hbz.regal.api.helper.XmlUtils;

/**
 * http://193.30.112.23:9280/fedora/get/dipp:1001?xml=true
 * http://193.30.112.23:9280/fedora/listDatastreams/dipp:1001?xml=true
 * http://193.30.112.23:9280/fedora/get/dipp:1001/DiPPExt
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class DippDownloader extends Downloader {

    protected void downloadObject(File dir, String pid) {
	try {
	    logger.debug(pid + " start download!");
	    URL url = new URL(getServer() + "get/" + pid + "?xml=true");
	    File file = new File(dir.getAbsolutePath() + File.separator
		    + URLEncoder.encode(pid, "utf-8") + ".xml");
	    String data = null;
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(url.openStream(), writer);
	    data = writer.toString();
	    FileUtils.writeStringToFile(file, data, "utf-8");

	    downloadStreams(dir, pid);
	    downloadConstituent(dir, pid);
	    downloadRelatedObject(dir, pid, "rel:hasPart");

	    downloadRelatedObject(dir, pid, "rel:isPartOf");
	    downloadRelatedObject(new File(getDownloadLocation()), pid,
		    "rel:isMemberOf");
	    downloadRelatedObject(new File(getDownloadLocation()), pid,
		    "rel:isSubsetOf");
	    downloadRelatedObject(new File(getDownloadLocation()), pid,
		    "rel:isMemberOfCollection");
	} catch (MalformedURLException e) {
	    logger.error(e.getMessage());
	} catch (IOException e) {
	    logger.error(e.getMessage());
	}

    }

    private void downloadConstituent(File dir, String pid) {
	String relation = "rel:hasConstituent";

	try {
	    URL url = new URL(getServer() + "get/" + pid + "/RELS-EXT");
	    String data = null;
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(url.openStream(), writer);
	    data = writer.toString();

	    Element root = XmlUtils.getDocument(data);
	    NodeList constituents = root.getElementsByTagName(relation);
	    if (constituents == null || constituents.getLength() == 0)
		return;
	    File zipDir = new File(dir.getAbsolutePath() + File.separator
		    + "content");
	    for (int i = 0; i < constituents.getLength(); i++) {

		Element c = (Element) constituents.item(i);
		String cPid = c.getAttribute("rdf:resource").replace(
			"info:fedora/", "");
		if (cPid.contains("temp")) {
		    logger.debug(cPid + " skip temporary object.");

		} else {
		    File cDir = new File(dir.getAbsolutePath() + File.separator

		    + URLEncoder.encode(cPid, "utf-8"));

		    try {
			downloadObject(cDir, cPid);
		    } catch (Exception e) {
			logger.warn(e.getMessage());
		    }
		    try {
			getMap().remove(cPid);
			downloadObject(zipDir, cPid);
		    } catch (Exception e) {
			logger.warn(e.getMessage());
		    }
		}

	    }
	    File cFile = new File(dir.getAbsolutePath() + File.separator
		    + "content.zip");
	    logger.debug("I will zip now! " + zipDir.getAbsolutePath() + " to "
		    + cFile.getAbsolutePath());
	    zip(zipDir, cFile);

	} catch (Exception e) {
	    logger.error(e.getMessage());
	}

    }

    private void downloadRelatedObject(File dir, String pid, String relation) {
	try {
	    URL url = new URL(getServer() + "get/" + pid + "/RELS-EXT");
	    String data = null;
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(url.openStream(), writer);
	    data = writer.toString();

	    Element root = XmlUtils.getDocument(data);
	    NodeList constituents = root.getElementsByTagName(relation);
	    for (int i = 0; i < constituents.getLength(); i++) {
		try {
		    Element c = (Element) constituents.item(i);
		    String cPid = c.getAttribute("rdf:resource").replace(
			    "info:fedora/", "");

		    logger.debug(pid + " " + relation + " " + cPid);
		    // if (!cPid.contains("oai") && !cPid.contains("temp")
		    // && !pid.contains("oai") && !pid.contains("temp"))
		    logger.info("DOWNLOAD-GRAPH: \"" + pid + "\"->\"" + cPid
			    + "\" [label=\"" + relation + "\"]");

		    if (cPid.contains("temp")) {
			logger.debug(cPid + " skip temporary object.");

		    } else {
			File cDir = new File(dir.getAbsolutePath()
				+ File.separator

				+ URLEncoder.encode(cPid, "utf-8"));

			downloadObject(cDir, cPid);
		    }
		} catch (Exception e) {
		    logger.debug(e.getMessage());
		}

	    }
	} catch (Exception e) {
	    logger.error(e.getMessage());
	}
    }

    private void downloadStreams(File dir, String pid) {
	try {
	    URL url = new URL(getServer() + "listDatastreams/" + pid
		    + "?xml=true");
	    String data = null;
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(url.openStream(), writer);
	    data = writer.toString();

	    Element root = XmlUtils.getDocument(data);
	    NodeList dss = root.getElementsByTagName("datastream");

	    for (int i = 0; i < dss.getLength(); i++) {
		Element dsel = (Element) dss.item(i);
		String datastreamName = dsel.getAttribute("dsid");
		String fileName = dsel.getAttribute("label");
		String mimeType = dsel.getAttribute("mimeType");

		if (mimeType.contains("xml")) {
		    fileName = datastreamName + ".xml";
		}
		if (mimeType.contains("html")) {
		    fileName = fileName + ".html";
		}

		File dataStreamFile = new File(dir.getAbsolutePath()
			+ File.separator + "" + fileName);
		download(dataStreamFile, getServer() + "get/" + pid + "/"
			+ datastreamName);
	    }

	} catch (MalformedURLException e) {
	    logger.error(e.getMessage());
	} catch (IOException e) {
	    logger.error(e.getMessage());
	}
    }

    /**
     * @param argv
     *            the argument vector must contain exactly one item which points
     *            to a valid property file
     */
    public static void main(String[] argv) {
	if (argv.length != 1) {
	    System.out.println("\nWrong Number of Arguments!");
	    System.out.println("Please specify a config.properties file!");
	    System.out
		    .println("Example: java -jar dtldownloader.jar dtldownloader.properties\n");
	    System.out
		    .println("Example Properties File:\n\tpidreporter.server=http://urania.hbz-nrw.de:1801/edowebOAI/\n\tpidreporter.set=null\n\tpidreporter.harvestFromScratch=true\n\tpidreporter.pidFile=pids.txt\n\tpiddownloader.server=http://klio.hbz-nrw.de:1801\n\tpiddownloader.downloadLocation=/tmp/zbmed");
	    System.exit(1);
	}

	Downloader downloader = new DippDownloader();
	downloader.run(argv[0]);

    }

}
