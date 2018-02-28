/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.el.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.el.*;

/**
 *
 * @author Dongbin Nie
 */
public class OverloadedMethodTest {

    ELProcessor elp;
    ExpressionFactory exprFactory;
    ELContext elContext;

    @Before
    public void setUp() {
        elp = new ELProcessor();
        exprFactory = elp.getELManager().getExpressionFactory();
        elContext = elp.getELManager().getELContext();
        
        elp.defineBean("foo", new MyBean());
        
        elp.defineBean("i1", new I1Impl());
        elp.defineBean("i2", new I2Impl());
        elp.defineBean("i12", new I1AndI2Impl());
        elp.defineBean("i12s", new I1AndI2ImplSub());
        
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testMethodWithNoArg() {
        assertEquals("methodWithNoArg", elp.eval("foo.methodWithNoArg()"));
    }
    
    @Test
    public void testMethodNotExisted() {
        try {
            elp.eval("foo.methodNotExisted()");
            fail("testNoExistedMethod Failed");
        } catch (MethodNotFoundException e) {
        }
    }
    
    @Test
    public void testMethodWithSingleArg() {
        assertEquals("I1", elp.eval("foo.methodWithSingleArg(i1)"));
        assertEquals("I2Impl", elp.eval("foo.methodWithSingleArg(i2)"));
        assertEquals("I1AndI2Impl", elp.eval("foo.methodWithSingleArg(i12)"));
    }
    
    @Test
    public void testMethodWithDoubleArgs() {
        assertEquals("I1Impl, I2", elp.eval("foo.methodWithDoubleArgs(i1, i2)"));
        assertEquals("I1, I2", elp.eval("foo.methodWithDoubleArgs(i12, i2)"));
        assertEquals("I1AndI2Impl, I1AndI2Impl", elp.eval("foo.methodWithDoubleArgs(i12, i12)"));
        assertEquals("I1AndI2Impl, I1AndI2Impl", elp.eval("foo.methodWithDoubleArgs(i12s, i12)"));
        assertEquals("I1AndI2Impl, I1AndI2Impl", elp.eval("foo.methodWithDoubleArgs(i12s, i12s)"));
    }
    
    @Test
    public void testMethodWithAmbiguousArgs() {
        assertEquals("I1AndI2Impl, I2", elp.eval("foo.methodWithAmbiguousArgs(i12, i2)"));
        assertEquals("I1, I1AndI2Impl", elp.eval("foo.methodWithAmbiguousArgs(i1, i12)"));
        try {
            elp.eval("foo.methodWithAmbiguousArgs(i12, i12)");
            fail("testMethodWithAmbiguousArgs Failed");
        } catch (MethodNotFoundException e) {
        }
    }
    
    @Test
    public void testMethodWithCoercibleArgs() {
        assertEquals("String, String", elp.eval("foo.methodWithCoercibleArgs('foo', 'bar')"));
        assertEquals("String, String", elp.eval("foo.methodWithCoercibleArgs(i1, i12)"));
        
        assertEquals("String, String", elp.eval("foo.methodWithCoercibleArgs2(i1, 12345678)"));
        assertEquals("Integer, Integer", elp.eval("foo.methodWithCoercibleArgs2(12345678, 12345678)"));
    }
    
    @Test
    public void testMethodWithVarArgs() {
        assertEquals("I1, I1...", elp.eval("foo.methodWithVarArgs(i1)"));
        assertEquals("I1, I1...", elp.eval("foo.methodWithVarArgs(i1, i1)"));
        assertEquals("I1, I1...", elp.eval("foo.methodWithVarArgs(i12, i1, i12)"));
        
        assertEquals("I1, I1AndI2Impl...", elp.eval("foo.methodWithVarArgs2(i1)"));
        assertEquals("I1, I1AndI2Impl...", elp.eval("foo.methodWithVarArgs2(i12)"));
        assertEquals("I1, I1...", elp.eval("foo.methodWithVarArgs2(i1, i1)"));
        assertEquals("I1, I1AndI2Impl...", elp.eval("foo.methodWithVarArgs2(i1, i12)"));
    }
    
    @Test
    public void testExactVarArgs() {
        String[] args = {"foo", "bar", "hello"};
        elp.defineBean("args", args);
        assertEquals("foo,bar,hello,", elp.eval("foo.methodWithExactVarArgs('foo', 'bar', 'hello')"));
        assertEquals("foo,foo,bar,hello,", elp.eval("foo.methodWithExactVarArgs('foo', args)"));
    }
    
    @Test
    public void testMethodInStdout() {
        elp.defineBean("out", System.out);
        elp.eval("out.println('hello!')");
        elp.eval("out.println(12345678)");
    }
    
    /**
     * JSF may invoke MethodExpression which has parameter declared,
     * but pass in no arguments (not null). 
     */
    @Test
    public void testMethodExprInvokingWithoutArgs() {
        MethodExpression methodExpr = exprFactory.createMethodExpression(
                elContext, 
                "${foo.methodForMethodExpr}", 
                String.class, 
                new Class<?>[]{String.class});
        
        assertNull(methodExpr.invoke(elContext, new Object[0]));
    }
    
    @Test
    public void testMethodExprInvoking() {
        MethodExpression methodExpr = exprFactory.createMethodExpression(
                elContext, 
                "${foo.methodForMethodExpr2}", 
                String.class, 
                new Class<?>[]{Runnable.class});
        assertEquals("Runnable", methodExpr.invoke(elContext, new Object[]{Thread.currentThread()}));
        try {
            methodExpr.invoke(elContext, new Object[]{"foo"});
            fail("testMethodExprInvoking Failed");
        } catch (ELException e) {
            System.out.println("The following is an expected exception:");
            e.printStackTrace(System.out);
        }
    }
    
    
    public static interface I1 {
    
    }
    
    public static interface I2 {
    
    }
    
    public static class I1Impl implements I1 {
    
    }
    
    public static class I2Impl implements I2 {
    
    }
    
    public static class I1AndI2Impl implements I1, I2 {
    
    }
    
    public static class I1AndI2ImplSub extends I1AndI2Impl {
    
    }

    static public class MyBean {

        public String methodWithNoArg() {
            return "methodWithNoArg";
        }

        public String methodWithSingleArg(I1 i1) {
            return "I1";
        }

        public String methodWithSingleArg(I2 i2) {
            return "I2";
        }

        public String methodWithSingleArg(I2Impl i2) {
            return "I2Impl";
        }

        public String methodWithSingleArg(I1AndI2Impl i1) {
            return "I1AndI2Impl";
        }

        public String methodWithDoubleArgs(I1 i1, I2 i2) {
            return "I1, I2";
        }

        public String methodWithDoubleArgs(I1Impl i1, I2 i2) {
            return "I1Impl, I2";
        }

        public String methodWithDoubleArgs(I1AndI2Impl i1, I1AndI2Impl i2) {
            return "I1AndI2Impl, I1AndI2Impl";
        }

        public String methodWithAmbiguousArgs(I1AndI2Impl i1, I2 i2) {
            return "I1AndI2Impl, I2";
        }

        public String methodWithAmbiguousArgs(I1 i1, I1AndI2Impl i2) {
            return "I1, I1AndI2Impl";
        }

        public String methodWithCoercibleArgs(String s1, String s2) {
            return "String, String";
        }

        public String methodWithCoercibleArgs2(String s1, String s2) {
            return "String, String";
        }

        public String methodWithCoercibleArgs2(Integer s1, Integer s2) {
            return "Integer, Integer";
        }

        public String methodWithVarArgs(I1 i1, I1... i2) {
            return "I1, I1...";
        }

        public String methodWithVarArgs2(I1 i1, I1... i2) {
            return "I1, I1...";
        }

        public String methodWithVarArgs2(I1 i1, I1AndI2Impl... i2) {
            return "I1, I1AndI2Impl...";
        }
        
        public String methodWithExactVarArgs(String arg1, String... args) {
            StringBuilder sb = new StringBuilder();
            sb.append(arg1).append(",");
            for (String arg : args) {
                sb.append(arg).append(",");
            }
            return sb.toString();
        }
        
        public String methodForMethodExpr(String arg1) {
            return arg1;
        }
        
        public String methodForMethodExpr2(Runnable r) {
            return "Runnable";
        }
        
        public String methodForMethodExpr2(String s) {
            return "String";
        }

    }
    
}
