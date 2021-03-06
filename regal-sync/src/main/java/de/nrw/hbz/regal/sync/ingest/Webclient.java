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
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

import de.nrw.hbz.regal.api.CreateObjectBean;
import de.nrw.hbz.regal.api.DCBeanAnnotated;
import de.nrw.hbz.regal.api.helper.ObjectType;
import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.Stream;
import de.nrw.hbz.regal.sync.extern.StreamType;

/**
 * Webclient collects typical api-calls and make them available in the
 * regal-sync module
 * 
 * @author Jan Schnasse schnasse@hbz-nrw.de
 * 
 */
public class Webclient {
    final static Logger logger = LoggerFactory.getLogger(Webclient.class);

    String namespace = null;
    String endpoint = null;
    String host = null;
    Client webclient = null;

    /**
     * @param namespace
     *            The namespace is used to prefix pids for resources
     * @param user
     *            a valid user to authenticate to the webapi
     * @param password
     *            a password for the webapi
     * @param host
     *            the host of the api. it is assumed that the regal-api is
     *            available under host:8080/api
     */
    public Webclient(String namespace, String user, String password, String host) {
	this.host = host;
	this.namespace = namespace;
	ClientConfig cc = new DefaultClientConfig();
	cc.getClasses().add(MultiPartWriter.class);
	cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
	cc.getFeatures().put(ClientConfig.FEATURE_DISABLE_XML_SECURITY, true);
	cc.getProperties().put(
		DefaultApacheHttpClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE,
		1024);
	webclient = Client.create(cc);
	webclient.addFilter(new HTTPBasicAuthFilter(user, password));
	endpoint = host + ":8080/api";
    }

    /**
     * Metadata performs typical metadata related api-actions like update the dc
     * stream enrich with catalogdata. Add the object to the searchindex and
     * provide it on the oai interface.
     * 
     * @param dtlBean
     *            A DigitalEntity to operate on
     */
    public void autoGenerateMetdata(DigitalEntity dtlBean) {
	try {
	    setIdentifier(dtlBean);

	    lobidify(dtlBean);
	} catch (Exception e) {
	    logger.error(dtlBean.getPid() + " " + e.getMessage(), e);
	}

    }

    private void setIdentifier(DigitalEntity dtlBean) {
	DCBeanAnnotated dc = new DCBeanAnnotated();
	dc.setIdentifier(dtlBean.getIdentifier());
	String pid = namespace + ":" + dtlBean.getPid();
	String resource = endpoint + "/resource/" + pid + "/dc";
	updateDc(resource, dc);
    }

    /**
     * @param dtlBean
     *            provides the entity to the searchengine and to the oai
     *            provider
     */
    public void publish(DigitalEntity dtlBean) {
	try {
	    index(dtlBean);
	} catch (Exception e) {
	    logger.error(dtlBean.getPid() + " " + e.getMessage(), e);
	}
	try {
	    oaiProvide(dtlBean);
	} catch (Exception e) {
	    logger.error(dtlBean.getPid() + " " + e.getMessage(), e);
	}
    }

    /**
     * Metadata performs typical metadata related api-actions like update the dc
     * stream enrich with catalogdata. Add the object to the searchindex and
     * provide it on the oai interface.
     * 
     * @param dtlBean
     *            A DigitalEntity to operate on
     * @param metadata
     *            n-triple metadata to integrate
     */
    public void autoGenerateMetadataMerge(DigitalEntity dtlBean, String metadata) {
	setMetadata(dtlBean, "");
	autoGenerateMetdata(dtlBean);
	String pid = namespace + ":" + dtlBean.getPid();
	String resource = endpoint + "/resource/" + pid;
	String m = "";
	try {
	    logger.debug("Metadata: " + metadata);
	    m = readMetadata(resource + "/metadata", dtlBean);

	} catch (Exception e) {
	    logger.error(dtlBean.getPid() + " " + e.getMessage(), e);
	}
	try {
	    String merge = appendMetadata(m, metadata);
	    logger.debug("MERGE: " + metadata);
	    updateMetadata(resource + "/metadata", merge);
	} catch (Exception e) {
	    logger.error(dtlBean.getPid() + " " + e.getMessage(), e);
	}
    }

