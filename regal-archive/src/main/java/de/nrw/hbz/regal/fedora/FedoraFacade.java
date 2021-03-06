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
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.CM_CONTENTMODEL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_COMPOSITE_MODEL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_COMPOSITE_MODEL_URI;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_INPUTSPEC;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_INPUTSPEC_URI;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_METHODMAP;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_METHODMAP_URI;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_METHODMAP_WSDL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_METHODMAP_WSDL_URI;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_WSDL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.DS_WSDL_URI;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.HAS_PART;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.IS_PART_OF;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.REL_HAS_MODEL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.REL_HAS_SERVICE;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.REL_IS_CONTRACTOR_OF;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.REL_IS_DEPLOYMENT_OF;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.SDEF_CONTENTMODEL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.SDEP_CONTENTMODEL;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.SIMPLE;
import static de.nrw.hbz.regal.fedora.FedoraVocabulary.SPO;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.client.request.AddDatastream;
import com.yourmediashelf.fedora.client.request.FedoraRequest;
import com.yourmediashelf.fedora.client.request.GetDatastream;
import com.yourmediashelf.fedora.client.request.GetNextPID;
import com.yourmediashelf.fedora.client.request.GetObjectProfile;
import com.yourmediashelf.fedora.client.request.Ingest;
import com.yourmediashelf.fedora.client.request.ListDatastreams;
import com.yourmediashelf.fedora.client.request.ModifyDatastream;
import com.yourmediashelf.fedora.client.request.PurgeObject;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.GetDatastreamResponse;
import com.yourmediashelf.fedora.client.response.GetNextPIDResponse;
import com.yourmediashelf.fedora.client.response.GetObjectProfileResponse;
import com.yourmediashelf.fedora.client.response.ListDatastreamsResponse;
import com.yourmediashelf.fedora.generated.access.DatastreamType;
import com.yourmediashelf.fedora.generated.management.PidList;

import de.nrw.hbz.regal.datatypes.ContentModel;
import de.nrw.hbz.regal.datatypes.Link;
import de.nrw.hbz.regal.datatypes.Node;
import de.nrw.hbz.regal.exceptions.ArchiveException;

/**
 * The FedoraFacade implements all Fedora-Calls as a singleton
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 */
class FedoraFacade implements FedoraInterface {

    private class DeleteException extends ArchiveException {

	private static final long serialVersionUID = -7879667636793687166L;

	public DeleteException(final String message, final Throwable cause) {
	    super(message, cause);
	}

    }

    private class ReadNodeException extends ArchiveException {

	private static final long serialVersionUID = 7338818611992590876L;

	public ReadNodeException(final String message, final Throwable cause) {
	    super(message, cause);
	}

    }

    private class InitializeFedoraFacadeException extends ArchiveException {

	private static final long serialVersionUID = 5357635794214927895L;

	public InitializeFedoraFacadeException(final String message,
		final Throwable cause) {
	    super(message, cause);
	}

	public InitializeFedoraFacadeException(final Throwable cause) {
	    super(cause);
	}

    }

    public class XmlException extends ArchiveException {

	private static final long serialVersionUID = -4955991522087336862L;

	public XmlException(final String message, final Throwable cause) {
	    super(message, cause);
	}
    }

    public class UpdateContentModel extends ArchiveException {

	private static final long serialVersionUID = 1794883693210840141L;

	public UpdateContentModel(final String message, final Throwable cause) {
	    super(message, cause);
	}
    }

    public class DeleteDatastreamException extends ArchiveException {

	private static final long serialVersionUID = 128120359698836741L;

	public DeleteDatastreamException(final String message,
		final Throwable cause) {
	    super(message, cause);
	}
    }

    public class GetPidException extends ArchiveException {

	private static final long serialVersionUID = 5316657644921457520L;

	public GetPidException(final String message, final Throwable cause) {
	    super(message, cause);
	}
    }

    public class CreateNodeException extends ArchiveException {

	private static final long serialVersionUID = 8569995140758544941L;

	public CreateNodeException(final String message, final Throwable cause) {
	    super(message, cause);
	}

	public CreateNodeException(final Throwable cause) {
	    super(cause);
	}

    }

    public class SearchException extends ArchiveException {

	private static final long serialVersionUID = -276889477323963368L;

	public SearchException(final String message, final Throwable cause) {
	    super(message, cause);
	}
    }

    public class NodeNotFoundException extends ArchiveException {

	private static final long serialVersionUID = 8851350561350951329L;

	public NodeNotFoundException(String message, Throwable cause) {
	    super(message, cause);
	}

