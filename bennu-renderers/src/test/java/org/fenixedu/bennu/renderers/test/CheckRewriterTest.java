package org.fenixedu.bennu.renderers.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;

@RunWith(JUnit4.class)
public class CheckRewriterTest {

    private final GenericChecksumRewriter EMPTY_REWRITER = new GenericChecksumRewriter(null);

    @Test
    public void linkTagWithoutHref() {
        checkNoChange("<a class=\"delete\" onclick=\"delete(this,1234)\" style=\"color: red\">X</a>");
    }

    @Test
    public void imgTagWithoutSrc() {
        checkNoChange("<img alt=\"\" />");
    }

    @Test
    public void formTagWithoutAction() {
        checkNoChange("<form method=\"POST\"></form>");
    }

    private void checkNoChange(String value) {
        assertThat(EMPTY_REWRITER.rewrite(new StringBuilder(value)).toString(), is(value));
    }

}
