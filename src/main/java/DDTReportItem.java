import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Created by BeyMelamed on 2/13/14.
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
 * <p>
 *    Instances of this class are items the DDTReport uses in its 'default' reporting mode.
 *    These items can derive from either TestItem instance or Verb with a DDTestContext
 * <p/>
 * When      |Who            |What
 * ==========|===============|========================================================
 * 11/16/14  |Bey            |Initial Version
 * ==========|===============|========================================================
 */
public class DDTReportItem extends DDTBase {
   private String userReport;
   private List<TestEvent> testEvents = null;
   private String status;
   private Long sessionStepNumber = 0L;
   private DDTTestContext testContext = null;

   public DDTReportItem() {

   }

   /**
    * Set an instance of DDTReportItem from a TestItem instance - this replicates the original reporting methodology
    * @param testItem
    */
   public DDTReportItem(TestItem testItem) {
      setUserReport(testItem.getUserReport());
      setStatus(testItem.getStatus());
      testEvents = testItem.getEvents();
      addError(testItem.getErrors());
      addComment(testItem.getComments());
      setSessionStepNumber(testItem.getSessionStepNumber());
   }

   /**
    * Set an instance of DDTReportItem from a Verb instance
    * @param
    */
   public DDTReportItem(Verb verb) {
      DDTTestContext tmp = verb.getContext();
      tmp.setProperty("action", verb.myName());
      setTestContext(tmp);
      setStatus(verb.hasErrors() ? "FAIL" : "PASS");
      addError(verb.getErrors());
      addComment(verb.getComments());
      setUserReport(tmp.toString());
      setSessionStepNumber(verb.myName());
   }

   /**
    * Set an instance of DDTReportItem from status and description
    * This provides for a basic reporting without the 'heavy-weight' details incorporated into TestItem instance
    * @param testItem
    */
   public DDTReportItem(String status, String description) {
      setUserReport(description);
      setStatus(status);
      setSessionStepNumber("testing");
   }

   /**
    * Set an instance of DDTReportItem from status, description, comments and errors
    * This provides for a basic+ reporting without the 'heavy-weight' details incorporated into TestItem instance
    * @param testItem
    */
   public DDTReportItem(String status, String description, String comments, String errors) {
      setUserReport(description);
      setStatus(status);
      addComment(comments);
      addError(errors);
      setSessionStepNumber("testing");
   }

   public void setSessionStepNumber(Long value) {
      sessionStepNumber = value;
   }

   public Long getSessionStepNumber () {
      if (sessionStepNumber == null)
         setSessionStepNumber("testing"); // The 'testing' screen should 'consider' this item as a reportable step
      return sessionStepNumber;
   }

   /**
    * Sets this session's and instances next session and next session reported step number(s)
    * This should be called from a test context not based on TestItem instance
    */
   public void setSessionStepNumber(String action) {
      DDTTestRunner.setNextReportingStep(action);
      sessionStepNumber = DDTTestRunner.currentSessionStep();
   }

   public String paddedReportedStepNumber() {
      return String.format("%06d", getSessionStepNumber());
   }

   public void setStatus(String value) {
      status = value;
   }

   public String getStatus() {
      return status;
   }

   public void setUserReport(String value) {
      userReport = value;
   }

   public String getUserReport() {
      return userReport;
   }

   public List<TestEvent> getEvents() {
      if (testEvents == null)
         testEvents = new ArrayList<>();
      return testEvents;
   }

   public boolean hasEventsToReport() {
      return (getEvents().size() > 0);
   }

   public void setTestContext(DDTTestContext tc) {
      testContext = tc;
   }

   public String errorsAsHtml() {

      if (this.hasErrors()) {
         return getErrors().replace("\n", "<br>");
      }
      else
         return "";
   }

   public String toString() {
      StringBuilder sb = new StringBuilder("");
      if (testContext instanceof DDTTestContext) {
         sb.append(testContext.toString());
      }

      if (!isBlank(getComments())) {
         if (sb.length() > 0)
            sb.append(", ");
         sb.append("Comments: " + getComments());
      }

      if (!isBlank(getErrors())) {
         if (sb.length() > 0)
            sb.append(", ");
         sb.append("Errors: " + getErrors());
      }

      return sb.toString();
   }

}
