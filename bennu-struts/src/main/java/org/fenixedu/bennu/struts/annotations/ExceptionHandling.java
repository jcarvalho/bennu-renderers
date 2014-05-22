package org.fenixedu.bennu.struts.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.struts.action.ExceptionHandler;

@Target({ ElementType.TYPE })
@Repeatable(Exceptions.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionHandling {

    public Class<? extends Exception> type();

    public String key() default "";

    public Class<? extends ExceptionHandler> handler();

    public String path() default "";

    public String scope() default "";

}