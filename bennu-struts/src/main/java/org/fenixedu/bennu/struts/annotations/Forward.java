/**
 * 
 */
package org.fenixedu.bennu.struts.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
@Repeatable(Forwards.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Forward {

    String name();

    String path();

    boolean redirect() default false;

    boolean contextRelative() default true;

}
