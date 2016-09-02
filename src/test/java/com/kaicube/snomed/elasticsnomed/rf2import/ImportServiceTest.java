package com.kaicube.snomed.elasticsnomed.rf2import;

import com.kaicube.elasticversioncontrol.api.BranchService;
import com.kaicube.elasticversioncontrol.domain.Branch;
import com.kaicube.snomed.elasticsnomed.Config;
import com.kaicube.snomed.elasticsnomed.TestConfig;
import com.kaicube.snomed.elasticsnomed.domain.Concept;
import com.kaicube.snomed.elasticsnomed.domain.Description;
import com.kaicube.snomed.elasticsnomed.services.ConceptService;
import com.kaicube.snomed.elasticsnomed.services.QueryIndexService;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Config.class, TestConfig.class})
public class ImportServiceTest {

	@Autowired
	private ImportService importService;

	@Autowired
	private BranchService branchService;

	@Autowired
	private ConceptService conceptService;

	@Autowired
	private QueryIndexService queryIndexService;

	@Test
	public void testImportFull() throws ReleaseImportException {
		final String branchPath = "MAIN";
		branchService.create(branchPath);
		Assert.assertEquals(1, branchService.findAll().size());

		importService.importFull(getClass().getResource("/MiniCT_INT_GB_20140131").getPath(), branchPath);

		final List<Branch> branches = branchService.findAll();
		Assert.assertEquals(26, branches.size());
		int a = 0;
		Assert.assertEquals("MAIN", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20020131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20020731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20030131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20030731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20040131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20040731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20050131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20050731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20060131", branches.get(a).getFatPath());

		a = 21;
		Assert.assertEquals("MAIN/20120131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20120731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20130131", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20130731", branches.get(a++).getFatPath());
		Assert.assertEquals("MAIN/20140131", branches.get(a).getFatPath());

		String path = "MAIN/20020131";
		Assert.assertEquals(88, conceptService.findAll(path, new PageRequest(0, 10)).getTotalElements());
		Assert.assertNull(conceptService.find("370136006", path));

		path = "MAIN/20020731";
		Assert.assertEquals(89, conceptService.findAll(path, new PageRequest(0, 10)).getTotalElements());
		Assert.assertNotNull(conceptService.find("370136006", path));

		// Test concept's description present and active
		final Concept concept138875005in2002 = conceptService.find("138875005", path);
		Assert.assertNotNull(concept138875005in2002);
		Assert.assertEquals(6, concept138875005in2002.getDescriptions().size());
		final Description description1237157018in2002 = concept138875005in2002.getDescription("1237157018");
		Assert.assertNotNull(description1237157018in2002);
		Assert.assertEquals("SNOMED CT July 2002 Release: 20020731 [R]", description1237157018in2002.getTerm());
		Assert.assertEquals(true, description1237157018in2002.isActive());

		path = "MAIN/20030131";
		Assert.assertEquals(89, conceptService.findAll(path, new PageRequest(0, 10)).getTotalElements());

		// Test concept's description present and inactive
		final Concept concept138875005in2003 = conceptService.find("138875005", path);
		Assert.assertNotNull(concept138875005in2003);
		Assert.assertEquals(7, concept138875005in2003.getDescriptions().size());
		final Description description1237157018in2003 = concept138875005in2003.getDescription("1237157018");
		Assert.assertNotNull(description1237157018in2003);
		Assert.assertEquals("SNOMED CT July 2002 Release: 20020731 [R]", description1237157018in2003.getTerm());
		Assert.assertEquals(false, description1237157018in2003.isActive());

		path = "MAIN/20140131";
		Assert.assertEquals(102, conceptService.findAll(path, new PageRequest(0, 10)).getTotalElements());
		Assert.assertEquals(102, conceptService.findAll("MAIN", new PageRequest(0, 10)).getTotalElements());

		Assert.assertEquals(asSet("250171008, 138875005, 118222006, 246188002"), queryIndexService.retrieveAncestors("131148009", "MAIN/20020131"));
		Assert.assertEquals(asSet("250171008, 138875005, 300577008, 118222006, 404684003"), queryIndexService.retrieveAncestors("131148009", "MAIN/20050131"));
		Assert.assertEquals(asSet("250171008, 138875005, 118222006, 404684003"), queryIndexService.retrieveAncestors("131148009", "MAIN/20060131"));
	}

	@Test
	public void testImportSnapshot() throws ReleaseImportException {
		branchService.create("MAIN");
		final String branchPath = "MAIN/import";
		branchService.create(branchPath);
		importService.importSnapshot(getClass().getResource("/MiniCT_INT_GB_20140131").getPath(), branchPath);

		final Concept conceptBleeding = conceptService.find("131148009", branchPath);
		final Set<Description> descriptions = conceptBleeding.getDescriptions();
		Assert.assertEquals(2, descriptions.size());
		Description description = null;
		for (Description d : descriptions) {
			if (d.getDescriptionId().equals("210860014")) {
				description = d;
			}
		}
		Assert.assertNotNull(description);
		Assert.assertEquals("Bleeding", description.getTerm());
		final Map<String, String> acceptabilityMap = description.getAcceptabilityMapFromLangRefsetMembers();
		Assert.assertEquals(1, acceptabilityMap.size());
		Assert.assertEquals("900000000000548007", acceptabilityMap.get("900000000000508004"));

		Assert.assertEquals(4, conceptBleeding.getRelationships().size());

		final Page<Concept> conceptPage = conceptService.findAll(branchPath, new PageRequest(0, 200));
		Assert.assertEquals(102, conceptPage.getNumberOfElements());
		final List<Concept> concepts = conceptPage.getContent();
		Concept conceptMechanicalAbnormality = null;
		for (Concept concept : concepts) {
			if (concept.getConceptId().equals("131148009")) {
				conceptMechanicalAbnormality = concept;
			}
		}

		Assert.assertNotNull(conceptMechanicalAbnormality);

		Assert.assertEquals(2, conceptMechanicalAbnormality.getDescriptions().size());
		Assert.assertEquals(4, conceptMechanicalAbnormality.getRelationships().size());
	}

	@Before
	@After
	public void tearDown() throws InterruptedException {
		conceptService.deleteAll();
		branchService.deleteAll();
		Thread.sleep(1000L);
	}

	private Set<Long> asSet(String string) {
		Set<Long> longs = new HashSet<>();
		for (String split : string.split(",")) {
			longs.add(Long.parseLong(split.trim()));
		}
		return longs;
	}

}