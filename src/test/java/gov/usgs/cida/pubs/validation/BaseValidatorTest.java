package gov.usgs.cida.pubs.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.ArrayList;
import java.util.List;

import javax.validation.metadata.ConstraintDescriptor;

import org.hibernate.validator.internal.engine.DefaultClockProvider;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.boot.test.mock.mockito.MockBean;

import gov.usgs.cida.pubs.BaseTest;
import gov.usgs.cida.pubs.SeverityLevel;

public abstract class BaseValidatorTest extends BaseTest {

	public static final String NOT_NULL_MSG = "must not be null";
	public static final String BAD_URL_MSG = "must be a valid URL";
	public static final String NO_PARENT_MSG = "The parent specified for this object does not exist.";
	public static final String LENGTH_1_TO_XXX_MSG = "length must be between 1 and ";
	public static final String LENGTH_0_TO_XXX_MSG = "length must be between 0 and ";
	public static final String REGEX_MSG = "must match \"";
	public static final String MAY_NOT_DELETE_MSG = "You may not delete this until all child objects have been deleted.";
	public static final String EMAIL_FORMAT_MSG = "must be a well-formed email address";
	public static final String URL_FORMAT_MSG = "must be a valid URL";

	public static final String NOT_NULL_TEXT = new ValidatorResult("text", NOT_NULL_MSG, SeverityLevel.FATAL, null).toString();
	public static final String MAY_NOT_DELETE_ID = new ValidatorResult("id", MAY_NOT_DELETE_MSG, SeverityLevel.FATAL, null).toString();

	protected ConstraintValidatorContextImpl context;
	protected List<String> methodParameterNames = new ArrayList<>();
	protected PathImpl propertyPath = PathImpl.createPathFromString("abc");

	@MockBean
	protected ConstraintDescriptor<?> constraintDescriptor;

	public void buildContext() throws Exception {
		context = new ConstraintValidatorContextImpl(methodParameterNames, DefaultClockProvider.INSTANCE, propertyPath, constraintDescriptor, null);
	}

	public void assertValidationResults(List<ValidatorResult> actual, String... expected) {
		List<String> actualStrings = new ArrayList<>();
		for (ValidatorResult x : actual) {
			actualStrings.add(x.toString());
		}
		assertThat(actualStrings, containsInAnyOrder(expected));
	}

}
