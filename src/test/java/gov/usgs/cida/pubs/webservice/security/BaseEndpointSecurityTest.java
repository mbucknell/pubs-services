package gov.usgs.cida.pubs.webservice.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import gov.usgs.cida.pubs.ConfigurationService;
import gov.usgs.cida.pubs.PubsConstantsHelper;
import gov.usgs.cida.pubs.busservice.intfc.IBusService;
import gov.usgs.cida.pubs.busservice.intfc.IMpListPublicationBusService;
import gov.usgs.cida.pubs.busservice.intfc.IMpPublicationBusService;
import gov.usgs.cida.pubs.busservice.intfc.IPublicationBusService;
import gov.usgs.cida.pubs.busservice.intfc.IPwPublicationBusService;
import gov.usgs.cida.pubs.busservice.intfc.ISippProcess;
import gov.usgs.cida.pubs.busservice.intfc.IXmlBusService;
import gov.usgs.cida.pubs.dao.CorporateContributorDaoIT;
import gov.usgs.cida.pubs.dao.PersonContributorDaoIT;
import gov.usgs.cida.pubs.dao.intfc.IDao;
import gov.usgs.cida.pubs.dao.intfc.IMpPublicationDao;
import gov.usgs.cida.pubs.dao.intfc.IPersonContributorDao;
import gov.usgs.cida.pubs.dao.intfc.IPublicationDao;
import gov.usgs.cida.pubs.dao.intfc.IPublishingServiceCenterDao;
import gov.usgs.cida.pubs.dao.intfc.IPwPublicationDao;
import gov.usgs.cida.pubs.dao.mp.MpListDaoIT;
import gov.usgs.cida.pubs.dao.mp.MpPublicationDaoIT;
import gov.usgs.cida.pubs.domain.Contributor;
import gov.usgs.cida.pubs.domain.ContributorType;
import gov.usgs.cida.pubs.domain.CorporateContributor;
import gov.usgs.cida.pubs.domain.CostCenter;
import gov.usgs.cida.pubs.domain.LinkFileType;
import gov.usgs.cida.pubs.domain.LinkType;
import gov.usgs.cida.pubs.domain.OutsideAffiliation;
import gov.usgs.cida.pubs.domain.OutsideContributor;
import gov.usgs.cida.pubs.domain.PersonContributor;
import gov.usgs.cida.pubs.domain.ProcessType;
import gov.usgs.cida.pubs.domain.Publication;
import gov.usgs.cida.pubs.domain.PublicationSeries;
import gov.usgs.cida.pubs.domain.PublicationSubtype;
import gov.usgs.cida.pubs.domain.PublicationType;
import gov.usgs.cida.pubs.domain.PublishingServiceCenter;
import gov.usgs.cida.pubs.domain.UsgsContributor;
import gov.usgs.cida.pubs.domain.mp.MpList;
import gov.usgs.cida.pubs.domain.mp.MpPublication;
import gov.usgs.cida.pubs.domain.pw.PwPublicationTest;
import gov.usgs.cida.pubs.domain.query.PwPublicationFilterParams;
import gov.usgs.cida.pubs.security.AuthorizationTestServer;
import gov.usgs.cida.pubs.security.BaseSecurityTest;
import gov.usgs.cida.pubs.security.UserDetailTestService;
import gov.usgs.cida.pubs.springinit.CustomUserAuthenticationConverter;
import gov.usgs.cida.pubs.springinit.SecurityConfig;
import gov.usgs.cida.pubs.validation.ValidationResults;
import gov.usgs.cida.pubs.webservice.AffliliationMvcService;
import gov.usgs.cida.pubs.webservice.AuthController;
import gov.usgs.cida.pubs.webservice.ContributorMvcService;
import gov.usgs.cida.pubs.webservice.LookupMvcService;
import gov.usgs.cida.pubs.webservice.PublicationSeriesMvcService;
import gov.usgs.cida.pubs.webservice.RedirectController;
import gov.usgs.cida.pubs.webservice.mp.MpListMvcService;
import gov.usgs.cida.pubs.webservice.mp.MpListMvcServiceTest;
import gov.usgs.cida.pubs.webservice.mp.MpListPublicationMvcService;
import gov.usgs.cida.pubs.webservice.mp.MpPublicationMvcService;
import gov.usgs.cida.pubs.webservice.pw.PwPublicationMvcService;
import gov.usgs.cida.pubs.webservice.pw.PwPublicationRssMvcService;