    /**
     * Sets the metadata to a resource represented by the passed DigitalEntity.
     * 
     * @param dtlBean
     *            The bean for the object
     * @param metadata
     *            The metadata
     */
    public void setMetadata(DigitalEntity dtlBean, String metadata) {
	String pid = namespace + ":" + dtlBean.getPid();
	String resource = endpoint + "/resource/" + pid;
	try {
	    updateMetadata(resource + "/metadata", metadata);
	} catch (Exception e) {
	    logger.error(pid + " " + e.getMessage(), e);
	}

    }

    private String appendMetadata(String m, String metadata) {
	return m + "\n" + metadata;
    }

    /**
     * @param dtlBean
     *            A DigitalEntity to operate on.
     * @param type
     *            The Object type
     */
    public void createObject(DigitalEntity dtlBean, ObjectType type) {
	String pid = namespace + ":" + dtlBean.getPid();
	String resource = endpoint + "/resource/" + pid;
	String data = resource + "/data";
	createDataResource(dtlBean, type, resource, data, dtlBean);
    }

    private void createDataResource(DigitalEntity dtlBean, ObjectType type,
	    String resource, String data, DigitalEntity fulltextObject) {
	createResource(type, dtlBean);
	updateData(data, fulltextObject);
	updateLabel(resource, dtlBean);
    }

    /**
     * @param type
     *            The ObjectType .
     * @param dtlBean
     *            The DigitalEntity to operate on
     */
    public void createResource(ObjectType type, DigitalEntity dtlBean) {

	String pid = namespace + ":" + dtlBean.getPid();
	String ppid = dtlBean.getParentPid();
	logger.info(pid + " is child of " + dtlBean.getParentPid());
	String parentPid = namespace + ":" + ppid;
	String resourceUrl = endpoint + "/resource/" + pid;
	WebResource resource = webclient.resource(resourceUrl);
	CreateObjectBean input = new CreateObjectBean();
	input.setType(type.toString());
	logger.debug(pid + " type: " + input.getType());
	if (ppid != null && !ppid.isEmpty()) {

	    input.setParentPid(parentPid);

	}

	try {
	    resource.put(input);
	} catch (UniformInterfaceException e) {
	    logger.info(pid + " " + e.getMessage(), e);
	}
    }

    // private void updateDC(String url, DigitalEntity dtlBean) {
    // String pid = namespace + ":" + dtlBean.getPid();
    // WebResource webpageDC = webclient.resource(url);
    //
    // DCBeanAnnotated dc = new DCBeanAnnotated();
    //
    // try {
    //
    // if (dtlBean.getStream(StreamType.MARC).getFile() != null)
    // dc.add(marc2dc(dtlBean));
    // else if (dtlBean.getDc() != null) {
    // dc.add(new DCBeanAnnotated(dtlBean.getDc()));
    // } else {
    // logger.warn(pid
    // + " not able to create dublin core data. No Marc or DC metadata found.");
    // }
    //
    // dc.addDescription(dtlBean.getLabel());
    // webpageDC.put(dc);
    //
    // } catch (UniformInterfaceException e) {
    // logger.info(pid + " " + e.getMessage());
    // } catch (Exception e) {
    // logger.debug(pid + " " + e.getMessage());
    // }
    // }

    private String readMetadata(String url, DigitalEntity dtlBean) {
	WebResource metadataRes = webclient.resource(url);
	return metadataRes.get(String.class);
    }

    private void updateMetadata(String url, String metadata) {
	WebResource metadataRes = webclient.resource(url);
	metadataRes.put(metadata);
    }

    private void updateDc(String url, DCBeanAnnotated dc) {
	WebResource metadataRes = webclient.resource(url);
	metadataRes.put(dc);
    }

    private void updateLabel(String url, DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	WebResource webpageDC = webclient.resource(url + "/dc");

	DCBeanAnnotated dc = new DCBeanAnnotated();

	try {
	    dc.addTitle("Version of: " + pid);
	    dc.addDescription(dtlBean.getLabel());
	    webpageDC.put(dc);

	} catch (UniformInterfaceException e) {
	    logger.info(pid + " " + e.getMessage(), e);
	} catch (Exception e) {
	    logger.debug(pid + " " + e.getMessage(), e);
	}
    }

