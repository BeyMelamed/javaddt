import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.io.File;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created with IntelliJ IDEA.
 * User: Avraham (Bey) Melamed
 * Date: 12/10/13
 * Time: 1:44 PM
 * Selenium Based Automation Project
 *
 * =============================================================================
 * Copyright 2014 Avraham (Bey) Melamed.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 *
 * Description
 * History
 * History
 * When        |Who      |What
 * ============|=========|====================================
 * 12/10/13    |Bey      |Initial Version
 * 02/27/14    |Bey      |Added numeric, currency and date verifications
 * 05/10/14    |Bey      |Added 'Between' comparison mode
 * 05/12/14    |Bey      |Added 'stripWhiteSpace' parameter (and data token)
 * 10/28/14    |Bey      |Inherit from DDTBase
 * 06/12/15    |Bey      |VerifyString -  introduce Not Equal comparison
 * 09/23/16    |Bey      |Fixed 'between' bug & avoid reversal of specs definition errors
 * 10/16/16    |Bey      |Adjust ddtSettings getters.
 * 01/23/17    |Bey      |Added File Verifier.
 * ============|=========|==================crypt==================
 */
public class Verifier extends DDTBase{

   private static final String EmptyComparisons = ",blank,isblank,empty,isempty,null,isnull,notblank,notempty,notnull,";
   private String[] values = new String[0];
   private String actualValue;
   private String expectedValue;
   private String compareMode;
   private String option;
   private String cls;
   private boolean stripWhiteSpace;
   private boolean andVerifier;
   private boolean orVerifier;
   private boolean shouldFail;
   private boolean isSpecError;