@ContextConfiguration(classes = FreeMarkerAutoConfiguration.class)
@SpringBootTest(webEnvironment=WebEnvironment.MOCK,
	classes={SecurityConfig.class, AuthorizationTestServer.class, AuthController.class, ConfigurationService.class,
		AffliliationMvcService.class, PublicationSeriesMvcService.class, UserDetailTestService.class,
		MpPublicationMvcService.class, PwPublicationMvcService.class, PwPublicationRssMvcService.class,
		ContributorMvcService.class, MpListMvcService.class, MpListPublicationMvcService.class,
		RedirectController.class, LookupMvcService.class, PublicationType.class, PublicationSubtype.class,
		PublicationSeries.class, CostCenter.class, OutsideAffiliation.class, ContributorType.class,
		LinkType.class, LinkFileType.class, PersonContributor.class, CorporateContributor.class,
		PublishingServiceCenter.class, Publication.class, Contributor.class, OutsideContributor.class,
		UsgsContributor.class, CustomUserAuthenticationConverter.class, PwPublicationFilterParams.class})
public abstract class BaseEndpointSecurityTest extends BaseSecurityTest {

	private static final String CONTENT_ID_1_JSON = "{\"id\":\"1\"}";
	private static final String CONTRIBUTOR_ID_1_JSON = "{\"contributorId\":\"1\"}";

	@MockBean(name="affiliationDao")
	protected IDao<?> affiliationDao;
	@MockBean(name="contributorDao")
	protected IDao<Contributor<?>> contributorDao;
	@MockBean(name="contributorTypeDao")
	protected IDao<ContributorType> contributorTypeDao;
	@MockBean(name="corporateContributorDao")
	protected IDao<Contributor<?>> corporateContributorDao;
	@MockBean(name="costCenterDao")
	protected IDao<CostCenter> costCenterDao;
	@MockBean(name="linkFileTypeDao")
	protected IDao<LinkFileType> linkFileTypeDao;
	@MockBean(name="linkTypeDao")
	protected IDao<LinkType> linkTypeDao;
	@MockBean(name="mpPublicationDao")
	protected IMpPublicationDao mpPublicationDao;
	@MockBean(name="outsideAffiliationDao")
	protected IDao<OutsideAffiliation> outsideAffiliationDao;
	@MockBean(name="personContributorDao")
	protected IPersonContributorDao personContributorDao;
	@MockBean(name="publicationSeriesDao")
	protected IDao<PublicationSeries> publicationSeriesDao;
	@MockBean(name="publicationSubtypeDao")
	protected IDao<PublicationSubtype> publicationSubtypeDao;
	@MockBean(name="publicationTypeDao")
	protected IDao<PublicationType> publicationTypeDao;
	@MockBean(name="publishingServiceCenterDao")
	protected IPublishingServiceCenterDao publishingServiceCenterDao;
	@MockBean(name="publicationDao")
	protected IPublicationDao publicationDao;
	@MockBean(name="pwPublicationDao")
	protected IPwPublicationDao pwPublicationDao;
	@MockBean(name="xmlBusService")
	protected IXmlBusService xmlBusService;

	@MockBean(name="costCenterBusService")
	protected IBusService<CostCenter> costCenterBusService;
	@MockBean(name="outsideAffiliationBusService")
	protected IBusService<OutsideAffiliation> outsideAffiliationBusService;
	@MockBean(name="mpPublicationBusService")
	protected IMpPublicationBusService mpPublicationBusService;
	@MockBean(name="publicationBusService")
	protected IPublicationBusService publicationBusService;
	@MockBean(name="pwPublicationBusService")
	protected IPwPublicationBusService pwPublicationBusService;
	@MockBean(name="corporateContributorBusService")
	protected IBusService<CorporateContributor> corporateContributorBusService;
	@MockBean(name="personContributorBusService")
	protected IBusService<PersonContributor<?>> personContributorBusService;
	@MockBean(name="mpListBusService")
	protected IBusService<MpList> mpListBusService;
	@MockBean(name="mpListPublicationBusService")
	protected IMpListPublicationBusService mpListPublicationBusService;
	@MockBean(name="publicationSeriesBusService")
	protected IBusService<PublicationSeries> publicationSeriesBusService;
	@MockBean(name="sippProcess")
	protected ISippProcess sippProcess;

	protected ArgumentCaptor<Class<?>> varargCapture;

