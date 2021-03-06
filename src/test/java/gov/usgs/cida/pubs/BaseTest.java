package gov.usgs.cida.pubs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.dataset.ReplacementDataSet;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.ReactorContextTestExecutionListener;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.util.FileCopyUtils;

import com.github.springtestdbunit.dataset.ReplacementDataSetModifier;

import gov.usgs.cida.pubs.domain.BaseDomain;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners({DirtiesContextTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
	MockitoTestExecutionListener.class, WithSecurityContextTestExecutionListener.class, ReactorContextTestExecutionListener.class})
public abstract class BaseTest {
	public static final Log LOG = LogFactory.getLog(BaseTest.class);

	public static final String SEARCH_POLYGON = "polygon((-122.3876953125 37.80869897600677,-122.3876953125 36.75979104322286,-123.55224609375 36.75979104322286," +
				"-123.55224609375 37.80869897600677,-122.3876953125 37.80869897600677))";
	public static final String GEOGRAPHIC_EXTENTS = "{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\",\"properties\": {},\"geometry\": {"
				+ "\"type\": \"Polygon\",\"coordinates\": [[[-91.91162109375,45.69850658738846],[-91.91162109375,47.16730970131578],"
				+ "[-90.3955078125,47.16730970131578],[-90.3955078125,45.69850658738846],[-91.91162109375,45.69850658738846]]]}}]}";
	public static final String PUBS_WAREHOUSE_ENDPOINT = "http://pubs.er.usgs.gov";
	public static final String SWAGGER_DISPLAY_HOST = "localhost:8444";

	/** random for the class. */
	protected static final Random RANDOM = new Random();

	/**
	 * @return next random positive int.
	 */
	protected static int randomPositiveInt() {
		return RANDOM.nextInt(999999999) + 1;
	}

	protected Integer id;

	protected class IdModifier extends ReplacementDataSetModifier {

		@Override
		protected void addReplacements(ReplacementDataSet dataSet) {
			dataSet.addReplacementSubstring("[id]", id.toString());
		}

	}

	public void assertDaoTestResults(final Class<?> inClass, final Object inObject, final Object resultObject) {
		assertDaoTestResults(inClass, inObject, resultObject, null, false, false, false);
	}

	public void assertDaoTestResults(final Class<?> inClass, final Object inObject, final Object resultObject, final List<String> ignoreProperties, final boolean ignoreInsertAudit, final boolean ignoreUpdateAudit) {
		assertDaoTestResults(inClass, inObject, resultObject, ignoreProperties, ignoreInsertAudit, ignoreUpdateAudit, false);
	}

	public void assertDaoTestResults(final Class<?> inClass, final Object inObject, final Object resultObject, final List<String> ignoreProperties, final boolean ignoreInsertAudit, final boolean ignoreUpdateAudit,
			final boolean allowNull) {
		for (PropertyDescriptor prop : getPropertyMap(inClass).values()) {
			if ((null != ignoreProperties && ignoreProperties.contains(prop.getName())) 
					|| (ignoreInsertAudit && (prop.getName().contentEquals("insertDate") || prop.getName().contentEquals("insertUsername")))
					|| (ignoreUpdateAudit && (prop.getName().contentEquals("updateDate") || prop.getName().contentEquals("updateUsername")))) {
				assertTrue(true);
			} else {
				try {
					if (null != prop.getReadMethod() && !"getClass".contentEquals(prop.getReadMethod().getName())) {
						Object inProp = prop.getReadMethod().invoke(inObject);
						Object resultProp = prop.getReadMethod().invoke(resultObject);
						if (!allowNull) {
							assertNotNull(inProp, prop.getName() + " original is null.");
							assertNotNull(resultProp, prop.getName() + " result is null.");
						}
						if (resultProp instanceof Collection) {
							//TODO - could try to match the lists...
							assertEquals(((Collection<?>) inProp).size(), ((Collection<?>) resultProp).size(), prop.getName());
						} else {
							assertProperty(inProp, resultProp, prop);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Error getting property: " + prop.getName(), e);
				}
			}
		}
	}

	private void assertProperty(final Object inProp, final Object resultProp, final PropertyDescriptor prop) throws Exception {
		if (resultProp instanceof BaseDomain) {
			LOG.debug(prop.getName() + " input ID: " + ((BaseDomain<?>) inProp).getId() 
					+ " result ID: " + ((BaseDomain<?>) resultProp).getId());
			assertEquals(((BaseDomain<?>) inProp).getId(), ((BaseDomain<?>) resultProp).getId(), prop.getName());
		} else {
			LOG.debug(prop.getName() + " input: " + inProp + " result: " + resultProp);
			assertEquals(inProp, resultProp, prop.getName());
		}
	}

	public HashMap<String, PropertyDescriptor> getPropertyMap(final Class<?> inClass) {
		HashMap<String, PropertyDescriptor> returnMethodList = new HashMap<String, PropertyDescriptor>();
		BeanInfo info = null;
		try {
			info = Introspector.getBeanInfo(inClass);
		} catch (IntrospectionException e1) {
			LOG.error("error introspecting bean: " + inClass.getCanonicalName(), e1);
		}
		returnMethodList = new HashMap<String, PropertyDescriptor>();
		if (info != null) {
			//for each of this objects setter method.
			for (PropertyDescriptor propDesc : info.getPropertyDescriptors()) {
				//assuming JavaBean convention
				returnMethodList.put(propDesc.getName(), propDesc);
			}
		}
		return returnMethodList;
	}

	public String harmonizeXml(String xmlDoc) {
		//remove carriage returns, new lines, tabs, spaces between elements, spaces at the start of the string.
		return xmlDoc.replace("\r", "").replace("\n", "").replace("\t", "").replaceAll("> *<", "><").replaceAll("^ *", "");
	}

	public String getCompareFile(String file) throws IOException {
		return getFile("testResult/" + file);
	}

	public String getFile(String file) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(new ClassPathResource(file).getInputStream()));
	}

	public String currentYear = String.valueOf(LocalDate.now().getYear());

}
