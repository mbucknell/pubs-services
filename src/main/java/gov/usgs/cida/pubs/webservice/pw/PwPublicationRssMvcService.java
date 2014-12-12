package gov.usgs.cida.pubs.webservice.pw;

import gov.usgs.cida.pubs.PubsConstants;
import gov.usgs.cida.pubs.busservice.intfc.IPwPublicationBusService;
import gov.usgs.cida.pubs.domain.BaseDomain;
import gov.usgs.cida.pubs.domain.Contributor;
import gov.usgs.cida.pubs.domain.CorporateContributor;
import gov.usgs.cida.pubs.domain.PersonContributor;
import gov.usgs.cida.pubs.domain.Publication;
import gov.usgs.cida.pubs.domain.PublicationContributor;
import gov.usgs.cida.pubs.domain.PublicationSeries;
import gov.usgs.cida.pubs.domain.UsgsContributor;
import gov.usgs.cida.pubs.domain.pw.PwPublication;
import gov.usgs.cida.pubs.webservice.MvcService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "publication/rss", produces="text/xml")
public class PwPublicationRssMvcService extends MvcService<PwPublication> {
	private static final int DEFAULT_RECORDS = 30;
	
	private static final Logger LOG = LoggerFactory.getLogger(PwPublicationRssMvcService.class);

    private final IPwPublicationBusService busService;

