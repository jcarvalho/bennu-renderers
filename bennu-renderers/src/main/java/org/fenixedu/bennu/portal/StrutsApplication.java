package org.fenixedu.bennu.portal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrutsApplication {

    Class<?> parent() default Object.class;

    String path();

    String bundle() default RenderersAnnotationProcessor.DELEGATE_TO_PARENT;

    String titleKey();

    String descriptionKey();

    String accessGroup() default "anyone";

}
