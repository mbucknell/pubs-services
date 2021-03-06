package gov.usgs.cida.pubs.validation.mp.unique;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import gov.usgs.cida.pubs.dao.mp.MpPublicationContributorDao;
import gov.usgs.cida.pubs.domain.Contributor;
import gov.usgs.cida.pubs.domain.ContributorType;
import gov.usgs.cida.pubs.domain.mp.MpPublicationContributor;
import gov.usgs.cida.pubs.validation.BaseValidatorTest;

@SpringBootTest(webEnvironment=WebEnvironment.NONE,
	classes={LocalValidatorFactoryBean.class, MpPublicationContributor.class})
public class UniqueKeyValidatorForMpPublicationContributorTest extends BaseValidatorTest {

	protected UniqueKeyValidatorForMpPublicationContributor validator;
	protected MpPublicationContributor mpPubContributor;
	protected Contributor<?> contributor;
	protected ContributorType type;

	@MockBean(name="mpPublicationContributorDao")
	protected MpPublicationContributorDao mpPublicationContributorDao;

	@BeforeEach
	public void setUp() throws Exception {
		buildContext();
		validator = new UniqueKeyValidatorForMpPublicationContributor();
		mpPubContributor = new MpPublicationContributor();
		contributor = new Contributor<>();
		type = new ContributorType();

		reset(mpPublicationContributorDao);
	}

	@Test
	public void isValidNPETest() {
		assertTrue(validator.isValid(null, null));
		assertTrue(validator.isValid(null, context));
		assertTrue(validator.isValid(mpPubContributor, null));

		assertTrue(validator.isValid(mpPubContributor, context));

		mpPubContributor.setContributorType(type);
		assertTrue(validator.isValid(mpPubContributor, context));
		type.setId(1);
		assertTrue(validator.isValid(mpPubContributor, context));

		mpPubContributor.setContributor(contributor);
		assertTrue(validator.isValid(mpPubContributor, context));
		contributor.setId(1);
		assertTrue(validator.isValid(mpPubContributor, context));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void isValidTrueTest() {
		when(mpPublicationContributorDao.getByMap(anyMap())).thenReturn(new ArrayList<>(), buildList(), new ArrayList<>());
		mpPubContributor.setContributorType(type);
		mpPubContributor.setContributor(contributor);
		mpPubContributor.setId(1);
		mpPubContributor.setPublicationId(1);
		contributor.setId(1);
		type.setId(1);
		
		//Works with empty list returned
		assertTrue(validator.isValid(mpPubContributor, context));
		verify(mpPublicationContributorDao).getByMap(anyMap());

		//Works with a list returned (contributor has same role on same record)
		assertTrue(validator.isValid(mpPubContributor, context));
		verify(mpPublicationContributorDao, times(2)).getByMap(anyMap());

		//Works with add and no list returned (MpPublicationContributor.getid() is null)
		mpPubContributor.setId("");
		assertTrue(validator.isValid(mpPubContributor, context));
		verify(mpPublicationContributorDao, times(3)).getByMap(anyMap());
		
	}

	@Test
	public void isValidFalseTest() {
		when(mpPublicationContributorDao.getByMap(anyMap())).thenReturn(buildList());
		mpPubContributor.setContributorType(type);
		mpPubContributor.setContributor(contributor);
		mpPubContributor.setPublicationId(1);
		contributor.setId(1);
		type.setId(1);
		
		//Works with add (MpPublicationContributor.getid() is null)
		assertFalse(validator.isValid(mpPubContributor, context));
		verify(mpPublicationContributorDao).getByMap(anyMap());

		//Works with a list returned (contributor already has same role on this pub)
		mpPubContributor.setId(2);
		assertFalse(validator.isValid(mpPubContributor, context));
		verify(mpPublicationContributorDao, times(2)).getByMap(anyMap());
	}

	public static List<MpPublicationContributor> buildList() {
		List<MpPublicationContributor> rtn = new ArrayList<>();
		MpPublicationContributor mpp = new MpPublicationContributor();
		mpp.setId(1);
		mpp.setPublicationId(1);
		mpp.setContributor(new Contributor<>());
		mpp.getContributor().setId(1);
		mpp.setContributorType(new ContributorType());
		mpp.getContributorType().setId(1);
		rtn.add(mpp);
		return rtn;
	}
}