	public NodeNotFoundException(String message) {
	    super(message);
	}

    }

    final static Logger logger = LoggerFactory.getLogger(FedoraFacade.class);

    static FedoraFacade me = null;
    Utils utils = null;

    ContentModelBuilder cmBuilder = new ContentModelBuilder();

    /**
     * @param host
     *            The url of the fedora web endpoint
     * @param aUser
     *            A valid fedora user
     * @param aPassword
     *            The password of the fedora user
     */
    private FedoraFacade(String host, String aUser, String aPassword) {
	utils = new Utils(host, aUser);
	try {
	    FedoraCredentials credentials = new FedoraCredentials(host, aUser,
		    aPassword);
	    FedoraClient fedora = new com.yourmediashelf.fedora.client.FedoraClient(
		    credentials);
	    FedoraRequest.setDefaultClient(fedora);

	} catch (MalformedURLException e) {
	    throw new InitializeFedoraFacadeException(e);
	}

    }

    /**
     * @param host
     *            The url of the fedora web endpoint
     * @param aUser
     *            A valid fedora user
     * @param aPassword
     *            The password of the fedora user
     * @return a instance of FedoraFacade singleton
     */
    public static FedoraFacade getInstance(String host, String aUser,
	    String aPassword) {
	if (me == null)
	    return new FedoraFacade(host, aUser, aPassword);
	else
	    return me;
    }

    @Override
    public void createNode(Node node) {

	try {

	    new Ingest(node.getPID()).label(node.getLabel()).execute();

	    utils.updateDc(node);
	    createContentModels(node);

	    if (node.getUploadFile() != null) {
		utils.createManagedStream(node);

	    }
	    if (node.getMetadataFile() != null) {
		utils.createMetadataStream(node);

	    }

	    Link link = new Link();
	    link.setObject(node.getContentType(), true);
	    link.setPredicate(REL_CONTENT_TYPE);
	    node.addRelation(link);

	    link = new Link();
	    link.setObject(node.getNodeType(), true);
	    link.setPredicate(REL_IS_NODE_TYPE);
	    node.addRelation(link);

	    utils.createRelsExt(node);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new CreateNodeException(e);
	}

    }

    @Override
    public Node createNode(Node parent, Node node) {
	String pid = node.getPID();
	if (nodeExists(pid)) {
	    throw new ArchiveException(pid);
	}
	String namespace = parent.getNamespace();// FedoraFacade.pred2pid(parent.getNamespace());
	if (pid == null) {
	    pid = getPid(namespace);
	    node.setPID(pid);
	    node.setNamespace(namespace);
	}
	if (!nodeExists(pid)) {
	    node.setNamespace(namespace);
	    createNode(node);
	}
	node = readNode(node.getPID());
	// Parent to node
	Link meToNode = new Link();
	meToNode.setPredicate(FedoraVocabulary.HAS_PART);
	meToNode.setObject(addUriPrefix(node.getPID()), false);
	parent.addRelation(meToNode);
	Link nodeToMe = new Link();
	nodeToMe.setPredicate(FedoraVocabulary.IS_PART_OF);
	nodeToMe.setObject(addUriPrefix(parent.getPID()), false);
	node.addRelation(nodeToMe);
	updateNode(node);
	updateNode(parent);
	return node;
    }

    @Override
    public Node createRootObject(String namespace) {
	Node rootObject = null;
	String pid = getPid(namespace);
	rootObject = new Node();
	rootObject.setPID(pid);
	rootObject.setLabel("Default Object");
	rootObject.setNamespace(namespace);
	createNode(rootObject);
	return rootObject;
    }

    @Override
    public Node readNode(String pid) {
	if (!nodeExists(pid))
	    throw new NodeNotFoundException(pid);

	Node node = new Node();
	node.setPID(pid);
	node.setNamespace(pid.substring(0, pid.indexOf(':')));

	try {
	    utils.readFedoraDcToNode(node);
	    utils.readRelsExt(node);
	    // TODO Fix me
	    // readContentModels(node);
	    GetObjectProfileResponse prof = new GetObjectProfile(pid).execute();
	    node.setLabel(prof.getLabel());
	    node.setLastModified(prof.getLastModifiedDate());

	} catch (FedoraClientException e) {
	    throw new ReadNodeException(pid, e);
	} catch (RemoteException e) {
	    throw new ReadNodeException(pid, e);
	}

	try {
	    GetDatastreamResponse response = new GetDatastream(pid, "data")
		    .execute();
	    node.setMimeType(response.getDatastreamProfile().getDsMIME());
	    node.setFileLabel(response.getDatastreamProfile().getDsLabel());
	} catch (FedoraClientException e) {
	    logger.debug("Missed mimetype and label", e);
	}

	return node;
    }

