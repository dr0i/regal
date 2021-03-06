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
package de.nrw.hbz.regal.fedora;

import static de.nrw.hbz.regal.datatypes.Vocabulary.REL_CONTENT_TYPE;
import static de.nrw.hbz.regal.datatypes.Vocabulary.REL_IS_NODE_TYPE;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.INFO_NAMESPACE;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.request.AddDatastream;
import com.yourmediashelf.fedora.client.request.AddRelationship;
import com.yourmediashelf.fedora.client.request.FindObjects;
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.request.ListDatastreams;
import com.yourmediashelf.fedora.client.request.ModifyDatastream;
import com.yourmediashelf.fedora.client.request.PurgeRelationship;
import com.yourmediashelf.fedora.client.request.Upload;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.FindObjectsResponse;
import com.yourmediashelf.fedora.client.response.ListDatastreamsResponse;
import com.yourmediashelf.fedora.client.response.UploadResponse;
import com.yourmediashelf.fedora.generated.access.DatastreamType;

import de.nrw.hbz.regal.datatypes.Link;
import de.nrw.hbz.regal.datatypes.Node;
import de.nrw.hbz.regal.exceptions.ArchiveException;

/**
 * The utils class provides commonly used "low-level-methods" to the
 * FedoraFacade.
 * 
 * @author Jan Schnasse schnasse@hbz-nrw.de
 * 
 */
public class Utils {

    @SuppressWarnings({ "javadoc", "serial" })
    public class NoPidFoundException extends RuntimeException {

	public NoPidFoundException() {
	}

	public NoPidFoundException(String arg0) {
	    super(arg0);
	}

	public NoPidFoundException(Throwable arg0) {
	    super(arg0);
	}

	public NoPidFoundException(String arg0, Throwable arg1) {
	    super(arg0, arg1);
	}

    }

    private String user = null;

    /**
     * @param host
     *            The fedora host.
     * @param user
     *            A valid fedora user.
     */
    public Utils(String host, String user) {
	this.user = user;
    }

    /**
     * Prefixes a pid with FEDORA_INFO_NAMESPACE
     * 
     * @param pid
     *            The pid that must be prefixed
     * @return FEDORA_INFO_NAMESPACE + pid
     */
    public String addUriPrefix(final String pid) {

	if (pid.contains(INFO_NAMESPACE.toString()))
	    return pid;
	String pred = INFO_NAMESPACE.toString() + pid;
	return pred;
    }