    private void updateData(String url, DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	WebResource data = webclient.resource(url);

	Stream dataStream = dtlBean.getStream(StreamType.DATA);

	try {
	    logger.info(pid + " Update data: " + dataStream.getMimeType());
	    MultiPart multiPart = new MultiPart();

	    File uploadFile = dataStream.getFile();
	    String uploadFileName = uploadFile.getName();
	    String uploadFileMimeType = dataStream.getMimeType();

	    multiPart.bodyPart(new StreamDataBodyPart("InputStream",
		    new FileInputStream(uploadFile), uploadFileName));
	    multiPart.bodyPart(new BodyPart(uploadFileMimeType,
		    MediaType.TEXT_PLAIN_TYPE));

	    logger.info("Upload: " + dataStream.getFile().getAbsolutePath());
	    multiPart.bodyPart(new BodyPart(uploadFileName.substring(0,
		    uploadFileName.indexOf('.')), MediaType.TEXT_PLAIN_TYPE));
	    data.type("multipart/mixed").post(multiPart);

	} catch (UniformInterfaceException e) {
	    logger.error(pid + " " + e.getMessage(), e);
	} catch (FileNotFoundException e) {
	    logger.error(pid + " " + "FileNotFound "
		    + dataStream.getFile().getAbsolutePath(), e);
	} catch (Exception e) {
	    logger.error(pid + " " + e.getMessage(), e);
	}

    }

    private void lobidify(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	WebResource lobid = webclient.resource(endpoint + "/utils/lobidify/"
		+ namespace + ":" + dtlBean.getPid());
	try

	{
	    lobid.type("text/plain").post();
	} catch (UniformInterfaceException e) {
	    logger.warn(pid + " fetching lobid-data failed", e);
	}
    }

    private void index(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {

	    WebResource index = webclient.resource(endpoint + "/utils/index/"
		    + pid);
	    index.post();
	    logger.info(pid + ": got indexed!");
	} catch (UniformInterfaceException e) {
	    logger.warn(pid + " " + "Not indexed! "
		    + e.getResponse().getEntity(String.class), e);
	} catch (Exception e) {
	    logger.warn(pid + " " + "Not indexed! " + e.getMessage(), e);
	}
    }

    private void oaiProvide(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	WebResource oaiSet = webclient.resource(endpoint + "/utils/makeOaiSet/"
		+ namespace + ":" + dtlBean.getPid());
	try {
	    oaiSet.post();
	} catch (UniformInterfaceException e) {
	    logger.warn(pid + " " + "Not oai provided! " + e.getMessage(), e);
	}
    }

    // private DCBeanAnnotated marc2dc(DigitalEntity dtlBean) {
    // String pid = namespace + ":" + dtlBean.getPid();
    // try {
    // StringWriter str = new StringWriter();
    // TransformerFactory tFactory = TransformerFactory.newInstance();
    // Transformer transformer = tFactory
    // .newTransformer(new StreamSource(ClassLoader
    // .getSystemResourceAsStream("MARC21slim2OAIDC.xsl")));
    // transformer.transform(
    // new StreamSource(dtlBean.getStream(StreamType.MARC)
    // .getFile()), new StreamResult(str));
    // String xmlStr = str.getBuffer().toString();
    // DCBeanAnnotated dc = new DCBeanAnnotated(xmlStr);
    // return dc;
    //
    // } catch (Throwable t) {
    // logger.warn(pid + " " + t.getCause().getMessage());
    // }
    // return null;
    // }

    /**
     * 
     * @param p
     *            A pid to delete
     */
    public void delete(String p) {
	String pid = namespace + ":" + p;

	WebResource delete = webclient.resource(endpoint + "/resource/" + pid);
	try {
	    delete.delete();
	} catch (UniformInterfaceException e) {
	    logger.info(pid + " Can't delete!" + e.getMessage(), e);
	}
    }

}