    @Override
    public void updateNode(Node node) {
	utils.updateDc(node);
	// updateContentModels(node);

	if (node.getUploadFile() != null) {
	    utils.updateManagedStream(node);
	}

	if (node.getMetadataFile() != null) {
	    utils.updateMetadataStream(node);
	}
	updateRelsExt(node);

    }

    @Override
    public List<String> findPids(String rdfQuery, String queryFormat) {

	if (queryFormat.compareTo(FedoraVocabulary.SIMPLE) == 0) {

	    return utils.findPidsSimple(rdfQuery);
	}

	else {
	    return findPidsRdf(rdfQuery, queryFormat);
	}

    }

    @Override
    public String getPid(String namespace) {
	try {
	    GetNextPIDResponse response = new GetNextPID().namespace(namespace)
		    .execute();
	    return response.getPid();
	} catch (FedoraClientException e) {
	    throw new GetPidException(namespace, e);
	}
    }

    @Override
    public String[] getPids(String namespace, int number)

    {
	try {
	    GetNextPIDResponse response = new GetNextPID().namespace(namespace)
		    .numPIDs(number).execute();
	    PidList list = response.getPids();
	    String[] arr = new String[list.getPid().size()];
	    list.getPid().toArray(arr);
	    return arr;
	} catch (FedoraClientException e) {
	    throw new GetPidException(namespace, e);
	}
    }

    @Override
    public void deleteNode(String rootPID) {
	try {
	    unlinkParent(rootPID);
	    new PurgeObject(rootPID).execute();
	} catch (FedoraClientException e) {

	    throw new DeleteException(rootPID, e);
	}
    }

    @Override
    public void deleteDatastream(String pid, String datastream) {

	try {
	    new ModifyDatastream(pid, datastream).dsState("D").execute();
	} catch (FedoraClientException e) {
	    throw new DeleteDatastreamException(pid, e);
	}

    }

    @Override
    public boolean nodeExists(String pid) {
	return utils.nodeExists(pid);
    }

    @Override
    public void updateContentModel(ContentModel cm) {
	if (nodeExists(cm.getContentModelPID()))
	    deleteNode(cm.getContentModelPID());
	if (nodeExists(cm.getServiceDefinitionPID()))
	    deleteNode(cm.getServiceDefinitionPID());
	if (nodeExists(cm.getServiceDeploymentPID()))
	    deleteNode(cm.getServiceDeploymentPID());
	try {
	    createContentModel(cm);
	} catch (UnsupportedEncodingException e) {
	    throw new UpdateContentModel(cm.toString(), e);
	} catch (FedoraClientException e) {
	    throw new UpdateContentModel(cm.toString(), e);
	}
    }

    @Override
    public InputStream findTriples(String query, String queryFormat,
	    String outputformat) {
	try {
	    FedoraResponse response = new RiSearch(query).format(outputformat)
		    .lang(queryFormat).type("triples").execute();
	    return response.getEntityInputStream();
	} catch (Exception e) {
	    throw new SearchException(query, e);
	}
    }

    @Override
    public String removeUriPrefix(String pred) {
	return utils.removeUriPrefix(pred);
    }

    @Override
    public String addUriPrefix(String pid) {

	return utils.addUriPrefix(pid);
    }

    @Override
    public List<String> findNodes(String searchTerm) {
	return findPids(searchTerm, SIMPLE);
    }

    @Override
    public void readDcToNode(Node node, InputStream in, String dcNamespace) {
	utils.readDcToNode(node, in, dcNamespace);
    }

    @Override
    public String deleteComplexObject(String rootPID) {
	if (!nodeExists(rootPID)) {
	    throw new NodeNotFoundException(rootPID);
	}
	// logger.info("deleteObject");

	deleteNode(rootPID);

	// Find all children
	List<String> pids = null;
	pids = findPids("* <" + IS_PART_OF + "> <" + rootPID + ">", SPO);
	// Delete all children
	if (pids != null)
	    for (String pid : pids) {
		Node node = readNode(pid);
		deleteComplexObject(node.getPID());
	    }
	return rootPID;
    }

    @Override
    public String getNodeParent(Node node) {
	List<Link> links = node.getRelsExt();
	for (Link link : links) {
	    if (link.getPredicate().compareTo(IS_PART_OF) == 0) {
		return link.getObject();
	    }
	}
	return null;
    }

