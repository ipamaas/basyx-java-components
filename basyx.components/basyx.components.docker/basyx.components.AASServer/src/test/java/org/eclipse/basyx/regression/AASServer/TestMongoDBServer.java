/*******************************************************************************
 * Copyright (C) 2021 the Eclipse BaSyx Authors
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.basyx.regression.AASServer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.ModelUrn;
import org.eclipse.basyx.components.aas.AASServerComponent;
import org.eclipse.basyx.components.aas.configuration.AASServerBackend;
import org.eclipse.basyx.components.aas.configuration.BaSyxAASServerConfiguration;
import org.eclipse.basyx.components.aas.mongodb.MongoDBAASAPI;
import org.eclipse.basyx.components.aas.mongodb.MongoDBAASAggregator;
import org.eclipse.basyx.components.aas.mongodb.MongoDBSubmodelAPI;
import org.eclipse.basyx.components.configuration.BaSyxContextConfiguration;
import org.eclipse.basyx.components.configuration.BaSyxMongoDBConfiguration;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.api.reference.IReference;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.eclipse.basyx.vab.modelprovider.api.IModelProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests the component using the test suite
 * 
 * @author espen
 *
 */
public class TestMongoDBServer extends AASServerSuite {

	private static final Identifier SM_IDENTIFICATION = new Identifier(IdentifierType.CUSTOM, "MongoDBId");
	private static final String SM_IDSHORT = "MongoDB";
	private static AASServerComponent component;
	private static BaSyxMongoDBConfiguration mongoDBConfig;
	private static BaSyxContextConfiguration contextConfig;
	private static BaSyxAASServerConfiguration aasConfig;

	@Override
	protected String getURL() {
		return component.getURL() + "/shells";
	}

	@BeforeClass
	public static void setUpClass() throws ParserConfigurationException, SAXException, IOException {
		initConfiguration();
		resetMongoDBTestData();
		component = new AASServerComponent(contextConfig, aasConfig, mongoDBConfig);
		component.startComponent();
	}

	private static void resetMongoDBTestData() {
		new MongoDBAASAggregator(mongoDBConfig).reset();
	}

	private static void initConfiguration() {
		mongoDBConfig = new BaSyxMongoDBConfiguration();
		mongoDBConfig.setAASCollection("basyxTestAAS");
		mongoDBConfig.setSubmodelCollection("basyxTestSM");
		contextConfig = new BaSyxContextConfiguration();
		contextConfig.loadFromResource(BaSyxContextConfiguration.DEFAULT_CONFIG_PATH);
		aasConfig = new BaSyxAASServerConfiguration(AASServerBackend.MONGODB, "");
	}

	@Test
	public void testAddSubmodelPersistency() throws Exception {
		createAssetAdministrationShell();
		createSubmodel();

		checkIfSubmodelHasBeenPersisted(SM_IDENTIFICATION);
		checkSubmodelReferencesSize(1);
	}

	@Test
	public void testAggregatorPersistency() throws Exception {
		createAssetAdministrationShell();
		createSubmodel();

		MongoDBAASAggregator aggregator = new MongoDBAASAggregator(mongoDBConfig);
		ISubmodel persistentSM = getSubmodelFromAggregator(aggregator);

		assertEquals(SM_IDSHORT, persistentSM.getIdShort());
	}

	@Test
	public void testDeleteSubmodelPersistency() {
		createAssetAdministrationShell();
		createSubmodel();

		deleteSubmodel();
		checkSubmodelReferencesSize(0);
	}

	private void checkIfSubmodelHasBeenPersisted(Identifier smIdentification) {
		MongoDBSubmodelAPI smAPI = new MongoDBSubmodelAPI(mongoDBConfig, smIdentification.getId());
		ISubmodel persistentSM = smAPI.getSubmodel();
		assertEquals(SM_IDSHORT, persistentSM.getIdShort());
	}

	private void checkSubmodelReferencesSize(int expectedSize) {
		MongoDBAASAPI aasAPI = new MongoDBAASAPI(mongoDBConfig, aasId);
		Collection<IReference> submodelReferences = aasAPI.getAAS().getSubmodelReferences();
		assertEquals(expectedSize, submodelReferences.size());
	}

	@SuppressWarnings("unchecked")
	private ISubmodel getSubmodelFromAggregator(MongoDBAASAggregator aggregator) {
		IModelProvider aasProvider = aggregator.getAASProvider(new ModelUrn(aasId));
		Object smObject = aasProvider.getValue("/aas/submodels/MongoDB/submodel");
		ISubmodel persistentSM = Submodel.createAsFacade((Map<String, Object>) smObject);
		return persistentSM;
	}

	private void createSubmodel() {
		Submodel sm = new Submodel(SM_IDSHORT, SM_IDENTIFICATION);
		manager.createSubmodel(new ModelUrn(aasId), sm);
	}

	private void deleteSubmodel() {
		manager.deleteSubmodel(new ModelUrn(aasId), SM_IDENTIFICATION);
	}

	private void createAssetAdministrationShell() {
		AssetAdministrationShell shell = new AssetAdministrationShell();
		IIdentifier identifier = new ModelUrn(aasId);
		shell.setIdentification(identifier);
		shell.setIdShort("aasIdShort");
		manager.createAAS(shell, getURL());
	}

	@AfterClass
	public static void tearDownClass() {
		component.stopComponent();
	}
}