	public void mockSetup() throws Exception {
		varargCapture = ArgumentCaptor.forClass(Class.class);

		when(costCenterBusService.getObject(anyInt())).thenReturn(new CostCenter());
		when(costCenterBusService.getObjects(anyMap())).thenReturn(null);
		when(costCenterBusService.createObject(any(CostCenter.class))).thenReturn(new CostCenter());
		when(costCenterBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());
		when(costCenterBusService.updateObject(any(CostCenter.class))).thenReturn(new CostCenter());

		when(outsideAffiliationBusService.getObject(anyInt())).thenReturn(new OutsideAffiliation());
		when(outsideAffiliationBusService.getObjects(anyMap())).thenReturn(null);
		when(outsideAffiliationBusService.createObject(any(OutsideAffiliation.class))).thenReturn(new OutsideAffiliation());
		when(outsideAffiliationBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());
		when(outsideAffiliationBusService.updateObject(any(OutsideAffiliation.class))).thenReturn(new OutsideAffiliation());

		when(publicationSeriesBusService.getObject(anyInt())).thenReturn(new PublicationSeries());
		when(publicationSeriesBusService.getObjects(anyMap())).thenReturn(null);
		when(publicationSeriesBusService.createObject(any(PublicationSeries.class))).thenReturn(new PublicationSeries());
		when(publicationSeriesBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());
		when(publicationSeriesBusService.updateObject(any(PublicationSeries.class))).thenReturn(new PublicationSeries());

		when(pwPublicationBusService.getObjects(any(PwPublicationFilterParams.class))).thenReturn(List.of(PwPublicationTest.buildAPub(1)));
		when(pwPublicationBusService.getByIndexId(anyString())).thenReturn(PwPublicationTest.buildAPub(1));

		when(mpPublicationBusService.getObject(anyInt())).thenReturn(MpPublicationDaoIT.buildAPub(1));
		when(mpPublicationBusService.checkAvailability(anyInt())).thenReturn(null);
		when(mpPublicationBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());
		when(mpPublicationBusService.createObject(any(MpPublication.class))).thenReturn(MpPublicationDaoIT.buildAPub(1));
		when(mpPublicationBusService.publish(anyInt())).thenReturn(new ValidationResults());
		when(mpPublicationBusService.getByIndexId(anyString())).thenReturn(MpPublicationDaoIT.buildAPub(1));
		when(mpPublicationBusService.updateObject(any(MpPublication.class))).thenReturn(MpPublicationDaoIT.buildAPub(1));
		when(mpPublicationBusService.purgePublication(anyInt())).thenReturn(new ValidationResults());

		when(corporateContributorBusService.getObject(anyInt())).thenReturn(CorporateContributorDaoIT.buildACorp(1));
		when(corporateContributorBusService.createObject(any(CorporateContributor.class), varargCapture.capture())).thenReturn(CorporateContributorDaoIT.buildACorp(1));
		when(corporateContributorBusService.updateObject(any(CorporateContributor.class), varargCapture.capture())).thenReturn(CorporateContributorDaoIT.buildACorp(1));

		when(mpListBusService.updateObject(any(MpList.class))).thenReturn(MpListDaoIT.buildMpList(66));
		when(mpListBusService.getObjects(anyMap())).thenReturn(MpListMvcServiceTest.getListOfMpList());
		when(mpListBusService.createObject(any(MpList.class))).thenReturn(MpListDaoIT.buildMpList(66));
		when(mpListBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());

		doReturn(PersonContributorDaoIT.buildAPerson(1, "USGS")).when(personContributorBusService).getObject(anyInt());
		doReturn(PersonContributorDaoIT.buildAPerson(1, "USGS")).when(personContributorBusService).createObject(any(UsgsContributor.class), varargCapture.capture());
		doReturn(PersonContributorDaoIT.buildAPerson(1, "OUTSIDE")).when(personContributorBusService).createObject(any(OutsideContributor.class), varargCapture.capture());
		doReturn(PersonContributorDaoIT.buildAPerson(1, "USGS")).when(personContributorBusService).updateObject(any(UsgsContributor.class), varargCapture.capture());
		doReturn(PersonContributorDaoIT.buildAPerson(1, "OUTSIDE")).when(personContributorBusService).updateObject(any(OutsideContributor.class), varargCapture.capture());
		when(personContributorBusService.deleteObject(anyInt())).thenReturn(new ValidationResults());

		when(affiliationDao.getByMap(anyMap())).thenReturn(null);
		when(contributorDao.getByMap(anyMap())).thenReturn(null);
		when(contributorDao.getById(anyInt())).thenReturn(new Contributor<>());
		when(contributorTypeDao.getByMap(anyMap())).thenReturn(null);
		when(corporateContributorDao.getByMap(anyMap())).thenReturn(null);
		when(costCenterDao.getByMap(anyMap())).thenReturn(null);
		when(linkFileTypeDao.getByMap(anyMap())).thenReturn(null);
		when(linkTypeDao.getByMap(anyMap())).thenReturn(null);
		when(outsideAffiliationDao.getByMap(anyMap())).thenReturn(null);
		when(personContributorDao.getByMap(anyMap())).thenReturn(null);
		when(publicationDao.getByMap(anyMap())).thenReturn(null);
		when(publicationSeriesDao.getByMap(anyMap())).thenReturn(null);
		when(publicationSubtypeDao.getByMap(anyMap())).thenReturn(null);
		when(publicationTypeDao.getByMap(anyMap())).thenReturn(null);
		when(publishingServiceCenterDao.getByMap(anyMap())).thenReturn(null);

		when(sippProcess.processInformationProduct(any(ProcessType.class), anyString())).thenReturn(new MpPublication());
		when(xmlBusService.getPublicationHtml(anyString())).thenReturn("");
	}