    void purgeRelationships(String pid, List<Link> list) {

	for (Link link : list) {
	    System.out.println("PURGE: " + addUriPrefix(pid) + " <"
		    + link.getPredicate() + "> " + link.getObject());
	    try {
		new PurgeRelationship(pid).subject(addUriPrefix(pid))
			.predicate(link.getPredicate())
			.object(link.getObject(), link.isLiteral()).execute();
	    } catch (FedoraClientException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    void addRelationships(String pid, List<Link> links) {

	if (links != null)
	    for (Link link : links) {

		try {
		    new AddRelationship(pid).predicate(link.getPredicate())
			    .object(link.getObject(), link.isLiteral())
			    .execute();
		} catch (Exception e) {
		    try {
			new AddRelationship(pid).predicate(link.getPredicate())
				.object(link.getObject(), true).execute();
		    } catch (Exception e2) {

		    }

		}

	    }

    }

    /**
     * @param pid
     *            The pid of the fedora object.
     * @param datastreamId
     *            The datastream ID of the datastream
     * @return true if the datastream exists, false if not.
     */
    public boolean dataStreamExists(String pid, String datastreamId) {
	try {

	    ListDatastreamsResponse response = new ListDatastreams(pid)
		    .execute();

	    for (DatastreamType ds : response.getDatastreams()) {
		if (ds.getDsid().compareTo(datastreamId) == 0)
		    return true;
	    }

	} catch (FedoraClientException e) {
	    return false;
	}
	return false;
    }

    String removeUriPrefix(String pred) {
	String pid = pred.replace(INFO_NAMESPACE, "");

	return pid;
    }

    void createRelsExt(Node node) {

	String pid = node.getPID();

	// IF DATASTREAM ! EXISTS
	// CREATE DATASTREAM
	// ADD RELATIONS

	if (!dataStreamExists(pid, "RELS-EXT")) {
	    System.out.println("PID " + pid + " doesn't exist, create new");
	    createFedoraXmlForRelsExt(pid);

	}

	List<Link> links = node.getRelsExt();
	createRelsExt(pid, links);

    }

    boolean nodeExists(String pid) {
	try {

	    FindObjectsResponse response = new FindObjects().terms(pid).pid()
		    .execute();
	    for (String p : response.getPids()) {
		if (p.compareTo(pid) == 0)
		    return true;
	    }

	} catch (Exception e) {
	    return false;
	}
	return false;
    }

    /**
     * Description: Allows to ingest a local file as managed datastream of the
     * object </p>
     * 
     * @param pid
     *            of the object
     * @param datastreamID
     *            to identify the datastream
     * @param fileLocation
     *            to specify the managed content of the datastream
     * @param mimeType
     *            of the uploaded file
     */
    void createManagedStream(Node node) {

	try {

	    File file = new File(node.getUploadFile());
	    UploadResponse response = new Upload(file).execute();

	    String location = response.getUploadLocation();
	    String label = node.getFileLabel();
	    if (label == null || label.isEmpty())
		label = file.getName();
	    new AddDatastream(node.getPID(), "data").versionable(true)
		    .dsLabel(label).dsState("A").controlGroup("M")
		    .mimeType(node.getMimeType()).dsLocation(location)
		    .execute();

	} catch (Exception e) {
	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	}
    }

    void createMetadataStream(Node node) {

	try {

	    Upload request = new Upload(new File(node.getMetadataFile()));
	    UploadResponse response = request.execute();
	    String location = response.getUploadLocation();

	    new AddDatastream(node.getPID(), "metadata").versionable(true)
		    .dsState("A").dsLabel("n-triple rdf metadata")
		    .controlGroup("M").mimeType(node.getMimeType())
		    .dsLocation(location).execute();
	} catch (Exception e) {
	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	}
    }

    void updateManagedStream(Node node) {

	try {
	    File file = new File(node.getUploadFile());
	    Upload request = new Upload(file);
	    UploadResponse response = request.execute();
	    String location = response.getUploadLocation();

	    if (dataStreamExists(node.getPID(), "data")) {
		new ModifyDatastream(node.getPID(), "data").versionable(true)
			.dsState("A").dsLabel(file.getName()).controlGroup("M")
			.mimeType(node.getMimeType()).dsLocation(location)
			.execute();
	    } else {
		new AddDatastream(node.getPID(), "data").versionable(true)
			.dsState("A").dsLabel(node.getFileLabel())
			.mimeType(node.getMimeType()).dsLocation(location)
			.controlGroup("M").execute();
	    }
	    // node.setDataUrl(new URL(host + "/objects/" + node.getPID()
	    // + "/datastreams/" + node.getFileName() + "/content"));
	    // Link link = new Link();
	    // link.setObject(node.getFileName(), true);
	    // link.setPredicate(HAS_DATASTREAM);
	    // node.addRelation(link);
	    //
	    // link = new Link();
	    // link.setObject(node.getMimeType(), true);
	    // link.setPredicate(DATASTREAM_MIME);
	    // node.addRelation(link);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	}
    }

    void updateMetadataStream(Node node) {
	try {

	    Upload request = new Upload(new File(node.getMetadataFile()));
	    UploadResponse response = request.execute();
	    String location = response.getUploadLocation();

	    if (dataStreamExists(node.getPID(), "metadata")) {
		new ModifyDatastream(node.getPID(), "metadata")
			.versionable(true).dsLabel("n-triple rdf metadata")
			.dsState("A").controlGroup("M").mimeType("text/plain")
			.dsLocation(location).execute();
	    } else {
		new AddDatastream(node.getPID(), "metadata").versionable(true)
			.dsState("A").dsLabel("n-triple rdf metadata")
			.controlGroup("M").mimeType("text/plain")
			.dsLocation(location).execute();
	    }
	    // node.setDataUrl(new URL(host + "/objects/" + node.getPID()
	    // + "/datastreams/" + newID + "/content"));
	    // Link link = new Link();
	    // link.setObject(node.getMetadataFile(), true);
	    // link.setPredicate(HAS_METADATASTREAM);
	    // node.addRelation(link);
	    //
	    // link = new Link();
	    // link.setObject("text/plain", true);
	    // link.setPredicate(METADATASTREAM_MIME);
	    // node.addRelation(link);
	} catch (Exception e) {
	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	}
    }

    void readFedoraDcToNode(Node node) throws RemoteException,
	    FedoraClientException {

	FedoraResponse response = new GetDatastreamDissemination(node.getPID(),
		"DC").download(true).execute();
	InputStream ds = response.getEntityInputStream();
	readDcToNode(node, ds, "dc");

    }

    /**
     * @param node
     *            dc stream will be added to this node
     * @param ds
     *            stream containing xml dc data
     * @param ns
     *            namespace of the dc
     */
    public void readDcToNode(Node node, InputStream ds, String ns) {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setExpandEntityReferences(false);
	factory.setIgnoringElementContentWhitespace(true);

	try {
	    DocumentBuilder docBuilder = factory.newDocumentBuilder();

	    Document doc = docBuilder.parse(new BufferedInputStream(ds)); // TODO
									  // Correct?
									  // UTF-8?
	    Element root = doc.getDocumentElement();
	    root.normalize();

	    NodeList contributer = root.getElementsByTagName(ns
		    + ":contributer");
	    NodeList coverage = root.getElementsByTagName(ns + ":coverage");
	    NodeList creator = root.getElementsByTagName(ns + ":creator");
	    NodeList date = root.getElementsByTagName(ns + ":date");
	    NodeList description = root.getElementsByTagName(ns
		    + ":description");
	    NodeList format = root.getElementsByTagName(ns + ":format");
	    NodeList identifier = root.getElementsByTagName(ns + ":identifier");
	    NodeList label = root.getElementsByTagName(ns + ":label");
	    NodeList language = root.getElementsByTagName(ns + ":language");
	    NodeList publisher = root.getElementsByTagName(ns + ":publisher");
	    NodeList rights = root.getElementsByTagName(ns + ":rights");
	    NodeList source = root.getElementsByTagName(ns + ":source");
	    NodeList subject = root.getElementsByTagName(ns + ":subject");
	    NodeList title = root.getElementsByTagName(ns + ":title");
	    NodeList type = root.getElementsByTagName(ns + ":type");

	    if (contributer != null && contributer.getLength() != 0) {
		node.setContributer(new Vector<String>());
		for (int i = 0; i < contributer.getLength(); i++) {
		    node.addContributer(transformFromXMLEntity(contributer
			    .item(i).getTextContent().trim()));
		}
	    }
	    if (coverage != null && coverage.getLength() != 0) {
		node.setCoverage(new Vector<String>());
		for (int i = 0; i < coverage.getLength(); i++) {
		    node.addCoverage(transformFromXMLEntity(coverage.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (creator != null && creator.getLength() != 0) {
		node.setCreator(new Vector<String>());
		for (int i = 0; i < creator.getLength(); i++) {
		    node.addCreator(transformFromXMLEntity(creator.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (date != null && date.getLength() != 0) {
		node.setDate(new Vector<String>());
		for (int i = 0; i < date.getLength(); i++) {
		    node.addDate(transformFromXMLEntity(date.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (description != null && description.getLength() != 0) {
		node.setDescription(new Vector<String>());
		for (int i = 0; i < description.getLength(); i++) {
		    node.addDescription(transformFromXMLEntity(description
			    .item(i).getTextContent().trim()));
		}
	    }
	    if (format != null && format.getLength() != 0) {
		node.setFormat(new Vector<String>());
		for (int i = 0; i < format.getLength(); i++) {
		    node.addFormat(transformFromXMLEntity(format.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (identifier != null && identifier.getLength() != 0) {
		node.setIdentifier(new Vector<String>());
		for (int i = 0; i < identifier.getLength(); i++) {
		    node.addIdentifier(transformFromXMLEntity(identifier
			    .item(i).getTextContent().trim()));
		}
	    }
	    if (label != null && label.getLength() != 0) {

		for (int i = 0; i < label.getLength(); i++) {
		    // TODO set oder add
		    node.setLabel(transformFromXMLEntity(label.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (language != null && language.getLength() != 0) {
		node.setLanguage(new Vector<String>());
		for (int i = 0; i < language.getLength(); i++) {
		    node.addLanguage(transformFromXMLEntity(language.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (publisher != null && publisher.getLength() != 0) {
		node.setPublisher(new Vector<String>());
		for (int i = 0; i < publisher.getLength(); i++) {
		    node.addPublisher(transformFromXMLEntity(publisher.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (rights != null && rights.getLength() != 0) {
		node.setRights(new Vector<String>());
		for (int i = 0; i < rights.getLength(); i++) {
		    node.addRights(transformFromXMLEntity(rights.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (source != null && source.getLength() != 0) {
		node.setSource(new Vector<String>());
		for (int i = 0; i < source.getLength(); i++) {
		    node.addSource(transformFromXMLEntity(source.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (subject != null && subject.getLength() != 0) {
		node.setSubject(new Vector<String>());
		for (int i = 0; i < subject.getLength(); i++) {
		    node.addSubject(transformFromXMLEntity(subject.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (title != null && title.getLength() != 0) {
		node.setTitle(new Vector<String>());
		for (int i = 0; i < title.getLength(); i++) {
		    node.addTitle(transformFromXMLEntity(title.item(i)
			    .getTextContent().trim()));
		}
	    }
	    if (type != null && type.getLength() != 0) {
		node.setType(new Vector<String>());
		for (int i = 0; i < type.getLength(); i++) {
		    node.addType(transformFromXMLEntity(type.item(i)
			    .getTextContent().trim()));
		}
	    }

	} catch (ParserConfigurationException e) {

	    throw new ArchiveException("An unknown exception occured.", e);
	} catch (SAXException e) {

	    throw new ArchiveException("An unknown exception occured.", e);
	} catch (IOException e) {

	    throw new ArchiveException("An unknown exception occured.", e);
	}
    }

    void readRelsExt(Node node) throws FedoraClientException {

	try {
	    FedoraResponse response = new GetDatastreamDissemination(
		    node.getPID(), "RELS-EXT").download(true).execute();
	    InputStream ds = response.getEntityInputStream();

	    // MIMETypedStream ds = fedoraAccess.getDatastreamDissemination(
	    // node.getPID(), "RELS-EXT", null);

	    Repository myRepository = new SailRepository(new MemoryStore());
	    myRepository.initialize();

	    RepositoryConnection con = myRepository.getConnection();
	    String baseURI = "";

	    try {
		ValueFactory f = myRepository.getValueFactory();
		URI objectId = f.createURI("info:fedora/" + node.getPID());
		con.add(new BufferedInputStream(ds), baseURI, RDFFormat.RDFXML);
		RepositoryResult<Statement> statements = con.getStatements(
			objectId, null, null, true);

		try {
		    while (statements.hasNext()) {
			Statement st = statements.next();

			URI predUri = st.getPredicate();
			Value objUri = st.getObject();

			Link link = new Link();
			link.setObject(objUri.stringValue(), false);
			link.setPredicate(predUri.stringValue());

			if (link.getPredicate().compareTo(REL_IS_NODE_TYPE) == 0) {
			    node.setType(link.getObject());
			} else if (link.getPredicate().compareTo(
				REL_CONTENT_TYPE) == 0) {
			    node.setContentType(link.getObject());
			}

			String object = link.getObject();
			try {
			    if (object == null)
				throw new URISyntaxException(" ", "Is a Null",
					0);
			    if (object.isEmpty())
				throw new URISyntaxException(" ",
					"Is an Empty String", 0);
			    if (!object.contains(":") && !object.contains("/"))
				throw new URISyntaxException(object,
					"Contains no namespace and no Slash", 0);

			    new java.net.URI(object);

			    link.setLiteral(false);
			    // System.out.println("Is not Literal");
			} catch (URISyntaxException e) {
			    // System.out.println(e.getMessage());
			    // System.out.println("Is Literal");
			}

			node.addRelation(link);

		    }
		} catch (Exception e) {
		    throw new ArchiveException(node.getPID()
			    + " an unknown exception occured.", e);
		}

		finally {
		    statements.close(); // make sure the result object is closed
					// properly
		}

	    } finally {
		con.close();
	    }

	} catch (RepositoryException e) {

	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	} catch (RemoteException e) {

	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	} catch (RDFParseException e) {

	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	} catch (IOException e) {

	    throw new ArchiveException(node.getPID()
		    + " an unknown exception occured.", e);
	}

    }

    void updateDc(Node node) {
	String preamble = ""
		+ "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
		+ "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
		+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
		+ "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"
		+ "http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">";
	String fazit = "</oai_dc:dc>";

	String tagStart = "<dc:";
	String tagEnd = ">";
	String endTagStart = "</dc:";

	StringBuffer update = new StringBuffer();

	List<String> contributer = null;
	List<String> coverage = null;
	List<String> creator = null;
	List<String> date = null;
	List<String> description = null;
	List<String> format = null;
	List<String> identifier = null;
	// String[] label = null;
	List<String> language = null;
	List<String> publisher = null;
	List<String> rights = null;
	List<String> source = null;
	List<String> subject = null;
	List<String> title = null;
	List<String> type = null;

	if ((contributer = node.getContributer()) != null) {
	    for (String str : contributer) {
		String scontributer = tagStart + "contributer" + tagEnd
			+ transformToXMLEntity(str) + endTagStart
			+ "contributer" + tagEnd;
		update.append(scontributer + "\n");
	    }
	}
	if ((coverage = node.getCoverage()) != null) {
	    for (String str : coverage) {
		String scoverage = tagStart + "coverage" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "coverage"
			+ tagEnd;
		update.append(scoverage + "\n");
	    }
	}
	if ((creator = node.getCreator()) != null) {
	    for (String str : creator) {

		String screator = tagStart + "creator" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "creator"
			+ tagEnd;
		update.append(screator + "\n");
	    }
	}
	if ((date = node.getDate()) != null) {
	    for (String str : date) {
		String sdate = tagStart + "date" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "date"

			+ tagEnd;
		update.append(sdate + "\n");
	    }
	}
	if ((description = node.getDescription()) != null) {
	    for (String str : description) {
		String sdescription = tagStart + "description" + tagEnd
			+ transformToXMLEntity(str) + endTagStart
			+ "description" + tagEnd;
		update.append(sdescription + "\n");
	    }
	}
	if ((format = node.getFormat()) != null) {
	    for (String str : format) {
		String sformat = tagStart + "format" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "format"
			+ tagEnd;
		update.append(sformat + "\n");
	    }
	}
	if ((identifier = node.getIdentifier()) != null) {
	    for (String str : identifier) {
		String sidentifier = tagStart + "identifier" + tagEnd
			+ transformToXMLEntity(str) + endTagStart
			+ "identifier" + tagEnd;
		update.append(sidentifier + "\n");
	    }
	}
	/*
	 * if ((label = node.getLabel()) != null) { for (int i = 0; i <
	 * label.length; i++) { String slabel = tagStart + "label" + tagEnd +
	 * label[i] + endTagStart + "label" + tagEnd; update.append(label +
	 * "\n"); } }
	 */
	if ((language = node.getLanguage()) != null) {
	    for (String str : language) {
		String slanguage = tagStart + "language" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "language"
			+ tagEnd;
		update.append(slanguage + "\n");
	    }
	}
	if ((publisher = node.getPublisher()) != null) {
	    for (String str : publisher) {
		String spublisher = tagStart + "publisher" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "publisher"
			+ tagEnd;
		update.append(spublisher + "\n");
	    }
	}
	if ((rights = node.getRights()) != null) {
	    for (String str : rights) {
		String srights = tagStart + "rights" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "rights"
			+ tagEnd;
		update.append(srights + "\n");
	    }
	}
	if ((source = node.getSource()) != null) {
	    for (String str : source) {
		String ssource = tagStart + "source" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "source"
			+ tagEnd;
		update.append(ssource + "\n");
	    }
	}
	if ((subject = node.getSubject()) != null) {
	    for (String str : subject) {
		String ssubject = tagStart + "subject" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "subject"
			+ tagEnd;
		update.append(ssubject + "\n");
	    }
	}
	if ((title = node.getTitle()) != null) {
	    for (String str : title) {
		String stitle = tagStart + "title" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "title"
			+ tagEnd;
		update.append(stitle + "\n");
	    }
	}
	if ((type = node.getType()) != null) {
	    for (String str : type) {
		String stype = tagStart + "type" + tagEnd
			+ transformToXMLEntity(str) + endTagStart + "type"
			+ tagEnd;
		update.append(stype + "\n");
	    }
	}

	try {
	    String result = preamble + update.toString() + fazit;

	    new ModifyDatastream(node.getPID(), "DC").mimeType("text/xml")
		    .formatURI("http://www.openarchives.org/OAI/2.0/oai_dc/")
		    .versionable(true).content(result).execute();

	} catch (FedoraClientException e) {
	    throw new ArchiveException(e.getMessage(), e);
	}
    }

    /**
     * Creates new Rels-Ext datastream in object 'pid' </p>
     * 
     * @param pid
     *            of the object
     * 
     */
    void createFedoraXmlForRelsExt(String pid) {
	// System.out.println("Create new REL-EXT "+pid);
	try {

	    String initialContent = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">"
		    + "    <rdf:Description rdf:about=\"info:fedora/"
		    + pid
		    + "\">" + "    </rdf:Description>" + "</rdf:RDF>";

	    new AddDatastream(pid, "RELS-EXT").mimeType("application/rdf+xml")
		    .formatURI("info:fedora/fedora-system:FedoraRELSExt-1.0")
		    .versionable(true).content(initialContent).execute();

	} catch (Exception e) {
	    throw new ArchiveException(e.getMessage(), e);
	}
    }

    void updateFedoraXmlForRelsExt(String pid, List<Link> statements) {
	// System.out.println("Create new REL-EXT "+pid);
	String initialContent = null;
	try {

	    initialContent = RdfUtils.getFedoraRelsExt(pid, statements);

	    new ModifyDatastream(pid, "RELS-EXT")
		    .mimeType("application/rdf+xml")
		    .formatURI("info:fedora/fedora-system:FedoraRELSExt-1.0")
		    .versionable(true).content(initialContent).execute();

	} catch (Exception e) {
	    throw new ArchiveException(initialContent, e);
	}
    }

    List<String> findPidsSimple(String rdfQuery) {

	try {
	    FindObjectsResponse response = new FindObjects().maxResults(50)
		    .resultFormat("xml").pid().terms(rdfQuery).execute();
	    if (!response.hasNext())
		return response.getPids();
	    List<String> result = response.getPids();
	    while (response.hasNext()) {

		response = new FindObjects().pid()
			.sessionToken(response.getToken()).maxResults(50)
			.resultFormat("xml").execute();
		result.addAll(response.getPids());

	    }

	    return result;
	} catch (FedoraClientException e) {
	    throw new NoPidFoundException(rdfQuery, e);
	}

    }

    String setOwnerToXMLString(String objXML) {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	try {
	    DocumentBuilder docBuilder = factory.newDocumentBuilder();
	    Document doc = docBuilder.parse(new BufferedInputStream(
		    new ByteArrayInputStream(objXML.getBytes())));
	    Element root = doc.getDocumentElement();
	    root.normalize();

	    NodeList properties = root.getElementsByTagName("foxml:property");
	    for (int i = 0; i < properties.getLength(); i++) {
		Element n = (Element) properties.item(i);
		String attribute = n.getAttribute("NAME");

		if (attribute
			.compareTo("info:fedora/fedora-system:def/model#ownerId") == 0) {

		    n.setAttribute("VALUE", user);
		    // String a = n.getAttribute("VALUE");

		    break;
		}
	    }

	    try {
		doc.normalize();
		Source source = new DOMSource(doc);
		StringWriter stringWriter = new StringWriter();
		Result result = new StreamResult(stringWriter);

		TransformerFactory fac = TransformerFactory.newInstance();
		Transformer transformer = fac.newTransformer();
		transformer.transform(source, result);

		return stringWriter.toString();

	    } catch (TransformerConfigurationException e) {
		e.printStackTrace();
	    } catch (TransformerException e) {
		e.printStackTrace();
	    }

	} catch (ParserConfigurationException e) {

	    e.printStackTrace();
	} catch (SAXException e) {

	    e.printStackTrace();
	} catch (IOException e) {

	    e.printStackTrace();
	}

	return null;
    }

    private String transformFromXMLEntity(String textContent) {
	if (textContent == null)
	    return null;
	return textContent.replaceAll("[&]amp;", "&");
    }

    private String transformToXMLEntity(String string) {
	if (string == null)
	    return null;
	final StringBuilder result = new StringBuilder();
	final StringCharacterIterator iterator = new StringCharacterIterator(
		string);
	char character = iterator.current();
	while (character != CharacterIterator.DONE) {
	    if (character == '<') {
		result.append("&lt;");
	    } else if (character == '>') {
		result.append("&gt;");
	    } else if (character == '\"') {
		result.append("&quot;");
	    } else if (character == '\'') {
		result.append("&#039;");
	    } else if (character == '&') {
		result.append("&amp;");
	    } else {

		result.append(character);
	    }
	    character = iterator.next();
	}
	return result.toString();

    }

    private void createRelsExt(String pid, List<Link> links) {
	if (links != null)
	    for (Link curHBZLink : links) {
		if (curHBZLink == null)
		    return;
		// System.out.println(" CREATE: <" + pid + "> <"
		// + curHBZLink.getPredicate() + "> <"
		// + curHBZLink.getObject() + ">");

		try {
		    if (curHBZLink.isLiteral()) {
			// System.out.println("isLiteral");

			new AddRelationship(pid)
				.predicate(curHBZLink.getPredicate())
				.object(curHBZLink.getObject(),
					curHBZLink.isLiteral()).execute();
		    } else {
			// System.out.println("NOT isLiteral");

			new AddRelationship(pid)
				.predicate(curHBZLink.getPredicate())
				.object(addUriPrefix(curHBZLink.getObject()),
					curHBZLink.isLiteral()).execute();

		    }
		} catch (Exception e) {
		    // System.out.println("UPDATE: Could not ingest: <" + pid
		    // + "> <" + curHBZLink.getPredicate() + "> <"
		    // + curHBZLink.getObject() + ">");
		}
	    }
    }

}
