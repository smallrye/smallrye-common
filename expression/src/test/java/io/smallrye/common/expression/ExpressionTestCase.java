package io.smallrye.common.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import io.smallrye.common.constraint.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ExpressionTestCase {
    @Test
    public void testEmpty() {
        final Expression expression = Expression.compile("");
        assertEquals("", expression.evaluate((c, b) -> {
            fail("No expressions should be found");
        }), "Should expand to empty string");
    }

    @Test
    public void testLiteral() {
        final String expected = "this is a literal";
        final Expression expression = Expression.compile(expected);
        assertEquals(expected, expression.evaluate((c, b) -> {
            fail("No expressions should be found");
        }));
    }

    @Test
    public void testExpr1() {
        final Expression expression = Expression.compile("${foo.bar}");
        assertEquals("", expression.evaluate((c, b) -> {
            assertEquals("foo.bar", c.getKey());
        }), "Should expand to empty string");
    }

    @Test
    public void testExpr1a() {
        final Expression expression = Expression.compile("${foo.bar}");
        assertEquals("fuzz", expression.evaluate((c, b) -> {
            assertEquals("foo.bar", c.getKey());
            b.append("fuzz");
        }));
    }

    @Test
    public void testNoTrim1() {
        final Expression expression1 = Expression.compile("   ${foo.bar}   ");
        assertEquals("", expression1.evaluate((c, b) -> {
        }));
        final Expression expression2 = Expression.compile("   ${foo.bar}   ", Expression.Flag.NO_TRIM);
        assertEquals("      ", expression2.evaluate((c, b) -> {
        }));
    }

    @Test
    public void testNoTrim2() {
        final Expression expression1 = Expression.compile("   this is a literal   ");
        assertEquals("this is a literal", expression1.evaluate((c, b) -> {
        }));
        final Expression expression2 = Expression.compile("   this is a literal   ", Expression.Flag.NO_TRIM);
        assertEquals("   this is a literal   ", expression2.evaluate((c, b) -> {
        }));
    }

    @Test
    public void testExpr2() {
        final Expression expression = Expression.compile("foo${foo.bar}bar");
        assertEquals("foobazbar", expression.evaluate((c, b) -> {
            assertEquals("foo.bar", c.getKey());
            b.append("baz");
        }));
    }

    @Test
    public void testExpr3() {
        final Expression expression = Expression.compile("foo${foo.bar}");
        assertEquals("foobaz", expression.evaluate((c, b) -> {
            assertEquals("foo.bar", c.getKey());
            b.append("baz");
        }));
    }

    @Test
    public void testExpr4() {
        final Expression expression = Expression.compile("${foo.bar}bar");
        assertEquals("bazbar", expression.evaluate((c, b) -> {
            assertEquals("foo.bar", c.getKey());
            b.append("baz");
        }));
    }

    @Test
    public void testRecurseKey1() {
        final Expression expression = Expression.compile("${${foo}.bar}bar");
        assertEquals("bazbar", expression.evaluate((c, b) -> {
            final String key = c.getKey();
            switch (key) {
                case "foo":
                    b.append("bzz");
                    return;
                case "bzz.bar":
                    b.append("baz");
                    return;
                default:
                    throw Assert.impossibleSwitchCase(key);
            }
        }));
    }

    @Test
    public void testRecurseDefault1() {
        final Expression expression = Expression.compile("${foo:${bar}}bar");
        assertEquals("bazbar", expression.evaluate((c, b) -> {
            final String key = c.getKey();
            switch (key) {
                case "foo":
                    c.expandDefault();
                    return;
                case "bar":
                    b.append("baz");
                    return;
                default:
                    throw Assert.impossibleSwitchCase(key);
            }
        }));
    }

    @Test
    public void testGeneralExpansion1() {
        final Expression expression = Expression.compile("foo${{zip}}${bar}", Expression.Flag.GENERAL_EXPANSION);
        assertEquals("foobazrab", expression.evaluate((c, b) -> {
            final String key = c.getKey();
            switch (key) {
                case "zip":
                    b.append("baz");
                    return;
                case "bar":
                    b.append("rab");
                    return;
                default:
                    throw Assert.impossibleSwitchCase(key);
            }
        }));
    }

    @Test
    public void testPoint1() {
        final Expression expression = Expression.compile("${${foo}}", Expression.Flag.NO_RECURSE_KEY);
        expression.evaluate((c, b) -> {
            assertEquals("${foo}", c.getKey());
        });
    }

    @Test
    public void testPoint2() {
        try {
            final Expression expression = Expression.compile("$");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint3() {
        final Expression expression = Expression.compile("$", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("$", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
        final Expression expression2 = Expression.compile("foo$", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo$", expression2.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint3_1() {
        final Expression expression = Expression.compile("$$");
        assertEquals("$", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
        final Expression expression2 = Expression.compile("foo$$");
        assertEquals("foo$", expression2.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    // no testPoint4

    @Test
    public void testPoint5() {
        try {
            Expression.compile("${expr");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint6() {
        final Expression expression = Expression.compile("${expr", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("result", expression.evaluate((c, b) -> {
            assertEquals("expr", c.getKey());
            b.append("result");
        }));
    }

    @Test
    public void testPoint7() {
        final Expression expression = Expression.compile("${expr::baz}", Expression.Flag.DOUBLE_COLON);
        assertEquals("result", expression.evaluate((c, b) -> {
            assertEquals("expr::baz", c.getKey());
            b.append("result");
        }));
    }

    @Test
    public void testPoint8() {
        final Expression expression = Expression.compile("${expr::baz}");
        assertEquals(":baz", expression.evaluate((c, b) -> {
            assertEquals("expr", c.getKey());
            c.expandDefault();
        }));
    }

    @Test
    public void testPoint9() {
        try {
            Expression.compile("${expr:foo");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint10() {
        final Expression expression = Expression.compile("${expr:foo", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo", expression.evaluate((c, b) -> {
            assertEquals("expr", c.getKey());
            c.expandDefault();
        }));
    }

    @Test
    public void testPoint11() {
        final Expression expression = Expression.compile("${expr:foo}bar");
        assertEquals("foobar", expression.evaluate((c, b) -> {
            assertEquals("expr", c.getKey());
            c.expandDefault();
        }));
    }

    @Test
    public void testPoint12() {
        final Expression expression = Expression.compile("${expr}bar");
        assertEquals("foobar", expression.evaluate((c, b) -> {
            assertEquals("expr", c.getKey());
            b.append("foo");
        }));
    }

    @Test
    public void testPoint13() {
        final Expression expression = Expression.compile("foo$$bar", Expression.Flag.MINI_EXPRS);
        assertEquals("foorizbar", expression.evaluate((c, b) -> {
            assertEquals("$", c.getKey());
            b.append("riz");
        }));
    }

    @Test
    public void testPoint14() {
        final Expression expression = Expression.compile("foo$$bar");
        assertEquals("foo$bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint15() {
        final Expression expression = Expression.compile("foo$}bar", Expression.Flag.MINI_EXPRS);
        assertEquals("foorizbar", expression.evaluate((c, b) -> {
            assertEquals("}", c.getKey());
            b.append("riz");
        }));
    }

    @Test
    public void testPoint16() {
        final Expression expression = Expression.compile("foo${bar$}baz", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foorizbaz", expression.evaluate((c, b) -> {
            assertEquals("bar$", c.getKey());
            b.append("riz");
        }));
    }

    @Test
    public void testPoint17() {
        try {
            Expression.compile("foo${bar$}baz");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint18() {
        final Expression expression = Expression.compile("foo$}bar", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo$}bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint19() {
        try {
            Expression.compile("foobar$}baz");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint20() {
        final Expression expression = Expression.compile("foo$:baz", Expression.Flag.MINI_EXPRS);
        assertEquals("foobarbaz", expression.evaluate((c, b) -> {
            assertEquals(":", c.getKey());
            b.append("bar");
        }));
    }

    @Test
    public void testPoint21() {
        final Expression expression = Expression.compile("${foo$:bar}", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("baz", expression.evaluate((c, b) -> {
            assertEquals("foo$", c.getKey());
            assertEquals("bar", c.getExpandedDefault());
            b.append("baz");
        }));
    }

    @Test
    public void testPoint22() {
        try {
            Expression.compile("${foo$:bar}");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint23() {
        final Expression expression = Expression.compile("foo$:bar", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo$:bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint24() {
        try {
            Expression.compile("foo$:bar");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint25() {
        final Expression expression = Expression.compile("foo$xbar", Expression.Flag.MINI_EXPRS);
        assertEquals("foobazbar", expression.evaluate((c, b) -> {
            assertEquals("x", c.getKey());
            b.append("baz");
        }));
    }

    @Test
    public void testPoint26() {
        final Expression expression = Expression.compile("foo$xbar", Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo$xbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint27() {
        try {
            Expression.compile("foo$xbar");
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint28() {
        final Expression expression = Expression.compile("${foo:bar}");
        assertEquals("bar", expression.evaluate((c, b) -> {
            assertEquals("foo", c.getKey());
            assertEquals("bar", c.getExpandedDefault());
            c.expandDefault();
        }));
    }

    @Test
    public void testPoint29() {
        final Expression expression = Expression.compile("foo:bar");
        assertEquals("foo:bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint1_2() {
        final Expression expression = Expression.compile("{${foo{bar}}}");
        assertEquals("{xxx}", expression.evaluate((c, b) -> {
            assertEquals("foo{bar}", c.getKey());
            b.append("xxx");
        }));
    }

    @Test
    public void testPoint1_3() {
        // also TP 30, 31
        final Expression expression = Expression.compile("{${foo{bar}}}", Expression.Flag.NO_SMART_BRACES);
        assertEquals("{xxx}}", expression.evaluate((c, b) -> {
            assertEquals("foo{bar", c.getKey());
            b.append("xxx");
        }));
    }

    // no tp 32

    @Test
    public void testPoint33() {
        final Expression expression = Expression.compile("foo\\", Expression.Flag.LENIENT_SYNTAX, Expression.Flag.ESCAPES);
        assertEquals("foo\\", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint34() {
        try {
            Expression.compile("foo\\", Expression.Flag.ESCAPES);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint35() {
        final Expression expression = Expression.compile("foo\\nbar", Expression.Flag.ESCAPES);
        assertEquals("foo\nbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint36() {
        final Expression expression = Expression.compile("foo\\rbar", Expression.Flag.ESCAPES);
        assertEquals("foo\rbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint37() {
        final Expression expression = Expression.compile("foo\\tbar", Expression.Flag.ESCAPES);
        assertEquals("foo\tbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint38() {
        final Expression expression = Expression.compile("foo\\bbar", Expression.Flag.ESCAPES);
        assertEquals("foo\bbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint39() {
        final Expression expression = Expression.compile("foo\\fbar", Expression.Flag.ESCAPES);
        assertEquals("foo\fbar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint40() {
        final Expression expression = Expression.compile("foo\\?bar", Expression.Flag.ESCAPES, Expression.Flag.LENIENT_SYNTAX);
        assertEquals("foo?bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint41() {
        try {
            Expression.compile("foo\\?bar", Expression.Flag.ESCAPES);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testPoint42() {
        final Expression expression = Expression.compile("foo\\?bar");
        assertEquals("foo\\?bar", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint43() {
        // also tp 44
        final Expression expression = Expression.compile("plain-content");
        assertEquals("plain-content", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint45() {
        final Expression expression = Expression.compile("W:\\\\workspace\\\\some-path\\\\xxxxyyyy", Expression.Flag.ESCAPES);
        assertEquals("W:\\workspace\\some-path\\xxxxyyyy", expression.evaluate((c, b) -> {
            fail("unexpected expansion");
        }));
    }

    @Test
    public void testPoint46() {
        // an empty default value is valid
        final Expression expression = Expression.compile("${foo.bar:}");
        assertEquals("", expression.evaluate((c, b) -> {
            assertTrue(c.hasDefault());
            assertEquals("", c.getExpandedDefault());
        }), "Should expand to empty string");
    }
}