    void unlinkParent(String pid) {
	try {
	    Node node = readNode(pid);
	    Node parent = readNode(getNodeParent(node));
	    parent.removeRelation(HAS_PART, node.getPID());
	    updateNode(parent);
	} catch (NodeNotFoundException e) {
	    // Nothing to do
	    logger.debug(pid + " has no parent!");
	} catch (ReadNodeException e) {
	    // Nothing to do
	    logger.debug(pid + " has no parent!");
	}
    }

    @Override
    public void unlinkParent(Node node) {
	try {
	    Node parent = readNode(getNodeParent(node));
	    parent.removeRelation(HAS_PART, node.getPID());
	    updateNode(parent);
	} catch (NodeNotFoundException e) {
	    // Nothing to do
	    // logger.debug(node.getPID() + " has no parent!");
	}
    }

    @Override
    public void linkToParent(Node node, String parentPid) {
	node.removeRelations(IS_PART_OF);
	Link link = new Link();
	link.setPredicate(IS_PART_OF);
	link.setObject(parentPid, false);
	node.addRelation(link);
	updateNode(node);
    }

    @Override
    public void linkParentToNode(String parentPid, String pid) {
	try {
	    Node parent = readNode(parentPid);
	    Link link = new Link();
	    link.setPredicate(HAS_PART);
	    link.setObject(pid, false);
	    parent.addRelation(link);
	    updateNode(parent);
	} catch (NodeNotFoundException e) {
	    // Nothing to do
	    // logger.debug(pid +
	    // " has no parent! ParentPid: "+parentPid+" is not a valid pid.");
	}
    }

    @Override
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

    private void updateRelsExt(Node node) {
	String pid = node.getPID();
	String type = node.getContentType();

	if (!dataStreamExists(pid, "RELS-EXT")) {
	    utils.createFedoraXmlForRelsExt(pid);
	}

	Link link = new Link();
	link.setPredicate(REL_CONTENT_TYPE);
	link.setObject(type, true);
	node.addRelation(link);

	link = new Link();
	link.setObject(node.getNodeType(), true);
	link.setPredicate(REL_IS_NODE_TYPE);
	node.addRelation(link);

	utils.updateFedoraXmlForRelsExt(pid, node.getRelsExt());
    }

    /*
     * Mit Letzter Zeile --------After Purge Local------------------- test:123
     * <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:MonographObjectModel test:123
     * <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:pdfObjectModel test:123
     * <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:headObjectModel test:123
     * <info:fedora/fedora-system:def/relations-external#isPartOf> test:234
     * --------------------------- --------After Purge Remote-------------------
     * test:123 <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:MonographObjectModel test:123
     * <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:pdfObjectModel test:123
     * <info:fedora/fedora-system:def/model#hasModel>
     * info:fedora/testCM:headObjectModel test:123
     * <info:hbz/hbz-ingest:def/model#contentType> monograph
     * ---------------------------
     */
    private List<String> findPidsRdf(String rdfQuery, String queryFormat) {
	InputStream stream = findTriples(rdfQuery, FedoraVocabulary.SPO,
		FedoraVocabulary.N3);

	List<String> resultVector = new Vector<String>();
	RepositoryConnection con = null;
	Repository myRepository = new SailRepository(new MemoryStore());

	try {

	    myRepository.initialize();

	    con = myRepository.getConnection();
	    String baseURI = "";

	    con.add(stream, baseURI, RDFFormat.N3);

	    RepositoryResult<Statement> statements = con.getStatements(null,
		    null, null, true);

	    while (statements.hasNext()) {
		Statement st = statements.next();
		String str = removeUriPrefix(st.getSubject().stringValue());

		resultVector.add(str);

	    }
	    return resultVector;

	} catch (RepositoryException e) {

	    throw new RdfException(rdfQuery, e);
	} catch (RDFParseException e) {

	    throw new RdfException(rdfQuery, e);
	} catch (IOException e) {

	    throw new RdfException(rdfQuery, e);
	} finally {
	    if (con != null) {
		try {
		    con.close();
		} catch (RepositoryException e) {
		    throw new RdfException(
			    rdfQuery + ". Can not close stream.", e);
		}
	    }
	}
    }

    private void createContentModels(Node node) {
	List<ContentModel> models = node.getContentModels();
	if (models == null)
	    return;
	for (ContentModel m : models) {
	    createContentModel(m, node);
	}
	// cmBuilder.createFedoraXMLForContentModels(node);
    }