   public Verifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
	      setEv(ev);
	      setAv(av);
	      setComp(md);
	      setOpt(opt);
	      setCls(cls);
	      setStripWhiteSpace(stripWhiteSpace);
	      setupMultiVerification();
	   }

	   public Verifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace, boolean shouldFail) {
	      setEv(ev);
	      setAv(av);
	      setComp(md);
	      setOpt(opt);
	      setCls(cls);
	      setStripWhiteSpace(stripWhiteSpace);
	      setupMultiVerification();
	      setShouldFail(shouldFail);
	   }

   /**
    *   Prepare string array for multiple verifications of 'or' or 'and' and set the type of multiple operation if any
    *   For now, assume one type of multiple either 'and' or 'or'
    */
   private void setupMultiVerification() {
      if (getEv().indexOf(orDelim()) > -1) {
         setValues(getEv().split(orDelim()));
         setOrVerifier(true);
      }

      if (getEv().toLowerCase().indexOf(andDelim()) > -1) {
         setValues(getEv().toLowerCase().split(andDelim()));
         setAndVerifier(true);
      }
   }

   private boolean isBlankVerification() {
      return (EmptyComparisons.contains("," + getComp().toLowerCase()+","));
   }

   private void setAndVerifier(boolean value) {
      andVerifier = value;
   }

   private void setOrVerifier(boolean value) {
      orVerifier = value;
   }

   private String getStandardComment() {
      if (getComp().equalsIgnoreCase("between"))
         adjustEvForAndComparison();
      if (getComp().equalsIgnoreCase("in") || getComp().equalsIgnoreCase("or"))
         adjustEvForOrComparison();
      String opt = isBlank(getOpt()) ? "" : " (Option: " + Util.sq(getOpt()) + ")";
      return "Successful Verification - Actual: " + Util.sq(getAv()) + " " + Util.sq(getComp()) +  " " + Util.sq(getEv()) + opt;
   }

   private String getStandardError() {
      if (getComp().equalsIgnoreCase("between"))
         adjustEvForAndComparison();
      if (getComp().equalsIgnoreCase("in") || getComp().equalsIgnoreCase("or"))
         adjustEvForOrComparison();
      String opt = isBlank(getOpt()) ? "" : " (Option: " + Util.sq(getOpt()) + ")";
      return "Failed Verification - Actual: " + Util.sq(getAv()) + " NOT " +Util.sq(getComp()) +  " " + Util.sq(getEv()) + opt;
   }

   private void adjustEvForAndComparison() {
      //String andDelim = (String) DDTTestRunner.getVarsMap().get("$and");
      String s = getEv().toLowerCase().replace(andDelim(), DDTSettings.Settings().getAndToken());
      setEv(s);
   }

   private void adjustEvForOrComparison() {
      //String orDelim = (String) DDTTestRunner.getVarsMap().get("$or");
      String s = getEv().replace(orDelim(),  DDTSettings.Settings().getOrToken());
      setEv(s);
   }

   public void clear() {
      super.clear();
      actualValue = "";
      expectedValue = "";
      compareMode = "";
      option = "";
      cls = "";
      isSpecError = false;
      stripWhiteSpace = false;
      setShouldFail(false);
   }
   /**
    * Convenience method to launch verification
    * @param testItem TestItem instance from which the various parameters to a Verifier instance can be surmized.
    * @return    - Verifier instance with verification result - us isPass() to determine success or failure
    */
   public static Verifier getVerifier(TestItem testItem) {

      // User may want to get a specific class for the verification (date, integer, amount, etc.)

      String cls =  testItem.getDataProperties().getString("class");

      // User may have specified some comparison mode other than equal (the default)
      String md = testItem.getDataProperties().getString("comparemode");

      if (isBlank(md))
         md = DDTSettings.Settings().getDefaultComparison();

      // User may have specified some option for the output (lowecase, ignorecase, etc.)
      String opt =  testItem.getDataProperties().getString("option");

      // User typically specifies some expected value
      String ev = testItem.getDataProperties().getString("value");

      // User can indicate whether or not to trim white spaces off of actual value and expected values prior to comparison
      // If the user did not specify anything (blank) then use the settings, else use the specified value
      String strip = testItem.getDataProperties().getString("stripWhiteSpace");
      if (isBlank(strip))
         strip = DDTSettings.Settings().getStripWhiteSpace() ? "true" : "false";
      boolean stripWhiteSpace = Util.asBoolean(strip);

      if(testItem.shouldFail())
         return new Verifier(ev, "", md, opt, cls, stripWhiteSpace, true);

      // The Actual Value (second param) will be set by caller
      return new Verifier(ev, "", md, opt, cls, stripWhiteSpace);
   }

   /**
    * Convenience method to launch verification
    * @param testContext DDTTestContext instance from which the various parameters to a Verifier instance can be surmized.
    * @return    - Verifier instance with verification result - us isPass() to determine success or failure
    */
   public static Verifier getVerifier(DDTTestContext testContext) {

      // User may want to get a specific class for the verification (date, integer, amount, etc.)

      String cls =  testContext.getString("class");

      // User may have specified some comparison mode other than equal (the default)
      String md = testContext.getString("comparemode");

      if (isBlank(md))
         md = DDTSettings.Settings().getDefaultComparison();

      // User may have specified some option for the output (lowecase, ignorecase, etc.)
      String opt =  testContext.getString("option");

      // User typically specifies some expected value
      String ev = testContext.getString("value");

      // User can indicate whether or not to trim white spaces off of actual value and expected values prior to comparison
      // If the user did not specify anything (blank) then use the settings, else use the specified value
      String strip = testContext.getString("stripWhiteSpace");
      if (isBlank(strip))
         strip = DDTSettings.Settings().getStripWhiteSpace() ? "true" : "false";
      boolean stripWhiteSpace = Util.asBoolean(strip);

      boolean shouldFail = testContext.getBoolean("shouldfail");

      // The Actual Value (second param) will be set by caller
      return new Verifier(ev, "", md, opt, cls, stripWhiteSpace, shouldFail);
   }

   public String toString() {
      String result = "Exp: " + Util.sq(getEv()) + ", Act: " + Util.sq(getAv()) + ", Cmp: " + Util.sq(getComp());
      if (isNotBlank(getCls()))
         result += ", Cls: " + Util.sq(getCls());
      if (isNotBlank(getOpt()))
         result += ", Option: " + Util.sq(getOpt());

      return result;
   }

   public void setEv(String value) {
      this.expectedValue =  value;
   }

   public String getEv() {
      if (this.expectedValue == null)
         setEv("");
      return this.expectedValue;
   }

   public void setAv(String value) {
      this.actualValue =  value;
   }

   public String getAv() {
      if (this.actualValue == null)
         setAv("");
      return this.actualValue;
   }

   public void setComp(String value) {
      this.compareMode =  value;
   }

   public String getComp() {
      if (isBlank(this.compareMode))
         setComp(DDTSettings.Settings().getDefaultComparison());
      return this.compareMode;
   }

   public void setOpt(String value) {
      this.option =  value;
   }

   public String getOpt() {
      if (this.option == null)
         setOpt("");
      return this.option;
   }

   public void setCls(String value) {
      this.cls =  value;
   }

   public String getCls() {
      if (this.cls == null)
         setCls("");
      return this.cls;
   }

   public void setStripWhiteSpace(boolean value) {
      stripWhiteSpace = value;
   }

   public boolean getStripWhiteSpace() {
      return stripWhiteSpace;
   }

   public static String andDelim () {
      return DDTSettings.Settings().getAndDelim();
   }

   public static String orDelim () {
      return DDTSettings.Settings().getOrDelim();
   }

   public String[] getValues() {
      return values;
   }

   public void setValues(String[] value) {
      values = value;
   }

   public boolean isPass() {
      return isValid();
   }

   public void verifyBlank() {
      switch (getComp().toLowerCase()) {
         case "isblank" :case "isnull" :case "isempty":case "blank":case "null":case "empty":
         {
            String prefix =    "";
            if (!getComp().toLowerCase().startsWith("is"))
               prefix = "Is";
            if (isBlank(getAv()))
               addComment("Successful Verification - Actual " + prefix + " " + Util.sq(getComp()));
            else
               addError("Failed Verification - Actual (" + Util.sq(getAv() + ") is NOT (but is expected to be) BLANK / EMPTY / NULL"));

            break;
         }
         case "notblank" :case "notnull" :case "notempty":
         {
            if (isBlank(getAv()))
               addError("Actual is BLANK but is expected to be NOT BLANK / EMPTY / NULL");
            else
               addComment("Actual (" +Util.sq(getAv()) + ") is not blank / empty / null.");

            break;
         }
         default: {}
      }
      adjustForExpectedFailure();
   }// verifyBlank

   private void adjustForExpectedFailure() {
      // Adjust only if should fail and it is not a specification error of some sort
      if(!shouldFail || isSpecError)
         return;

      String suffix = " - But Failure Expected!";

      if(getErrors().isEmpty()) {
         // Reverse PASS
         // System.out.println("** Reversing Pass **");
         String blurb = (getComments().isEmpty() ? "Verification Passed" : getComments() + suffix);
         clearErrors();
         addError(blurb);
         clearComments();
      }
      else {
         // Reverse FAIL
         // System.out.println("** Reversing Fail ** = Errors: '" + getErrors() + "'");
         String blurb = getErrors() + suffix;
         clearComments();
         addComment(blurb);
         clearErrors();
      }
   }

   public void setShouldFail(boolean value) {
      shouldFail = value;
   }

   boolean getShouldFail() {
      return shouldFail;
   }

   /**
    * Verifies whether expectation is met based on the instance's values.
    * The values to compare come in as strings.
    * getCls() indicates whether or not the class of the objects to compare is not the (default) String.
    * @throws Exception
    */
   public void verify() throws Exception {
      if (getCls().equalsIgnoreCase("string"))
         setCls("");  // String is the default class we deal with

      // If appropriate, perform the blank verification (the Actual value is (or is not) blank / null / empty)
      // This is done before having to convert the (possibly empty) actual value to some object...
      if (isBlankVerification()) {
         verifyBlank();
         return;
      }

      try {

         switch (getCls().toLowerCase()) {
            case "" : verifyStrings() ; break;
            case "int" :case "integer" :
            {
               IntegerVerifier verifier = new IntegerVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
               verifier.setShouldFail(getShouldFail());
               if (isBlank(verifier.getErrors()))
                  verifier.verify();
               addComment(verifier.getComments());
               addError(verifier.getErrors());
               break;
            }

            case "long" :
            {
               LongVerifier verifier = new LongVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
               if (isBlank(verifier.getErrors()))
                  verifier.verify();
               addComment(verifier.getComments());
               addError(verifier.getErrors());
               break;
            }

            case "double" : case "float" :case "decimal" :
            {
               DecimalVerifier verifier = new DecimalVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
               if (isBlank(verifier.getErrors()))
                  verifier.verify();
               addComment(verifier.getComments());
               addError(verifier.getErrors());
               break;
            }

            case "currency" :
            {
               AmountVerifier verifier = new AmountVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
               if (isBlank(verifier.getErrors()))
                  verifier.verify();
               addComment(verifier.getComments());
               addError(verifier.getErrors());
               break;
            }

            case "date" :
            {
               DateVerifier verifier = new DateVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
               if (isBlank(verifier.getErrors()))
                  verifier.verify();
               addComment(verifier.getComments());
               addError(verifier.getErrors());
               break;
            }
            case "file" :
            {
                 FileVerifier verifier = new FileVerifier(getEv(), getAv(), getComp(), getOpt(), getCls(), getStripWhiteSpace());
                 if (isBlank(verifier.getErrors()))
                     verifier.verify();
                 addComment(verifier.getComments());
                 addError(verifier.getErrors());
                 break;
            }


            default: addError("Invalid numeric object class specified: " +Util.sq(getCls()) + ", options are: " + NumberVerifier.ValidFormats);
         } // Switch

         adjustForExpectedFailure();

      } //Try
      catch (Exception e) {
         addError("Verifier generated general exception " + e.getCause().toString());
      }
   } // verify

   /**
    * Verifies whether expectation is met based on the instance's values.
    * The values to compare are strings.
    * @throws Exception
    */
   public void verifyStrings() throws Exception {
      String compareMode = getComp().toLowerCase();
      String expected = getEv();
      String actual = getAv();

      // Special case - date-related components - these are strings but are derived from date values & manipulations
      if (expected.toLowerCase().startsWith("%date")) {
         // Convert the input to date components per the specs.
         // Expected value is string representation of date or its component(s)
         try {
            DDTDate dateParser = new DDTDate(getEv());
            if (dateParser.hasException()) {
               addError(dateParser.getException().getMessage().toString());
               return;
            }
            setEv(dateParser.getOutput());
            expected = getEv();
         }
         catch (Exception e) {
            addError("Exception in Date Parser: " + e.getCause().toString());
            return;
         }
      }

      // Null protection
      if (isBlank(actual))
         actual = "";

      if (isBlank(expected))
         expected = "";

      if (getStripWhiteSpace()) {
         actual = Util.stripWhiteSpace(actual);
         expected = Util.stripWhiteSpace(expected);
      }

      // If comparison method not specified, get it from the settings.
      if (isBlank(compareMode))
         compareMode = DDTSettings.Settings().getDefaultComparison().toLowerCase();

      // Case sensitivity considerations
      if (getOpt().toLowerCase().equals("ignorecase")
            && compareMode != "islowercase"
            && compareMode != "isuppercase"
            && (isBlank(getCls()) || getCls().equalsIgnoreCase("string")))
      {
         // convert both actual and expected values to lowercase
         expected=expected.toLowerCase();
         actual=actual.toLowerCase();
      }

      try {

         switch (compareMode.toLowerCase()) {
            // all booleans will be evaluated against the strings true/false
            case "equals" : case "equal" : case "is" : case "eq": case "=" : case "isenabled" :case "isdisplayed" :case "isselected" : // NOTE: booleans are represented as string!
            {
               if (actual.equals(expected))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());
               break;
            }
            case "ne" :case "notequalsto" : case "notequalto" : case "notequal" :case "notequals" :case "!=" :case "<>":
            {
               if (!actual.equals(expected))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());

               break;
            }
            case "startswith" :case "startwith" :
            {
               if (actual.startsWith(expected))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());

               break;
            }
            case "endswith" :case "endwith" :
            {
               if (actual.endsWith(expected))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());
               break;
            }
            case "contains" : case "contain" :
            {
               if ((actual.contains(expected)))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());
               break;
            }
            case "notcontains" :case "notcontain" :
            {
               if (actual.contains(expected))
                  addError(getStandardError());
               else
                  addComment(getStandardComment());
               break;
            }
            case "islowercase" :
            {
               if (actual.toLowerCase().equals(actual))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());

               break;
            }
            case "isuppercase" :
            {
               if (actual.toUpperCase().equals(actual))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());

               break;
            }
            case "matches" :case "match" :
            {
               //@TODO case "matches" : the code below does not work when expression is evaluated in debugger - it does - WHY?
               //actual = ".*" + actual + ".*";
               //if ((actual.matches(expected)))
               if (actual.matches(expected))
                  addComment(getStandardComment());
               else
                  addError(getStandardError());

               break;
            }
            case "between": {
               String fromValue = null;
               String toValue = null;
               String blurb = "";

               if (getValues().length == 2) {
                  fromValue = values[0];
                  toValue = values[1];
                  if (fromValue.hashCode() > actual.hashCode() || toValue.hashCode() < actual.hashCode()) {
                     addError(getStandardError());
                  }
                  else {
                     addComment(getStandardComment());
                  }
               } else {
                  isSpecError = true;
                  addError("Invalid 'Between' String specifications.");
               }
               break;
            }
            default: {
                  isSpecError = true;
                  addError("Invalid comparison mode specified: " + Util.sq(getComp()));
            }
         } // Switch

      } // Try
      catch (Exception e) {
         isSpecError = true;
         addError("Verifier generated general exception " + e.getCause().toString());
      }
   } //VerifyStrings - the default verification logic

   public class NumberVerifier extends Verifier {
      int comparisonResult=0;

      public NumberVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
          super(ev, av, md, opt, cls, stripWhiteSpace);
          removeGroupingSeparator();
       }

      public int getComparisonResult() {
         return comparisonResult;
      }

      public void setComparisonResult(int value) {
         comparisonResult = value;
      }

      private static final String ValidFormats =   "'int', 'long', 'double', 'float', 'decimal', 'currency', 'date'";
      private void removeGroupingSeparator() {
         DecimalFormat format = new java.text.DecimalFormat();
         DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
         char sep=symbols.getGroupingSeparator();
         setEv(getEv().replace(String.valueOf(sep), "").replaceAll("\\s", ""));
         setAv(getAv().replace(String.valueOf(sep), "").replaceAll("\\s", ""));
         if (getComp().equalsIgnoreCase("between")) {
            values[0] = values[0].replace(String.valueOf(sep), "").replaceAll("\\s", "");
            values[1] = values[1].replace(String.valueOf(sep), "").replaceAll("\\s", "");
         }
      }

      /**
       * Verifies whether expectation is met based on the (numeric) instance's values.
       * The values to compare are numbers of some sort and have been compared to one another before to produce the comparisonResult
       *  1: Actual > expected
       * -1: Expected > Actual
       *  0: Expected = Actual
       * @throws Exception
       */

      public void verify(Number actual) throws Exception {
         String compareMode = getComp().toLowerCase();

         // If comparison method not specified, get it from the settings.
         if (StringUtils.isBlank(compareMode))
            compareMode = DDTSettings.Settings().getDefaultComparison().toLowerCase();

         try {

            switch (compareMode) {
               case "equals" :case "equal" : case "is" :case "=" : case "eq" : case "==" :
               {
                  if (getComparisonResult() == 0)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());
                  break;
               }
               case "gt" : case "greaterthan" : case ">" :
               {
                  if (getComparisonResult() == 1)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "ge" : case "greaterthanorequalsto" : case "greaterthanorequalto" :case ">=": case "=>" :
               {
                  if (getComparisonResult() > -1)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "lt" :case "lessthan" :case "<":
               {
                  if (getComparisonResult() == -1)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "le" :case "lessthanorequalsto" : case "lessthanorequalto" :case "<=" : case "=<" :
               {
                  if (getComparisonResult() < 0)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());
                  break;
               }
               case "ne" :case "notequalsto" : case "notequalto" :case "!=" :
               {
                  if (getComparisonResult() != 0)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());
                  break;
               }
               case "between": {
                  BigDecimal fromValue = null;
                  BigDecimal toValue = null;
                  String blurb = "";

                  if (getValues().length == 2) {
                     try {
                        fromValue = BigDecimal.valueOf(Double.valueOf(values[0]));
                     } catch (Exception ex) {
                        blurb = "Invalid (expected) value: " + Util.sq(values[0]) + "  ";
                     }

                     try {
                        toValue = BigDecimal.valueOf(Double.valueOf(values[1]));
                     }
                     catch (Exception ex) {
                        blurb += "Invalid (actual) value: " + Util.sq(values[1]) + "  ";
                     }
                     // Here we are
                     if (isBlank(blurb)) {
                        //if (fromValue.hashCode() > actual.hashCode() || toValue.hashCode() < actual.hashCode())
                        if (fromValue.doubleValue() > actual.doubleValue() || toValue.doubleValue() < actual.doubleValue())
                           addError(getStandardError());
                        else
                           addComment(getStandardComment());
                     }
                     else {
                        isSpecError = true;
                        addError("Invalid numeric specifications - " + blurb);
                     }
                  }
                  else {
                     isSpecError = true;
                     addError("Invalid 'Between' specifications.");
                  }
                  break;

               }
               default: {
                  isSpecError = true;
                  addError("Invalid comparison mode specified: "  +Util.sq(getComp()));
               }
            } // Switch
         } // Try
         catch (Exception e) {
            isSpecError = true;
            addError("Verifier generated general exception " + e.getCause().toString());
         }
      } // Verify
   } // NumberVerifier

   public class IntegerVerifier extends NumberVerifier {

      private IntegerVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, stripWhiteSpace);
         setValuesFromStrings();
      }

      private int expected;
      private int actual;

      /**
       * Set the expected and actual values from their string representations
       */
      private void setValuesFromStrings() {
         String blurb = "";

         if (!getComp().equalsIgnoreCase("between")) {
            try {
               expected = Integer.valueOf(getEv());
            } catch (Exception ex) {
               blurb = "Invalid (expected) Integer value: " + Util.sq(getEv()) + "  ";
            }
         }

         try {
            actual = Integer.valueOf(getAv());
         }
         catch (Exception ex) {
            blurb += "Invalid (actual) Integer value: " +Util.sq(getAv()) + "  ";
         }

         if (!isBlank(blurb)) {
            isSpecError = true;
            addError(blurb + "Verification aborted.");
         }
      }

      public void verify() throws Exception {
         if (actual == expected)
            setComparisonResult(0);
         if (actual > expected)
            setComparisonResult(1);
         if (actual < expected)
            setComparisonResult(-1);

         if (isBlank(getErrors()))
            verify(BigDecimal.valueOf(actual * 1.00));
      }
   }

   public class LongVerifier extends NumberVerifier {

      private LongVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, stripWhiteSpace);
         setValuesFromStrings();
      }

      private Long expected;
      private Long actual;

      /**
       * Set the expected and actual values from their string representations
       */
      private void setValuesFromStrings() {
         String blurb = "";

         if (!getComp().equalsIgnoreCase("between")) {
            try {
               expected = Long.valueOf(getEv());
            } catch (Exception ex) {
               blurb = "Invalid (expected) Long value: " + Util.sq(getEv()) + "  ";
            }
         }

         try {
            actual = Long.valueOf(getAv());
         } catch (Exception ex) {
            blurb += "Invalid (actual) Long value: " + Util.sq(getAv()) + "  ";
         }

         if (!isBlank(blurb)) {
            isSpecError = true;
            addError(blurb + "Verification aborted.");
         }
      }

      public void verify() throws Exception {
         if (actual != null && expected != null)
            setComparisonResult(actual.compareTo(expected));
         if (isBlank(getErrors()))
            verify(BigDecimal.valueOf((Long) actual * 1.00));
      }
   }

   public class DecimalVerifier extends NumberVerifier {

      private DecimalVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, stripWhiteSpace);
         setValuesFromStrings();
      }

      private BigDecimal expected;
      private BigDecimal actual;

      /**
       * Set the expected and actual values from their string representations
       */
      private void setValuesFromStrings() {
         String blurb = "";

         if (!getComp().equalsIgnoreCase("between")) {
            try {
               expected = BigDecimal.valueOf(Double.valueOf(getEv()));
            } catch (Exception ex) {
               blurb = "Invalid (expected) Decimal value: " + Util.sq(getEv()) + "  ";
            }
         }

         try {
            actual = BigDecimal.valueOf(Double.valueOf(getAv()));
         }
         catch (Exception ex) {
            blurb += "Invalid (actual) Decimal value: " +Util.sq(getAv()) + "  ";
         }

         if (!isBlank(blurb)) {
            isSpecError = true;
            addError(blurb + "Verification aborted.");
         }
      }  // setValuesFromStrings

      public void verify() throws Exception {
         if (expected != null && actual != null)
            setComparisonResult(actual.compareTo(expected));
         verify(actual);
      }

   }  // Decimal Verifier

   public class AmountVerifier extends NumberVerifier {

      private AmountVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, stripWhiteSpace);
         setValuesFromStrings();
      }

      private BigDecimal expected;
      private BigDecimal actual;

      /**
       * Set the expected and actual values from their string representations
       */
      private void setValuesFromStrings() {
         String blurb = "";
         String origEv = getEv();
         String origAv = getAv();
         Currency curr = Currency.getInstance(Locale.getDefault());
         String currSymbol = curr.getSymbol();
         setAv(getAv().replace(currSymbol, ""));
         setEv(getEv().replace(currSymbol,""));

         if (!getComp().equalsIgnoreCase("between")) {
            try {
               expected = BigDecimal.valueOf(Double.valueOf(getEv()));
            } catch (Exception ex) {
               blurb = "Invalid (expected) Currency value: " + Util.sq(origEv) + "  ";
            }
         }

         try {
            actual = BigDecimal.valueOf(Double.valueOf(getAv()));
         }
         catch (Exception ex) {
            blurb += "Invalid (actual) Currency value: " +Util.sq(origAv) + "  ";
         }

         if (!isBlank(blurb)) {
            isSpecError = true;
            addError(blurb + "Verification aborted.");
         }
      } // setValuesFromStrings

      public void verify() throws Exception {
         if (actual != null && expected != null)
            setComparisonResult(actual.compareTo(expected));

         verify(actual);
      }
   }  // NumberVerifier

   private class DateVerifier extends Verifier {

      private DateVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, stripWhiteSpace);
         setValuesFromStrings();
      }

      private Date expected;
      private Date actual;

      /**
       * Set the expected and actual values from their string representations
       */
      private void setValuesFromStrings() {
         try {
            String dateFormat = getOpt();
            if (isBlank(dateFormat))
               dateFormat = DDTSettings.Settings().getDateFormat();
            expected = new SimpleDateFormat(dateFormat).parse(getEv());
            actual = new SimpleDateFormat(dateFormat).parse(getAv());
         }
         catch (ParseException e ) {
            isSpecError = true;
            addError(e.getCause().toString());
         }
      }// setValuesFromStrings

      /**
       * Verifies whether expectation is met based on the (date) instance's values.
       * The date(s) is/are first parsed then compared using the BigDecimal getTime() value as though this is a Number Verifier;
       * The main difference between the verifications of the Date Verifier and Number Verifier is the date-like names of some verifications.
       * @throws Exception
       */

      public void verify() throws Exception {
         String compareMode = getComp().toLowerCase();

         // If comparison method not specified, default it
         if (isBlank(compareMode))
            compareMode = "equals";

         try {

            Long expectedTime = null;
            if (!getComp().equalsIgnoreCase("between"))
               expectedTime = ((Date) expected).getTime();

            Long actualTime = ((Date) actual).getTime();

            switch (compareMode) {
               // all booleans will be evaluated against the strings true/false
               case "equals" :case "is" :case "equal":case "=" : case "eq" :
               {
                  if (actualTime.equals(expectedTime))
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());
                  break;
               }
               case "gt" : case "greaterthan" : case ">" :case "after" :
               {
                  if (actualTime > expectedTime)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());
                  break;
               }
               case "ge" : case "greaterthanorequalsto" : case "greaterthanorequalto" :case ">=":case "=>" : case "onorafter":
               {
                  if (actualTime >= expectedTime)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "lt" :case "lessthan" :case "<":case "before":
               {
                  if (actualTime < expectedTime)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "le" :case "lessthanorequalsto" : case "lessthanorequalto" :case "<=" : case "onorbefore":
               {
                  if (actualTime <= expectedTime)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "ne" :case "notequalsto" : case "notequalto" : case "noton" :case "!=" :
               {
                  if (actualTime != expectedTime)
                     addComment(getStandardComment());
                  else
                     addError(getStandardError());

                  break;
               }
               case "between" :
               {
                  String dateFormat = getOpt();
                  if (isBlank(dateFormat))
                     dateFormat = DDTSettings.Settings().getDateFormat();

                  if (getValues().length == 2) {
                     // Use long because getTime returns a Long (milliseconds)
                     Long startTime = ((Date) new SimpleDateFormat(dateFormat).parse(getValues()[0])).getTime();
                     Long endTime = ((Date) new SimpleDateFormat(dateFormat).parse(getValues()[1])).getTime();
                     if (startTime > actualTime || endTime < actualTime)
                        addError(getStandardError());
                     else
                        addComment(getStandardComment());
                  }
                  else {
                     isSpecError = true;
                     addError("Invalid 'Between' dates specifications.");
                  }
                  break;
               }
               default: {
                  isSpecError = true;
                  addError("Invalid comparison mode specified: "+Util.sq(getComp()));
               }
            } // Switch
         } // Try
         catch (Exception e) {
            isSpecError = true;
            addError("Verifier generated general exception " + e.getCause().toString());
         }
      } // verify()
   }// Date Verifier class

   /**
    * Verifies the existence or absence of file (ActualValue) when the class (cls) of the instance is "file" (case insensitive)
    * An instance is created when the cls (class) is "file"
    * When md (CompareMode) is "exist" the file should exist
    * When md (CompareMode) is "absent" the file should not exist
    * When md (CompareMode) is blank - the verification passes (once the format of "value" passes)
    * @throws Exception
    */

   public class FileVerifier extends Verifier {
      private File theFile;
      private Boolean shouldExist = false;
      private Boolean shouldVerifyExistence = false;

      private FileVerifier(String ev, String av, String md, String opt, String cls, boolean stripWhiteSpace) {
         super(ev, av, md, opt, cls, false); // We NEVER want to strip White Space
         setValuesFromStrings();
      }

      private void setValuesFromStrings() {
         String blurb = "";

         if (!getComp().isEmpty())
            shouldVerifyExistence = true;

         if (getComp().equalsIgnoreCase("exist") && shouldVerifyExistence)
            shouldExist = true;

         if (getAv().isEmpty())
            blurb += "Invalid File Specification (cannot be blank) ";

         if (!isBlank(blurb)) {
            isSpecError = true;
            addError(blurb + "File Verification aborted. ");
         }
      }

      public void verify() throws Exception {
          Path tmpPath;
          // Determine whether the path exists on the system
          try {
              Path fp = Paths.get(getAv());
              tmpPath = fp.toAbsolutePath();
          } catch (Throwable ex) {
              addError("Invalid File Path Specification: " + Util.sq(getAv()) + " --- " + ex.getMessage());
              return;
          }

          // If no verification of existence is requested, return with an appropriate comment
          if (!shouldVerifyExistence) {
             addComment("File or Path Specified!");
             return;
         }

         // Verify the existence of the file with respect to the expectation (absent, exist)
         try {
            File theFile = new File(DDTSettings.asValidOSPath(getAv(), true));
            Assert.assertTrue(theFile.exists() && theFile.canRead());
            addComment("Verified: " + theFile.toPath().toString() + " size: " + theFile.length());
            if (!shouldExist)
                addError("File " + getAv() + " Should not exist but it DOES exist!");
         }
         catch (Throwable ex) {
             if (shouldExist)
                 addError("File " + getAv() + " Should exist but it DOES NOT exist (or unreadable)!");
         }

         if (getErrors().isEmpty()) {
             if (shouldExist)
                 addComment("File " + getAv() + " Exists!");
             else
                 addComment("File " + getAv() + " Does Not Exist!");
         }
      }
   }
}