	public void publicTest(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Lookups
		mockMvc.perform(get("/lookup/contributortypes?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/corporations?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/costcenters?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/linkfiletypes?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/linktypes?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/outsideaffiliates?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/people?mimetype=json&text=out").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publications").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publicationseries").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publicationsubtypes").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publicationtype/4/publicationsubtypes").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publicationtype/4/publicationsubtype/1/publicationseries").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publicationtypes").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/lookup/publishingServiceCenters?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Version
		mockMvc.perform(get("/version?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(status().isFound());

		//PW Publication
		mockMvc.perform(get("/publication?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/publication/1?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/publication/crossref").accept(PubsConstantsHelper.MEDIA_TYPE_CROSSREF_VALUE)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/publication/full/1").accept(MediaType.TEXT_HTML)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//PW Publication RSS
		mockMvc.perform(get("/publication/rss").accept(PubsConstantsHelper.MEDIA_TYPE_RSS)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Deleted Publication
		mockMvc.perform(get("/publication/deleted").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}

	public void authenticatedTest(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Auth
		mockMvc.perform(post("/auth/logout").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MP Publication
		mockMvc.perform(get("/mppublications/1/preview").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}

	public void pubsAuthorizedTestGets(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Affiliation
		mockMvc.perform(get("/costcenter").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/costcenter/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/outsideaffiliation").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/outsideaffiliation/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Contributor
		mockMvc.perform(get("/contributor/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/corporation/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/person/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpList
		mockMvc.perform(get("/lists?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpPublication
		mockMvc.perform(get("/mppublications?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/mppublications/1?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//These two are posts, but they return the same codes as a get when successful...
		mockMvc.perform(post("/mppublications/publish").content("{\"id\":1}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(post("/mppublications/release").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Publication Series
		mockMvc.perform(get("/publicationSeries?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(get("/publicationSeries/1?mimetype=json").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

	}

	public void pubsAuthorizedTestDeletes(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Affiliation
		mockMvc.perform(delete("/costcenter/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(delete("/outsideaffiliation/-1").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpListPublication
		mockMvc.perform(delete("/lists/66/pubs/12").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpPublication
		mockMvc.perform(delete("/mppublications/1").secure(true).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Publication Series
		mockMvc.perform(delete("/publicationSeries/1").secure(true).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}

	public void pubsAuthorizedTestPosts(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Affiliation
		mockMvc.perform(post("/costcenter").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(post("/outsideaffiliation").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Contributor
		mockMvc.perform(post("/corporation").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(post("/outsidecontributor").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(post("/usgscontributor").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpListPublication
		mockMvc.perform(post("/lists/66/pubs?publicationId=12").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpPublication
		mockMvc.perform(post("/mppublications").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
		mockMvc.perform(post("/mppublications/sipp").content("{\"ProcessType\": \"dissemination\", \"IPNumber\": \"IP-123456\"}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Publication Series
		mockMvc.perform(post("/publicationSeries").content("{}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}

	public void pubsAuthorizedTestPuts(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//Affiliation
		mockMvc.perform(put("/costcenter/1").content(CONTENT_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(put("/outsideaffiliation/1").content(CONTENT_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Contributor
		mockMvc.perform(put("/corporation/1").content(CONTRIBUTOR_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(put("/outsidecontributor/1").content(CONTRIBUTOR_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		mockMvc.perform(put("/usgscontributor/1").content(CONTRIBUTOR_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//MpPublication
		mockMvc.perform(put("/mppublications/1").content(CONTENT_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);

		//Publication Series
		mockMvc.perform(put("/publicationSeries/1").content(CONTENT_ID_1_JSON).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}

	public void administratorTest(RequestPostProcessor requestPostProcessor, ResultMatcher expectedStatus) throws Exception {
		//purge
		mockMvc.perform(delete("/mppublications/1/purge").accept(MediaType.APPLICATION_JSON)
				.secure(true).with(csrf()).with(requestPostProcessor))
			.andExpect(expectedStatus);
	}


}
