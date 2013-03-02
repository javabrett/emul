/*
  AllTests.java

  (c) 2011 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.tests.inst9900;

import v9t9.tests.inst9900.StatusTest9900;
import v9t9.tests.inst9900.TestBlocks9900;
import v9t9.tests.inst9900.TestTopDown1_9900;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author ejs
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for 9900");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestBlocks9900.class);
		suite.addTestSuite(StatusTest9900.class);
		suite.addTestSuite(TestTopDown1_9900.class);
		//$JUnit-END$
		return suite;
	}
}