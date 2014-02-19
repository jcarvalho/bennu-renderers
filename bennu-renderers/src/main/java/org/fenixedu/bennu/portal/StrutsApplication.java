package org.fenixedu.bennu.portal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrutsApplication {

    String path();

    String bundle();

    String titleKey();

    String descriptionKey() default RenderersAnnotationProcessor.INFER_VALUE;

    String accessGroup() default "anyone";

}
