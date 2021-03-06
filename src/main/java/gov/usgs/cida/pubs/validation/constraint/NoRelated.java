package gov.usgs.cida.pubs.validation.constraint;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;

import gov.usgs.cida.pubs.validation.constraint.norelated.NoRelatedValidatorForPwPublication;

@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy={
		NoRelatedValidatorForPwPublication.class
})
@Documented
public @interface NoRelated {

	String message() default "{publication.related.exist}";
	Class<?>[] groups() default {};
	public abstract Class<?>[] payload() default {};

	String[] propertyName() default {};
}