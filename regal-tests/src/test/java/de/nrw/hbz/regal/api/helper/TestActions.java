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
package de.nrw.hbz.regal.api.helper;

import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.nrw.hbz.regal.api.CreateObjectBean;
import de.nrw.hbz.regal.api.DCBeanAnnotated;
import de.nrw.hbz.regal.datatypes.Link;
import de.nrw.hbz.regal.datatypes.Node;
import de.nrw.hbz.regal.fedora.CopyUtils;

/**
 * 
 * @author Jan Schnasse schnasse@hbz-nrw.de
 * 
 */
@SuppressWarnings("javadoc")
public class TestActions {

    Actions actions;

    @Before
    public void setUp() throws IOException {
	actions = new Actions();
	cleanUp();
    }

    private void cleanUp() {
	actions.deleteNamespace("test");
	actions.deleteNamespace("testCM");
    }

    @Test
    public void testFindByType() throws IOException {
	createTestObject("123");
	int count = 10;
	for (String result : actions
		.findByType(ObjectType.monograph.toString())) {
	    if (count <= 0)
		break;
	    count--;
	    Node node = actions.readNode(result);
	    String type = node.getContentType();

	    if (type == null || type.isEmpty())
		Assert.fail();
	    else if (ObjectType.monograph.toString().compareTo(type) != 0) {
		Assert.fail();
	    }
	}
    }

    public void createTestObject(String pid) throws IOException {
	actions.contentModelsInit("test");
	CreateObjectBean input = new CreateObjectBean();
	input.setType("monograph");
	actions.createResource(input, pid, "test", null);
	DCBeanAnnotated dc = new DCBeanAnnotated();
	dc.addIdentifier("HT015702837");
	actions.updateDC("test:" + pid, dc);
	actions.updateData("test:" + pid, Thread.currentThread()
		.getContextClassLoader().getResourceAsStream("test.pdf"),
		"application/pdf", "TestFile");

	// TODO Make tests for each action. Add Assertions
	Link link = new Link();
	link.setPredicate("http://hbz-nrw.de/regal#testlink");
	link.setObject("aLiteral", true);
	actions.addLink("test:" + pid, link);
	link.setObject("http://hbz-nrw.de/regal#aUri", false);
	actions.addLink("test:" + pid, link);
	String result = actions.lobidify("test:" + pid);
	System.out.println(result);
	actions.makeOAISet("test:" + pid);
	actions.pdfbox(actions.readNode("test:" + pid));

	actions.updateMetadata("test:" + pid, CopyUtils.copyToString(
		Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("test.nt"), "utf-8"));
	actions.index(pid, "test");

	actions.outdex("test:" + pid);
	actions.readDC("test:" + pid);
    }

    @Test(expected = HttpArchiveException.class)
    public void deleteMetadata() throws IOException {
	createTestObject("123");
	actions.readMetadata("test:123");
	actions.deleteMetadata("test:123");
	actions.deleteData("test:123");
	actions.readMetadata("test:123");
    }

    @Test
    public void epicur() throws IOException, URISyntaxException {
	createTestObject("123");

	String assumed = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<epicur xmlns=\"urn:nbn:de:1111-2004033116\" xmlns:xsi=\"http://www.w3.com/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd\">\n"
		+ "\t<administrative_data>\n"
		+ "\t\t<delivery>\n"
		+ "\t\t\t<update_status type=\""
		+ "urn_new"
		+ "\"></update_status>\n"
		+ "\t\t\t<transfer type=\"oai\"></transfer>\n"
		+ "\t\t</delivery>\n"
		+ "\t</administrative_data>\n"
		+ "<record>\n"
		+ "\t<identifier scheme=\"urn:nbn:de\">"
		+ "urn:nbn:de:test-1231"
		+ "</identifier>\n"
		+ "\t<resource>\n"
		+ "\t\t<identifier origin=\"original\" role=\"primary\" scheme=\"url\" type=\"frontpage\">"
		+ actions.getServer()
		+ "/resource/test:123"
		+ "</identifier>\n"
		+ "\t\t<format scheme=\"imt\">text/html</format>\n"
		+ "\t</resource>" + "</record>\n" + "</epicur> ";
	Services services = actions.getServices();
	Assert.assertEquals("urn:nbn:de:test-1231",
		services.generateUrn("123", "test"));
	actions.addUrn("123", "test");
	String response = actions.epicur("123", "test");
	System.out.println(response);
	Assert.assertEquals(assumed, response);
	response = actions.readMetadata("test:123");
	System.out.println(response);

    }

    @Test
    public void pdfa() throws IOException, URISyntaxException {
	createTestObject("123");
	Node node = actions.readNode("test:123");
	String response = actions.pdfa(node);
	Assert.assertNotNull(response);
	System.out.println(response);
    }

    @Test
    public void pdfbox() throws IOException, URISyntaxException {
	createTestObject("123");
	Node node = actions.readNode("test:123");
	String response = actions.pdfbox(node);
	Assert.assertNotNull(response);
	Assert.assertEquals("test\n", response);
	System.out.println(response);
    }

    @Test
    public void itext() throws IOException, URISyntaxException {
	createTestObject("123");
	Node node = actions.readNode("test:123");
	String response = actions.itext(node);
	Assert.assertNotNull(response);
	Assert.assertEquals("test", response);
	System.out.println(response);
    }

    @Test
    public void html() throws IOException {
	createTestObject("123");
	String str = actions.getReM("test:123", "text/html");
	System.out.println(str);
    }

    @After
    public void tearDown() throws IOException {
	cleanUp();
    }
}