    private void createContentModel(ContentModel hbzNodeContentModel, Node node) {

	try {
	    // If necessary create Model
	    createContentModel(hbzNodeContentModel);
	} catch (Exception e) {

	}
	// Add Model to Object
	Link link = new Link();
	link.setPredicate(REL_HAS_MODEL);
	link.setObject(
		utils.addUriPrefix(hbzNodeContentModel.getContentModelPID()),
		false);
	node.addRelation(link);

    }

    private void createContentModel(ContentModel cm)
	    throws FedoraClientException, UnsupportedEncodingException {
	String foCMPid = cm.getContentModelPID();
	String foSDefPid = cm.getServiceDefinitionPID();
	String foSDepPid = cm.getServiceDeploymentPID();

	new Ingest(foCMPid).label("Content Model").execute();

	new Ingest(foSDefPid).label("ServiceDefinition").execute();

	new Ingest(foSDepPid).label("ServiceDeployment").execute();

	// Add Relations
	Vector<Link> cmHBZLinks = new Vector<Link>();
	Link cmHBZLink1 = new Link();
	cmHBZLink1.setPredicate(REL_HAS_SERVICE);
	cmHBZLink1.setObject(utils.addUriPrefix(foSDefPid), false);

	cmHBZLinks.add(cmHBZLink1);

	Link cmHBZLink2 = new Link();
	cmHBZLink2.setPredicate(REL_HAS_MODEL);
	cmHBZLink2.setObject(utils.addUriPrefix(CM_CONTENTMODEL), false);
	cmHBZLinks.add(cmHBZLink2);

	utils.addRelationships(foCMPid, cmHBZLinks);

	Vector<Link> sDefHBZLinks = new Vector<Link>();
	Link sDefHBZLink = new Link();
	sDefHBZLink.setPredicate(REL_HAS_MODEL);
	sDefHBZLink.setObject(utils.addUriPrefix(SDEF_CONTENTMODEL), false);
	sDefHBZLinks.add(sDefHBZLink);

	utils.addRelationships(foSDefPid, sDefHBZLinks);

	Vector<Link> sDepHBZLinks = new Vector<Link>();
	Link sDepHBZLink1 = new Link();
	sDepHBZLink1.setPredicate(REL_IS_DEPLOYMENT_OF);
	sDepHBZLink1.setObject(utils.addUriPrefix(foSDefPid), false);
	sDepHBZLinks.add(sDepHBZLink1);

	Link sDepHBZLink2 = new Link();
	sDepHBZLink2.setPredicate(REL_IS_CONTRACTOR_OF);
	sDepHBZLink2.setObject(utils.addUriPrefix(foCMPid), false);
	sDepHBZLinks.add(sDepHBZLink2);

	Link sDepHBZLink3 = new Link();
	sDepHBZLink3.setPredicate(REL_HAS_MODEL);
	sDepHBZLink3.setObject(utils.addUriPrefix(SDEP_CONTENTMODEL), false);
	sDepHBZLinks.add(sDepHBZLink3);

	utils.addRelationships(foSDepPid, sDepHBZLinks);

	new AddDatastream(foCMPid, DS_COMPOSITE_MODEL)
		.dsLabel("DS-Composite-Stream").versionable(true)
		.formatURI(DS_COMPOSITE_MODEL_URI).dsState("A")
		.controlGroup("X").mimeType("text/xml")
		.content(cmBuilder.getDsCompositeModel(cm)).execute();

	new AddDatastream(foSDefPid, DS_METHODMAP).dsLabel("Methodmap-Stream")
		.versionable(true).formatURI(DS_METHODMAP_URI).dsState("A")
		.controlGroup("X").mimeType("text/xml")
		.content(cmBuilder.getMethodMap(cm)).execute();

	new AddDatastream(foSDepPid, DS_METHODMAP_WSDL)
		.dsLabel("Methodmap-Stream").versionable(true)
		.formatURI(DS_METHODMAP_WSDL_URI).dsState("A")
		.controlGroup("X").mimeType("text/xml")
		.content(cmBuilder.getMethodMapToWsdl(cm)).execute();

	new AddDatastream(foSDepPid, DS_INPUTSPEC)
		.dsLabel("DSINPUTSPEC-Stream").versionable(true)
		.formatURI(DS_INPUTSPEC_URI).dsState("A").controlGroup("X")
		.mimeType("text/xml").content(cmBuilder.getDSInputSpec())
		.execute();

	new AddDatastream(foSDepPid, DS_WSDL).dsLabel("WSDL-Stream")
		.versionable(true).formatURI(DS_WSDL_URI).dsState("A")
		.controlGroup("X").mimeType("text/xml")
		.content(cmBuilder.getWsdl(cm)).execute();

    }
}