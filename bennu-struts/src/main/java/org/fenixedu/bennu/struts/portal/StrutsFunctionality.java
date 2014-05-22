package org.fenixedu.bennu.struts.portal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrutsFunctionality {

    Class<?> app();

    String path();

    String bundle() default BennuStrutsAnnotationProcessor.DELEGATE;

    String titleKey();

    String descriptionKey() default BennuStrutsAnnotationProcessor.DELEGATE;

    String accessGroup() default BennuStrutsAnnotationProcessor.DELEGATE;

}