    @Autowired
    PwPublicationRssMvcService(@Qualifier("pwPublicationBusService")
    		final IPwPublicationBusService busService) {
    	this.busService = busService;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public void getRSS(
    		@RequestParam(value="q", required=false) String searchTerms, //single string search
            @RequestParam(value="title", required=false) String[] title,
            @RequestParam(value="abstract", required=false) String[] pubAbstract,
            @RequestParam(value="contributor", required=false) String[] contributor,
            @RequestParam(value="prodId", required=false) String[] prodId,
            @RequestParam(value="indexId", required=false) String[] indexId,
            @RequestParam(value="ipdsId", required=false) String[] ipdsId,
            @RequestParam(value="year", required=false) String[] year,
            @RequestParam(value="startYear", required=false) String yearStart,
            @RequestParam(value="endYear", required=false) String yearEnd,
    		@RequestParam(value="contributingOffice", required=false) String[] contributingOffice,
            @RequestParam(value="typeName", required=false) String[] typeName,
            @RequestParam(value="subtypeName", required=false) String[] subtypeName,
            @RequestParam(value="seriesName", required=false) String[] reportSeries,
            @RequestParam(value="reportNumber", required=false) String[] reportNumber,
            @RequestParam(value="pub_x_days", required=false) String pubXDays,
            @RequestParam(value="pub_date_low", required=false) String pubDateLow,
            @RequestParam(value="pub_date_high", required=false) String pubDateHigh,
            @RequestParam(value="mod_x_days", required=false) String modXDays,
            @RequestParam(value="mod_date_low", required=false) String modDateLow,
            @RequestParam(value="mod_date_high", required=false) String modDateHigh,
            @RequestParam(value="orderBy", required=false) String orderBy,
			HttpServletResponse response) {

        Map<String, Object> filters = new HashMap<>();
        
        /**
         * Per JIMK on JIRA PUBSTWO-971:
         * 
         * 	"if mod_x_days is there, and pub_x_days is not, mod_x_days should override the default pub_x_days = 30"
         */
        if((pubXDays == null) || (pubXDays.isEmpty())) {
        	if((modXDays == null) || (modXDays.isEmpty())) {
        		pubXDays = "" + DEFAULT_RECORDS;
        	}
        }
    	
    	configureSingleSearchFilters(filters, searchTerms);

    	addToFiltersIfNotNull(filters, "title", title);
    	addToFiltersIfNotNull(filters, "abstract", pubAbstract);
    	addToFiltersIfNotNull(filters, "contributor", contributor);
    	addToFiltersIfNotNull(filters, "id", prodId);
    	addToFiltersIfNotNull(filters, "indexId", indexId);
    	addToFiltersIfNotNull(filters, "ipdsId", ipdsId);
    	addToFiltersIfNotNull(filters, "year", year);
    	addToFiltersIfNotNull(filters, "yearStart", yearStart);
    	addToFiltersIfNotNull(filters, "yearEnd", yearEnd);
    	addToFiltersIfNotNull(filters, "contributingOffice", contributingOffice);
    	addToFiltersIfNotNull(filters, "typeName", typeName);
    	addToFiltersIfNotNull(filters, "subtypeName", subtypeName);
    	addToFiltersIfNotNull(filters, "reportSeries", reportSeries);
    	addToFiltersIfNotNull(filters, "reportNumber", reportNumber);
    	addToFiltersIfNotNull(filters, "pubXDays", pubXDays);
    	addToFiltersIfNotNull(filters, "pubDateLow", pubDateLow);
    	addToFiltersIfNotNull(filters, "pubDateHigh", pubDateHigh);
    	addToFiltersIfNotNull(filters, "modXDays", modXDays);
    	addToFiltersIfNotNull(filters, "modDateLow", modDateLow);
    	addToFiltersIfNotNull(filters, "modDateHigh", modDateHigh);    	
    	filters.put("orderby", buildOrderBy(orderBy));
    	
        List<PwPublication> pubs = busService.getObjects(filters);

        String rssResults = getSearchResultsAsRSS(pubs);
        
        response.setCharacterEncoding(PubsConstants.DEFAULT_ENCODING);
    	response.setContentType(PubsConstants.MIME_TYPE_APPLICATION_RSS);
    	try {
			response.setContentLength(rssResults.getBytes(PubsConstants.DEFAULT_ENCODING).length);
		} catch (UnsupportedEncodingException e) {
			LOG.error("Unable to set content length of resulting RSS content: " + e.getMessage());
		}
    	
    	response.setStatus(HttpStatus.SC_OK);
    	try {
			response.getWriter().write(rssResults);
		} catch (IOException e) {
			LOG.error("Unable to write to response: " + e.getMessage());
		}
    }
    
    private String getSearchResultsAsRSS(List<PwPublication> records) {
    	/**
    	 * Per JIM JIRA PUBSTWO-971  ā
    	 * 
    	 * 		<rss version="2.0">
    	 * 			<channel>
    	 * 				<title>USGS Publications Warehouse</title>
    	 * 				<link>http://pubs.er.usgs.gov</link>
    	 * 				<description>New publications of the USGS.</description>
    	 * 				<language>en-us</language>
    	 * 				<lastBuildDate>{{ current date/time }}</lastBuildDate>
    	 * 				<webMaster>http://pubs.er.usgs.gov/feedback</webmaster>
    	 * 				<pubDate>{{ current date/time }}</pubDate>
    	 * 				<item>
    	 * 					<title>{{pubdata["title"]}}</title>
    	 * 					<author>{{pubdata["authors["text"], order by ["rank"]"]}}</author>
    	 * 					<link>http://pubs.er.usgs.gov/publication/{{pubdata["indexId"]}}</link>
    	 * 					<description>{{pubdata["abstract"]}}</description>
    	 * 					<pubDate>{{pubdata["displayToPublicDate"]}}</pubDate>
    	 * 					<category>{{pubdata['seriesTitle']["text"]}}</category>
    	 * 				</item>
    	 * 				...
    	 * 			</channel>
    	 * 		</rss>
    	 * 
    	 */
    	StringBuffer rssResults = new StringBuffer("<?xml version='1.0' encoding='UTF-8'?>\n");
    	
    	// Date in the form of "Sat, 29 Nov 2014 10:38 -0600"
    	SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    	String todayDate = sdf.format(new Date());
    	
    	rssResults.append("<rss version=\"2.0\">\n");
    	rssResults.append("\t<channel>\n");
    	rssResults.append("\t\t<title>USGS Publications Warehouse</title>\n");
    	rssResults.append("\t\t<link>http://pubs.er.usgs.gov</link>\n");
    	rssResults.append("\t\t<description>New publications of the USGS.</description>\n");
    	rssResults.append("\t\t<language>en-us</language>\n");
    	rssResults.append("\t\t<lastBuildDate>" + todayDate + "</lastBuildDate>\n");
    	rssResults.append("\t\t<webmaster>http://pubs.er.usgs.gov/feedback</webmaster>\n");
    	rssResults.append("\t\t<pubDate>" + todayDate + "</pubDate>\n");
    	
    	/**
    	 * Now per item
    	 */
    	if(records != null) {
	    	for(BaseDomain<?> record : records) {
	    		Publication<?> publication = (Publication<?>)record;
	    		
	    		rssResults.append("\t\t<item>\n");
	    		
	    		// ==== TITLE
	    		rssResults.append("\t\t\t<title>");
	    		
	    		String itemTitle = publication.getTitle();
	    		if(itemTitle != null) {
	    			rssResults.append(StringEscapeUtils.escapeXml10(itemTitle.trim()));
	    		}
	    		rssResults.append("</title>\n");
	    		
	    		// ==== AUTHORS
	    		/**
	    		 * Authors is a list
	    		 */
	    		rssResults.append("\t\t\t<author>");
	    		StringBuffer authorship = new StringBuffer();
	    		List<PublicationContributor<?>> authors = (List<PublicationContributor<?>>) publication.getAuthors();
	    		try {
		    		if(authors != null) {
			    		for(int i = 0; i < authors.size(); i++) {
			    			PublicationContributor<?> author = authors.get(i);
			    			
			    			Contributor<?> contributor = author.getContributor();
			    			
			    			if(contributor != null) {
			    				if(contributor.isCorporation()) {
				    				CorporateContributor corpContributor = (CorporateContributor) contributor;
				    				
				    				String organization = corpContributor.getOrganization();
				    				if(organization != null) {
				    					authorship.append(organization.trim());
				    				}
				    			} else if(contributor.isUsgs()) {
				    				UsgsContributor usgsContributor = (UsgsContributor) contributor;
				    				
				    				String family = usgsContributor.getFamily();
				    				if(family != null) {
				    					authorship.append(family.trim());
				    					authorship.append(", ");
				    				}
				    				
				    				String given = usgsContributor.getGiven();
				    				if(given != null) {
				    					authorship.append(given.trim());
				    				}
				    			} else {
				    				if(contributor instanceof PersonContributor) {
				    					PersonContributor<?> person = (PersonContributor<?>) contributor;
				
				    					String family = person.getFamily();
					    				if(family != null) {
					    					authorship.append(family.trim());
					    					authorship.append(", ");
					    				}
					    				
					    				String given = person.getGiven();
					    				if(given != null) {
					    					authorship.append(given.trim());
					    				}
				    				} else {
				    					authorship.append(contributor.getId());
				    				}
				    			}
			    			}
			    			
			    			if((i + 1) < authors.size()) {
			    				authorship.append("; ");
			    			}
			    		}
		    		}
	    		} catch (ClassCastException e) {
	    			LOG.error("Error extracting contributor information: " + e.getMessage());
	    		}
	    		rssResults.append(StringEscapeUtils.escapeXml10(authorship.toString()));
	    		rssResults.append("</author>\n");
	    		
	    		// ==== LINKS
	    		/**
	    		 * Links is a list
	    		 */
	    		rssResults.append("\t\t\t<link>");
	    		String pubId = publication.getIndexId();
	    		if(pubId != null) {
		    		rssResults.append("http://pubs.er.usgs.gov/publication/" + pubId.trim());
	    		}
	    		rssResults.append("</link>\n");
	    		
	    		// ==== DESCRIPTION
	    		rssResults.append("\t\t\t<description>");
	    		
	    		String itemDesc = publication.getDocAbstract();
	    		if(itemDesc != null) {
	    			rssResults.append(StringEscapeUtils.escapeXml10(itemDesc.trim()));
	    		}
	    		rssResults.append("</description>\n");
	    		
	    		// ==== PUBLICATION DATE
	    		rssResults.append("\t\t\t<pubDate>");
	    		LocalDateTime pubLocalDateTime = publication.getUpdateDate();
	    		if(pubLocalDateTime != null) {
		    		Date pubDateTime = pubLocalDateTime.toDate();
		    		String pubDate = sdf.format(pubDateTime);
		    		rssResults.append(pubDate);
	    		}
	    		rssResults.append("</pubDate>\n");
	    		
	    		// ==== CATEGORY
	    		rssResults.append("\t\t\t<category>");
	    		PublicationSeries pubSeries = publication.getSeriesTitle();
	    		if(pubSeries != null) {
	    			String pubSeriesTitle = pubSeries.getText();
	    			if(pubSeriesTitle != null) {
	    				rssResults.append(StringEscapeUtils.escapeXml10(pubSeriesTitle.trim()));
	    			}
	    		}
	    		rssResults.append("</category>\n");
	    		
	    		
	    		rssResults.append("\t\t</item>\n");
	    	}
    	}
    	
    	rssResults.append("\t</channel>\n");
    	rssResults.append("</rss>\n");
    	
    	return rssResults.toString();
    }
}