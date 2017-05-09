package gov.usgs.cida.pubs.busservice;

import gov.usgs.cida.pubs.PubsConstants;
import gov.usgs.cida.pubs.busservice.intfc.ICrossRefBusService;
import gov.usgs.cida.pubs.busservice.intfc.IPublicationBusService;
import gov.usgs.cida.pubs.domain.CorporateContributor;
import gov.usgs.cida.pubs.domain.CrossRefLog;
import gov.usgs.cida.pubs.domain.PersonContributor;
import gov.usgs.cida.pubs.domain.Publication;
import gov.usgs.cida.pubs.domain.PublicationContributor;
import gov.usgs.cida.pubs.domain.mp.MpPublication;
import gov.usgs.cida.pubs.transform.TransformerFactory;
import gov.usgs.cida.pubs.transform.intfc.ITransformer;
import gov.usgs.cida.pubs.utility.PubsEMailer;
import gov.usgs.cida.pubs.utility.PubsUtilities;
import gov.usgs.cida.pubs.validation.xml.XMLValidationException;
import gov.usgs.cida.pubs.validation.xml.XMLValidator;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CrossRefBusService implements ICrossRefBusService {

	private static final Logger LOG = LoggerFactory.getLogger(CrossRefBusService.class);

	public static final String FIRST = "first";
	public static final String ADDITIONAL = "additional";

	public static final String SERIES_NAME_REPLACE = "{series_name}";
	public static final String ONLINE_ISSN_REPLACE = "{online_issn}";
	public static final String SURNAME_REPLACE = "{surname}";
	public static final String GIVEN_NAME_REPLACE = "{given_name}";
	public static final String SUFFIX_REPLACE = "{suffix}";
	public static final String ORGANIZATION_REPLACE = "{organization}";
	public static final String SEQUENCE_REPLACE = "{sequence}";
	public static final String CONTRIBUTOR_TYPE_REPLACE = "{contributor_type}";
	public static final String DEPOSITOR_EMAIL_REPLACE = "{depositor_email}";
	public static final String DOI_BATCH_ID_REPLACE = "{doi_batch_id}";
	public static final String SUBMISSION_TIMESTAMP_REPLACE = "{submission_timestamp}";
	public static final String DISSEMINATION_YEAR_REPLACE = "{dissemination_year}";
	public static final String CONTRIBUTORS_REPLACE = "{contributers}";
	public static final String TITLE_REPLACE = "{title}";
	public static final String PAGES_REPLACE = "{pages}";
	public static final String DOI_NAME_REPLACE = "{doi_name}";
	public static final String INDEX_PAGE_REPLACE = "{index_page}";
	public static final String SERIES_NUMBER_REPLACE = "{series_number}";
	public static final String START_PAGE_REPLACE = "{start_page}";
	public static final String END_PAGE_REPLACE = "{end_page}";

	protected final String crossRefProtocol;
	protected final String crossRefHost;
	protected final String crossRefUrl;
	protected final Integer crossRefPort;
	protected final String crossRefUser;
	protected final String crossRefPwd;
	protected final String numberedSeriesXml;
	protected final String unNumberedSeriesXml;
	protected final String personNameXml;
	protected final String organizationNameXml;
	protected final String pagesXml;
	protected final String depositorEmail;
	protected final PubsEMailer pubsEMailer;
	protected final String warehouseEndpoint;
	protected final String crossRefSchemaUrl;
	protected final IPublicationBusService pubBusService;
	protected final TransformerFactory transformerFactory;
	protected final XMLValidator xmlValidator;
	@Autowired
	public CrossRefBusService(
			@Qualifier("crossRefProtocol")
			final String crossRefProtocol,
			@Qualifier("crossRefHost")
			final String crossRefHost,
			@Qualifier("crossRefUrl")
			final String crossRefUrl,
			@Qualifier("crossRefSchemaUrl")
			final String crossRefSchemaUrl,
			@Qualifier("crossRefPort")
			final Integer crossRefPort,
			@Qualifier("crossRefUser")
			final String crossRefUser,
			@Qualifier("crossRefPwd")
			final String crossRefPwd,
			@Qualifier("numberedSeriesXml")
			final String numberedSeriesXml,
			@Qualifier("unNumberedSeriesXml")
			final String unNumberedSeriesXml,
			@Qualifier("organizationNameXml")
			final String organizationNameXml,
			@Qualifier("personNameXml")
			final String personNameXml,
			@Qualifier("pagesXml")
			final String pagesXml,
			@Qualifier("crossRefDepositorEmail")
			final String depositorEmail,
			final PubsEMailer pubsEMailer,
			@Qualifier("warehouseEndpoint")
			final String warehouseEndpoint,
			final IPublicationBusService pubBusService,
			final TransformerFactory transformerFactory,
			final XMLValidator xmlValidator) {
		this.crossRefProtocol = crossRefProtocol;
		this.crossRefHost = crossRefHost;
		this.crossRefUrl = crossRefUrl;
		this.crossRefPort = crossRefPort;
		this.crossRefUser = crossRefUser;
		this.crossRefPwd = crossRefPwd;
		this.numberedSeriesXml = numberedSeriesXml;
		this.unNumberedSeriesXml = unNumberedSeriesXml;
		this.organizationNameXml = organizationNameXml;
		this.personNameXml = personNameXml;
		this.pagesXml = pagesXml;
		this.depositorEmail = depositorEmail;
		this.pubsEMailer = pubsEMailer;
		this.warehouseEndpoint = warehouseEndpoint;
		this.crossRefSchemaUrl = crossRefSchemaUrl;
		this.pubBusService = pubBusService;
		this.transformerFactory = transformerFactory;
		this.xmlValidator = xmlValidator;
	}

	public String getCrossrefXml(Publication<?> pub) throws IOException, UnsupportedEncodingException{
		String xml = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		ITransformer transformer = transformerFactory.getTransformer(PubsConstants.MEDIA_TYPE_XML_EXTENSION, baos, null);
		transformer.write(pub);
		transformer.end();
	
		xml = new String(baos.toByteArray(), PubsConstants.DEFAULT_ENCODING);
		
		return xml;
	}
	
	@Override
	public void submitCrossRef(final MpPublication mpPublication) {
		String crossrefXml;
		String msg;
		try {
			crossrefXml = getCrossrefXml(mpPublication);
		} catch (Exception ex) {
			
		}
		
		try {
			xmlValidator.validate(crossRefSchemaUrl, crossrefXml);
						LOG.debug("Posting to http://" + crossRefHost + ":" + crossRefPort);

			StringBuilder url = new StringBuilder(crossRefUrl).append("?operation=doMDUpload&login_id=")
				.append(crossRefUser).append("&login_passwd=").append(crossRefPwd).append("&area=live");

			HttpResponse rtn = null;
			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost(url.toString());
			HttpHost httpHost = new HttpHost(crossRefHost, crossRefPort, crossRefProtocol);

			try {
				File tempFile = File.createTempFile("crossRef", "xml");
				String fileName = tempFile.getAbsolutePath();
				FileBody file = new FileBody(new File(fileName), ContentType.TEXT_XML, mpPublication.getIndexId() + ".xml");
				HttpEntity httpEntity = MultipartEntityBuilder.create()
					.addPart("fname", file)
					.build();
				httpPost.setEntity(httpEntity);
				rtn = httpClient.execute(httpHost, httpPost, new BasicHttpContext());
			} catch (IOException e) {
				String subject = "Unexpected error in POST to crossref";
				LOG.info(subject, e);
				pubsEMailer.sendMail(subject, e.getMessage());
			}

			if (null == rtn || null == rtn.getStatusLine()
				|| HttpStatus.SC_OK != rtn.getStatusLine().getStatusCode()) {
				msg = null == rtn ? "rtn is null" : rtn.getStatusLine().toString();
				LOG.info("not cool" + msg);
				pubsEMailer.sendMail("Unexpected error in POST to crossref", msg);
			}
		} catch (XMLValidationException ex) {
			msg = "Unexpected error converting publication with Index ID '" + +"' to Crossref XML.";
			LOG.error(, ex);
			
			pubsEMailer.sendMail(, msg);
		}



	}

	protected String buildXml(final MpPublication pub, final String indexPage) {
		File temp = null;
		if (null == pub || null == indexPage || null == pub.getIndexId()) {
			return null;
		}
		String xml = null;
		if (PubsUtilities.isUsgsNumberedSeries(pub.getPublicationSubtype())) {
			xml = buildBaseXml(pub, indexPage, numberedSeriesXml);
		} else {
			xml = buildBaseXml(pub, indexPage, unNumberedSeriesXml);
		}
		String batchId = xml.substring(xml.indexOf("<doi_batch_id>") + 14, xml.indexOf("</doi_batch_id>"));
		CrossRefLog log = new CrossRefLog(batchId, pub.getId(), xml);
		CrossRefLog.getDao().add(log);
		try {
			temp = File.createTempFile(pub.getIndexId(), ".xml");
			LOG.debug("TEMP FILE IS:" + temp.getAbsolutePath());
			temp.deleteOnExit();
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			bw.write(xml);
			bw.close();
		} catch (IOException e) {
			String subject = "Unexpected error in building xml for crossref";
			LOG.info(subject, e);
			pubsEMailer.sendMail(subject, e.getMessage());
		}
		return null == temp ? null : temp.getAbsolutePath();
	}

	protected String buildBaseXml(final MpPublication pub, final String indexPage, final String xml) {
		if (null == pub || null == indexPage || null == xml) {
			return "";
		} else {
			String rtn = xml;
			rtn = replacePlaceHolder(rtn, DOI_BATCH_ID_REPLACE, getBatchId());
			rtn = replacePlaceHolder(rtn, SUBMISSION_TIMESTAMP_REPLACE, String.valueOf(new Date().getTime()));
			rtn = replacePlaceHolder(rtn, DEPOSITOR_EMAIL_REPLACE, StringEscapeUtils.escapeXml10(depositorEmail));
			rtn = replacePlaceHolder(rtn, DISSEMINATION_YEAR_REPLACE, StringEscapeUtils.escapeXml10(pub.getPublicationYear()));
			rtn = replacePlaceHolder(rtn, CONTRIBUTORS_REPLACE, getContributors(pub));
			rtn = replacePlaceHolder(rtn, TITLE_REPLACE, StringEscapeUtils.escapeXml10(pub.getTitle()));
			rtn = replacePlaceHolder(rtn, PAGES_REPLACE, getPages(pub));
			rtn = replacePlaceHolder(rtn, DOI_NAME_REPLACE, StringEscapeUtils.escapeXml10(pub.getDoi()));
			rtn = replacePlaceHolder(rtn, INDEX_PAGE_REPLACE, StringEscapeUtils.escapeXml10(indexPage));
			if (null != pub.getSeriesTitle()) {
				if (null != pub.getSeriesTitle().getText()) {
					rtn = replacePlaceHolder(rtn, SERIES_NAME_REPLACE, StringEscapeUtils.escapeXml10(pub.getSeriesTitle().getText()));
				} else {
					rtn = replacePlaceHolder(rtn, SERIES_NAME_REPLACE, "");
				}
				if (null != pub.getSeriesTitle().getText()) {
					rtn = replacePlaceHolder(rtn, ONLINE_ISSN_REPLACE, StringEscapeUtils.escapeXml10(pub.getSeriesTitle().getOnlineIssn()));
				} else {
					rtn = replacePlaceHolder(rtn, ONLINE_ISSN_REPLACE, "");
				}
			} else {
				rtn = replacePlaceHolder(rtn, SERIES_NAME_REPLACE, "");
				rtn = replacePlaceHolder(rtn, ONLINE_ISSN_REPLACE, "");
			}
			rtn = replacePlaceHolder(rtn, SERIES_NUMBER_REPLACE, StringEscapeUtils.escapeXml10(pub.getSeriesNumber()));
			return rtn;
		}
	}

	protected String replacePlaceHolder(String rawString, String placeHolder, String replaceWith) {
		if (null == rawString) {
			return "";
		}
		if (null == placeHolder || -1 == rawString.indexOf(placeHolder)) {
			return rawString;
		} else {
			if (StringUtils.isBlank(replaceWith)) {
				return rawString.replace(placeHolder, "");
			} else {
				return rawString.replace(placeHolder, replaceWith);
			}
		}
	}

	protected String getBatchId() {
		return String.valueOf(new Date().getTime());
	}
	
	protected String getContributors(MpPublication pub) {
		StringBuilder rtn = new StringBuilder("");
		//This process requires that the contributors are in rank order.
		//And that the contributor is valid.
		if (null != pub && null != pub.getContributors() && !pub.getContributors().isEmpty()) {
			Map<String, List<PublicationContributor<?>>> contributors = pub.getContributorsToMap();
			String sequence = FIRST;
			Collection<PublicationContributor<?>> authors = contributors.get(PubsUtilities.getAuthorKey());
			if (null != authors && !authors.isEmpty()) {
				for (PublicationContributor<?> author : authors) {
					if (author.getContributor() instanceof PersonContributor) {
						rtn.append(processPerson(author, sequence));
					} else {
						rtn.append(processCorporation(author, sequence));
			}
					sequence = ADDITIONAL;
					rtn.append("\n");
			}
		}

			Collection<PublicationContributor<?>> editors = contributors.get(PubsUtilities.getEditorKey());
			if (null != editors && !editors.isEmpty()) {
				for (PublicationContributor<?> editor : editors) {
					if (editor.getContributor() instanceof PersonContributor) {
						rtn.append(processPerson(editor, sequence));
				} else {
						rtn.append(processCorporation(editor, sequence));
				}
				sequence = ADDITIONAL;
				rtn.append("\n");
			}
		}
		}
		return rtn.toString();
	}

	protected String processPerson(PublicationContributor<?> pubContributor, String sequence) {
		PersonContributor<?> contributor = (PersonContributor<?>) pubContributor.getContributor();
		String template = personNameXml;
		template = template.replace(SEQUENCE_REPLACE, sequence);
		template = template.replace(CONTRIBUTOR_TYPE_REPLACE, getContributorType(pubContributor));
		if (StringUtils.isNotBlank(contributor.getFamily())) {
			template = template.replace(SURNAME_REPLACE, StringEscapeUtils.escapeXml10(contributor.getFamily()));
		} else {
			template = template.replace(SURNAME_REPLACE, "");
		}
		if (StringUtils.isNotBlank(contributor.getGiven())) {
			template = template.replace(GIVEN_NAME_REPLACE, "<given_name>" + StringEscapeUtils.escapeXml10(contributor.getGiven()) + "</given_name>");
		} else {
			template = template.replace(GIVEN_NAME_REPLACE, "");
		}
		if (StringUtils.isNotBlank(contributor.getSuffix())) {
			template = template.replace(SUFFIX_REPLACE, "<suffix>" + StringEscapeUtils.escapeXml10(contributor.getSuffix()) + "</suffix>");
		} else {
			template = template.replace(SUFFIX_REPLACE, "");
		}
		return template;
	}

	protected String processCorporation(PublicationContributor<?> pubContributor, String sequence) {
		CorporateContributor contributor = (CorporateContributor) pubContributor.getContributor();
		String template = organizationNameXml;
		template = template.replace(SEQUENCE_REPLACE, sequence);
		template = template.replace(CONTRIBUTOR_TYPE_REPLACE, getContributorType(pubContributor));
		if (StringUtils.isNotBlank(contributor.getOrganization())) {
			template = template.replace(ORGANIZATION_REPLACE, StringEscapeUtils.escapeXml10(contributor.getOrganization()));
		} else {
			template = template.replace(ORGANIZATION_REPLACE, "");
		}
		return template;
	}

	protected String getContributorType(PublicationContributor<?> pubContributor) {
		if (null != pubContributor && null != pubContributor.getContributorType()
				&& StringUtils.isNotBlank(pubContributor.getContributorType().getText())) {
			return StringEscapeUtils.escapeXml10(pubContributor.getContributorType().getText().toLowerCase().replaceAll("s$", ""));
		} else {
			return "";
		}
	}

	protected String getPages(MpPublication pub) {
		String rtn = "";
		if (null != pub && StringUtils.isNotBlank(pub.getStartPage())
				&& StringUtils.isNotBlank(pub.getEndPage())) {
			rtn = pagesXml.replace(START_PAGE_REPLACE, StringEscapeUtils.escapeXml10(pub.getStartPage().trim()))
					.replace(END_PAGE_REPLACE, StringEscapeUtils.escapeXml10(pub.getEndPage().trim()));
		}
		return rtn;
	}
	
}
